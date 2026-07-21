package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.entity.publics.EducationStage;
import com.gcoedu.core.domain.entity.tenant.*;
import com.gcoedu.core.repository.publics.EducationStageRepository;
import com.gcoedu.core.repository.tenant.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class EvaluationResultService {

    private final EvaluationResultRepository evaluationResultRepository;
    private final TestRepository testRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final StudentRepository studentRepository;
    private final EducationStageRepository educationStageRepository;
    private final EvaluationCalculator evaluationCalculator;

    public EvaluationResultService(EvaluationResultRepository evaluationResultRepository,
                                   TestRepository testRepository,
                                   TestQuestionRepository testQuestionRepository,
                                   StudentAnswerRepository studentAnswerRepository,
                                   StudentRepository studentRepository,
                                   EducationStageRepository educationStageRepository,
                                   EvaluationCalculator evaluationCalculator) {
        this.evaluationResultRepository = evaluationResultRepository;
        this.testRepository = testRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.studentRepository = studentRepository;
        this.educationStageRepository = educationStageRepository;
        this.evaluationCalculator = evaluationCalculator;
    }

    public boolean checkMultipleChoiceAnswer(String studentAnswer, String correctAnswer) {
        if (studentAnswer == null || correctAnswer == null) {
            return false;
        }
        return studentAnswer.trim().equalsIgnoreCase(correctAnswer.trim());
    }

    private double roundToTwoDecimals(double value) {
        return Math.round(value * 100.0) / 100.0;
    }

    private List<Map<String, Object>> calculateSubjectSpecificResults(
            String testId, String studentId, List<Question> questions,
            List<StudentAnswer> answers, String courseName) {

        // Group questions by subject
        Map<String, List<Question>> questionsBySubject = new HashMap<>();
        for (Question q : questions) {
            if (q.getSubject() != null) {
                String subjectId = q.getSubject().getId().toString();
                questionsBySubject.computeIfAbsent(subjectId, k -> new ArrayList<>()).add(q);
            }
        }

        List<Map<String, Object>> subjectResults = new ArrayList<>();

        for (Map.Entry<String, List<Question>> entry : questionsBySubject.entrySet()) {
            String subjectId = entry.getKey();
            List<Question> subjectQuestions = entry.getValue();
            if (subjectQuestions.isEmpty()) {
                continue;
            }

            String subjectName = subjectQuestions.get(0).getSubject().getName();
            int totalQuestionsSubject = subjectQuestions.size();

            // Filter student answers for this subject's questions
            Set<String> subjectQuestionIds = subjectQuestions.stream().map(Question::getId).collect(Collectors.toSet());
            List<StudentAnswer> subjectAnswers = answers.stream()
                    .filter(a -> subjectQuestionIds.contains(a.getQuestion().getId()))
                    .collect(Collectors.toList());

            int answeredQuestions = subjectAnswers.size();

            // Calculate correct answers count for this subject
            int correctAnswersSubject = 0;
            for (StudentAnswer answer : subjectAnswers) {
                Question q = answer.getQuestion();
                if (q != null) {
                    if (checkMultipleChoiceAnswer(answer.getAnswer(), q.getCorrectOption())) {
                        correctAnswersSubject++;
                    }
                }
            }

            // Calculate evaluation metrics
            Map<String, Object> eval = evaluationCalculator.calculateCompleteEvaluation(
                    correctAnswersSubject, totalQuestionsSubject, courseName, subjectName, false
            );

            double scorePercentage = totalQuestionsSubject > 0 ?
                    ((double) correctAnswersSubject / totalQuestionsSubject) * 100.0 : 0.0;

            Map<String, Object> sr = new LinkedHashMap<>();
            sr.put("subject_id", subjectId);
            sr.put("subject_name", subjectName);
            sr.put("correct_answers", correctAnswersSubject);
            sr.put("total_questions", totalQuestionsSubject);
            sr.put("answered_questions", answeredQuestions);
            sr.put("score_percentage", roundToTwoDecimals(scorePercentage));
            sr.put("grade", eval.get("grade"));
            sr.put("proficiency", eval.get("proficiency"));
            sr.put("classification", eval.get("classification"));

            subjectResults.add(sr);
        }

        return subjectResults;
    }

    public Map<String, Object> calculateAndSaveResult(String testId, String studentId, String sessionId) {
        log.info("Iniciando cálculo de resultado para aluno={} no teste={}", studentId, testId);

        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Teste não encontrado: " + testId));

        Student student = studentRepository.findById(studentId)
                .orElseThrow(() -> new RuntimeException("Aluno não encontrado: " + studentId));

        // Get test questions ordered by index
        List<TestQuestion> testQuestions = testQuestionRepository.findByTestIdOrderByOrderIndex(testId);
        if (testQuestions.isEmpty()) {
            log.warn("Nenhuma questão associada ao teste {}", testId);
            return null;
        }

        List<Question> questions = testQuestions.stream()
                .map(TestQuestion::getQuestion)
                .collect(Collectors.toList());

        int totalQuestions = questions.size();

        // Get student answers and deduplicate keeping the most recent one per question
        List<StudentAnswer> rawAnswers = studentAnswerRepository.findByTestIdAndStudentId(testId, studentId);
        Map<String, StudentAnswer> answersByQuestion = new HashMap<>();
        for (StudentAnswer a : rawAnswers) {
            String qid = a.getQuestion().getId();
            StudentAnswer prev = answersByQuestion.get(qid);
            if (prev == null) {
                answersByQuestion.put(qid, a);
            } else if (a.getAnsweredAt() != null && prev.getAnsweredAt() != null && a.getAnsweredAt().isAfter(prev.getAnsweredAt())) {
                answersByQuestion.put(qid, a);
            }
        }
        List<StudentAnswer> answers = new ArrayList<>(answersByQuestion.values());

        // Count correct answers
        int correctAnswers = 0;
        Set<String> testQuestionIds = questions.stream().map(Question::getId).collect(Collectors.toSet());
        for (StudentAnswer answer : answers) {
            Question q = answer.getQuestion();
            if (q != null && testQuestionIds.contains(q.getId())) {
                if (checkMultipleChoiceAnswer(answer.getAnswer(), q.getCorrectOption())) {
                    correctAnswers++;
                }
            }
        }

        // Determine course name
        String courseName = "Anos Iniciais";
        if (test.getCourse() != null) {
            try {
                UUID stageId = UUID.fromString(test.getCourse());
                Optional<EducationStage> stage = educationStageRepository.findById(stageId);
                if (stage.isPresent()) {
                    courseName = stage.get().getName();
                }
            } catch (IllegalArgumentException e) {
                // Fallback to name if not UUID
                courseName = test.getCourse();
            }
        }

        // Check if multiple subjects exist
        boolean useSubjectsInfo = false;
        String mainSubjectName = test.getSubjectRel() != null ? test.getSubjectRel().getName() : "Outras";

        long questionsWithSubjectCount = questions.stream().filter(q -> q.getSubject() != null).count();
        if (questionsWithSubjectCount > 0) {
            useSubjectsInfo = true;
        }

        // Simple or complex grade calculation type
        boolean useSimpleCalculation = false; // defaults to complex
        if ("simple".equalsIgnoreCase(test.getModel()) || "simple".equalsIgnoreCase(test.getEvaluationMode())) {
            useSimpleCalculation = true;
        }

        double proficiencyGeral = 0.0;
        double gradeGeral = 0.0;
        String classificationGeral = EvaluationCalculator.Classification.ABAIXO_BASICO.getValue();

        List<Map<String, Object>> subjectSpecificResults = new ArrayList<>();

        if (useSubjectsInfo) {
            subjectSpecificResults = calculateSubjectSpecificResults(testId, studentId, questions, answers, courseName);
            if (!subjectSpecificResults.isEmpty()) {
                double sumProficiencies = 0.0;
                double sumGrades = 0.0;
                boolean hasMatematica = false;

                for (Map<String, Object> sr : subjectSpecificResults) {
                    sumProficiencies += (Double) sr.get("proficiency");
                    sumGrades += (Double) sr.get("grade");
                    String sName = (String) sr.get("subject_name");
                    if (sName != null && sName.toLowerCase().contains("matem")) {
                        hasMatematica = true;
                    }
                }

                proficiencyGeral = sumProficiencies / subjectSpecificResults.size();
                gradeGeral = sumGrades / subjectSpecificResults.size();
                classificationGeral = evaluationCalculator.determineClassification(
                        proficiencyGeral, courseName, "GERAL", hasMatematica
                );
            } else {
                Map<String, Object> eval = evaluationCalculator.calculateCompleteEvaluation(
                        correctAnswers, totalQuestions, courseName, mainSubjectName, useSimpleCalculation
                );
                proficiencyGeral = (Double) eval.get("proficiency");
                gradeGeral = (Double) eval.get("grade");
                classificationGeral = (String) eval.get("classification");
            }
        } else {
            Map<String, Object> eval = evaluationCalculator.calculateCompleteEvaluation(
                    correctAnswers, totalQuestions, courseName, mainSubjectName, useSimpleCalculation
            );
            proficiencyGeral = (Double) eval.get("proficiency");
            gradeGeral = (Double) eval.get("grade");
            classificationGeral = (String) eval.get("classification");
        }

        double scorePercentage = totalQuestions > 0 ? ((double) correctAnswers / totalQuestions) * 100.0 : 0.0;

        // Structure subject results JSONB map
        Map<String, Object> subjectResultsMap = null;
        if (useSubjectsInfo && !subjectSpecificResults.isEmpty()) {
            subjectResultsMap = new LinkedHashMap<>();
            for (Map<String, Object> sr : subjectSpecificResults) {
                String subId = (String) sr.get("subject_id");
                Map<String, Object> details = new LinkedHashMap<>(sr);
                details.remove("subject_id");
                subjectResultsMap.put(subId, details);
            }
        }

        // Fetch or create EvaluationResult
        Optional<EvaluationResult> existingResultOpt = evaluationResultRepository.findByTestIdAndStudentId(testId, studentId);
        EvaluationResult evalResult;

        if (existingResultOpt.isPresent()) {
            evalResult = existingResultOpt.get();
        } else {
            evalResult = new EvaluationResult();
            evalResult.setTestId(testId);
            evalResult.setStudentId(studentId);
            evalResult.setSessionId(sessionId);
        }

        evalResult.setCorrectAnswers(correctAnswers);
        evalResult.setTotalQuestions(totalQuestions);
        evalResult.setScorePercentage(scorePercentage);
        evalResult.setGrade(roundToTwoDecimals(gradeGeral));
        evalResult.setProficiency(roundToTwoDecimals(proficiencyGeral));
        evalResult.setClassification(classificationGeral);
        evalResult.setSubjectResults(subjectResultsMap);
        evalResult.setCalculatedAt(LocalDateTime.now());

        // Capture placement snapshots from student
        if (student.getSchool() != null) {
            evalResult.setSchoolIdSnapshot(student.getSchool().getId());
        }
        if (student.getSchoolClass() != null) {
            evalResult.setClassIdSnapshot(student.getSchoolClass().getId());
        }
        if (student.getGradeId() != null) {
            evalResult.setGradeIdSnapshot(student.getGradeId().toString());
        }
        if (student.getRegistration() != null) {
            evalResult.setEnrollmentIdSnapshot(student.getRegistration());
        }

        evaluationResultRepository.save(evalResult);
        log.info("✅ Resultado salvo com sucesso! Nota={}, Proficiência={}", gradeGeral, proficiencyGeral);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("proficiency", roundToTwoDecimals(proficiencyGeral));
        response.put("grade", roundToTwoDecimals(gradeGeral));
        response.put("classification", classificationGeral);
        response.put("correct_answers", correctAnswers);
        response.put("total_questions", totalQuestions);
        response.put("accuracy_rate", roundToTwoDecimals(scorePercentage));

        return response;
    }
}
