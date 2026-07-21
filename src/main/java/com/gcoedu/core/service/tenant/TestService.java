package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.dto.tenant.TestDTO;
import com.gcoedu.core.domain.entity.tenant.Question;
import com.gcoedu.core.domain.entity.tenant.Test;
import com.gcoedu.core.domain.entity.tenant.TestQuestion;
import com.gcoedu.core.mapper.tenant.TestMapper;
import com.gcoedu.core.repository.tenant.QuestionRepository;
import com.gcoedu.core.repository.tenant.TestQuestionRepository;
import com.gcoedu.core.repository.tenant.TestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TestService {

    private final TestRepository testRepository;
    private final TestMapper testMapper;
    private final TestQuestionRepository testQuestionRepository;
    private final QuestionRepository questionRepository;

    public TestService(TestRepository testRepository, TestMapper testMapper,
                       TestQuestionRepository testQuestionRepository, QuestionRepository questionRepository) {
        this.testRepository = testRepository;
        this.testMapper = testMapper;
        this.testQuestionRepository = testQuestionRepository;
        this.questionRepository = questionRepository;
    }

    public List<TestDTO> findAll() {
        return testRepository.findAll().stream()
                .map(testMapper::toDto)
                .collect(Collectors.toList());
    }

    public TestDTO findById(String id) {
        return testRepository.findById(id)
                .map(testMapper::toDto)
                .orElseThrow(() -> new RuntimeException("Prova não encontrada: " + id));
    }

    public TestDTO create(TestDTO dto) {
        Test test = testMapper.toEntity(dto);
        if (test.getId() == null) {
            test.setId(java.util.UUID.randomUUID().toString());
        }
        return testMapper.toDto(testRepository.save(test));
    }

    public TestDTO update(String id, TestDTO dto) {
        Test test = testRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Prova não encontrada: " + id));
        testMapper.updateEntity(dto, test);
        return testMapper.toDto(testRepository.save(test));
    }

    public void delete(String id) {
        testRepository.deleteById(id);
    }

    /**
     * Adiciona uma questão a uma prova em uma posição específica.
     */
    public void addQuestion(String testId, String questionId, int orderIndex, double weight) {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Prova não encontrada: " + testId));
        Question question = questionRepository.findById(questionId)
                .orElseThrow(() -> new RuntimeException("Questão não encontrada: " + questionId));

        TestQuestion tq = new TestQuestion();
        tq.setTest(test);
        tq.setQuestion(question);
        tq.setOrderIndex(orderIndex);
        tq.setWeight(weight);
        testQuestionRepository.save(tq);
    }

    /**
     * Remove uma questão de uma prova.
     */
    public void removeQuestion(String testId, String questionId) {
        testQuestionRepository.deleteByTestIdAndQuestionId(testId, questionId);
    }

    /**
     * Lista as questões de uma prova em ordem.
     */
    public List<TestQuestion> getQuestionsForTest(String testId) {
        return testQuestionRepository.findByTestIdOrderByOrderIndex(testId);
    }
}
