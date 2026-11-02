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
import com.platform.idcard.template.entity.CardTemplate;
import com.platform.idcard.template.entity.TemplateVersion;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class CardPdfRenderingService {

    private static final Logger log = LoggerFactory.getLogger(CardPdfRenderingService.class);
    private static final float POINTS_PER_MM = 72.0f / 25.4f; // ~2.8346457 pt/mm

    private final ObjectMapper objectMapper;
    private final FileStorageService fileStorageService;

    public CardPdfRenderingService(ObjectMapper objectMapper, FileStorageService fileStorageService) {
        this.objectMapper = objectMapper;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Renders a 2-page print-quality PDF for a card template version.
     * Page 1 = Front side
     * Page 2 = Back side
     */
    public byte[] renderCardPdf(
            CardTemplate template,
            TemplateVersion version,
            Student student,
            School school,
            String academicSession
    ) {
        Student effectiveStudent = student != null ? student : createSampleStudent(school);
        School effectiveSchool = school != null ? school : (template.getSchool() != null ? template.getSchool() : effectiveStudent.getSchool());
        String effectiveSession = academicSession != null ? academicSession : "2026-2027";

        float widthMm = version.getCardWidthMm() != null ? version.getCardWidthMm().floatValue() : 85.60f;
        float heightMm = version.getCardHeightMm() != null ? version.getCardHeightMm().floatValue() : 54.00f;

        float widthPt = widthMm * POINTS_PER_MM;
        float heightPt = heightMm * POINTS_PER_MM;
        PDRectangle pageSize = new PDRectangle(widthPt, heightPt);

        try (PDDocument document = new PDDocument()) {
            // Page 1: Front
            PDPage frontPage = new PDPage(pageSize);
            document.addPage(frontPage);
            renderCanvasSide(document, frontPage, version.getFrontDesignJson(), true, effectiveStudent, effectiveSchool, effectiveSession, widthPt, heightPt);

            // Page 2: Back
            PDPage backPage = new PDPage(pageSize);
            document.addPage(backPage);
            renderCanvasSide(document, backPage, version.getBackDesignJson(), false, effectiveStudent, effectiveSchool, effectiveSession, widthPt, heightPt);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (Exception e) {
            log.error("Failed to render card template PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Card PDF rendering failed: " + e.getMessage(), e);
        }
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
            // 1. Draw default white card background
            stream.setNonStrokingColor(Color.WHITE);
            stream.addRect(0, 0, widthPt, heightPt);
            stream.fill();

            // 2. Parse elements
            JsonNode elementsNode = extractElementsNode(designJson, isFront);
            if (elementsNode != null && elementsNode.isArray() && elementsNode.size() > 0) {
                for (JsonNode element : elementsNode) {
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
            log.error("Error drawing canvas side (isFront={}): {}", isFront, e.getMessage(), e);
        }
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
            if (root.has(key) && root.get(key).has("elements") && root.get(key).get("elements").isArray()) {
                return root.get(key).get("elements");
            }
        } catch (Exception e) {
            log.warn("Could not parse design JSON: {}", e.getMessage());
        }
        return null;
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

        float xMm = element.hasNonNull("x") ? (float) element.get("x").asDouble() : 0f;
        float yMm = element.hasNonNull("y") ? (float) element.get("y").asDouble() : 0f;
        float widthMm = element.hasNonNull("width") ? (float) element.get("width").asDouble() : 20f;
        float heightMm = element.hasNonNull("height") ? (float) element.get("height").asDouble() : 20f;

        float xPt = xMm * POINTS_PER_MM;
        float widthPt = widthMm * POINTS_PER_MM;
        float heightPt = heightMm * POINTS_PER_MM;
        // In PDFBox, (0,0) is bottom-left. Canvas designer (0,0) is top-left.
        float yPt = pageHeightPt - (yMm * POINTS_PER_MM) - heightPt;

        try {
            switch (type) {
                case "shape", "rect", "rectangle", "banner", "line" -> {
                    String colorStr = getProperty(element, "fillColor", "color", "backgroundColor", "#1E3A8A");
                    Color color = parseColor(colorStr, new Color(30, 58, 138));
                    stream.setNonStrokingColor(color);
                    stream.addRect(xPt, yPt, widthPt, heightPt);
                    stream.fill();
                }
                case "image", "photo" -> {
                    String field = getProperty(element, "field", "name", "src", "student_photo").toLowerCase();
                    BufferedImage img;
                    if (field.contains("logo") || field.contains("school")) {
                        img = loadSchoolLogo(school);
                    } else {
                        img = loadStudentPhoto(student);
                    }
                    if (img != null) {
                        PDImageXObject pdImg = LosslessFactory.createFromImage(document, img);
                        stream.drawImage(pdImg, xPt, yPt, widthPt, heightPt);
                    }
                }
                case "qrcode", "qr" -> {
                    String field = getProperty(element, "field", "value", "content", "admission_number");
                    String qrValue = interpolateValue(field, student, school, academicSession);
                    if (qrValue.isBlank()) {
                        qrValue = student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "ID-PREVIEW";
                    }
                    BufferedImage qrImg = generateQrCodeImage(qrValue, 250, 250);
                    if (qrImg != null) {
                        PDImageXObject pdImg = LosslessFactory.createFromImage(document, qrImg);
                        stream.drawImage(pdImg, xPt, yPt, widthPt, heightPt);
                    }
                }
                case "barcode", "bar" -> {
                    String field = getProperty(element, "field", "value", "content", "admission_number");
                    String barValue = interpolateValue(field, student, school, academicSession);
                    if (barValue.isBlank()) {
                        barValue = student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "ADM2026001";
                    }
                    BufferedImage barImg = generateBarcodeImage(barValue, 300, 80);
                    if (barImg != null) {
                        PDImageXObject pdImg = LosslessFactory.createFromImage(document, barImg);
                        stream.drawImage(pdImg, xPt, yPt, widthPt, heightPt);
                    }
                }
                default -> { // Text
                    String rawText = getProperty(element, "text", "value", "label", "");
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

                    float fontSize = element.hasNonNull("fontSize") ? (float) element.get("fontSize").asDouble() : 10f;
                    if (fontSize <= 0) fontSize = 10f;

                    boolean isBold = isBold(element);
                    PDType1Font font = isBold
                            ? new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD)
                            : new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                    String colorStr = getProperty(element, "color", "textColor", "#000000");
                    Color textColor = parseColor(colorStr, Color.BLACK);

                    stream.beginText();
                    stream.setFont(font, fontSize);
                    stream.setNonStrokingColor(textColor);
                    // Baseline offset for text within its bounding box
                    float baselineY = yPt + (heightPt > fontSize ? (heightPt - fontSize) / 2 : 0);
                    stream.newLineAtOffset(xPt, baselineY);
                    stream.showText(sanitizeForPdf(resolvedText));
                    stream.endText();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to render element type={}: {}", type, e.getMessage());
        }
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
        float bannerHeight = 36f;
        stream.addRect(0, heightPt - bannerHeight, widthPt, bannerHeight);
        stream.fill();

        // School Name
        String schoolName = school != null && school.getSchoolName() != null ? school.getSchoolName() : "Springfield International School";
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 10.5f);
        stream.setNonStrokingColor(Color.WHITE);
        stream.newLineAtOffset(10f, heightPt - 22f);
        stream.showText(sanitizeForPdf(schoolName));
        stream.endText();

        // Subtitle
        stream.beginText();
        stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 6.5f);
        stream.setNonStrokingColor(new Color(224, 231, 255));
        stream.newLineAtOffset(10f, heightPt - 31f);
        stream.showText("STUDENT IDENTITY CARD");
        stream.endText();

        // Student Photo
        BufferedImage photo = loadStudentPhoto(student);
        if (photo != null) {
            PDImageXObject pdPhoto = LosslessFactory.createFromImage(document, photo);
            stream.drawImage(pdPhoto, 12f, 24f, 62f, 80f);
        }

        // Student Info
        PDType1Font boldFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        PDType1Font regularFont = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

        float textX = 84f;
        float currentY = heightPt - bannerHeight - 18f;

        // Student Name
        stream.beginText();
        stream.setFont(boldFont, 11f);
        stream.setNonStrokingColor(new Color(15, 23, 42));
        stream.newLineAtOffset(textX, currentY);
        stream.showText(sanitizeForPdf(student.getStudentName() != null ? student.getStudentName() : "Aarav Sharma"));
        stream.endText();

        currentY -= 14f;
        drawInfoRow(stream, regularFont, boldFont, "Adm No:", student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "ADM-2026-001", textX, currentY);

        currentY -= 11f;
        String classSec = (student.getClassName() != null ? student.getClassName() : "10th") + " - " + (student.getSection() != null ? student.getSection() : "A");
        drawInfoRow(stream, regularFont, boldFont, "Class/Sec:", classSec, textX, currentY);

        currentY -= 11f;
        drawInfoRow(stream, regularFont, boldFont, "Roll No:", student.getRollNumber() != null ? student.getRollNumber() : "12", textX, currentY);

        currentY -= 11f;
        drawInfoRow(stream, regularFont, boldFont, "Blood Group:", student.getBloodGroup() != null ? student.getBloodGroup() : "B+", textX, currentY);

        // QR Code bottom right
        String qrContent = "ID:" + (student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "ADM-2026-001");
        BufferedImage qrImg = generateQrCodeImage(qrContent, 150, 150);
        if (qrImg != null) {
            PDImageXObject pdQr = LosslessFactory.createFromImage(document, qrImg);
            stream.drawImage(pdQr, widthPt - 52f, 12f, 40f, 40f);
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

        float currentY = heightPt - 38f;
        float textX = 12f;

        drawInfoRow(stream, regularFont, boldFont, "Father's Name:", student.getFatherName() != null ? student.getFatherName() : "Rajesh Sharma", textX, currentY);
        currentY -= 11f;
        drawInfoRow(stream, regularFont, boldFont, "Emergency Mobile:", student.getEmergencyContact() != null ? student.getEmergencyContact() : "9876543219", textX, currentY);
        currentY -= 11f;
        drawInfoRow(stream, regularFont, boldFont, "Residential Address:", student.getAddress() != null ? student.getAddress() : "42 Lake View Apartments, Springfield", textX, currentY);
        currentY -= 11f;
        String address = school != null && school.getAddress() != null ? school.getAddress() : "123 Education Lane, Knowledge City";
        drawInfoRow(stream, regularFont, boldFont, "Campus Address:", address, textX, currentY);
        currentY -= 11f;
        drawInfoRow(stream, regularFont, boldFont, "Academic Session:", academicSession != null ? academicSession : "2026-2027", textX, currentY);

        // Barcode at bottom
        String barcodeVal = student.getAdmissionNumber() != null ? student.getAdmissionNumber() : "ADM2026001";
        BufferedImage barImg = generateBarcodeImage(barcodeVal, 260, 45);
        if (barImg != null) {
            PDImageXObject pdBar = LosslessFactory.createFromImage(document, barImg);
            stream.drawImage(pdBar, (widthPt - 130f) / 2f, 10f, 130f, 24f);
        }
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
        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 12));
        String initials = extractInitials(studentName);
        java.awt.FontMetrics fm = g.getFontMetrics();
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

        g.setFont(new java.awt.Font("SansSerif", java.awt.Font.BOLD, 22));
        String initials = extractInitials(schoolName);
        java.awt.FontMetrics fm = g.getFontMetrics();
        int textX = (size - fm.stringWidth(initials)) / 2;
        int textY = ((size - fm.getHeight()) / 2) + fm.getAscent();
        g.drawString(initials, textX, textY);

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
        if (school != null) {
            s.setSchool(school);
        }
        return s;
    }

    private String interpolatePlaceholders(String text, Student student, School school, String academicSession) {
        if (text == null) return "";
        return text
                .replace("{{student_name}}", val(student.getStudentName(), "Aarav Sharma"))
                .replace("{{admission_number}}", val(student.getAdmissionNumber(), "ADM-2026-001"))
                .replace("{{class}}", val(student.getClassName(), "10th"))
                .replace("{{class_name}}", val(student.getClassName(), "10th"))
                .replace("{{section}}", val(student.getSection(), "A"))
                .replace("{{roll_number}}", val(student.getRollNumber(), "12"))
                .replace("{{father_name}}", val(student.getFatherName(), "Rajesh Sharma"))
                .replace("{{mother_name}}", val(student.getMotherName(), "Pooja Sharma"))
                .replace("{{blood_group}}", val(student.getBloodGroup(), "B+"))
                .replace("{{date_of_birth}}", student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : "2010-05-15")
                .replace("{{dob}}", student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : "2010-05-15")
                .replace("{{emergency_contact}}", val(student.getEmergencyContact(), "9876543219"))
                .replace("{{mobile}}", val(student.getMobile(), "9876543210"))
                .replace("{{address}}", val(student.getAddress(), "42 Lake View Apartments, Springfield"))
                .replace("{{school_name}}", school != null && school.getSchoolName() != null ? school.getSchoolName() : "Springfield International School")
                .replace("{{school_address}}", school != null && school.getAddress() != null ? school.getAddress() : "123 Education Lane, Springfield")
                .replace("{{academic_session}}", academicSession != null ? academicSession : "2026-2027");
    }

    private String interpolateValue(String field, Student student, School school, String academicSession) {
        if (field == null) return "";
        String normalized = field.toLowerCase().replace("{", "").replace("}", "").trim();
        return switch (normalized) {
            case "student_name", "name" -> val(student.getStudentName(), "Aarav Sharma");
            case "admission_number", "admission_no", "adm_no" -> val(student.getAdmissionNumber(), "ADM-2026-001");
            case "class", "class_name" -> val(student.getClassName(), "10th");
            case "section", "sec" -> val(student.getSection(), "A");
            case "roll_number", "roll_no" -> val(student.getRollNumber(), "12");
            case "father_name", "father" -> val(student.getFatherName(), "Rajesh Sharma");
            case "mother_name", "mother" -> val(student.getMotherName(), "Pooja Sharma");
            case "blood_group", "blood" -> val(student.getBloodGroup(), "B+");
            case "date_of_birth", "dob" -> student.getDateOfBirth() != null ? student.getDateOfBirth().toString() : "2010-05-15";
            case "emergency_contact", "emergency" -> val(student.getEmergencyContact(), "9876543219");
            case "mobile", "phone" -> val(student.getMobile(), "9876543210");
            case "address" -> val(student.getAddress(), "42 Lake View Apartments, Springfield");
            case "school_name" -> school != null && school.getSchoolName() != null ? school.getSchoolName() : "Springfield International School";
            case "school_address" -> school != null && school.getAddress() != null ? school.getAddress() : "123 Education Lane, Springfield";
            case "academic_session", "session" -> academicSession != null ? academicSession : "2026-2027";
            default -> field;
        };
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
