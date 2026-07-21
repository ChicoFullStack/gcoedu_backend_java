package com.gcoedu.core.service.omr;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import lombok.extern.slf4j.Slf4j;
import nu.pattern.OpenCV;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.*;

@Slf4j
@Service
public class OmrCorrectionService {

    static {
        OpenCV.loadShared();
    }

    private static final int A4_WIDTH_PX = 2480;
    private static final int A4_HEIGHT_PX = 3508;

    private static final double ROW_HEIGHT_PX = 51.97;
    private static final int BUBBLE_RADIUS_PX = 25;
    private static final int BUBBLE_SPACING_PX = 61;
    private static final int BLOCK_OFFSET_X = 115;
    private static final int BLOCK_OFFSET_Y = 40;
    private static final double BUBBLE_WIDTH_PX = 15.0;
    private static final double FILL_THRESHOLD = 0.45;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class BlockCandidate {
        public int x, y, w, h;
        public double area;
        public double aspectRatio;

        public BlockCandidate(int x, int y, int w, int h, double area, double aspectRatio) {
            this.x = x;
            this.y = y;
            this.w = w;
            this.h = h;
            this.area = area;
            this.aspectRatio = aspectRatio;
        }
    }

    public static class BubbleInfo {
        public int qNum;
        public String alternative;
        public int cx;
        public int cy;
        public int r;

        public BubbleInfo(int qNum, String alternative, int cx, int cy, int r) {
            this.qNum = qNum;
            this.alternative = alternative;
            this.cx = cx;
            this.cy = cy;
            this.r = r;
        }
    }

    public static class SquareCandidate {
        public final MatOfPoint contour;
        public final Point center;
        public final double area;

        public SquareCandidate(MatOfPoint contour, Point center, double area) {
            this.contour = contour;
            this.center = center;
            this.area = area;
        }
    }

    /** Versão básica — processa apenas a imagem sem calcular gabarito. */
    public void correctAnswerSheet(byte[] imageData) throws Exception {
        correctAnswerSheet(imageData, "{}", Collections.emptyList(), null);
    }

    /**
     * Versão compatível legada.
     */
    public Map<String, Object> correctAnswerSheet(byte[] imageData,
                                                  List<String> correctAnswers,
                                                  String jobId) throws Exception {
        return correctAnswerSheet(imageData, "{}", correctAnswers, jobId);
    }

    /**
     * Versão completa — detecta respostas via OpenCV e calcula o resultado.
     *
     * @param imageData      bytes da imagem JPEG/PNG do cartão-resposta
     * @param blocksConfigJson JSON de configuração de blocos (topology)
     * @param correctAnswers respostas corretas em ordem ["A","B","C",...]
     * @param jobId          ID do job Redis (pode ser null)
     * @return mapa com: qrData, detectedAnswers (List), correctCount, scorePercentage, grade
     */
    public Map<String, Object> correctAnswerSheet(byte[] imageData,
                                                  String blocksConfigJson,
                                                  List<String> correctAnswers,
                                                  String jobId) throws Exception {
        log.info("Iniciando processamento OMR. Tamanho imagem: {} bytes", imageData.length);

        Mat img = Imgcodecs.imdecode(new MatOfByte(imageData), Imgcodecs.IMREAD_COLOR);
        if (img.empty()) {
            throw new IllegalArgumentException("Imagem inválida ou corrompida.");
        }

        // 1. Detectar QR Code
        String qrData = detectQrCode(imageData);
        log.info("QR Code detectado: {}", qrData);

        // 2. Pré-processamento
        log.info("Etapa 1 & 2: Pré-processamento e detecção de âncoras A4");
        Map<String, Point> anchors = detectA4Anchors(img);
        if (anchors == null || anchors.size() < 4) {
            img.release();
            throw new RuntimeException("Âncoras A4 não detectadas nos cantos do papel.");
        }

        // 3. Normalizar perspectiva A4
        log.info("Etapa 3: Normalizar para A4 lógico (2480x3508)");
        Mat imgA4 = normalizeToA4(img, anchors);

        // Parse Blocks Configuration (Topology)
        JsonNode rootNode = objectMapper.readTree(blocksConfigJson);
        int numBlocksExpected = rootNode.path("num_blocks").asInt(4);
        if (numBlocksExpected <= 0) {
            numBlocksExpected = 4;
        }

        // 4 & 5. Detectar blocos
        log.info("Etapa 4 & 5: Detectar blocos de resposta. Esperado: {}", numBlocksExpected);
        List<BlockCandidate> blocks = detectAnswerBlocksInFullA4(imgA4, numBlocksExpected);
        if (blocks == null || blocks.size() != numBlocksExpected) {
            img.release();
            imgA4.release();
            throw new RuntimeException("Erro ao localizar as áreas de blocos de respostas. Esperado: " + numBlocksExpected + ", Detectado: " + (blocks != null ? blocks.size() : 0));
        }

        log.info("Etapas 6-8: Processamento OMR nos blocos");
        JsonNode blocksTopology = rootNode.path("topology").path("blocks");
        Map<Integer, String> allAnswers = new TreeMap<>();

        for (int idx = 0; idx < blocks.size(); idx++) {
            BlockCandidate block = blocks.get(idx);
            int blockNum = idx + 1;

            // Extrair ROI do bloco
            Rect roiRect = new Rect(block.x, block.y, block.w, block.h);
            Mat blockRoi = new Mat(imgA4, roiRect);

            // Mapear topologia do JSON para o bloco
            JsonNode blockConfig = blocksTopology.get(idx);
            if (blockConfig == null || blockConfig.isMissingNode()) {
                // Se não há topologia correspondente, criar padrão para 26 questões
                blockConfig = createDefaultBlockConfig(blockNum, correctAnswers.size());
            }

            // Etapa 6: Mapear topologia -> grid
            List<BubbleInfo> bubbles = mapTopologyToBubbles(blockRoi, blockConfig);

            // Etapa 8: Detectar marcações
            Map<Integer, String> blockAnswers = detectMarkedBubbles(blockRoi, bubbles);
            allAnswers.putAll(blockAnswers);

            blockRoi.release();
        }

        // Liberar memória nativa
        img.release();
        imgA4.release();

        // 9. Calcular resultado final comparando com gabarito
        List<String> detectedAnswersList = new ArrayList<>();
        int total = correctAnswers.size();
        int correctCount = 0;

        for (int i = 1; i <= total; i++) {
            String answer = allAnswers.get(i);
            detectedAnswersList.add(answer); // pode ser null ou "INVALID"
            
            if (i - 1 < correctAnswers.size()) {
                String correct = correctAnswers.get(i - 1);
                if (answer != null && answer.equalsIgnoreCase(correct)) {
                    correctCount++;
                }
            }
        }

        double scorePercentage = total > 0 ? (correctCount * 100.0 / total) : 0.0;
        double grade = Math.round(scorePercentage / 10.0) / 10.0;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("qrData", qrData);
        result.put("detectedAnswers", detectedAnswersList);
        result.put("correctCount", correctCount);
        result.put("scorePercentage", scorePercentage);
        result.put("grade", grade);

        log.info("OMR Concluído! Acertos: {}/{}, Nota: {}", correctCount, total, grade);
        return result;
    }

    private String detectQrCode(byte[] imageData) {
        try {
            BufferedImage bImage = ImageIO.read(new ByteArrayInputStream(imageData));
            LuminanceSource source = new BufferedImageLuminanceSource(bImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
            return new MultiFormatReader().decode(bitmap).getText();
        } catch (Exception e) {
            log.warn("Erro ao ler QR Code: {}", e.getMessage());
            return null;
        }
    }

    private Map<String, Point> detectA4Anchors(Mat img) {
        Mat gray = new Mat();
        if (img.channels() == 3) {
            Imgproc.cvtColor(img, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            img.copyTo(gray);
        }

        double imgHeight = gray.rows();
        double imgWidth = gray.cols();
        double imgArea = imgWidth * imgHeight;

        Mat binary = new Mat();
        Imgproc.threshold(gray, binary, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(binary, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        double minArea = imgArea * 0.0001;
        double maxArea = imgArea * 0.002;
        double marginX = imgWidth * 0.1;
        double marginY = imgHeight * 0.1;

        List<SquareCandidate> squares = new ArrayList<>();

        for (int i = 0; i < contours.size(); i++) {
            // Usar hierarquia para pular contornos internos
            if (hierarchy.total() > 0) {
                double[] hVal = hierarchy.get(0, i);
                if (hVal != null && hVal[3] != -1) {
                    continue;
                }
            }

            MatOfPoint cnt = contours.get(i);
            double area = Imgproc.contourArea(cnt);
            if (area < minArea || area > maxArea) {
                continue;
            }

            MatOfPoint2f cnt2f = new MatOfPoint2f(cnt.toArray());
            double peri = Imgproc.arcLength(cnt2f, true);
            MatOfPoint2f approx2f = new MatOfPoint2f();
            Imgproc.approxPolyDP(cnt2f, approx2f, 0.04 * peri, true);
            MatOfPoint approx = new MatOfPoint(approx2f.toArray());

            if (approx.total() != 4) {
                approx.release();
                approx2f.release();
                cnt2f.release();
                continue;
            }

            Rect rect = Imgproc.boundingRect(approx);
            double aspectRatio = (double) rect.width / rect.height;
            if (aspectRatio < 0.9 || aspectRatio > 1.1) {
                approx.release();
                approx2f.release();
                cnt2f.release();
                continue;
            }

            Moments moments = Imgproc.moments(approx);
            if (moments.m00 == 0) {
                approx.release();
                approx2f.release();
                cnt2f.release();
                continue;
            }

            double cx = moments.m10 / moments.m00;
            double cy = moments.m01 / moments.m00;

            boolean isNearEdge = cx < marginX || cx > imgWidth - marginX || cy < marginY || cy > imgHeight - marginY;
            if (!isNearEdge) {
                approx.release();
                approx2f.release();
                cnt2f.release();
                continue;
            }

            squares.add(new SquareCandidate(approx, new Point(cx, cy), area));
            cnt2f.release();
            approx2f.release();
        }

        binary.release();
        hierarchy.release();
        gray.release();

        if (squares.size() < 4) {
            log.warn("Apenas {} quadrados de âncoras detectados nos cantos", squares.size());
            return null;
        }

        List<SquareCandidate> tlList = new ArrayList<>();
        List<SquareCandidate> trList = new ArrayList<>();
        List<SquareCandidate> brList = new ArrayList<>();
        List<SquareCandidate> blList = new ArrayList<>();

        double midX = imgWidth / 2.0;
        double midY = imgHeight / 2.0;

        for (SquareCandidate s : squares) {
            if (s.center.x < midX && s.center.y < midY) {
                tlList.add(s);
            } else if (s.center.x >= midX && s.center.y < midY) {
                trList.add(s);
            } else if (s.center.x >= midX && s.center.y >= midY) {
                brList.add(s);
            } else {
                blList.add(s);
            }
        }

        if (tlList.isEmpty() || trList.isEmpty() || brList.isEmpty() || blList.isEmpty()) {
            return null;
        }

        Map<String, Point> orderedSquares = new HashMap<>();
        orderedSquares.put("TL", getCorrectCornerVertex("TL", tlList));
        orderedSquares.put("TR", getCorrectCornerVertex("TR", trList));
        orderedSquares.put("BR", getCorrectCornerVertex("BR", brList));
        orderedSquares.put("BL", getCorrectCornerVertex("BL", blList));

        return orderedSquares;
    }

    private Point getCorrectCornerVertex(String corner, List<SquareCandidate> candidates) {
        SquareCandidate best = candidates.stream().max(Comparator.comparingDouble(c -> c.area)).orElseThrow();
        Point[] vertices = best.contour.toArray();
        Point bestVertex = vertices[0];

        switch (corner) {
            case "TL": {
                double minDist = Double.MAX_VALUE;
                for (Point v : vertices) {
                    double dist = v.x + v.y;
                    if (dist < minDist) {
                        minDist = dist;
                        bestVertex = v;
                    }
                }
                break;
            }
            case "TR": {
                double maxScore = -Double.MAX_VALUE;
                for (Point v : vertices) {
                    double score = v.x - v.y;
                    if (score > maxScore) {
                        maxScore = score;
                        bestVertex = v;
                    }
                }
                break;
            }
            case "BR": {
                double maxDist = -Double.MAX_VALUE;
                for (Point v : vertices) {
                    double dist = v.x + v.y;
                    if (dist > maxDist) {
                        maxDist = dist;
                        bestVertex = v;
                    }
                }
                break;
            }
            case "BL": {
                double minScore = Double.MAX_VALUE;
                for (Point v : vertices) {
                    double score = v.x - v.y;
                    if (score < minScore) {
                        minScore = score;
                        bestVertex = v;
                    }
                }
                break;
            }
        }
        return bestVertex;
    }

    private Mat normalizeToA4(Mat img, Map<String, Point> anchors) {
        MatOfPoint2f srcPts = new MatOfPoint2f(
                anchors.get("TL"), anchors.get("TR"),
                anchors.get("BR"), anchors.get("BL"));
        MatOfPoint2f dstPts = new MatOfPoint2f(
                new Point(0, 0), new Point(A4_WIDTH_PX, 0),
                new Point(A4_WIDTH_PX, A4_HEIGHT_PX), new Point(0, A4_HEIGHT_PX));
        Mat matrix = Imgproc.getPerspectiveTransform(srcPts, dstPts);
        Mat warped = new Mat();
        Imgproc.warpPerspective(img, warped, matrix, new Size(A4_WIDTH_PX, A4_HEIGHT_PX));
        matrix.release();
        srcPts.release();
        dstPts.release();
        return warped;
    }

    private List<BlockCandidate> detectAnswerBlocksInFullA4(Mat imgA4, int numBlocksExpected) {
        int h = imgA4.rows();
        int w = imgA4.cols();

        Mat gray = new Mat();
        if (imgA4.channels() == 3) {
            Imgproc.cvtColor(imgA4, gray, Imgproc.COLOR_BGR2GRAY);
        } else {
            imgA4.copyTo(gray);
        }

        Mat blurred = new Mat();
        Imgproc.GaussianBlur(gray, blurred, new Size(5, 5), 0);

        Mat thresh = new Mat();
        Imgproc.threshold(blurred, thresh, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Mat dilated = new Mat();
        Mat kernelSmall = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, new Size(3, 3));
        Imgproc.dilate(thresh, dilated, kernelSmall);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(dilated, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);

        double imgArea = w * h;
        double minBlockArea = imgArea * 0.02;
        double maxBlockArea = imgArea * 0.15;
        double minBlockWidth = w * 0.10;
        double maxBlockWidth = w * 0.30;
        double minBlockHeight = h * 0.10;

        List<BlockCandidate> candidates = new ArrayList<>();

        for (int i = 0; i < contours.size(); i++) {
            if (hierarchy.total() > 0) {
                double[] hVal = hierarchy.get(0, i);
                if (hVal != null && hVal[3] != -1) {
                    continue;
                }
            }

            MatOfPoint cnt = contours.get(i);
            double area = Imgproc.contourArea(cnt);
            if (area < minBlockArea || area > maxBlockArea) {
                continue;
            }

            MatOfPoint2f cnt2f = new MatOfPoint2f(cnt.toArray());
            double peri = Imgproc.arcLength(cnt2f, true);
            MatOfPoint2f approx2f = new MatOfPoint2f();
            Imgproc.approxPolyDP(cnt2f, approx2f, 0.02 * peri, true);
            MatOfPoint approx = new MatOfPoint(approx2f.toArray());

            if (approx.total() < 4) {
                approx.release();
                approx2f.release();
                cnt2f.release();
                continue;
            }

            Rect rect = Imgproc.boundingRect(approx);
            if (rect.width < minBlockWidth || rect.width > maxBlockWidth) {
                approx.release();
                approx2f.release();
                cnt2f.release();
                continue;
            }
            if (rect.height < minBlockHeight) {
                approx.release();
                approx2f.release();
                cnt2f.release();
                continue;
            }

            double aspectRatio = (double) rect.width / rect.height;
            if (aspectRatio < 0.3 || aspectRatio > 2.0) {
                approx.release();
                approx2f.release();
                cnt2f.release();
                continue;
            }

            candidates.add(new BlockCandidate(rect.x, rect.y, rect.width, rect.height, area, aspectRatio));
            approx.release();
            approx2f.release();
            cnt2f.release();
        }

        gray.release();
        blurred.release();
        thresh.release();
        dilated.release();
        kernelSmall.release();
        hierarchy.release();

        candidates.sort(Comparator.comparingInt(b -> b.x));
        return candidates;
    }

    private JsonNode createDefaultBlockConfig(int blockNum, int totalQuestions) {
        int startQ = (blockNum - 1) * 26 + 1;
        int endQ = Math.min(blockNum * 26, totalQuestions);

        com.fasterxml.jackson.databind.node.ObjectNode blockNode = objectMapper.createObjectNode();
        blockNode.put("block_id", blockNum);
        com.fasterxml.jackson.databind.node.ArrayNode questionsArray = objectMapper.createArrayNode();

        for (int q = startQ; q <= endQ; q++) {
            com.fasterxml.jackson.databind.node.ObjectNode questionNode = objectMapper.createObjectNode();
            questionNode.put("q", q);
            com.fasterxml.jackson.databind.node.ArrayNode altsArray = objectMapper.createArrayNode();
            altsArray.add("A").add("B").add("C").add("D");
            questionNode.set("alternatives", altsArray);
            questionsArray.add(questionNode);
        }

        blockNode.set("questions", questionsArray);
        return blockNode;
    }

    private List<BubbleInfo> mapTopologyToBubbles(Mat blockRoi, JsonNode blockConfig) {
        List<BubbleInfo> bubbles = new ArrayList<>();
        JsonNode questionsNode = blockConfig.path("questions");
        if (questionsNode.isMissingNode() || !questionsNode.isArray()) {
            return bubbles;
        }

        for (int rowIdx = 0; rowIdx < questionsNode.size(); rowIdx++) {
            JsonNode questionNode = questionsNode.get(rowIdx);
            int qNum = questionNode.path("q").asInt();
            JsonNode altsNode = questionNode.path("alternatives");

            if (altsNode.isMissingNode() || !altsNode.isArray()) {
                continue;
            }

            double cy = BLOCK_OFFSET_Y + (ROW_HEIGHT_PX * rowIdx) + (ROW_HEIGHT_PX / 2.0);

            for (int colIdx = 0; colIdx < altsNode.size(); colIdx++) {
                String altLetter = altsNode.get(colIdx).asText();
                double cx = BLOCK_OFFSET_X + (colIdx * BUBBLE_SPACING_PX) + (BUBBLE_WIDTH_PX / 2.0);
                bubbles.add(new BubbleInfo(qNum, altLetter, (int) cx, (int) cy, BUBBLE_RADIUS_PX));
            }
        }

        return bubbles;
    }

    private Map<Integer, String> detectMarkedBubbles(Mat blockRoi, List<BubbleInfo> bubbles) {
        Mat gray = new Mat();
        Imgproc.cvtColor(blockRoi, gray, Imgproc.COLOR_BGR2GRAY);
        Mat blur = new Mat();
        Imgproc.GaussianBlur(gray, blur, new Size(5, 5), 0);

        Mat thresh = new Mat();
        Imgproc.threshold(blur, thresh, 0, 255, Imgproc.THRESH_BINARY_INV + Imgproc.THRESH_OTSU);

        Map<Integer, List<BubbleInfo>> questionsMap = new LinkedHashMap<>();
        for (BubbleInfo b : bubbles) {
            questionsMap.computeIfAbsent(b.qNum, k -> new ArrayList<>()).add(b);
        }

        Map<Integer, String> answers = new LinkedHashMap<>();

        for (Map.Entry<Integer, List<BubbleInfo>> entry : questionsMap.entrySet()) {
            int qNum = entry.getKey();
            List<BubbleInfo> qBubbles = entry.getValue();

            class FillRatioResult {
                final String letter;
                final double fillRatio;

                FillRatioResult(String letter, double fillRatio) {
                    this.letter = letter;
                    this.fillRatio = fillRatio;
                }
            }

            List<FillRatioResult> fillRatios = new ArrayList<>();

            for (BubbleInfo bubble : qBubbles) {
                Mat mask = Mat.zeros(thresh.size(), thresh.type());
                Imgproc.circle(mask, new Point(bubble.cx, bubble.cy), bubble.r, new Scalar(255), -1);

                Mat masked = new Mat();
                Core.bitwise_and(thresh, mask, masked);

                int blackPixels = Core.countNonZero(masked);
                int totalPixels = Core.countNonZero(mask);

                double fillRatio = totalPixels > 0 ? (double) blackPixels / totalPixels : 0.0;
                fillRatios.add(new FillRatioResult(bubble.alternative, fillRatio));

                mask.release();
                masked.release();
            }

            fillRatios.sort((a, b) -> Double.compare(b.fillRatio, a.fillRatio));

            long markedCount = fillRatios.stream().filter(fr -> fr.fillRatio > FILL_THRESHOLD).count();

            if (markedCount == 0) {
                answers.put(qNum, null);
            } else if (markedCount == 1) {
                answers.put(qNum, fillRatios.get(0).letter);
            } else {
                answers.put(qNum, "INVALID");
            }
        }

        gray.release();
        blur.release();
        thresh.release();
        return answers;
    }
}
