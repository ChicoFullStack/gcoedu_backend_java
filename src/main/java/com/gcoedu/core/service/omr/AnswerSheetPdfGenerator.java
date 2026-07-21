package com.gcoedu.core.service.omr;

import com.hubspot.jinjava.Jinjava;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@Service
public class AnswerSheetPdfGenerator {

    public byte[] generatePdf(String templateName, Map<String, Object> variables) throws Exception {
        // 1. Carrega o template HTML que está no classpath (src/main/resources/templates/)
        ClassPathResource resource = new ClassPathResource("templates/" + templateName);
        String templateString = Files.readString(Path.of(resource.getURI()));

        // 2. Renderiza o HTML via Jinjava (Mesma sintaxe 100% igual ao Python/Jinja2)
        Jinjava jinjava = new Jinjava();
        String htmlContent = jinjava.render(templateString, variables);

        // 3. Converte o HTML para PDF usando OpenHtmlToPdf (equivalente ao WeasyPrint)
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            // A baseUri ajuda a encontrar imagens e CSS locais se houver
            builder.withHtmlContent(htmlContent, "classpath:/templates/");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }
}
