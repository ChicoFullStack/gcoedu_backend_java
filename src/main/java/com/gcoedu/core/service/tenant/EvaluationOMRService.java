package com.gcoedu.core.service.tenant;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gcoedu.core.domain.dto.tenant.OmrUploadResponseDTO;
import com.gcoedu.core.domain.entity.tenant.*;
import com.gcoedu.core.repository.tenant.*;
import com.gcoedu.core.service.JobProgressService;
import com.gcoedu.core.service.MinioService;
import com.gcoedu.core.service.omr.OmrCorrectionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Orquestra o fluxo completo de correção de cartão-resposta:
 * 1. Salva a imagem original no MinIO.
 * 2. Roda o OpenCV de forma assíncrona (@Async).
 * 3. Persiste os resultados em answer_sheet_results e evaluation_results.
 */
@Slf4j
@Service
public class EvaluationOMRService {

    private final MinioService minioService;
    private final JobProgressService jobProgressService;
    private final OmrCorrectionService omrCorrectionService;
    private final AnswerSheetGabaritoRepository gabaritoRepository;
    private final AnswerSheetResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final TestQuestionRepository testQuestionRepository;
    private final StudentAnswerRepository studentAnswerRepository;
    private final TestSessionRepository testSessionRepository;
    private final EvaluationResultService evaluationResultService;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    public EvaluationOMRService(
            MinioService minioService,
            JobProgressService jobProgressService,
            OmrCorrectionService omrCorrectionService,
            AnswerSheetGabaritoRepository gabaritoRepository,
            AnswerSheetResultRepository resultRepository,
            StudentRepository studentRepository,
            TestQuestionRepository testQuestionRepository,
            StudentAnswerRepository studentAnswerRepository,
            TestSessionRepository testSessionRepository,
            EvaluationResultService evaluationResultService) {
        this.minioService = minioService;
        this.jobProgressService = jobProgressService;
        this.omrCorrectionService = omrCorrectionService;
        this.gabaritoRepository = gabaritoRepository;
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.testQuestionRepository = testQuestionRepository;
        this.studentAnswerRepository = studentAnswerRepository;
        this.testSessionRepository = testSessionRepository;
        this.evaluationResultService = evaluationResultService;
    }

    /**
     * Passo 1 (síncrono): Salva no MinIO e retorna o jobId imediatamente.
     * O processamento OpenCV acontece em background via @Async.
     */
    public OmrUploadResponseDTO receiveUpload(String gabaritoId, String studentId,
                                               MultipartFile file) throws Exception {
        String jobId = UUID.randomUUID().toString();
        String objectKey = "omr/" + gabaritoId + "/" + studentId + "/" + jobId + ".jpg";

        // Persiste a imagem original no MinIO
        minioService.uploadFile(objectKey, file.getInputStream(), file.getSize(), file.getContentType());

        // Inicializa o job de progresso no Redis (total = 100 etapas)
        jobProgressService.initJob(jobId, 100);
        jobProgressService.incrementProgress(jobId, 5);

        // Despacha o processamento para o thread pool assíncrono
        processAsync(jobId, gabaritoId, studentId, objectKey);

        return new OmrUploadResponseDTO(jobId, "Upload recebido. Correção em andamento...", objectKey);
    }

    /**
     * Passo 2 (assíncrono — Thread pool 'omrTaskExecutor'):
     * Busca os dados no banco, roda o OpenCV e persiste o resultado.
     */
    @Async("omrTaskExecutor")
    @Transactional
    public void processAsync(String jobId, String gabaritoId, String studentId, String objectKey) {
        try {
            jobProgressService.incrementProgress(jobId, 10);

            AnswerSheetGabarito gabarito = gabaritoRepository.findById(gabaritoId)
                    .orElseThrow(() -> new RuntimeException("Gabarito não encontrado: " + gabaritoId));
            Student student = studentRepository.findById(studentId)
                    .orElseThrow(() -> new RuntimeException("Aluno não encontrado: " + studentId));

            jobProgressService.incrementProgress(jobId, 10);

            // Baixar os bytes reais do MinIO
            InputStream imgStream = minioService.downloadFile(objectKey);
            byte[] imageData = imgStream.readAllBytes();
            imgStream.close();

            jobProgressService.incrementProgress(jobId, 10);

            List<String> correctAnswers = objectMapper.readValue(
                    gabarito.getCorrectAnswers(), new TypeReference<>() {});

            jobProgressService.incrementProgress(jobId, 10);

            // Correção via OpenCV usando os dados reais de imagem e configuração de blocos
            String blocksConfigJson = gabarito.getBlocksConfig() != null ? gabarito.getBlocksConfig() : "{}";
            Map<String, Object> correctionResult = omrCorrectionService
                    .correctAnswerSheet(imageData, blocksConfigJson, correctAnswers, jobId);

            jobProgressService.incrementProgress(jobId, 20);

            // Persiste resultado em AnswerSheetResult
            @SuppressWarnings("unchecked")
            List<String> detectedAnswers = (List<String>) correctionResult.get("detectedAnswers");

            AnswerSheetResult result = new AnswerSheetResult();
            result.setGabarito(gabarito);
            result.setStudent(student);
            result.setDetectedAnswers(objectMapper.writeValueAsString(detectedAnswers));
            result.setCorrectAnswersCount((Integer) correctionResult.get("correctCount"));
            result.setScorePercentage((Double) correctionResult.get("scorePercentage"));
            result.setGrade((Double) correctionResult.get("grade"));
            resultRepository.save(result);

            jobProgressService.incrementProgress(jobId, 10);

            // Se o gabarito estiver associado a uma prova (Test), persistir também em student_answers e evaluation_results
            if (gabarito.getTest() != null) {
                Test test = gabarito.getTest();

                // Obter as questões da prova ordenadas
                List<TestQuestion> testQuestions = testQuestionRepository.findByTestIdOrderByOrderIndex(test.getId());

                // Salvar as respostas na tabela student_answers
                for (int i = 0; i < detectedAnswers.size(); i++) {
                    if (i >= testQuestions.size()) {
                        break;
                    }
                    TestQuestion tq = testQuestions.get(i);
                    Question question = tq.getQuestion();
                    String answerLetter = detectedAnswers.get(i);

                    Boolean isCorrect = null;
                    if (answerLetter != null && question.getCorrectOption() != null) {
                        isCorrect = answerLetter.equalsIgnoreCase(question.getCorrectOption());
                    }

                    Optional<StudentAnswer> existingAnswerOpt = studentAnswerRepository
                            .findByStudentIdAndTestIdAndQuestionId(student.getId(), test.getId(), question.getId());

                    StudentAnswer studentAnswer;
                    if (existingAnswerOpt.isPresent()) {
                        studentAnswer = existingAnswerOpt.get();
                        studentAnswer.setAnswer(answerLetter != null ? answerLetter : "");
                        studentAnswer.setIsCorrect(isCorrect);
                        studentAnswer.setAnsweredAt(LocalDateTime.now());
                    } else {
                        studentAnswer = new StudentAnswer();
                        studentAnswer.setStudent(student);
                        studentAnswer.setTest(test);
                        studentAnswer.setQuestion(question);
                        studentAnswer.setAnswer(answerLetter != null ? answerLetter : "");
                        studentAnswer.setIsCorrect(isCorrect);
                        studentAnswer.setAnsweredAt(LocalDateTime.now());
                    }
                    studentAnswerRepository.save(studentAnswer);
                }

                jobProgressService.incrementProgress(jobId, 10);

                // Criar ou obter sessão de teste mínima
                String userAgent = "Physical Test Correction (NewGrid)";
                List<TestSession> existingSessions = testSessionRepository
                        .findByTestIdAndStudentIdAndStatusAndUserAgent(test.getId(), student.getId(), "corrigida", userAgent);

                TestSession session;
                if (!existingSessions.isEmpty()) {
                    session = existingSessions.get(0);
                } else {
                    session = new TestSession();
                    session.setStudent(student);
                    session.setTest(test);
                    session.setStatus("corrigida");
                    session.setUserAgent(userAgent);
                    session.setStartedAt(LocalDateTime.now());
                    session.setSubmittedAt(LocalDateTime.now());
                    session = testSessionRepository.save(session);
                }

                // Chamar o serviço para calcular e salvar o resultado final da avaliação
                evaluationResultService.calculateAndSaveResult(test.getId(), student.getId(), session.getId());
            }

            jobProgressService.incrementProgress(jobId, 10);
            jobProgressService.finishJob(jobId);

        } catch (Exception e) {
            log.error("Erro no processamento OMR em background: {}", e.getMessage(), e);
            jobProgressService.failJob(jobId, e.getMessage());
        }
    }
}
