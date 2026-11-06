package com.platform.idcard.template.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.platform.idcard.file.service.FileStorageService;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.student.entity.Student;
import com.platform.idcard.template.dto.PrintSettingsDto;
import com.platform.idcard.template.entity.CardTemplate;
import com.platform.idcard.template.entity.TemplateVersion;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.pdmodel.graphics.state.RenderingMode;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.util.Matrix;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class CardPdfRenderingService {

    private static final Logger log = LoggerFactory.getLogger(CardPdfRenderingService.class);
    private static final float POINTS_PER_MM = 72.0f / 25.4f; // ~2.8346457 pt/mm
    private static final DateTimeFormatter DOB_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");

    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    public CardPdfRenderingService(ObjectMapper objectMapper, FileStorageService fileStorageService) {
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Renders a 300 DPI high-resolution BufferedImage for a single card side (front or back).
     * Strictly matches the top-down coordinate canvas of the frontend (CardCanvas.jsx)
     * with element clipping, correct font-size mapping, photo shapes, and dynamic elements.
     */
    public BufferedImage renderCardToBufferedImage(
            CardTemplate template,
            TemplateVersion version,
            Student student,
            School school,
            String academicSession,
            boolean isFront
    ) {
        Student effectiveStudent = student != null ? student : createSampleStudent(school);
        School effectiveSchool = getEffectiveSchool(school, template, effectiveStudent);
        String effectiveSession = academicSession != null ? academicSession : "2026-2027";

        float cardWidthMm = version != null && version.getCardWidthMm() != null ? version.getCardWidthMm().floatValue() : 54.00f;
        float cardHeightMm = version != null && version.getCardHeightMm() != null ? version.getCardHeightMm().floatValue() : 85.60f;

        // 300 DPI scaling: 1 inch = 25.4 mm = 300 px (~11.811 px/mm)
        float scale = 300.0f / 25.4f;
        int cardWidthPx = Math.max(100, Math.round(cardWidthMm * scale));
        int cardHeightPx = Math.max(100, Math.round(cardHeightMm * scale));

        BufferedImage cardImg = new BufferedImage(cardWidthPx, cardHeightPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = cardImg.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        String designJson = isFront
                ? (version != null ? version.getFrontDesignJson() : null)
                : (version != null ? version.getBackDesignJson() : null);

        // 1. Draw card canvas background color
        Color bgColor = extractBackgroundColor(designJson, isFront);
        g2.setColor(bgColor != null ? bgColor : Color.WHITE);
        g2.fillRect(0, 0, cardWidthPx, cardHeightPx);

        // 2. Parse and sort elements by zIndex
        JsonNode elementsNode = extractElementsNode(designJson, isFront);
        if (elementsNode != null && elementsNode.isArray() && elementsNode.size() > 0) {
            java.util.List<JsonNode> sortedElements = new java.util.ArrayList<>();
            for (JsonNode el : elementsNode) {
                sortedElements.add(el);
            }
            sortedElements.sort(java.util.Comparator.comparingInt(this::getElementZIndex));

            for (JsonNode el : sortedElements) {
                renderElementToJava2D(g2, el, effectiveStudent, effectiveSchool, effectiveSession, cardWidthMm, cardHeightMm, scale);
            }
        } else {
            renderFallbackJava2D(g2, isFront, effectiveStudent, effectiveSchool, effectiveSession, cardWidthPx, cardHeightPx, scale);
        }

        g2.dispose();
        return cardImg;
    }

    /**
     * Renders a 2-page print-quality PDF for a card template version.
     * Page 1 = Front side
     * Page 2 = Back side
     */
    private PDImageXObject createCardPdfImage(PDDocument document, BufferedImage img) throws Exception {
        if (img == null) return null;
        try {
            return JPEGFactory.createFromImage(document, img, 0.95f);
        } catch (Exception e) {
            return LosslessFactory.createFromImage(document, img);
        }
    }

    public byte[] renderCardPdf(
            CardTemplate template,
            TemplateVersion version,
            Student student,
            School school,
            String academicSession
    ) {
        Student effectiveStudent = student != null ? student : createSampleStudent(school);
        School effectiveSchool = getEffectiveSchool(school, template, effectiveStudent);
        String effectiveSession = academicSession != null ? academicSession : "2026-2027";

        float widthMm = version != null && version.getCardWidthMm() != null ? version.getCardWidthMm().floatValue() : 54.00f;
        float heightMm = version != null && version.getCardHeightMm() != null ? version.getCardHeightMm().floatValue() : 85.60f;

        float widthPt = widthMm * POINTS_PER_MM;
        float heightPt = heightMm * POINTS_PER_MM;
        PDRectangle pageSize = new PDRectangle(widthPt, heightPt);

        BufferedImage frontImg = renderCardToBufferedImage(template, version, effectiveStudent, effectiveSchool, effectiveSession, true);
        BufferedImage backImg = renderCardToBufferedImage(template, version, effectiveStudent, effectiveSchool, effectiveSession, false);

        String frontJson = version != null ? version.getFrontDesignJson() : null;
        String backJson = version != null ? version.getBackDesignJson() : null;

        try (PDDocument document = new PDDocument()) {
            // Page 1: Front
            PDPage frontPage = new PDPage(pageSize);
            document.addPage(frontPage);
            try (PDPageContentStream stream = new PDPageContentStream(document, frontPage)) {
                PDImageXObject pdFront = createCardPdfImage(document, frontImg);
                stream.drawImage(pdFront, 0, 0, widthPt, heightPt);
                addInvisibleTextLayer(stream, frontJson, true, effectiveStudent, effectiveSchool, effectiveSession, widthPt, heightPt);
            }

            // Page 2: Back
            PDPage backPage = new PDPage(pageSize);
            document.addPage(backPage);
            try (PDPageContentStream stream = new PDPageContentStream(document, backPage)) {
                PDImageXObject pdBack = createCardPdfImage(document, backImg);
                stream.drawImage(pdBack, 0, 0, widthPt, heightPt);
                addInvisibleTextLayer(stream, backJson, false, effectiveStudent, effectiveSchool, effectiveSession, widthPt, heightPt);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render card template PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Card PDF rendering failed: " + e.getMessage(), e);
        }
    }

    /**
     * Renders a multi-page print-quality PDF batch for a list of students.
     * Generates Front & Back pages for each student.
     */
    public byte[] renderBatchPdf(
            CardTemplate template,
            TemplateVersion version,
            java.util.List<Student> students,
            School school,
            String academicSession
    ) {
        float widthMm = version != null && version.getCardWidthMm() != null ? version.getCardWidthMm().floatValue() : 54.00f;
        float heightMm = version != null && version.getCardHeightMm() != null ? version.getCardHeightMm().floatValue() : 85.60f;

        float widthPt = widthMm * POINTS_PER_MM;
        float heightPt = heightMm * POINTS_PER_MM;
        PDRectangle pageSize = new PDRectangle(widthPt, heightPt);

        School effectiveSchool = getEffectiveSchool(school, template, null);
        String effectiveSession = academicSession != null ? academicSession : "2026-2027";
        java.util.List<Student> effectiveStudents = (students != null && !students.isEmpty())
                ? students
                : java.util.List.of(createSampleStudent(effectiveSchool));

        String frontJson = version != null ? version.getFrontDesignJson() : null;
        String backJson = version != null ? version.getBackDesignJson() : null;

        try (PDDocument document = new PDDocument()) {
            for (Student student : effectiveStudents) {
                School studentSchool = student.getSchool() != null ? student.getSchool() : effectiveSchool;

                BufferedImage frontImg = renderCardToBufferedImage(template, version, student, studentSchool, effectiveSession, true);
                BufferedImage backImg = renderCardToBufferedImage(template, version, student, studentSchool, effectiveSession, false);

                // Front
                PDPage frontPage = new PDPage(pageSize);
                document.addPage(frontPage);
                try (PDPageContentStream stream = new PDPageContentStream(document, frontPage)) {
                    PDImageXObject pdFront = createCardPdfImage(document, frontImg);
                    stream.drawImage(pdFront, 0, 0, widthPt, heightPt);
                    addInvisibleTextLayer(stream, frontJson, true, student, studentSchool, effectiveSession, widthPt, heightPt);
                }

                // Back
                PDPage backPage = new PDPage(pageSize);
                document.addPage(backPage);
                try (PDPageContentStream stream = new PDPageContentStream(document, backPage)) {
                    PDImageXObject pdBack = createCardPdfImage(document, backImg);
                    stream.drawImage(pdBack, 0, 0, widthPt, heightPt);
                    addInvisibleTextLayer(stream, backJson, false, student, studentSchool, effectiveSession, widthPt, heightPt);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render batch PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Batch card PDF rendering failed: " + e.getMessage(), e);
        }
    }

    /**
     * Renders a fully customizable prepress imposition PDF.
     * Supports:
     * - PLASTIC_CARD mode: 1-by-1 direct PVC card pages (CR80 size 54x85.6mm)
     * - SHEET mode: Multi-card imposition grid on A4, A3, or Letter sheets
     * - Configurable columns, rows, horizontal gap (mm), vertical gap (mm), margins, auto-centering
     * - Side selection: DUPLEX (front + back mirrored), FRONT_ONLY, BACK_ONLY
     * - Crop marks and cut guides toggle
     */
    public byte[] renderCustomImpositionPdf(
            CardTemplate template,
            TemplateVersion version,
            java.util.List<Student> students,
            School school,
            String academicSession,
            PrintSettingsDto settings
    ) {
        School effectiveSchool = getEffectiveSchool(school, template, null);
        String effectiveSession = academicSession != null ? academicSession : "2026-2027";
        java.util.List<Student> effectiveStudents = (students != null && !students.isEmpty())
                ? students
                : java.util.List.of(createSampleStudent(effectiveSchool));

        float cardWidthMm = version != null && version.getCardWidthMm() != null ? version.getCardWidthMm().floatValue() : 54.00f;
        float cardHeightMm = version != null && version.getCardHeightMm() != null ? version.getCardHeightMm().floatValue() : 85.60f;
        boolean isPortrait = cardHeightMm >= cardWidthMm;

        PrintSettingsDto cfg = settings != null ? settings.sanitize(isPortrait) : PrintSettingsDto.defaultSettings(isPortrait);

        float cardWidthPt = cardWidthMm * POINTS_PER_MM;
        float cardHeightPt = cardHeightMm * POINTS_PER_MM;
        String schoolName = effectiveSchool.getSchoolName() != null ? effectiveSchool.getSchoolName() : "School";

        // MODE: PLASTIC_CARD (CR80 direct PVC card printer, 1 card per page)
        if (PrintSettingsDto.MODE_PLASTIC.equalsIgnoreCase(cfg.printMode())
                || PrintSettingsDto.PAPER_CR80.equalsIgnoreCase(cfg.paperSize())) {
            PDRectangle cr80PageSize = new PDRectangle(cardWidthPt, cardHeightPt);
            try (PDDocument document = new PDDocument()) {
                for (Student student : effectiveStudents) {
                    School studentSchool = student.getSchool() != null ? student.getSchool() : effectiveSchool;

                    if (!PrintSettingsDto.SIDES_BACK_ONLY.equalsIgnoreCase(cfg.sides())) {
                        PDPage frontPage = new PDPage(cr80PageSize);
                        document.addPage(frontPage);
                        try (PDPageContentStream stream = new PDPageContentStream(document, frontPage)) {
                            BufferedImage frontImg = renderCardToBufferedImage(template, version, student, studentSchool, effectiveSession, true);
                            PDImageXObject pdFront = createCardPdfImage(document, frontImg);
                            stream.drawImage(pdFront, 0, 0, cardWidthPt, cardHeightPt);
                            addInvisibleTextLayer(stream, version != null ? version.getFrontDesignJson() : null, true, student, studentSchool, effectiveSession, cardWidthPt, cardHeightPt);
                        }
                    }

                    if (!PrintSettingsDto.SIDES_FRONT_ONLY.equalsIgnoreCase(cfg.sides())) {
                        PDPage backPage = new PDPage(cr80PageSize);
                        document.addPage(backPage);
                        try (PDPageContentStream stream = new PDPageContentStream(document, backPage)) {
                            BufferedImage backImg = renderCardToBufferedImage(template, version, student, studentSchool, effectiveSession, false);
                            PDImageXObject pdBack = createCardPdfImage(document, backImg);
                            stream.drawImage(pdBack, 0, 0, cardWidthPt, cardHeightPt);
                            addInvisibleTextLayer(stream, version != null ? version.getBackDesignJson() : null, false, student, studentSchool, effectiveSession, cardWidthPt, cardHeightPt);
                        }
                    }
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                document.save(out);
                return out.toByteArray();
            } catch (Exception e) {
                log.error("Failed to render plastic card PDF: {}", e.getMessage(), e);
                throw new RuntimeException("Plastic card PDF rendering failed: " + e.getMessage(), e);
            }
        }

        // MODE: SHEET (Imposition grid on A4, A3, Letter)
        PDRectangle sheetSize = PDRectangle.A4;
        if (PrintSettingsDto.PAPER_A3.equalsIgnoreCase(cfg.paperSize())) {
            sheetSize = PDRectangle.A3;
        } else if (PrintSettingsDto.PAPER_LETTER.equalsIgnoreCase(cfg.paperSize())) {
            sheetSize = PDRectangle.LETTER;
        }

        float sheetWidthPt = sheetSize.getWidth();
        float sheetHeightPt = sheetSize.getHeight();

        int numCols = cfg.columns() != null && cfg.columns() > 0 ? cfg.columns() : (isPortrait ? 3 : 2);
        int numRows = cfg.rows() != null && cfg.rows() > 0 ? cfg.rows() : (isPortrait ? 3 : 4);

        float hGapPt = (float) (cfg.horizontalSpacingMm() != null ? cfg.horizontalSpacingMm() * POINTS_PER_MM : 0f);
        float vGapPt = (float) (cfg.verticalSpacingMm() != null ? cfg.verticalSpacingMm() * POINTS_PER_MM : 0f);

        float gridWidthPt = (numCols * cardWidthPt) + (Math.max(0, numCols - 1) * hGapPt);
        float gridHeightPt = (numRows * cardHeightPt) + (Math.max(0, numRows - 1) * vGapPt);

        float marginXPt;
        float marginYPt;
        if (Boolean.TRUE.equals(cfg.autoCenter())) {
            marginXPt = Math.max(8f, (sheetWidthPt - gridWidthPt) / 2f);
            marginYPt = Math.max(8f, (sheetHeightPt - gridHeightPt) / 2f);
        } else {
            marginXPt = (float) (cfg.marginLeftMm() != null ? cfg.marginLeftMm() * POINTS_PER_MM : 10f * POINTS_PER_MM);
            marginYPt = (float) (cfg.marginBottomMm() != null ? cfg.marginBottomMm() * POINTS_PER_MM : 10f * POINTS_PER_MM);
        }

        int cardsPerPage = numCols * numRows;
        int totalStudents = effectiveStudents.size();
        int totalSheets = (int) Math.ceil((double) totalStudents / cardsPerPage);
        if (totalSheets == 0) totalSheets = 1;

        boolean renderFront = !PrintSettingsDto.SIDES_BACK_ONLY.equalsIgnoreCase(cfg.sides());
        boolean renderBack = !PrintSettingsDto.SIDES_FRONT_ONLY.equalsIgnoreCase(cfg.sides());
        boolean isDuplex = PrintSettingsDto.SIDES_DUPLEX.equalsIgnoreCase(cfg.sides());

        try (PDDocument document = new PDDocument()) {
            for (int chunkIndex = 0; chunkIndex < totalSheets; chunkIndex++) {
                int start = chunkIndex * cardsPerPage;
                int end = Math.min(start + cardsPerPage, totalStudents);
                java.util.List<Student> chunkStudents = effectiveStudents.subList(start, end);

                // --- Front Side Sheet ---
                if (renderFront) {
                    PDPage frontPage = new PDPage(sheetSize);
                    document.addPage(frontPage);
                    try (PDPageContentStream stream = new PDPageContentStream(document, frontPage)) {
                        renderSheetMarginHeader(stream, schoolName, effectiveSession, chunkIndex + 1, totalSheets, "FRONT", marginXPt, sheetHeightPt - (marginYPt * 0.45f));

                        for (int i = 0; i < chunkStudents.size(); i++) {
                            Student student = chunkStudents.get(i);
                            School studentSchool = student.getSchool() != null ? student.getSchool() : effectiveSchool;

                            int col = i % numCols;
                            int row = i / numCols;
                            float slotX = marginXPt + (col * (cardWidthPt + hGapPt));
                            float slotY = sheetHeightPt - marginYPt - cardHeightPt - (row * (cardHeightPt + vGapPt));

                            if (Boolean.TRUE.equals(cfg.showCropMarks()) || Boolean.TRUE.equals(cfg.showCutGuides())) {
                                drawCropMarks(stream, slotX, slotY, cardWidthPt, cardHeightPt);
                            }

                            BufferedImage cardImg = renderCardToBufferedImage(template, version, student, studentSchool, effectiveSession, true);
                            PDImageXObject pdCard = createCardPdfImage(document, cardImg);
                            stream.drawImage(pdCard, slotX, slotY, cardWidthPt, cardHeightPt);

                            stream.saveGraphicsState();
                            stream.transform(Matrix.getTranslateInstance(slotX, slotY));
                            addInvisibleTextLayer(stream, version != null ? version.getFrontDesignJson() : null, true, student, studentSchool, effectiveSession, cardWidthPt, cardHeightPt);
                            stream.restoreGraphicsState();
                        }
                    }
                }

                // --- Back Side Sheet ---
                if (renderBack) {
                    PDPage backPage = new PDPage(sheetSize);
                    document.addPage(backPage);
                    try (PDPageContentStream stream = new PDPageContentStream(document, backPage)) {
                        String sideLabel = isDuplex ? "BACK (DUPLEX MIRRORED)" : "BACK";
                        renderSheetMarginHeader(stream, schoolName, effectiveSession, chunkIndex + 1, totalSheets, sideLabel, marginXPt, sheetHeightPt - (marginYPt * 0.45f));

                        for (int i = 0; i < chunkStudents.size(); i++) {
                            Student student = chunkStudents.get(i);
                            School studentSchool = student.getSchool() != null ? student.getSchool() : effectiveSchool;

                            int col = i % numCols;
                            int row = i / numCols;
                            int backCol = isDuplex ? ((numCols - 1) - col) : col;
                            int backRow = row;

                            float slotX = marginXPt + (backCol * (cardWidthPt + hGapPt));
                            float slotY = sheetHeightPt - marginYPt - cardHeightPt - (backRow * (cardHeightPt + vGapPt));

                            if (Boolean.TRUE.equals(cfg.showCropMarks()) || Boolean.TRUE.equals(cfg.showCutGuides())) {
                                drawCropMarks(stream, slotX, slotY, cardWidthPt, cardHeightPt);
                            }

                            BufferedImage backCardImg = renderCardToBufferedImage(template, version, student, studentSchool, effectiveSession, false);
                            PDImageXObject pdBackCard = createCardPdfImage(document, backCardImg);
                            stream.drawImage(pdBackCard, slotX, slotY, cardWidthPt, cardHeightPt);

                            stream.saveGraphicsState();
                            stream.transform(Matrix.getTranslateInstance(slotX, slotY));
                            addInvisibleTextLayer(stream, version != null ? version.getBackDesignJson() : null, false, student, studentSchool, effectiveSession, cardWidthPt, cardHeightPt);
                            stream.restoreGraphicsState();
                        }
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render custom imposition sheet PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Imposition sheet PDF rendering failed: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy/Default A4 Imposition Sheet PDF generator delegating to renderCustomImpositionPdf.
     */
    public byte[] renderA4SheetPdf(
            CardTemplate template,
            TemplateVersion version,
            java.util.List<Student> students,
            School school,
            String academicSession
    ) {
        float cardWidthMm = version != null && version.getCardWidthMm() != null ? version.getCardWidthMm().floatValue() : 54.00f;
        float cardHeightMm = version != null && version.getCardHeightMm() != null ? version.getCardHeightMm().floatValue() : 85.60f;
        boolean isPortrait = cardHeightMm >= cardWidthMm;
        int numCols = isPortrait ? 3 : 2;
        int numRows = isPortrait ? 3 : 5;
        if (!isPortrait && (numRows * cardHeightMm * POINTS_PER_MM > PDRectangle.A4.getHeight())) {
            numRows = 4;
        }
        PrintSettingsDto settings = new PrintSettingsDto(
                PrintSettingsDto.MODE_SHEET,
                PrintSettingsDto.PAPER_A4,
                PrintSettingsDto.SIDES_DUPLEX,
                numCols,
                numRows,
                0.0,
                0.0,
                10.0,
                10.0,
                10.0,
                10.0,
                true,
                true,
                true,
                false
        );
        return renderCustomImpositionPdf(template, version, students, school, academicSession, settings);
    }

    /**
     * Renders a customizable prepress imposition sheet as a 300 DPI high-resolution JPEG.
     */
    public byte[] renderCustomImpositionImage(
            CardTemplate template,
            TemplateVersion version,
            java.util.List<Student> students,
            School school,
            String academicSession,
            String side,
            PrintSettingsDto settings
    ) {
        boolean isFront = !"back".equalsIgnoreCase(side);
        School effectiveSchool = getEffectiveSchool(school, template, null);
        String effectiveSession = academicSession != null ? academicSession : "2026-2027";
        java.util.List<Student> effectiveStudents = (students != null && !students.isEmpty())
                ? students
                : java.util.List.of(createSampleStudent(effectiveSchool));

        float cardWidthMm = version != null && version.getCardWidthMm() != null ? version.getCardWidthMm().floatValue() : 54.00f;
        float cardHeightMm = version != null && version.getCardHeightMm() != null ? version.getCardHeightMm().floatValue() : 85.60f;
        boolean isPortrait = cardHeightMm >= cardWidthMm;

        PrintSettingsDto cfg = settings != null ? settings.sanitize(isPortrait) : PrintSettingsDto.defaultSettings(isPortrait);

        float scale = 300.0f / 25.4f;

        // MODE: PLASTIC_CARD (Single card 300 DPI image)
        if (PrintSettingsDto.MODE_PLASTIC.equalsIgnoreCase(cfg.printMode())
                || PrintSettingsDto.PAPER_CR80.equalsIgnoreCase(cfg.paperSize())) {
            Student firstStudent = effectiveStudents.get(0);
            BufferedImage cardImg = renderCardToBufferedImage(template, version, firstStudent, effectiveSchool, effectiveSession, isFront);
            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                ImageIO.write(cardImg, "jpg", out);
                return out.toByteArray();
            } catch (Exception e) {
                throw new RuntimeException("Plastic card JPEG export failed: " + e.getMessage(), e);
            }
        }

        // MODE: SHEET (A4, A3, Letter)
        int sheetWidthMm = 210;
        int sheetHeightMm = 297;
        if (PrintSettingsDto.PAPER_A3.equalsIgnoreCase(cfg.paperSize())) {
            sheetWidthMm = 297;
            sheetHeightMm = 420;
        } else if (PrintSettingsDto.PAPER_LETTER.equalsIgnoreCase(cfg.paperSize())) {
            sheetWidthMm = 216;
            sheetHeightMm = 279;
        }

        int sheetWidthPx = Math.round(sheetWidthMm * scale);
        int sheetHeightPx = Math.round(sheetHeightMm * scale);

        int cardWidthPx = Math.round(cardWidthMm * scale);
        int cardHeightPx = Math.round(cardHeightMm * scale);

        int numCols = cfg.columns() != null && cfg.columns() > 0 ? cfg.columns() : (isPortrait ? 3 : 2);
        int numRows = cfg.rows() != null && cfg.rows() > 0 ? cfg.rows() : (isPortrait ? 3 : 4);

        int hGapPx = Math.round((float) (cfg.horizontalSpacingMm() != null ? cfg.horizontalSpacingMm() * scale : 0f));
        int vGapPx = Math.round((float) (cfg.verticalSpacingMm() != null ? cfg.verticalSpacingMm() * scale : 0f));

        int gridWidthPx = (numCols * cardWidthPx) + (Math.max(0, numCols - 1) * hGapPx);
        int gridHeightPx = (numRows * cardHeightPx) + (Math.max(0, numRows - 1) * vGapPx);

        int marginXPx;
        int marginYPx;
        if (Boolean.TRUE.equals(cfg.autoCenter())) {
            marginXPx = Math.max(0, (sheetWidthPx - gridWidthPx) / 2);
            marginYPx = Math.max(0, (sheetHeightPx - gridHeightPx) / 2);
        } else {
            marginXPx = Math.round((float) (cfg.marginLeftMm() != null ? cfg.marginLeftMm() * scale : 10f * scale));
            marginYPx = Math.round((float) (cfg.marginTopMm() != null ? cfg.marginTopMm() * scale : 10f * scale));
        }

        BufferedImage sheetImg = new BufferedImage(sheetWidthPx, sheetHeightPx, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = sheetImg.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, sheetWidthPx, sheetHeightPx);

        String schoolName = effectiveSchool.getSchoolName() != null ? effectiveSchool.getSchoolName() : "School";
        boolean isDuplex = PrintSettingsDto.SIDES_DUPLEX.equalsIgnoreCase(cfg.sides());
        String sideTitle = isFront ? "FRONT" : (isDuplex ? "BACK (DUPLEX MIRRORED)" : "BACK");
        String headerText = schoolName + "  |  Session: " + effectiveSession + "  |  [" + cfg.paperSize() + " IMPOSITION SHEET - " + sideTitle + "]";
        g2.setColor(new Color(100, 116, 139));
        g2.setFont(new Font("SansSerif", Font.BOLD, 26));
        g2.drawString(headerText, Math.max(40, marginXPx), Math.max(40, marginYPx / 2 + 10));

        int cardsPerPage = numCols * numRows;
        int studentsToDraw = Math.min(effectiveStudents.size(), cardsPerPage);

        for (int i = 0; i < studentsToDraw; i++) {
            Student student = effectiveStudents.get(i);
            School studentSchool = student.getSchool() != null ? student.getSchool() : effectiveSchool;

            int col = i % numCols;
            int row = i / numCols;
            if (!isFront && isDuplex) {
                col = (numCols - 1) - col; // Mirror column for duplex alignment
            }

            int slotX = marginXPx + (col * (cardWidthPx + hGapPx));
            int slotY = marginYPx + (row * (cardHeightPx + vGapPx));

            if (Boolean.TRUE.equals(cfg.showCropMarks()) || Boolean.TRUE.equals(cfg.showCutGuides())) {
                drawCropMarksJava2D(g2, slotX, slotY, cardWidthPx, cardHeightPx);
            }

            BufferedImage cardImg = renderCardToBufferedImage(template, version, student, studentSchool, effectiveSession, isFront);
            g2.drawImage(cardImg, slotX, slotY, cardWidthPx, cardHeightPx, null);
        }

        g2.dispose();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(sheetImg, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render imposition sheet JPEG: {}", e.getMessage(), e);
            throw new RuntimeException("Imposition sheet JPEG export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Legacy/Default A4 Imposition Sheet JPEG generator delegating to renderCustomImpositionImage.
     */
    public byte[] renderA4SheetImage(
            CardTemplate template,
            TemplateVersion version,
            java.util.List<Student> students,
            School school,
            String academicSession,
            String side
    ) {
        float cardWidthMm = version != null && version.getCardWidthMm() != null ? version.getCardWidthMm().floatValue() : 54.00f;
        float cardHeightMm = version != null && version.getCardHeightMm() != null ? version.getCardHeightMm().floatValue() : 85.60f;
        boolean isPortrait = cardHeightMm >= cardWidthMm;
        int numCols = isPortrait ? 3 : 2;
        int numRows = isPortrait ? 3 : 5;
        if (!isPortrait && (numRows * cardHeightMm * POINTS_PER_MM > PDRectangle.A4.getHeight())) {
            numRows = 4;
        }
        PrintSettingsDto settings = new PrintSettingsDto(
                PrintSettingsDto.MODE_SHEET,
                PrintSettingsDto.PAPER_A4,
                PrintSettingsDto.SIDES_DUPLEX,
                numCols,
                numRows,
                0.0,
                0.0,
                10.0,
                10.0,
                10.0,
                10.0,
                true,
                true,
                true,
                false
        );
        return renderCustomImpositionImage(template, version, students, school, academicSession, side, settings);
    }

    /**
     * Bundles 300 DPI high-resolution JPEG images of each student's card (front and back) into a ZIP file.
     */
    public byte[] renderJobImagesZip(
            CardTemplate template,
            TemplateVersion version,
            java.util.List<Student> students,
            School school,
            String academicSession
    ) {
        School effectiveSchool = getEffectiveSchool(school, template, null);
        String effectiveSession = academicSession != null ? academicSession : "2026-2027";
        java.util.List<Student> effectiveStudents = (students != null && !students.isEmpty())
                ? students
                : java.util.List.of(createSampleStudent(effectiveSchool));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos)) {

            Set<String> addedEntries = new java.util.HashSet<>();
            int idx = 1;

            for (Student student : effectiveStudents) {
                School studentSchool = student.getSchool() != null ? student.getSchool() : effectiveSchool;

                BufferedImage frontImg = renderCardToBufferedImage(template, version, student, studentSchool, effectiveSession, true);
                ByteArrayOutputStream frontOut = new ByteArrayOutputStream();
                ImageIO.write(frontImg, "jpg", frontOut);
                byte[] frontBytes = frontOut.toByteArray();

                BufferedImage backImg = renderCardToBufferedImage(template, version, student, studentSchool, effectiveSession, false);
                ByteArrayOutputStream backOut = new ByteArrayOutputStream();
                ImageIO.write(backImg, "jpg", backOut);
                byte[] backBytes = backOut.toByteArray();

                String rawAdm = (student.getAdmissionNumber() != null && !student.getAdmissionNumber().isBlank())
                        ? student.getAdmissionNumber().trim().replaceAll("[\\\\/:*?\"<>|\\s]", "_")
                        : ("student_" + idx);

                String baseName = rawAdm;
                int suffix = 1;
                while (addedEntries.contains(baseName + "_front.jpg")) {
                    baseName = rawAdm + "_" + suffix++;
                }

                String frontEntryName = baseName + "_front.jpg";
                String backEntryName = baseName + "_back.jpg";
                addedEntries.add(frontEntryName);
                addedEntries.add(backEntryName);

                ZipEntry frontEntry = new ZipEntry(frontEntryName);
                zos.putNextEntry(frontEntry);
                zos.write(frontBytes);
                zos.closeEntry();

                ZipEntry backEntry = new ZipEntry(backEntryName);
                zos.putNextEntry(backEntry);
                zos.write(backBytes);
                zos.closeEntry();

                idx++;
            }

            zos.finish();
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render job images zip: {}", e.getMessage(), e);
            throw new RuntimeException("Job images ZIP export failed: " + e.getMessage(), e);
        }
    }

    /**
     * Renders a single student card side (front or back) as high-resolution 300 DPI JPEG bytes.
     */
    public byte[] renderSingleStudentImage(
            CardTemplate template,
            TemplateVersion version,
            Student student,
            School school,
            String academicSession,
            String side
    ) {
        boolean isFront = !"back".equalsIgnoreCase(side);
        Student effectiveStudent = student != null ? student : createSampleStudent(school);
        School effectiveSchool = getEffectiveSchool(school, template, effectiveStudent);
        String effectiveSession = academicSession != null ? academicSession : "2026-2027";

        BufferedImage img = renderCardToBufferedImage(template, version, effectiveStudent, effectiveSchool, effectiveSession, isFront);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            ImageIO.write(img, "jpg", out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render single student image: {}", e.getMessage(), e);
            throw new RuntimeException("Single student card image rendering failed: " + e.getMessage(), e);
        }
    }

    private void addInvisibleTextLayer(
            PDPageContentStream stream,
            String designJson,
            boolean isFront,
            Student student,
            School school,
            String academicSession,
            float widthPt,
            float heightPt
    ) {
        try {
            JsonNode elementsNode = extractElementsNode(designJson, isFront);
            stream.saveGraphicsState();
            stream.setRenderingMode(RenderingMode.NEITHER);

            if (elementsNode == null || !elementsNode.isArray() || elementsNode.size() == 0) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f);
                stream.newLineAtOffset(10f, heightPt - 20f);
                if (isFront) {
                    String schoolName = val(school != null ? school.getSchoolName() : null, "Greenwood International School");
                    String studentName = val(student != null ? student.getStudentName() : null, "Aarav Sharma");
                    String adm = val(student != null ? student.getAdmissionNumber() : null, "ADM-2026-001");
                    String cls = val(student != null ? student.getClassName() : null, "10th");
                    String sec = val(student != null ? student.getSection() : null, "A");
                    stream.showText(sanitizeForPdf(schoolName + " " + studentName + " " + adm + " " + cls + " " + sec));
                } else {
                    String instructions = "INSTRUCTIONS & EMERGENCY CONTACT";
                    String father = val(student != null ? student.getFatherName() : null, "Rajesh Sharma");
                    String address = val(school != null ? school.getAddress() : null, "14 Heritage Boulevard");
                    stream.showText(sanitizeForPdf(instructions + " " + father + " " + address));
                }
                stream.endText();
                stream.restoreGraphicsState();
                return;
            }

            for (JsonNode el : elementsNode) {
                String type = el.hasNonNull("type") ? el.get("type").asText().toLowerCase() : "text";
                if (!"text".equals(type)) continue;

                String rawText = getProperty(el, "content", "text", "value", "label", "");
                String field = getProperty(el, "field", "");
                String label = getProperty(el, "label", "");

                String resolved;
                if (!rawText.isBlank()) {
                    resolved = interpolatePlaceholders(rawText, student, school, academicSession);
                } else if (!field.isBlank()) {
                    String val = interpolateValue(field, student, school, academicSession);
                    resolved = label.isBlank() ? val : (label + " " + val);
                } else {
                    resolved = label;
                }
                if (resolved.isBlank()) continue;

                float xMm = el.hasNonNull("x") ? (float) el.get("x").asDouble() : 0f;
                float yMm = el.hasNonNull("y") ? (float) el.get("y").asDouble() : 0f;
                float xPt = xMm * POINTS_PER_MM;
                float yPt = heightPt - (yMm * POINTS_PER_MM) - 10f;

                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f);
                stream.newLineAtOffset(Math.max(0, xPt), Math.max(0, yPt));
                stream.showText(sanitizeForPdf(resolved.replace("\n", " ")));
                stream.endText();
            }

            stream.restoreGraphicsState();
        } catch (Exception e) {
            log.debug("Invisible text layer skipped: {}", e.getMessage());
        }
    }

    private void drawCropMarksJava2D(Graphics2D g2, int x, int y, int w, int h) {
        Stroke oldStroke = g2.getStroke();
        Color oldColor = g2.getColor();

        // 1. Subtle dashed border around card bounding box
        g2.setColor(new Color(203, 213, 225)); // Slate-300
        float[] dash = {6f, 6f};
        g2.setStroke(new BasicStroke(1.5f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
        g2.drawRect(x, y, w, h);

        // 2. Corner cutting tick marks
        g2.setColor(new Color(148, 163, 184)); // Slate-400
        g2.setStroke(new BasicStroke(2f));
        int markLen = 25; // ~2mm at 300 DPI
        int gap = 8;

        // Top-Left
        g2.drawLine(x - gap - markLen, y, x - gap, y);
        g2.drawLine(x, y - gap - markLen, x, y - gap);

        // Top-Right
        g2.drawLine(x + w + gap, y, x + w + gap + markLen, y);
        g2.drawLine(x + w, y - gap - markLen, x + w, y - gap);

        // Bottom-Left
        g2.drawLine(x - gap - markLen, y + h, x - gap, y + h);
        g2.drawLine(x, y + h + gap, x, y + h + gap + markLen);

        // Bottom-Right
        g2.drawLine(x + w + gap, y + h, x + w + gap + markLen, y + h);
        g2.drawLine(x + w, y + h + gap, x + w, y + h + gap + markLen);

        g2.setStroke(oldStroke);
        g2.setColor(oldColor);
    }

    private void renderElementToJava2D(
            Graphics2D g2,
            JsonNode element,
            Student student,
            School school,
            String academicSession,
            float cardWidthMm,
            float cardHeightMm,
            float scale
    ) {
        if (element == null) return;
        String type = element.hasNonNull("type") ? element.get("type").asText().toLowerCase() : "text";

        float xMm = element.hasNonNull("x")
                ? (float) element.get("x").asDouble()
                : (element.hasNonNull("left") ? (float) element.get("left").asDouble() : 0f);
        float yMm = element.hasNonNull("y")
                ? (float) element.get("y").asDouble()
                : (element.hasNonNull("top") ? (float) element.get("top").asDouble() : 0f);
        float widthMm = element.hasNonNull("width") ? (float) element.get("width").asDouble() : 20f;
        float heightMm = element.hasNonNull("height") ? (float) element.get("height").asDouble() : 20f;

        // ADAPTIVE AUTO-FIT: Clamp element width and position if it overflows the card canvas boundary
        if (cardWidthMm < cardHeightMm) { // Portrait card mode (e.g. 54 x 85.6 mm)
            if (!"rect".equalsIgnoreCase(type) && !"shape".equalsIgnoreCase(type) && !"banner".equalsIgnoreCase(type)) {
                if (xMm + widthMm > cardWidthMm - 1f) {
                    widthMm = Math.max(10f, cardWidthMm - xMm - 2f);
                }
                if (xMm >= cardWidthMm - 8f) {
                    xMm = Math.max(2f, cardWidthMm - widthMm - 4f);
                }
            } else {
                if (widthMm > cardWidthMm) {
                    widthMm = cardWidthMm;
                }
            }
        }

        // GHOST ELEMENT FILTER: Filter out elements outside card boundaries or invalid sizes
        if (xMm >= cardWidthMm || yMm >= cardHeightMm || widthMm <= 0 || heightMm <= 0) {
            return;
        }

        int elX = Math.round(xMm * scale);
        int elY = Math.round(yMm * scale);
        int elW = Math.round(widthMm * scale);
        int elH = Math.round(heightMm * scale);

        Shape oldClip = g2.getClip();
        if ("text".equalsIgnoreCase(type) || "barcode".equalsIgnoreCase(type)) {
            g2.clipRect(elX, Math.max(0, elY - 2), elW + 4, elH + 4);
        } else {
            g2.clipRect(elX, elY, elW, elH);
        }

        try {
            switch (type) {
                case "shape", "rect", "rectangle", "banner", "line", "circle", "oval" -> {
                    renderShapeJava2D(g2, element, type, elX, elY, elW, elH, scale);
                }
                case "signature" -> {
                    renderSignatureJava2D(g2, element, school, elX, elY, elW, elH, scale);
                }
                case "image", "photo" -> {
                    String field = getProperty(element, "field", "name", "src", "student_photo").toLowerCase();
                    String label = getProperty(element, "label", "").toLowerCase();
                    if (field.contains("signature") || field.contains("sign") || label.contains("sign")) {
                        renderSignatureJava2D(g2, element, school, elX, elY, elW, elH, scale);
                    } else if (field.contains("logo") || field.contains("school") || label.contains("logo")) {
                        renderSchoolLogoJava2D(g2, element, school, elX, elY, elW, elH, scale);
                    } else {
                        renderStudentPhotoJava2D(g2, element, student, elX, elY, elW, elH, scale);
                    }
                }
                case "qrcode", "qr" -> {
                    renderQrCodeJava2D(g2, element, student, school, academicSession, elX, elY, elW, elH);
                }
                case "barcode", "bar" -> {
                    renderBarcodeJava2D(g2, element, student, school, academicSession, elX, elY, elW, elH);
                }
                case "background" -> {
                    // Handled at canvas level
                }
                default -> { // Text
                    renderTextJava2D(g2, element, student, school, academicSession, elX, elY, elW, elH, scale);
                }
            }
        } finally {
            g2.setClip(oldClip);
        }
    }

    private void renderShapeJava2D(
            Graphics2D g2,
            JsonNode element,
            String type,
            int elX,
            int elY,
            int elW,
            int elH,
            float scale
    ) {
        boolean isCircle = "circle".equalsIgnoreCase(type) || "oval".equalsIgnoreCase(type);
        boolean isLine = "line".equalsIgnoreCase(type) || elH <= 2;

        float borderRadiusMm = element.hasNonNull("borderRadius")
                ? (float) element.get("borderRadius").asDouble()
                : (element.hasNonNull("radius") ? (float) element.get("radius").asDouble() : 0f);

        String fillStr = getProperty(element, "fillColor", "backgroundColor", "color", "fill", "");
        Color fillColor = (!fillStr.isBlank() && !fillStr.equalsIgnoreCase("transparent") && !fillStr.equalsIgnoreCase("none"))
                ? parseColor(fillStr, null)
                : null;

        String borderStr = getProperty(element, "borderColor", "strokeColor", "stroke", "");
        Color borderColor = (!borderStr.isBlank() && !borderStr.equalsIgnoreCase("transparent") && !borderStr.equalsIgnoreCase("none"))
                ? parseColor(borderStr, null)
                : null;

        float borderWidthMm = element.hasNonNull("borderWidth")
                ? (float) element.get("borderWidth").asDouble()
                : (element.hasNonNull("strokeWidth") ? (float) element.get("strokeWidth").asDouble() : 0f);
        int borderWidthPx = Math.round(borderWidthMm * (scale / 4.0f));

        if (fillColor == null && borderColor == null) {
            fillColor = new Color(30, 58, 138); // Default navy
        }

        if (isLine) {
            Color strokeCol = borderColor != null ? borderColor : (fillColor != null ? fillColor : Color.BLACK);
            g2.setColor(strokeCol);
            g2.setStroke(new BasicStroke(Math.max(1f, borderWidthPx)));
            g2.drawLine(elX, elY + elH / 2, elX + elW, elY + elH / 2);
            return;
        }

        if (isCircle) {
            if (fillColor != null) {
                g2.setColor(fillColor);
                g2.fillOval(elX, elY, elW, elH);
            }
            if (borderColor != null && borderWidthPx > 0) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(borderWidthPx));
                g2.drawOval(elX, elY, elW, elH);
            }
        } else if (borderRadiusMm > 0) {
            int rPx = Math.round(borderRadiusMm * scale);
            if (fillColor != null) {
                g2.setColor(fillColor);
                g2.fillRoundRect(elX, elY, elW, elH, rPx * 2, rPx * 2);
            }
            if (borderColor != null && borderWidthPx > 0) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(borderWidthPx));
                g2.drawRoundRect(elX, elY, elW, elH, rPx * 2, rPx * 2);
            }
        } else {
            if (fillColor != null) {
                g2.setColor(fillColor);
                g2.fillRect(elX, elY, elW, elH);
            }
            if (borderColor != null && borderWidthPx > 0) {
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(borderWidthPx));
                g2.drawRect(elX, elY, elW, elH);
            }
        }
    }

    private void renderStudentPhotoJava2D(
            Graphics2D g2,
            JsonNode element,
            Student student,
            int elX,
            int elY,
            int elW,
            int elH,
            float scale
    ) {
        BufferedImage photo = loadStudentPhoto(student);
        if (photo == null) return;

        String shape = getProperty(element, "shape", "photoShape", "photo_shape", "");
        float borderRadiusMm = element.hasNonNull("borderRadius") ? (float) element.get("borderRadius").asDouble() : 0f;
        if (shape.isBlank()) {
            shape = (borderRadiusMm > 10f) ? "circle" : (borderRadiusMm > 0 ? "rounded" : "square");
        }

        String borderStr = getProperty(element, "borderColor", "strokeColor", "");
        Color borderColor = (!borderStr.isBlank() && !borderStr.equalsIgnoreCase("transparent") && !borderStr.equalsIgnoreCase("none"))
                ? parseColor(borderStr, new Color(203, 213, 225))
                : null;
        float borderWidthMm = element.hasNonNull("borderWidth") ? (float) element.get("borderWidth").asDouble() : 0f;
        int borderPx = Math.max(0, Math.round(borderWidthMm * (scale / 4.0f)));

        Shape clipShape;
        if ("circle".equalsIgnoreCase(shape) || "round".equalsIgnoreCase(shape)) {
            clipShape = new Ellipse2D.Float(elX, elY, elW, elH);
        } else if ("pill".equalsIgnoreCase(shape)) {
            clipShape = new RoundRectangle2D.Float(elX, elY, elW, elH, elW, elW);
        } else if ("rounded".equalsIgnoreCase(shape) || borderRadiusMm > 0) {
            int rPx = Math.round(Math.max(2f, borderRadiusMm) * scale);
            clipShape = new RoundRectangle2D.Float(elX, elY, elW, elH, rPx * 2, rPx * 2);
        } else {
            clipShape = new Rectangle2D.Float(elX, elY, elW, elH);
        }

        Shape oldClip = g2.getClip();
        g2.clip(clipShape);

        // Maintain aspect ratio cover
        double srcAspect = (double) photo.getWidth() / photo.getHeight();
        double dstAspect = (double) elW / elH;
        int drawW, drawH, drawX, drawY;
        if (srcAspect > dstAspect) {
            drawH = elH;
            drawW = (int) Math.round(elH * srcAspect);
            drawX = elX - (drawW - elW) / 2;
            drawY = elY;
        } else {
            drawW = elW;
            drawH = (int) Math.round(elW / srcAspect);
            drawX = elX;
            drawY = elY - (drawH - elH) / 2;
        }
        g2.drawImage(photo, drawX, drawY, drawW, drawH, null);

        g2.setClip(oldClip);

        // Draw photo border
        if (borderColor != null && borderPx > 0) {
            g2.setColor(borderColor);
            g2.setStroke(new BasicStroke(borderPx));
            if ("circle".equalsIgnoreCase(shape) || "round".equalsIgnoreCase(shape)) {
                g2.draw(new Ellipse2D.Float(elX + borderPx / 2f, elY + borderPx / 2f, elW - borderPx, elH - borderPx));
            } else if ("pill".equalsIgnoreCase(shape)) {
                g2.draw(new RoundRectangle2D.Float(elX + borderPx / 2f, elY + borderPx / 2f, elW - borderPx, elH - borderPx, elW, elW));
            } else if ("rounded".equalsIgnoreCase(shape) || borderRadiusMm > 0) {
                int rPx = Math.round(Math.max(2f, borderRadiusMm) * scale);
                g2.draw(new RoundRectangle2D.Float(elX + borderPx / 2f, elY + borderPx / 2f, elW - borderPx, elH - borderPx, rPx * 2, rPx * 2));
            } else {
                g2.drawRect(elX, elY, elW, elH);
            }
        }
    }

    private void renderSchoolLogoJava2D(
            Graphics2D g2,
            JsonNode element,
            School school,
            int elX,
            int elY,
            int elW,
            int elH,
            float scale
    ) {
        BufferedImage logo = loadSchoolLogo(school);
        if (logo != null) {
            double srcAspect = (double) logo.getWidth() / logo.getHeight();
            double dstAspect = (double) elW / elH;
            int drawW, drawH, drawX, drawY;
            if (srcAspect > dstAspect) {
                drawW = elW;
                drawH = (int) Math.round(elW / srcAspect);
                drawX = elX;
                drawY = elY + (elH - drawH) / 2;
            } else {
                drawH = elH;
                drawW = (int) Math.round(elH * srcAspect);
                drawX = elX + (elW - drawW) / 2;
                drawY = elY;
            }
            g2.drawImage(logo, drawX, drawY, drawW, drawH, null);
        } else {
            int pad = 2;
            g2.setColor(new Color(238, 242, 255));
            g2.fillRoundRect(elX + pad, elY + pad, elW - pad * 2, elH - pad * 2, 8, 8);
            g2.setColor(new Color(79, 70, 229));
            g2.setStroke(new BasicStroke(1.5f));
            g2.drawRoundRect(elX + pad, elY + pad, elW - pad * 2, elH - pad * 2, 8, 8);
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(8, elH / 3)));
            String text = "LOGO";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(text, elX + (elW - fm.stringWidth(text)) / 2, elY + (elH + fm.getAscent() / 2) / 2);
        }
    }

    private void renderSignatureJava2D(
            Graphics2D g2,
            JsonNode element,
            School school,
            int elX,
            int elY,
            int elW,
            int elH,
            float scale
    ) {
        BufferedImage sign = loadSchoolSignature(school);
        boolean hasCustomSignFile = school != null && school.getSignatureFileId() != null && !school.getSignatureFileId().isBlank();

        if (hasCustomSignFile && sign != null) {
            double srcAspect = (double) sign.getWidth() / sign.getHeight();
            double dstAspect = (double) elW / elH;
            int drawW, drawH, drawX, drawY;
            if (srcAspect > dstAspect) {
                drawW = (int) (elW * 0.9);
                drawH = (int) Math.round(drawW / srcAspect);
                drawX = elX + (elW - drawW) / 2;
                drawY = elY + (elH - drawH) / 2;
            } else {
                drawH = (int) (elH * 0.7);
                drawW = (int) Math.round(drawH * srcAspect);
                drawX = elX + (elW - drawW) / 2;
                drawY = elY + (elH - drawH) / 2;
            }
            g2.drawImage(sign, drawX, drawY, drawW, drawH, null);
        } else {
            // Styled signature box matching CardCanvas.jsx & screen preview
            g2.setColor(new Color(248, 250, 252, 180));
            g2.fillRoundRect(elX, elY, elW, elH, 8, 8);

            g2.setColor(new Color(148, 163, 184));
            Stroke oldStroke = g2.getStroke();
            float[] dash = {4f, 4f};
            g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, dash, 0f));
            g2.drawRoundRect(elX, elY, elW, elH, 8, 8);

            // Smooth cubic Bézier ink signature curve in ink blue
            g2.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.setColor(new Color(30, 58, 138)); // Deep ink navy

            Path2D.Float path = new Path2D.Float();
            float startX = elX + elW * 0.15f;
            float startY = elY + elH * 0.45f;
            path.moveTo(startX, startY);
            path.curveTo(elX + elW * 0.35f, elY + elH * 0.10f, elX + elW * 0.42f, elY + elH * 0.60f, elX + elW * 0.55f, elY + elH * 0.40f);
            path.curveTo(elX + elW * 0.65f, elY + elH * 0.20f, elX + elW * 0.75f, elY + elH * 0.65f, elX + elW * 0.88f, elY + elH * 0.30f);
            g2.draw(path);

            // Underline separator
            g2.setStroke(new BasicStroke(1f));
            g2.setColor(new Color(148, 163, 184));
            int lineY = elY + Math.round(elH * 0.72f);
            g2.drawLine((int) (elX + elW * 0.12f), lineY, (int) (elX + elW * 0.88f), lineY);

            // Principal Name / Signatory Label underneath
            String principal = (school != null && school.getPrincipalName() != null && !school.getPrincipalName().isBlank())
                    ? school.getPrincipalName().toUpperCase()
                    : (element.hasNonNull("label") ? element.get("label").asText().toUpperCase() : "DR. RAJESH VERMA");
            g2.setColor(new Color(100, 116, 139));
            g2.setFont(new Font("SansSerif", Font.BOLD, Math.max(9, Math.round(elH * 0.18f))));
            FontMetrics fm = g2.getFontMetrics();
            int textX = elX + (elW - fm.stringWidth(principal)) / 2;
            int textY = elY + Math.round(elH * 0.92f);
            g2.drawString(principal, textX, textY);

            g2.setStroke(oldStroke);
        }
    }

    private void renderQrCodeJava2D(
            Graphics2D g2,
            JsonNode element,
            Student student,
            School school,
            String academicSession,
            int elX,
            int elY,
            int elW,
            int elH
    ) {
        String qrTemplate = getProperty(element, "content", "data", "field", "value", "");
        String qrText = "";
        if (!qrTemplate.isBlank()) {
            qrText = interpolatePlaceholders(qrTemplate, student, school, academicSession);
        }
        if (qrText.isBlank()
                || qrText.equalsIgnoreCase("admission_number")
                || qrText.equalsIgnoreCase("student_verification_qr")
                || qrText.equalsIgnoreCase("default")
                || qrText.equalsIgnoreCase("qrcode")) {
            String schoolName = school != null && school.getSchoolName() != null ? school.getSchoolName() : "School";
            String studentName = val(student != null ? student.getStudentName() : null, "Student");
            String admNo = val(student != null ? student.getAdmissionNumber() : null, "N/A");
            String classSec = val(student != null ? student.getClassName() : null, "") +
                    ((student != null && student.getSection() != null && !student.getSection().isBlank()) ? "-" + student.getSection() : "");
            String blood = val(student != null ? student.getBloodGroup() : null, "N/A");
            String emergency = val(student != null ? student.getEmergencyContact() : null, val(student != null ? student.getMobile() : null, ""));

            qrText = "BEGIN:VCARD\nVERSION:3.0\nFN:" + studentName +
                    "\nORG:" + schoolName +
                    "\nNOTE:Adm No: " + admNo + " | Class: " + classSec + " | Blood: " + blood +
                    "\nTEL:" + emergency +
                    "\nEND:VCARD";
        }

        qrText = qrText.replace("\\n", "\n").replace("\\r", "");
        int qrSize = Math.max(elW, elH);
        BufferedImage qrImg = generateQrCodeImage(qrText, qrSize, qrSize);

        g2.setColor(Color.WHITE);
        g2.fillRoundRect(elX, elY, elW, elH, 6, 6);
        g2.setColor(new Color(226, 232, 240));
        g2.setStroke(new BasicStroke(1f));
        g2.drawRoundRect(elX, elY, elW, elH, 6, 6);

        if (qrImg != null) {
            int pad = 4;
            g2.drawImage(qrImg, elX + pad, elY + pad, elW - pad * 2, elH - pad * 2, null);
        }
    }

    private void renderBarcodeJava2D(
            Graphics2D g2,
            JsonNode element,
            Student student,
            School school,
            String academicSession,
            int elX,
            int elY,
            int elW,
            int elH
    ) {
        String field = getProperty(element, "content", "field", "value", "data", "admission_number");
        String barValue = interpolatePlaceholders(field, student, school, academicSession);
        if (barValue.isBlank() || barValue.equalsIgnoreCase("admission_number") || barValue.equalsIgnoreCase("student_barcode")) {
            barValue = (student != null && student.getAdmissionNumber() != null) ? student.getAdmissionNumber() : "ADM2026001";
        }

        BufferedImage barImg = generateBarcodeImage(barValue, elW, (int) (elH * 0.75));
        g2.setColor(Color.WHITE);
        g2.fillRect(elX, elY, elW, elH);

        if (barImg != null) {
            g2.drawImage(barImg, elX, elY, elW, (int) (elH * 0.75), null);
        }

        g2.setColor(Color.BLACK);
        g2.setFont(new Font("Monospaced", Font.PLAIN, Math.max(9, (int) (elH * 0.2))));
        FontMetrics fm = g2.getFontMetrics();
        int textX = elX + (elW - fm.stringWidth(barValue)) / 2;
        g2.drawString(barValue, textX, elY + elH - 2);
    }

    private void renderTextJava2D(
            Graphics2D g2,
            JsonNode element,
            Student student,
            School school,
            String academicSession,
            int elX,
            int elY,
            int elW,
            int elH,
            float scale
    ) {
        String rawText = getProperty(element, "content", "text", "value", "label", "");
        String field = getProperty(element, "field", "");
        String label = getProperty(element, "label", "");

        String resolved;
        if (!rawText.isBlank()) {
            resolved = interpolatePlaceholders(rawText, student, school, academicSession);
        } else if (!field.isBlank()) {
            String val = interpolateValue(field, student, school, academicSession);
            resolved = label.isBlank() ? val : (label + " " + val);
        } else if (!label.isBlank()) {
            resolved = label;
        } else {
            resolved = "";
        }

        if (resolved.isBlank()) return;

        // Background box fill behind text if specified
        String bgStr = getProperty(element, "backgroundColor", "bgColor", "");
        if (!bgStr.isBlank() && !bgStr.equalsIgnoreCase("transparent") && !bgStr.equalsIgnoreCase("none")) {
            Color textBg = parseColor(bgStr, null);
            if (textBg != null) {
                g2.setColor(textBg);
                float bgRadiusMm = element.hasNonNull("borderRadius") ? (float) element.get("borderRadius").asDouble() : 0f;
                if (bgRadiusMm > 0) {
                    int rPx = Math.round(bgRadiusMm * scale);
                    g2.fillRoundRect(elX, elY, elW, elH, rPx * 2, rPx * 2);
                } else {
                    g2.fillRect(elX, elY, elW, elH);
                }
            }
        }

        float fontSize = element.hasNonNull("fontSize") ? (float) element.get("fontSize").asDouble() : 8f;
        if (fontSize <= 0) fontSize = 8f;

        boolean isBold = isBold(element);
        boolean isItalic = element.hasNonNull("fontStyle") && element.get("fontStyle").asText().equalsIgnoreCase("italic");
        int style = Font.PLAIN;
        if (isBold && isItalic) style = Font.BOLD | Font.ITALIC;
        else if (isBold) style = Font.BOLD;
        else if (isItalic) style = Font.ITALIC;

        String family = getProperty(element, "fontFamily", "family", "sans-serif");
        String fontName = Font.SANS_SERIF;
        if (family.toLowerCase().contains("serif") || family.toLowerCase().contains("times")) {
            fontName = Font.SERIF;
        } else if (family.toLowerCase().contains("mono")) {
            fontName = Font.MONOSPACED;
        }

        // CSS px to 300 DPI conversion ratio = 300 / 96 = 3.125
        int fontPx = Math.max(10, Math.round(fontSize * (300.0f / 96.0f)));

        String colorStr = getProperty(element, "color", "textColor", "fill", "#000000");
        Color textColor = parseColor(colorStr, Color.BLACK);
        g2.setColor(textColor);

        String align = getProperty(element, "textAlign", "align", "left").toLowerCase();
        String normalized = resolved.replace("\\n", "\n").replace("\\r", "");
        String[] paragraphs = normalized.split("\n");

        // Dynamic auto-fitting: Shrink font size if text wraps excessively for elH
        Font font = new Font(fontName, style, fontPx);
        FontMetrics fm = g2.getFontMetrics(font);
        java.util.List<String> wrappedLines = wrapJava2DText(paragraphs, fm, elW);

        while (wrappedLines.size() * (fm.getAscent() + fm.getDescent() + 2) > elH && fontPx > 11) {
            fontPx -= 1;
            font = new Font(fontName, style, fontPx);
            fm = g2.getFontMetrics(font);
            wrappedLines = wrapJava2DText(paragraphs, fm, elW);
        }

        g2.setFont(font);

        int lineHeight = fm.getAscent() + fm.getDescent() + 2;
        int totalTextH = wrappedLines.size() * lineHeight;

        int startY = elY + Math.max(0, (elH - totalTextH) / 2) + fm.getAscent();

        for (int i = 0; i < wrappedLines.size(); i++) {
            String line = wrappedLines.get(i);
            int lineWidth = fm.stringWidth(line);
            int drawX;
            if ("center".equals(align)) {
                drawX = elX + (elW - lineWidth) / 2;
            } else if ("right".equals(align)) {
                drawX = elX + elW - lineWidth - 2;
            } else {
                drawX = elX + 2;
            }
            int drawY = startY + (i * lineHeight);
            g2.drawString(line, drawX, drawY);

            if (element.hasNonNull("textDecoration") && element.get("textDecoration").asText().contains("underline")
                    || (element.hasNonNull("underline") && element.get("underline").asBoolean())) {
                g2.drawLine(drawX, drawY + 2, drawX + lineWidth, drawY + 2);
            }
        }
    }

    private java.util.List<String> wrapJava2DText(String[] paragraphs, FontMetrics fm, int maxW) {
        java.util.List<String> result = new java.util.ArrayList<>();
        for (String para : paragraphs) {
            String[] words = para.trim().split("\\s+");
            if (words.length == 0 || words[0].isEmpty()) {
                result.add("");
                continue;
            }
            StringBuilder line = new StringBuilder();
            for (String word : words) {
                String candidate = line.length() == 0 ? word : line + " " + word;
                if (fm.stringWidth(candidate) <= maxW || line.length() == 0) {
                    line = new StringBuilder(candidate);
                } else {
                    result.add(line.toString());
                    line = new StringBuilder(word);
                }
            }
            if (line.length() > 0) {
                result.add(line.toString());
            }
        }
        return result;
    }

    private void renderFallbackJava2D(
            Graphics2D g2,
            boolean isFront,
            Student student,
            School school,
            String session,
            int cardW,
            int cardH,
            float scale
    ) {
        if (isFront) {
            Color navy = new Color(30, 58, 138);
            g2.setColor(navy);
            int bannerH = Math.round(cardH * 0.18f);
            g2.fillRect(0, 0, cardW, bannerH);

            BufferedImage logo = loadSchoolLogo(school);
            if (logo != null) {
                int logoSize = Math.round(bannerH * 0.75f);
                g2.drawImage(logo, 20, (bannerH - logoSize) / 2, logoSize, logoSize, null);
            }

            String schoolName = school != null && school.getSchoolName() != null ? school.getSchoolName() : "Greenwood International School";
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 28));
            g2.drawString(schoolName, bannerH + 20, bannerH / 2 + 5);

            BufferedImage photo = loadStudentPhoto(student);
            int photoW = Math.round(cardW * 0.40f);
            int photoH = Math.round(photoW * 1.25f);
            int photoX = (cardW - photoW) / 2;
            int photoY = bannerH + 30;
            if (photo != null) {
                g2.drawImage(photo, photoX, photoY, photoW, photoH, null);
                g2.setColor(new Color(203, 213, 225));
                g2.setStroke(new BasicStroke(3f));
                g2.drawRect(photoX, photoY, photoW, photoH);
            }

            g2.setColor(new Color(15, 23, 42));
            g2.setFont(new Font("SansSerif", Font.BOLD, 30));
            String sName = student.getStudentName() != null ? student.getStudentName() : "Aarav Sharma";
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(sName, (cardW - fm.stringWidth(sName)) / 2, photoY + photoH + 45);

            g2.setColor(navy);
            g2.fillRect(0, cardH - 15, cardW, 15);
        } else {
            g2.setColor(new Color(51, 65, 85));
            g2.fillRect(0, 0, cardW, 60);
            g2.setColor(Color.WHITE);
            g2.setFont(new Font("SansSerif", Font.BOLD, 22));
            g2.drawString("CARD RULES & INFORMATION", 30, 40);

            g2.setColor(new Color(30, 41, 59));
            g2.setFont(new Font("SansSerif", Font.PLAIN, 18));
            g2.drawString("1. This card is non-transferable.", 30, 100);
            g2.drawString("2. If found, please return to school office.", 30, 130);
        }
    }

    private void renderSheetMarginHeader(
            PDPageContentStream stream,
            String schoolName,
            String session,
            int currentSheet,
            int totalSheets,
            String sideLabel,
            float x,
            float y
    ) {
        try {
            stream.beginText();
            stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 8f);
            stream.setNonStrokingColor(new Color(100, 116, 139));
            stream.newLineAtOffset(x, y);
            stream.showText(sanitizeForPdf(schoolName + "  |  Session: " + session + "  |  Sheet " + currentSheet + " of " + totalSheets + " [" + sideLabel + "]"));
            stream.endText();
        } catch (Exception ignored) {}
    }

    private void drawCropMarks(PDPageContentStream stream, float x, float y, float w, float h) throws Exception {
        stream.saveGraphicsState();

        // 1. Subtle cutting guide border around the card slot
        stream.setLineWidth(0.4f);
        stream.setStrokingColor(new Color(203, 213, 225)); // Slate-300
        stream.setLineDashPattern(new float[]{3f, 3f}, 0f);
        stream.addRect(x, y, w, h);
        stream.stroke();

        // 2. Corner crop marks (solid tick marks)
        stream.setLineDashPattern(new float[]{}, 0f); // solid
        stream.setStrokingColor(new Color(148, 163, 184)); // Slate-400
        stream.setLineWidth(0.5f);

        float markLen = 7f; // ~2.5mm
        float gap = 2f;

        // Bottom-Left
        stream.moveTo(x - gap - markLen, y);
        stream.lineTo(x - gap, y);
        stream.moveTo(x, y - gap - markLen);
        stream.lineTo(x, y - gap);

        // Bottom-Right
        stream.moveTo(x + w + gap, y);
        stream.lineTo(x + w + gap + markLen, y);
        stream.moveTo(x + w, y - gap - markLen);
        stream.lineTo(x + w, y - gap);

        // Top-Left
        stream.moveTo(x - gap - markLen, y + h);
        stream.lineTo(x - gap, y + h);
        stream.moveTo(x, y + h + gap);
        stream.lineTo(x, y + h + gap + markLen);

        // Top-Right
        stream.moveTo(x + w + gap, y + h);
        stream.lineTo(x + w + gap + markLen, y + h);
        stream.moveTo(x + w, y + h + gap);
        stream.lineTo(x + w, y + h + gap + markLen);

        stream.stroke();
        stream.restoreGraphicsState();
    }

    private void renderCanvasSide(
            PDDocument document,
            PDPage page,
            String designJson,
            boolean isFront,
            Student student,
            School school,
            String academicSession,
            float widthPt,
            float heightPt
    ) {
        try (PDPageContentStream stream = new PDPageContentStream(document, page)) {
            renderCardSideContent(document, stream, designJson, isFront, student, school, academicSession, widthPt, heightPt);
        } catch (Exception e) {
            log.error("Error drawing canvas side (isFront={}): {}", isFront, e.getMessage(), e);
        }
    }

    private void renderCardSideContent(
            PDDocument document,
            PDPageContentStream stream,
            String designJson,
            boolean isFront,
            Student student,
            School school,
            String academicSession,
            float widthPt,
            float heightPt
    ) {
        try {
            // 1. Draw background color (extracted from design JSON, side config or default white)
            Color bgColor = extractBackgroundColor(designJson, isFront);
            stream.setNonStrokingColor(bgColor);
            stream.addRect(0, 0, widthPt, heightPt);
            stream.fill();

            // 2. Parse and sort elements by layer (Z-index) so background shapes never occlude foreground content
            JsonNode elementsNode = extractElementsNode(designJson, isFront);
            if (elementsNode != null && elementsNode.isArray() && elementsNode.size() > 0) {
                java.util.List<JsonNode> sortedElements = new java.util.ArrayList<>();
                for (JsonNode element : elementsNode) {
                    sortedElements.add(element);
                }
                sortedElements.sort(java.util.Comparator.comparingInt(this::getElementZIndex));
                for (JsonNode element : sortedElements) {
                    renderElement(document, stream, element, student, school, academicSession, widthPt, heightPt);
                }
            } else {
                // Render fallback layout if design JSON has no elements
                if (isFront) {
                    renderFallbackFront(document, stream, student, school, widthPt, heightPt);
                } else {
                    renderFallbackBack(document, stream, student, school, academicSession, widthPt, heightPt);
                }
            }
        } catch (Exception e) {
            log.error("Error rendering card side content (isFront={}): {}", isFront, e.getMessage(), e);
        }
    }

    private Color extractBackgroundColor(String designJson, boolean isFront) {
        if (designJson == null || designJson.isBlank()) {
            return Color.WHITE;
        }
        try {
            JsonNode root = objectMapper.readTree(designJson);
            String sideKey = isFront ? "front" : "back";
            if (root.has(sideKey) && root.get(sideKey).isObject()) {
                JsonNode sideNode = root.get(sideKey);
                if (sideNode.hasNonNull("backgroundColor")) {
                    return parseColor(sideNode.get("backgroundColor").asText(), Color.WHITE);
                }
                if (sideNode.hasNonNull("bgColor")) {
                    return parseColor(sideNode.get("bgColor").asText(), Color.WHITE);
                }
            }
            if (root.hasNonNull("backgroundColor")) {
                return parseColor(root.get("backgroundColor").asText(), Color.WHITE);
            }
            if (root.hasNonNull("bgColor")) {
                return parseColor(root.get("bgColor").asText(), Color.WHITE);
            }
            if (root.hasNonNull("background")) {
                return parseColor(root.get("background").asText(), Color.WHITE);
            }

            // Also check if any element represents the canvas background
            JsonNode elementsNode = extractElementsNode(designJson, isFront);
            if (elementsNode != null && elementsNode.isArray()) {
                for (JsonNode el : elementsNode) {
                    String type = el.hasNonNull("type") ? el.get("type").asText().toLowerCase() : "";
                    String id = el.hasNonNull("id") ? el.get("id").asText().toLowerCase() : "";
                    if ("background".equals(type) || id.contains("canvas-bg") || id.contains("canvas-background")) {
                        String colorStr = getProperty(el, "backgroundColor", "fillColor", "color", "fill");
                        if (!colorStr.isBlank()) {
                            return parseColor(colorStr, Color.WHITE);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse background color from design JSON: {}", e.getMessage());
        }
        return Color.WHITE;
    }

    private JsonNode extractElementsNode(String designJson, boolean isFront) {
        if (designJson == null || designJson.isBlank()) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(designJson);
            if (root.isArray()) {
                return root;
            }
            if (root.has("elements") && root.get("elements").isArray()) {
                return root.get("elements");
            }
            String key = isFront ? "front" : "back";
            if (root.has(key)) {
                JsonNode side = root.get(key);
                if (side.isArray()) {
                    return side;
                }
                if (side.has("elements") && side.get("elements").isArray()) {
                    return side.get("elements");
                }
            }
        } catch (Exception e) {
            log.warn("Could not parse design JSON: {}", e.getMessage());
        }
        return null;
    }

    private int getElementZIndex(JsonNode el) {
        if (el == null) return 0;
        if (el.hasNonNull("zIndex")) {
            return el.get("zIndex").asInt();
        }
        String type = el.hasNonNull("type") ? el.get("type").asText().toLowerCase() : "text";
        return switch (type) {
            case "background" -> 0;
            case "rect", "shape", "banner", "line", "circle", "oval" -> 10;
            case "image", "photo" -> {
                String field = getProperty(el, "field", "name", "").toLowerCase();
                if (field.contains("background") || field.contains("watermark")) yield 12;
                yield 20;
            }
            case "qrcode", "qr", "barcode", "bar" -> 22;
            case "signature" -> 25;
            default -> 30; // Text is highest priority layer so it is NEVER covered by shapes or backgrounds
        };
    }

    private void renderElement(
            PDDocument document,
            PDPageContentStream stream,
            JsonNode element,
            Student student,
            School school,
            String academicSession,
            float pageWidthPt,
            float pageHeightPt
    ) {
        String type = element.hasNonNull("type") ? element.get("type").asText().toLowerCase() : "text";

        float xMm = element.hasNonNull("x")
                ? (float) element.get("x").asDouble()
                : (element.hasNonNull("left") ? (float) element.get("left").asDouble() : 0f);
        float yMm = element.hasNonNull("y")
                ? (float) element.get("y").asDouble()
                : (element.hasNonNull("top") ? (float) element.get("top").asDouble() : 0f);
        float widthMm = element.hasNonNull("width") ? (float) element.get("width").asDouble() : 20f;
        float heightMm = element.hasNonNull("height") ? (float) element.get("height").asDouble() : 20f;

        float xPt = xMm * POINTS_PER_MM;
        float widthPt = widthMm * POINTS_PER_MM;
        float heightPt = heightMm * POINTS_PER_MM;
        // In PDFBox, (0,0) is bottom-left. Canvas designer (0,0) is top-left.
        float yPt = pageHeightPt - (yMm * POINTS_PER_MM) - heightPt;

        try {
            switch (type) {
                case "shape", "rect", "rectangle", "banner", "line", "circle", "oval" -> {
                    renderShapeElement(stream, element, type, xPt, yPt, widthPt, heightPt);
                }
                case "signature" -> {
                    BufferedImage signImg = loadSchoolSignature(school);
                    if (signImg != null) {
                        drawContainedImage(document, stream, signImg, xPt, yPt, widthPt, heightPt);
                    }
                }
                case "image", "photo" -> {
                    String field = getProperty(element, "field", "name", "src", "student_photo").toLowerCase();
                    BufferedImage img;
                    boolean isLogoOrSign = false;
                    if (field.contains("signature") || field.contains("sign")) {
                        img = loadSchoolSignature(school);
                        isLogoOrSign = true;
                    } else if (field.contains("logo") || field.contains("school")) {
                        img = loadSchoolLogo(school);
                        isLogoOrSign = true;
                    } else {
                        img = loadStudentPhoto(student);
                    }
                    if (img != null) {
                        if (isLogoOrSign) {
                            drawContainedImage(document, stream, img, xPt, yPt, widthPt, heightPt);
                        } else {
                            String shape = getProperty(element, "shape", "photoShape", "photo_shape", "");
                            float borderRadiusMm = element.hasNonNull("borderRadius")
                                    ? (float) element.get("borderRadius").asDouble()
                                    : 0f;
                            String borderColorStr = getProperty(element, "borderColor", "strokeColor", "");
                            Color borderColor = (!borderColorStr.isBlank() && !borderColorStr.equalsIgnoreCase("transparent") && !borderColorStr.equalsIgnoreCase("none"))
                                    ? parseColor(borderColorStr, null)
                                    : null;
                            float borderWidthMm = element.hasNonNull("borderWidth")
                                    ? (float) element.get("borderWidth").asDouble()
                                    : 0f;

                            BufferedImage shapedImg = applyPhotoShape(img, shape, borderRadiusMm, borderColor, borderWidthMm, widthPt, heightPt);
                            PDImageXObject pdImg = LosslessFactory.createFromImage(document, shapedImg != null ? shapedImg : img);
                            stream.drawImage(pdImg, xPt, yPt, widthPt, heightPt);
                        }
                    }
                }
                case "qrcode", "qr" -> {
                    String qrTemplate = getProperty(element, "content", "data", "field", "value", "");
                    String qrText = "";
                    if (!qrTemplate.isBlank()) {
                        qrText = interpolatePlaceholders(qrTemplate, student, school, academicSession);
                    }
                    if (qrText.isBlank()
                            || qrText.equalsIgnoreCase("admission_number")
                            || qrText.equalsIgnoreCase("student_verification_qr")
                            || qrText.equalsIgnoreCase("default")
                            || qrText.equalsIgnoreCase("qrcode")) {
                        String schoolName = school != null && school.getSchoolName() != null ? school.getSchoolName() : "School";
                        String studentName = val(student != null ? student.getStudentName() : null, "Student");
                        String admNo = val(student != null ? student.getAdmissionNumber() : null, "N/A");
                        String classSec = val(student != null ? student.getClassName() : null, "") +
                                ((student != null && student.getSection() != null && !student.getSection().isBlank()) ? "-" + student.getSection() : "");
                        String blood = val(student != null ? student.getBloodGroup() : null, "N/A");
                        String emergency = val(student != null ? student.getEmergencyContact() : null, val(student != null ? student.getMobile() : null, ""));

                        qrText = "BEGIN:VCARD\nVERSION:3.0\nFN:" + studentName +
                                "\nORG:" + schoolName +
                                "\nNOTE:Adm No: " + admNo + " | Class: " + classSec + " | Blood: " + blood +
                                "\nTEL:" + emergency +
                                "\nEND:VCARD";
                    }

                    qrText = qrText.replace("\\n", "\n").replace("\\r", "");
                    BufferedImage qrImg = generateQrCodeImage(qrText, 300, 300);
                    if (qrImg != null) {
                        PDImageXObject pdImg = LosslessFactory.createFromImage(document, qrImg);
                        stream.drawImage(pdImg, xPt, yPt, widthPt, heightPt);
                    }
                }
                case "barcode", "bar" -> {
                    String field = getProperty(element, "content", "field", "value", "data", "admission_number");
                    String barValue = interpolatePlaceholders(field, student, school, academicSession);
                    if (barValue.isBlank() || barValue.equalsIgnoreCase("admission_number") || barValue.equalsIgnoreCase("student_barcode")) {
                        barValue = (student != null && student.getAdmissionNumber() != null) ? student.getAdmissionNumber() : "ADM2026001";
                    }
                    BufferedImage barImg = generateBarcodeImage(barValue, 300, 80);
                    if (barImg != null) {
                        PDImageXObject pdImg = LosslessFactory.createFromImage(document, barImg);
                        stream.drawImage(pdImg, xPt, yPt, widthPt, heightPt);
                    }
                }
                case "background" -> {
                    // Handled at canvas level; ignore or render as rect if additional layer
                }
                default -> { // Text
                    String rawText = getProperty(element, "content", "text", "value", "label", "");
                    String field = getProperty(element, "field", "");
                    String label = getProperty(element, "label", "");

                    String resolvedText;
                    if (!rawText.isBlank()) {
                        resolvedText = interpolatePlaceholders(rawText, student, school, academicSession);
                    } else if (!field.isBlank()) {
                        String val = interpolateValue(field, student, school, academicSession);
                        resolvedText = label.isBlank() ? val : (label + " " + val);
                    } else if (!label.isBlank()) {
                        resolvedText = label;
                    } else {
                        resolvedText = "";
                    }

                    if (resolvedText.isBlank()) {
                        return;
                    }

                    float fontSize = element.hasNonNull("fontSize") ? (float) element.get("fontSize").asDouble() : 10f;
                    if (fontSize <= 0) fontSize = 10f;

                    boolean isBold = isBold(element);
                    PDType1Font font = resolveFont(element, isBold);

                    String colorStr = getProperty(element, "color", "textColor", "fill", "#000000");
                    Color textColor = parseColor(colorStr, Color.BLACK);

                    String align = getProperty(element, "textAlign", "align", "left").toLowerCase();
                    float lineHeightFactor = element.hasNonNull("lineHeight") ? (float) element.get("lineHeight").asDouble() : 1.25f;

                    // Clamp maxWidth to ensure text NEVER extends past the page edge or bounding box
                    float maxWidthPt = Math.min(widthPt, Math.max(15f, pageWidthPt - xPt - 4f));
                    String normalized = resolvedText.replace("\\n", "\n").replace("\\r", "");
                    String[] paragraphs = normalized.split("\n");
                    java.util.List<String> allLines = new java.util.ArrayList<>();

                    for (String para : paragraphs) {
                        java.util.List<String> wrapped = wrapText(para, font, fontSize, maxWidthPt);
                        if (wrapped.isEmpty()) {
                            allLines.add("");
                        } else {
                            allLines.addAll(wrapped);
                        }
                    }

                    // Auto-shrink font size if a short or single-line title overflows the box
                    if (allLines.size() > 1 && heightPt <= fontSize * 1.6f) {
                        while (allLines.size() > 1 && fontSize > 6.0f) {
                            fontSize = Math.max(5.5f, fontSize - 0.5f);
                            allLines.clear();
                            for (String para : paragraphs) {
                                allLines.addAll(wrapText(para, font, fontSize, maxWidthPt));
                            }
                        }
                    }

                    float lineHeight = fontSize * lineHeightFactor;
                    float totalTextHeight = allLines.size() * lineHeight;
                    float startY = yPt + heightPt - fontSize;
                    if (heightPt > totalTextHeight) {
                        startY = yPt + heightPt - ((heightPt - totalTextHeight) / 2f) - (fontSize * 0.85f);
                    }

                    for (int i = 0; i < allLines.size(); i++) {
                        String cleanLine = sanitizeForPdf(allLines.get(i));
                        if (cleanLine.isBlank()) continue;
                        float textWidth = (font.getStringWidth(cleanLine) / 1000f) * fontSize;
                        float lineX = xPt;
                        if ("center".equals(align)) {
                            lineX = xPt + Math.max(0, (maxWidthPt - textWidth) / 2f);
                        } else if ("right".equals(align)) {
                            lineX = xPt + Math.max(0, maxWidthPt - textWidth);
                        }
                        float currentY = startY - (i * lineHeight);
                        if (currentY < 1f) break;

                        stream.beginText();
                        stream.setFont(font, fontSize);
                        stream.setNonStrokingColor(textColor);
                        stream.newLineAtOffset(lineX, currentY);
                        stream.showText(cleanLine);
                        stream.endText();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Failed to render element type={}: {}", type, e.getMessage());
        }
    }

    private void renderShapeElement(
            PDPageContentStream stream,
            JsonNode element,
            String type,
            float xPt,
            float yPt,
            float widthPt,
            float heightPt
    ) throws Exception {
        String shapeAttr = getProperty(element, "shape", "").toLowerCase();
        boolean isCircle = type.equals("circle") || type.equals("oval") || shapeAttr.equals("circle") || shapeAttr.equals("oval");
        boolean isLine = type.equals("line") || shapeAttr.equals("line");

        float borderRadiusMm = element.hasNonNull("borderRadius")
                ? (float) element.get("borderRadius").asDouble()
                : (element.hasNonNull("radius") ? (float) element.get("radius").asDouble() : 0f);
        float radiusPt = borderRadiusMm * POINTS_PER_MM;

        String fillStr = getProperty(element, "fillColor", "backgroundColor", "color", "fill", "");
        Color fillColor = (!fillStr.isBlank() && !fillStr.equalsIgnoreCase("transparent") && !fillStr.equalsIgnoreCase("none"))
                ? parseColor(fillStr, null)
                : null;

        String borderStr = getProperty(element, "borderColor", "strokeColor", "stroke", "");
        Color borderColor = (!borderStr.isBlank() && !borderStr.equalsIgnoreCase("transparent") && !borderStr.equalsIgnoreCase("none"))
                ? parseColor(borderStr, null)
                : null;

        float borderWidthMm = element.hasNonNull("borderWidth")
                ? (float) element.get("borderWidth").asDouble()
                : (element.hasNonNull("strokeWidth") ? (float) element.get("strokeWidth").asDouble() : 0f);
        float borderWidthPt = borderWidthMm * POINTS_PER_MM;

        if (fillColor == null && borderColor == null) {
            fillColor = new Color(30, 58, 138); // Default navy
        }

        if (isLine) {
            Color strokeCol = borderColor != null ? borderColor : (fillColor != null ? fillColor : Color.BLACK);
            stream.setStrokingColor(strokeCol);
            stream.setLineWidth(borderWidthPt > 0 ? borderWidthPt : 1f);
            stream.moveTo(xPt, yPt + (heightPt / 2f));
            stream.lineTo(xPt + widthPt, yPt + (heightPt / 2f));
            stream.stroke();
            return;
        }

        if (fillColor != null) {
            stream.setNonStrokingColor(fillColor);
        }
        if (borderColor != null) {
            stream.setStrokingColor(borderColor);
            stream.setLineWidth(borderWidthPt > 0 ? borderWidthPt : 1f);
        }

        if (isCircle) {
            addEllipsePath(stream, xPt, yPt, widthPt, heightPt);
        } else if (radiusPt > 0) {
            addRoundedRectPath(stream, xPt, yPt, widthPt, heightPt, radiusPt);
        } else {
            stream.addRect(xPt, yPt, widthPt, heightPt);
        }

        if (fillColor != null && borderColor != null && borderWidthPt > 0) {
            stream.fillAndStroke();
        } else if (fillColor != null) {
            stream.fill();
        } else if (borderColor != null) {
            stream.stroke();
        }
    }

    private void addRoundedRectPath(PDPageContentStream stream, float x, float y, float w, float h, float r) throws Exception {
        r = Math.min(r, Math.min(w / 2f, h / 2f));
        float k = 0.5522847498f * r;
        stream.moveTo(x + r, y);
        stream.lineTo(x + w - r, y);
        stream.curveTo(x + w - r + k, y, x + w, y + r - k, x + w, y + r);
        stream.lineTo(x + w, y + h - r);
        stream.curveTo(x + w, y + h - r + k, x + w - r + k, y + h, x + w - r, y + h);
        stream.lineTo(x + r, y + h);
        stream.curveTo(x + r - k, y + h, x, y + h - r + k, x, y + h - r);
        stream.lineTo(x, y + r);
        stream.curveTo(x, y + r - k, x + r - k, y, x + r, y);
        stream.closePath();
    }

    private void addEllipsePath(PDPageContentStream stream, float x, float y, float w, float h) throws Exception {
        float rx = w / 2f;
        float ry = h / 2f;
        float cx = x + rx;
        float cy = y + ry;
        float kx = 0.5522847498f * rx;
        float ky = 0.5522847498f * ry;

        stream.moveTo(cx - rx, cy);
        stream.curveTo(cx - rx, cy + ky, cx - kx, cy + ry, cx, cy + ry);
        stream.curveTo(cx + kx, cy + ry, cx + rx, cy + ky, cx + rx, cy);
        stream.curveTo(cx + rx, cy - ky, cx + kx, cy - ry, cx, cy - ry);
        stream.curveTo(cx - kx, cy - ry, cx - rx, cy - ky, cx - rx, cy);
        stream.closePath();
    }

    private BufferedImage applyPhotoShape(
            BufferedImage src,
            String shape,
            float borderRadiusMm,
            Color borderColor,
            float borderWidthMm,
            float widthPt,
            float heightPt
    ) {
        if (src == null) return null;

        String s = shape != null ? shape.toLowerCase().trim() : "";
        boolean isCircle = s.contains("circle") || (s.contains("round") && !s.contains("corner") && !s.contains("rounded"));
        boolean isRounded = s.contains("rounded") || s.contains("corner") || borderRadiusMm > 0;

        if (!isCircle && !isRounded && (borderColor == null || borderWidthMm <= 0)) {
            return src;
        }

        int targetW = Math.max(300, (int) (widthPt * 4));
        int targetH = Math.max(300, (int) (heightPt * 4));
        BufferedImage output = new BufferedImage(targetW, targetH, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = output.createGraphics();

        try {
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            float borderPx = borderWidthMm > 0 ? (borderWidthMm * POINTS_PER_MM * 4f) : 0f;
            float inset = borderPx / 2f;
            float contentW = targetW - borderPx;
            float contentH = targetH - borderPx;

            Shape clipShape;
            if (isCircle) {
                clipShape = new Ellipse2D.Float(inset, inset, contentW, contentH);
            } else if (isRounded) {
                float radiusMm = borderRadiusMm > 0 ? borderRadiusMm : 4.0f;
                float radiusPx = radiusMm * POINTS_PER_MM * 4f;
                clipShape = new RoundRectangle2D.Float(inset, inset, contentW, contentH, radiusPx * 2, radiusPx * 2);
            } else {
                clipShape = new Rectangle2D.Float(inset, inset, contentW, contentH);
            }

            g2.setColor(Color.WHITE);
            g2.fill(clipShape);

            // Proportional cover-fit: scale to fill the shape without distortion, cropping excess evenly
            double srcRatio = (double) src.getWidth() / src.getHeight();
            double targetRatio = (double) contentW / contentH;
            int drawW, drawH, drawX, drawY;
            if (srcRatio > targetRatio) {
                // Source is wider than target box: match height, crop width evenly
                drawH = (int) contentH;
                drawW = Math.max(1, (int) Math.round(contentH * srcRatio));
                drawX = (int) Math.round(inset + (contentW - drawW) / 2.0);
                drawY = (int) Math.round(inset);
            } else {
                // Source is taller than target box: match width, crop height evenly
                drawW = (int) contentW;
                drawH = Math.max(1, (int) Math.round(contentW / srcRatio));
                drawX = (int) Math.round(inset);
                drawY = (int) Math.round(inset + (contentH - drawH) / 2.0);
            }

            g2.setComposite(AlphaComposite.SrcIn);
            g2.drawImage(src, drawX, drawY, drawW, drawH, null);

            if (borderColor != null && borderPx > 0) {
                g2.setComposite(AlphaComposite.SrcOver);
                g2.setColor(borderColor);
                g2.setStroke(new BasicStroke(borderPx, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.draw(clipShape);
            }
        } finally {
            g2.dispose();
        }
        return output;
    }

    /**
     * Draws an image preserving aspect ratio (object-fit: contain), perfectly centered in the target bounding box.
     */
    private void drawContainedImage(
            PDDocument document,
            PDPageContentStream stream,
            BufferedImage img,
            float xPt,
            float yPt,
            float widthPt,
            float heightPt
    ) throws Exception {
        if (img == null) return;
        double srcRatio = (double) img.getWidth() / img.getHeight();
        double targetRatio = (double) widthPt / heightPt;
        float drawW, drawH, drawX, drawY;
        if (srcRatio > targetRatio) {
            // Source is wider: fit width, center vertically
            drawW = widthPt;
            drawH = (float) (widthPt / srcRatio);
            drawX = xPt;
            drawY = yPt + (heightPt - drawH) / 2f;
        } else {
            // Source is taller: fit height, center horizontally
            drawH = heightPt;
            drawW = (float) (heightPt * srcRatio);
            drawX = xPt + (widthPt - drawW) / 2f;
            drawY = yPt;
        }
        PDImageXObject pdImg = LosslessFactory.createFromImage(document, img);
        stream.drawImage(pdImg, drawX, drawY, drawW, drawH);
    }

    /**
     * Splits a text line into words and wraps them so each wrapped line does not exceed maxWidthPt.
     */
    private java.util.List<String> wrapText(String text, PDType1Font font, float fontSize, float maxWidthPt) {
        java.util.List<String> result = new java.util.ArrayList<>();
        if (text == null || text.isBlank()) return result;
        String[] words = text.trim().split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String candidate = currentLine.length() == 0 ? word : currentLine + " " + word;
            float textWidthPt;
            try {
                textWidthPt = (font.getStringWidth(sanitizeForPdf(candidate)) / 1000f) * fontSize;
            } catch (Exception e) {
                textWidthPt = candidate.length() * fontSize * 0.5f;
            }

            if (textWidthPt <= maxWidthPt || currentLine.length() == 0) {
                currentLine = new StringBuilder(candidate);
            } else {
                result.add(currentLine.toString());
                currentLine = new StringBuilder(word);
            }
        }
        if (currentLine.length() > 0) {
            result.add(currentLine.toString());
        }
        return result;
    }

    private void renderFallbackFront(
            PDDocument document,
            PDPageContentStream stream,
            Student student,
            School school,
            float widthPt,
            float heightPt
    ) throws Exception {
        // Header banner
        Color navy = new Color(30, 58, 138);
        stream.setNonStrokingColor(navy);
        float bannerHeight = 38f;
        stream.addRect(0, heightPt - bannerHeight, widthPt, bannerHeight);
        stream.fill();

        // School Logo inside banner
        BufferedImage logo = loadSchoolLogo(school);
        if (logo != null) {
            PDImageXObject pdLogo = LosslessFactory.createFromImage(document, logo);
            stream.drawImage(pdLogo, 10f, heightPt - bannerHeight + 5f, 28f, 28f);
        }

        // School Name
        String schoolName = school != null && school.getSchoolName() != null ? school.getSchoolName() : "Springfield International School";
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10.5f);
        stream.setNonStrokingColor(Color.WHITE);
        stream.newLineAtOffset(44f, heightPt - 18f);
        stream.showText(sanitizeForPdf(schoolName));
        stream.endText();

        // Subtitle / Tagline
        String tagline = (school != null && school.getTagline() != null && !school.getTagline().isBlank())
                ? school.getTagline()
                : "STUDENT IDENTITY CARD";
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 6.5f);
        stream.setNonStrokingColor(new Color(224, 231, 255));
        stream.newLineAtOffset(44f, heightPt - 28f);
        stream.showText(sanitizeForPdf(tagline));
        stream.endText();

        // Student Photo (with clean rounded frame)
        BufferedImage rawPhoto = loadStudentPhoto(student);
        BufferedImage photo = applyPhotoShape(rawPhoto, "rounded", 2.0f, new Color(203, 213, 225), 1.0f, 62f, 78f);
        if (photo != null) {
            PDImageXObject pdPhoto = LosslessFactory.createFromImage(document, photo);
            stream.drawImage(pdPhoto, 12f, 28f, 62f, 78f);
        }

        // Student Info
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        float textX = 84f;
        float currentY = heightPt - bannerHeight - 16f;

        // Student Name
        stream.beginText();
        stream.setFont(boldFont, 11f);
        stream.setNonStrokingColor(new Color(15, 23, 42));
        stream.newLineAtOffset(textX, currentY);
        stream.showText(sanitizeForPdf(student.getStudentName() != null ? student.getStudentName() : "Aarav Sharma"));
        stream.endText();

        currentY -= 13f;
        drawInfoRow(stream, regularFont, boldFont, "Adm No:", student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "ADM-2026-001", textX, currentY);

        currentY -= 11f;
        String classSec = (student.getClassName() != null ? student.getClassName() : "10th") + " - " + (student.getSection() != null ? student.getSection() : "A");
        drawInfoRow(stream, regularFont, boldFont, "Class/Sec:", classSec, textX, currentY);

        currentY -= 11f;
        drawInfoRow(stream, regularFont, boldFont, "Roll No:", student.getRollNumber() != null ? student.getRollNumber() : "12", textX, currentY);

        currentY -= 11f;
        drawInfoRow(stream, regularFont, boldFont, "Blood Group:", student.getBloodGroup() != null ? student.getBloodGroup() : "B+", textX, currentY);

        currentY -= 11f;
        drawInfoRow(stream, regularFont, boldFont, "Emergency:", student.getEmergencyContact() != null ? student.getEmergencyContact() : "9876543219", textX, currentY);

        // School Signature at bottom right
        BufferedImage sign = loadSchoolSignature(school);
        if (sign != null) {
            PDImageXObject pdSign = LosslessFactory.createFromImage(document, sign);
            stream.drawImage(pdSign, widthPt - 78f, 6f, 68f, 26f);
        }

        // Bottom accent line
        stream.setNonStrokingColor(navy);
        stream.addRect(0, 0, widthPt, 4f);
        stream.fill();
    }

    private void renderFallbackBack(
            PDDocument document,
            PDPageContentStream stream,
            Student student,
            School school,
            String academicSession,
            float widthPt,
            float heightPt
    ) throws Exception {
        // Back header
        Color darkSlate = new Color(51, 65, 85);
        stream.setNonStrokingColor(darkSlate);
        stream.addRect(0, heightPt - 22f, widthPt, 22f);
        stream.fill();

        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 8f);
        stream.setNonStrokingColor(Color.WHITE);
        stream.newLineAtOffset(12f, heightPt - 15f);
        stream.showText("INSTRUCTIONS & EMERGENCY CONTACT");
        stream.endText();

        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        float currentY = heightPt - 36f;
        float textX = 12f;

        drawInfoRow(stream, regularFont, boldFont, "Father's Name:", student.getFatherName() != null ? student.getFatherName() : "Rajesh Sharma", textX, currentY);
        currentY -= 10f;
        drawInfoRow(stream, regularFont, boldFont, "Emergency Mobile:", student.getEmergencyContact() != null ? student.getEmergencyContact() : "9876543219", textX, currentY);
        currentY -= 10f;
        drawInfoRow(stream, regularFont, boldFont, "Residential Address:", student.getAddress() != null ? student.getAddress() : "42 Lake View Apartments, Springfield", textX, currentY);
        currentY -= 10f;
        String address = school != null && school.getAddress() != null ? school.getAddress() : "123 Education Lane, Knowledge City";
        drawInfoRow(stream, regularFont, boldFont, "Campus Address:", address, textX, currentY);
        currentY -= 10f;
        String sessionStr = academicSession != null ? academicSession : "2026-2027";
        drawInfoRow(stream, regularFont, boldFont, "Session / Valid:", sessionStr + " (Valid: " + calculateValidUntil(sessionStr) + ")", textX, currentY);

        // QR Code on right side
        String schoolName = school != null && school.getSchoolName() != null ? school.getSchoolName() : "School";
        String vCard = "BEGIN:VCARD\nVERSION:3.0\nFN:" + val(student.getStudentName(), "Student") +
                "\nORG:" + schoolName +
                "\nNOTE:Adm No: " + val(student.getAdmissionNumber(), "N/A") + " | Class: " + val(student.getClassName(), "") + "-" + val(student.getSection(), "") + " | Blood: " + val(student.getBloodGroup(), "N/A") +
                "\nTEL:" + val(student.getEmergencyContact(), val(student.getMobile(), "")) +
                "\nEND:VCARD";
        BufferedImage qrImg = generateQrCodeImage(vCard, 200, 200);
        if (qrImg != null) {
            PDImageXObject pdQr = LosslessFactory.createFromImage(document, qrImg);
            stream.drawImage(pdQr, widthPt - 55f, 32f, 42f, 42f);
        }

        // Barcode at bottom
        String barcodeVal = student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "ADM2026001";
        BufferedImage barImg = generateBarcodeImage(barcodeVal, 260, 45);
        if (barImg != null) {
            PDImageXObject pdBar = LosslessFactory.createFromImage(document, barImg);
            stream.drawImage(pdBar, (widthPt - 130f) / 2f, 8f, 130f, 20f);
        }

        // Bottom accent line
        stream.setNonStrokingColor(darkSlate);
        stream.addRect(0, 0, widthPt, 3f);
        stream.fill();
    }

    private void drawInfoRow(
            PDPageContentStream stream,
            PDType1Font regularFont,
            PDType1Font boldFont,
            String label,
            String value,
            float x,
            float y
    ) throws Exception {
        stream.beginText();
        stream.setFont(boldFont, 7.5f);
        stream.setNonStrokingColor(new Color(100, 116, 139));
        stream.newLineAtOffset(x, y);
        stream.showText(sanitizeForPdf(label));
        stream.endText();

        stream.beginText();
        stream.setFont(regularFont, 7.5f);
        stream.setNonStrokingColor(new Color(30, 41, 59));
        stream.newLineAtOffset(x + 65f, y);
        stream.showText(sanitizeForPdf(value != null ? value : ""));
        stream.endText();
    }

    private BufferedImage loadStudentPhoto(Student student) {
        if (student != null && student.getPhotoFileId() != null) {
            try {
                Resource resource = fileStorageService.loadAsResource(student.getPhotoFileId());
                if (resource != null && resource.isReadable()) {
                    try (InputStream is = resource.getInputStream()) {
                        BufferedImage img = ImageIO.read(is);
                        if (img != null) return img;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not read student photo file: {}", e.getMessage());
            }
        }
        return generateCleanSilhouette(student != null ? student.getStudentName() : "Student");
    }

    private BufferedImage loadSchoolLogo(School school) {
        if (school != null && school.getLogoFileId() != null && !school.getLogoFileId().isBlank()) {
            try {
                UUID logoId = UUID.fromString(school.getLogoFileId());
                Resource resource = fileStorageService.loadAsResource(logoId);
                if (resource != null && resource.isReadable()) {
                    try (InputStream is = resource.getInputStream()) {
                        BufferedImage img = ImageIO.read(is);
                        if (img != null) return img;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not read school logo file: {}", e.getMessage());
            }
        }
        return generateSchoolBadge(school != null ? school.getSchoolName() : "School");
    }

    private BufferedImage loadSchoolSignature(School school) {
        if (school != null && school.getSignatureFileId() != null && !school.getSignatureFileId().isBlank()) {
            try {
                UUID signId = UUID.fromString(school.getSignatureFileId());
                Resource resource = fileStorageService.loadAsResource(signId);
                if (resource != null && resource.isReadable()) {
                    try (InputStream is = resource.getInputStream()) {
                        BufferedImage img = ImageIO.read(is);
                        if (img != null) return img;
                    }
                }
            } catch (Exception e) {
                log.warn("Could not read school signature file: {}", e.getMessage());
            }
        }
        return generateSchoolSignature(school);
    }

    public BufferedImage generateCleanSilhouette(String studentName) {
        int width = 200;
        int height = 250;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Background
        g.setColor(new Color(241, 245, 249)); // #F1F5F9
        g.fillRect(0, 0, width, height);

        // Thin border
        g.setColor(new Color(203, 213, 225)); // #CBD5E1
        g.setStroke(new BasicStroke(2f));
        g.drawRect(1, 1, width - 2, height - 2);

        // Silhouette color
        g.setColor(new Color(148, 163, 184)); // #94A3B8

        // Head
        int headRadius = 38;
        int headX = (width / 2) - headRadius;
        int headY = 45;
        g.fillOval(headX, headY, headRadius * 2, headRadius * 2);

        // Shoulders / Torso
        int torsoWidth = 140;
        int torsoHeight = 110;
        int torsoX = (width / 2) - (torsoWidth / 2);
        int torsoY = 135;
        g.fillArc(torsoX, torsoY, torsoWidth, torsoHeight * 2, 0, 180);

        // Clean label
        g.setColor(new Color(100, 116, 139));
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        String initials = extractInitials(studentName);
        FontMetrics fm = g.getFontMetrics();
        int textX = (width - fm.stringWidth(initials)) / 2;
        g.drawString(initials, textX, height - 15);

        g.dispose();
        return image;
    }

    private BufferedImage generateSchoolBadge(String schoolName) {
        int size = 150;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g.setColor(new Color(238, 242, 255));
        g.fillRect(0, 0, size, size);

        g.setColor(new Color(79, 70, 229));
        g.setStroke(new BasicStroke(3f));
        g.drawOval(10, 10, size - 20, size - 20);

        g.setFont(new Font("SansSerif", Font.BOLD, 22));
        String initials = extractInitials(schoolName);
        FontMetrics fm = g.getFontMetrics();
        int textX = (size - fm.stringWidth(initials)) / 2;
        int textY = ((size - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(initials, textX, textY);

        g.dispose();
        return image;
    }

    private BufferedImage generateSchoolSignature(School school) {
        int width = 300;
        int height = 120;
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Ink handwriting simulation
        g.setColor(new Color(30, 58, 138)); // Deep navy ink #1E3A8A
        g.setStroke(new BasicStroke(2.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

        Path2D path = new Path2D.Float();
        path.moveTo(35, 65);
        path.curveTo(55, 25, 80, 85, 110, 40);
        path.curveTo(130, 15, 150, 70, 175, 45);
        path.curveTo(195, 25, 215, 75, 240, 48);
        path.curveTo(255, 30, 270, 60, 280, 52);
        g.draw(path);

        Path2D flourish = new Path2D.Float();
        flourish.moveTo(50, 75);
        flourish.curveTo(90, 85, 180, 78, 265, 80);
        g.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.draw(flourish);

        // Divider signature line
        g.setColor(new Color(148, 163, 184)); // Slate-400
        g.setStroke(new BasicStroke(1.0f));
        g.drawLine(25, 92, 275, 92);

        // Label under line
        String signatoryName = (school != null && school.getPrincipalName() != null && !school.getPrincipalName().isBlank())
                ? school.getPrincipalName()
                : "Authorized Signatory";
        g.setColor(new Color(71, 85, 105)); // Slate-600
        g.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fm = g.getFontMetrics();
        int textX = (width - fm.stringWidth(signatoryName)) / 2;
        g.drawString(signatoryName, textX, 108);

        g.dispose();
        return image;
    }

    private BufferedImage generateQrCodeImage(String content, int width, int height) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
            hints.put(EncodeHintType.MARGIN, 1);
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            log.warn("Failed to generate QR code: {}", e.getMessage());
            return null;
        }
    }

    private BufferedImage generateBarcodeImage(String content, int width, int height) {
        try {
            MultiFormatWriter writer = new MultiFormatWriter();
            Map<EncodeHintType, Object> hints = new HashMap<>();
            hints.put(EncodeHintType.MARGIN, 2);
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.CODE_128, width, height, hints);
            return MatrixToImageWriter.toBufferedImage(bitMatrix);
        } catch (Exception e) {
            log.warn("Failed to generate Barcode: {}", e.getMessage());
            return null;
        }
    }

    public Student createSampleStudent(School school) {
        Student s = new Student();
        s.setId(UUID.randomUUID());
        s.setAdmissionNumber("ADM-2026-001");
        s.setStudentName("Aarav Sharma");
        s.setFatherName("Rajesh Sharma");
        s.setMotherName("Pooja Sharma");
        s.setDateOfBirth(LocalDate.of(2010, 5, 15));
        s.setGender("MALE");
        s.setBloodGroup("B+");
        s.setClassName("10th");
        s.setSection("A");
        s.setRollNumber("12");
        s.setAddress("42 Lake View Apartments, Springfield");
        s.setMobile("9876543210");
        s.setEmergencyContact("9876543219");
        s.setStatus("ACTIVE");
        s.setPhotoFileId(UUID.fromString("f0d479ad-456c-48e6-8fa8-1eb90505ba6c"));
        if (school != null) {
            s.setSchool(school);
        }
        return s;
    }

    private School getEffectiveSchool(School school, CardTemplate template, Student student) {
        if (school != null) return school;
        if (template != null && template.getSchool() != null) return template.getSchool();
        if (student != null && student.getSchool() != null) return student.getSchool();
        return createSampleSchool();
    }

    public School createSampleSchool() {
        School s = new School();
        s.setId(UUID.randomUUID());
        s.setSchoolName("Springfield International School");
        s.setSchoolCode("SCH-001");
        s.setAddress("123 Education Lane, Knowledge City");
        s.setMobile("+91 80 2345 6789");
        s.setEmail("office@springfield.edu");
        s.setWebsite("www.springfield.edu");
        s.setAffiliationNumber("CBSE / AFF-830192");
        s.setTagline("Empowering Minds, Inspiring Futures");
        s.setPrincipalName("Dr. Rajesh Verma");
        s.setStatus("ACTIVE");
        return s;
    }

    private String interpolatePlaceholders(String text, Student student, School school, String academicSession) {
        if (text == null) return "";
        String session = academicSession != null ? academicSession : "2026-2027";
        String validUntil = calculateValidUntil(session);

        return text
                .replace("{{student_name}}", val(student != null ? student.getStudentName() : null, "Aarav Sharma"))
                .replace("{{admission_number}}", val(student != null ? student.getAdmissionNumber() : null, "ADM-2026-001"))
                .replace("{{class}}", val(student != null ? student.getClassName() : null, "10th"))
                .replace("{{class_name}}", val(student != null ? student.getClassName() : null, "10th"))
                .replace("{{section}}", val(student != null ? student.getSection() : null, "A"))
                .replace("{{roll_number}}", val(student != null ? student.getRollNumber() : null, "12"))
                .replace("{{roll_no}}", val(student != null ? student.getRollNumber() : null, "12"))
                .replace("{{father_name}}", val(student != null ? student.getFatherName() : null, "Rajesh Sharma"))
                .replace("{{father}}", val(student != null ? student.getFatherName() : null, "Rajesh Sharma"))
                .replace("{{mother_name}}", val(student != null ? student.getMotherName() : null, "Pooja Sharma"))
                .replace("{{mother}}", val(student != null ? student.getMotherName() : null, "Pooja Sharma"))
                .replace("{{blood_group}}", val(student != null ? student.getBloodGroup() : null, "B+"))
                .replace("{{date_of_birth}}", formatDateOfBirth(student))
                .replace("{{dob}}", formatDateOfBirth(student))
                .replace("{{gender}}", val(student != null ? student.getGender() : null, "MALE"))
                .replace("{{mobile}}", val(student != null ? student.getMobile() : null, "9876543210"))
                .replace("{{phone}}", val(student != null ? student.getMobile() : null, "9876543210"))
                .replace("{{emergency_contact}}", val(student != null ? student.getEmergencyContact() : null, "9876543219"))
                .replace("{{emergency}}", val(student != null ? student.getEmergencyContact() : null, "9876543219"))
                .replace("{{address}}", val(student != null ? student.getAddress() : null, "42 Lake View Apartments, Springfield"))
                .replace("{{academic_session}}", session)
                .replace("{{session}}", session)
                .replace("{{valid_until}}", validUntil)
                .replace("{{school_name}}", school != null && school.getSchoolName() != null ? school.getSchoolName() : "Springfield International School")
                .replace("{{school_code}}", school != null && school.getSchoolCode() != null ? school.getSchoolCode() : "SCH-001")
                .replace("{{school_address}}", school != null && school.getAddress() != null ? school.getAddress() : "123 Education Lane, Springfield")
                .replace("{{school_phone}}", school != null && school.getMobile() != null ? school.getMobile() : "+91 80 2345 6789")
                .replace("{{school_email}}", school != null && school.getEmail() != null ? school.getEmail() : "office@school.edu")
                .replace("{{school_website}}", school != null && school.getWebsite() != null ? school.getWebsite() : "www.school.edu")
                .replace("{{affiliation_number}}", school != null && school.getAffiliationNumber() != null ? school.getAffiliationNumber() : "CBSE / AFF-830192")
                .replace("{{school_tagline}}", school != null && school.getTagline() != null ? school.getTagline() : "Empowering Minds, Inspiring Futures")
                .replace("{{principal_name}}", school != null && school.getPrincipalName() != null ? school.getPrincipalName() : "Dr. Rajesh Verma");
    }

    private String interpolateValue(String field, Student student, School school, String academicSession) {
        if (field == null) return "";
        String normalized = field.toLowerCase().replace("{", "").replace("}", "").trim();
        String session = academicSession != null ? academicSession : "2026-2027";
        return switch (normalized) {
            case "student_name", "name" -> val(student != null ? student.getStudentName() : null, "Aarav Sharma");
            case "admission_number", "admission_no", "adm_no" -> val(student != null ? student.getAdmissionNumber() : null, "ADM-2026-001");
            case "class", "class_name" -> val(student != null ? student.getClassName() : null, "10th");
            case "section", "sec" -> val(student != null ? student.getSection() : null, "A");
            case "roll_number", "roll_no" -> val(student != null ? student.getRollNumber() : null, "12");
            case "father_name", "father" -> val(student != null ? student.getFatherName() : null, "Rajesh Sharma");
            case "mother_name", "mother" -> val(student != null ? student.getMotherName() : null, "Pooja Sharma");
            case "blood_group", "blood" -> val(student != null ? student.getBloodGroup() : null, "B+");
            case "date_of_birth", "dob" -> formatDateOfBirth(student);
            case "gender" -> val(student != null ? student.getGender() : null, "MALE");
            case "mobile", "phone" -> val(student != null ? student.getMobile() : null, "9876543210");
            case "emergency_contact", "emergency" -> val(student != null ? student.getEmergencyContact() : null, "9876543219");
            case "address" -> val(student != null ? student.getAddress() : null, "42 Lake View Apartments, Springfield");
            case "academic_session", "session" -> session;
            case "valid_until" -> calculateValidUntil(session);
            case "school_name" -> school != null && school.getSchoolName() != null ? school.getSchoolName() : "Springfield International School";
            case "school_code" -> school != null && school.getSchoolCode() != null ? school.getSchoolCode() : "SCH-001";
            case "school_address" -> school != null && school.getAddress() != null ? school.getAddress() : "123 Education Lane, Springfield";
            case "school_phone", "school_mobile" -> school != null && school.getMobile() != null ? school.getMobile() : "+91 80 2345 6789";
            case "school_email" -> school != null && school.getEmail() != null ? school.getEmail() : "office@school.edu";
            case "school_website", "website" -> school != null && school.getWebsite() != null ? school.getWebsite() : "www.school.edu";
            case "affiliation_number", "affiliation_no" -> school != null && school.getAffiliationNumber() != null ? school.getAffiliationNumber() : "CBSE / AFF-830192";
            case "school_tagline", "tagline" -> school != null && school.getTagline() != null ? school.getTagline() : "Empowering Minds, Inspiring Futures";
            case "principal_name", "principal" -> school != null && school.getPrincipalName() != null ? school.getPrincipalName() : "Dr. Rajesh Verma";
            default -> field;
        };
    }

    private String formatDateOfBirth(Student student) {
        if (student != null && student.getDateOfBirth() != null) {
            return student.getDateOfBirth().format(DOB_FORMATTER);
        }
        return "15-05-2010";
    }

    private String calculateValidUntil(String session) {
        if (session != null && session.contains("-")) {
            String[] parts = session.split("-");
            if (parts.length >= 2) {
                String endYear = parts[1].trim();
                if (endYear.length() == 4) {
                    return "31-03-" + endYear;
                } else if (endYear.length() == 2) {
                    return "31-03-20" + endYear;
                }
            }
        }
        return "31-03-2027";
    }

    private String val(String str, String fallback) {
        return (str != null && !str.isBlank()) ? str : fallback;
    }

    private String extractInitials(String name) {
        if (name == null || name.isBlank()) return "ST";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }

    private String getProperty(JsonNode node, String... keys) {
        for (String key : keys) {
            if (node.hasNonNull(key)) {
                return node.get(key).asText();
            }
        }
        return "";
    }

    private boolean isBold(JsonNode node) {
        if (node.hasNonNull("fontWeight")) {
            String weight = node.get("fontWeight").asText().toLowerCase();
            return weight.contains("bold") || weight.equals("700") || weight.equals("800");
        }
        if (node.hasNonNull("bold")) {
            return node.get("bold").asBoolean();
        }
        return false;
    }

    private PDType1Font resolveFont(JsonNode node, boolean isBold) {
        String family = getProperty(node, "fontFamily", "font", "").toLowerCase();
        if (family.contains("times") || (family.contains("serif") && !family.contains("sans"))) {
            return isBold
                    ? new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD)
                    : new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
        } else if (family.contains("courier") || family.contains("mono")) {
            return isBold
                    ? new PDType1Font(Standard14Fonts.FontName.COURIER_BOLD)
                    : new PDType1Font(Standard14Fonts.FontName.COURIER);
        } else {
            return isBold
                    ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                    : new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        }
    }

    private Color parseColor(String hex, Color defaultColor) {
        if (hex == null || hex.isBlank()) return defaultColor;
        try {
            String clean = hex.trim();
            if (clean.startsWith("#")) {
                if (clean.length() == 7) {
                    return Color.decode(clean);
                } else if (clean.length() == 4) { // #RGB
                    char r = clean.charAt(1);
                    char g = clean.charAt(2);
                    char b = clean.charAt(3);
                    return Color.decode("#" + r + r + g + g + b + b);
                }
            }
        } catch (Exception ignored) {}
        return defaultColor;
    }

    private String sanitizeForPdf(String text) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 32 && c <= 126) {
                sb.append(c);
            } else if (c >= 160 && c <= 255) {
                sb.append(c);
            } else {
                sb.append(' ');
            }
        }
        return sb.toString();
    }
}
