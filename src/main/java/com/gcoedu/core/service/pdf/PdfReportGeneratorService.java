package com.gcoedu.core.service.pdf;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;

@Service
public class PdfReportGeneratorService {

    /**
     * Converts an HTML string to a PDF byte array.
     *
     * @param htmlContent The HTML content as a string.
     * @return The generated PDF as a byte array.
     * @throws Exception If PDF generation fails.
     */
    public byte[] generatePdfFromHtml(String htmlContent) throws Exception {
        try (ByteArrayOutputStream os = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(htmlContent, "");
            builder.toStream(os);
            builder.run();
            return os.toByteArray();
        }
    }
}
