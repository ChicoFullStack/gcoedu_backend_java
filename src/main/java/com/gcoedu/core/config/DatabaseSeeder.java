package com.gcoedu.core.config;

import com.gcoedu.core.domain.entity.tenant.*;
import com.gcoedu.core.repository.tenant.*;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;
import java.util.Random;

import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Transactional
public class DatabaseSeeder implements CommandLineRunner {

    private final SchoolRepository schoolRepository;
    private final ClassRepository classRepository;
    private final StudentRepository studentRepository;
    private final TestRepository testRepository;
    private final QuestionRepository questionRepository;
    private final AnswerSheetGabaritoRepository gabaritoRepository;
    private final AnswerSheetResultRepository resultRepository;

    @Override
    public void run(String... args) {
        if (schoolRepository.count() == 0) {
            seedDatabase();
        }
    }

    private void seedDatabase() {
        System.out.println("========== INICIANDO DATABASE SEEDER ==========");

        // 1. Criar Escola
        School school = new School();
        school.setName("Escola GCO Edu Oficial");
        school.setInepCode("12345678");
        schoolRepository.save(school);

        // 2. Criar Turmas
        SchoolClass classA = new SchoolClass();
        classA.setName("1º Ano A");
        classA.setSchool(school);
        classA.setShift("Manhã");
        classRepository.save(classA);

        SchoolClass classB = new SchoolClass();
        classB.setName("2º Ano B");
        classB.setSchool(school);
        classB.setShift("Tarde");
        classRepository.save(classB);

        // 3. Criar Alunos
        String[] studentNames = {
            "Ana Silva", "Bruno Souza", "Carlos Almeida", "Daniela Costa", "Eduardo Pereira",
            "Fernanda Lima", "Gabriel Santos", "Helena Oliveira", "Igor Martins", "Julia Rocha"
        };
        
        for (int i = 0; i < 10; i++) {
            Student student = new Student();
            student.setName(studentNames[i]);
            student.setRegistration("MAT" + (1000 + i));
            student.setSchool(school);
            student.setSchoolClass(i < 5 ? classA : classB);
            studentRepository.save(student);
        }

        // 4. Criar Questões (sem Subject pois não temos o SubjectRepository)
        for (int i = 1; i <= 10; i++) {
            Question q = new Question();
            q.setStatement("Questão de Matemática " + i + ": Qual é o resultado de " + i + " + " + i + "?");
            q.setCorrectOption("A");
            questionRepository.save(q);
        }

        // 5. Criar Avaliações (Testes)
        Test test1 = new Test();
        test1.setTitle("Simulado de Matemática - 1º Bimestre");
        test1.setStatus("completed");
        test1.setCreatedAt(LocalDateTime.now().minusDays(10));
        testRepository.save(test1);

        Test test2 = new Test();
        test2.setTitle("Prova Final de História");
        test2.setStatus("active");
        test2.setCreatedAt(LocalDateTime.now().minusDays(2));
        testRepository.save(test2);

        // 6. Criar Gabaritos
        AnswerSheetGabarito gabarito1 = new AnswerSheetGabarito();
        gabarito1.setTest(test1);
        gabarito1.setNumQuestions(10);
        gabarito1.setCorrectAnswers("[\"A\",\"B\",\"C\",\"D\",\"E\",\"A\",\"B\",\"C\",\"D\",\"E\"]");
        gabarito1.setSchoolClass(classA);
        gabaritoRepository.save(gabarito1);

        AnswerSheetGabarito gabarito2 = new AnswerSheetGabarito();
        gabarito2.setTest(test2);
        gabarito2.setNumQuestions(10);
        gabarito2.setCorrectAnswers("[\"A\",\"A\",\"A\",\"B\",\"B\",\"C\",\"C\",\"D\",\"D\",\"E\"]");
        gabarito2.setSchoolClass(classB);
        gabaritoRepository.save(gabarito2);

        // 7. Criar Resultados (AnswerSheetResults)
        List<Student> allStudents = studentRepository.findAll();
        Random random = new Random();

        for (Student s : allStudents) {
            AnswerSheetResult result = new AnswerSheetResult();
            result.setStudent(s);
            result.setGabarito(s.getSchoolClass().getName().contains("1º") ? gabarito1 : gabarito2);
            result.setDetectedAnswers("[\"A\",\"B\",\"A\",\"A\",\"E\",\"A\",\"A\",\"C\",\"D\",\"E\"]"); // Respostas aleatórias
            
            // Simular nota aleatória de 5.0 a 10.0
            double nota = 5.0 + (random.nextDouble() * 5.0);
            result.setGrade(Math.round(nota * 10.0) / 10.0); 
            result.setScorePercentage(result.getGrade() * 10.0);
            result.setCorrectAnswersCount(8);
            
            resultRepository.save(result);
        }

        System.out.println("========== SEED CONCLUÍDO COM SUCESSO ==========");
    }
}
