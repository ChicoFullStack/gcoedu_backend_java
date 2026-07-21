package com.gcoedu.core.service.tenant;

import com.gcoedu.core.domain.entity.tenant.AnswerSheetResult;
import com.gcoedu.core.domain.entity.tenant.Test;
import com.gcoedu.core.repository.tenant.AnswerSheetResultRepository;
import com.gcoedu.core.repository.tenant.TestRepository;
import com.gcoedu.core.service.PermissionService;
import com.gcoedu.core.service.pdf.PdfReportGeneratorService;
import com.hubspot.jinjava.Jinjava;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportService {

    private final AnswerSheetResultRepository resultRepository;
    private final TestRepository testRepository;
    private final PdfReportGeneratorService pdfGeneratorService;
    private final PermissionService permissionService;

    public ReportService(
            AnswerSheetResultRepository resultRepository,
            TestRepository testRepository,
            PdfReportGeneratorService pdfGeneratorService,
            PermissionService permissionService) {
        this.resultRepository = resultRepository;
        this.testRepository = testRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.permissionService = permissionService;
    }

    public byte[] generateTestReportPdf(String testId) throws Exception {
        Test test = testRepository.findById(testId)
                .orElseThrow(() -> new RuntimeException("Test not found: " + testId));

        List<AnswerSheetResult> results = resultRepository.findByGabaritoTestId(testId).stream()
                .filter(res -> permissionService.canAccessStudent(res.getStudent()))
                .collect(Collectors.toList());

        // Render HTML template
        String htmlContent = renderHtmlReport(test, results);

        // Convert HTML to PDF
        return pdfGeneratorService.generatePdfFromHtml(htmlContent);
    }

    private String renderHtmlReport(Test test, List<AnswerSheetResult> results) throws Exception {
        Jinjava jinjava = new Jinjava();
        Map<String, Object> context = new HashMap<>();

        // Load Template
        ClassPathResource resource = new ClassPathResource("templates/test_report.html");
        String templateContent;
        try (InputStream is = resource.getInputStream()) {
            templateContent = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        // Calculate Metrics
        double averageGrade = 0.0;
        if (!results.isEmpty()) {
            double totalGrade = results.stream().mapToDouble(AnswerSheetResult::getGrade).sum();
            averageGrade = totalGrade / results.size();
        }

        // Format Date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String dateStr = LocalDateTime.now().format(formatter);

        // Map Results for Template
        List<Map<String, Object>> resultMaps = results.stream().map(res -> {
            Map<String, Object> map = new HashMap<>();
            map.put("studentName", res.getStudent().getName() != null ? res.getStudent().getName() : "Aluno sem Nome");
            map.put("correctAnswers", res.getCorrectAnswersCount());
            map.put("scorePercentage", String.format("%.1f", res.getScorePercentage()));
            map.put("grade", String.format("%.1f", res.getGrade()));
            return map;
        }).collect(Collectors.toList());

        context.put("testName", "Avaliação OMR - ID: " + test.getId());
        context.put("totalStudents", results.size());
        context.put("averageGrade", String.format("%.1f", averageGrade));
        context.put("generationDate", dateStr);
        context.put("results", resultMaps);

        return jinjava.render(templateContent, context);
    }
}
