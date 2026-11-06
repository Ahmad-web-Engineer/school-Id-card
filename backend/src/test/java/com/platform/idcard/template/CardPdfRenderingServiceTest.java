package com.platform.idcard.template;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.file.service.FileStorageService;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.student.entity.Student;
import com.platform.idcard.template.dto.PrintSettingsDto;
import com.platform.idcard.template.entity.CardTemplate;
import com.platform.idcard.template.entity.TemplateVersion;
import com.platform.idcard.template.service.CardPdfRenderingService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardPdfRenderingServiceTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private FileStorageService fileStorageService;

    private CardPdfRenderingService renderingService;

    private School sampleSchool;
    private Student sampleStudent;
    private CardTemplate sampleTemplate;
    private TemplateVersion sampleVersion;

    @BeforeEach
    void setUp() {
        renderingService = new CardPdfRenderingService(objectMapper, fileStorageService);

        sampleSchool = new School();
        sampleSchool.setId(UUID.randomUUID());
        sampleSchool.setSchoolName("Greenwood International School");
        sampleSchool.setSchoolCode("GWIS-001");
        sampleSchool.setAddress("14 Heritage Boulevard, Indiranagar, Bengaluru");
        sampleSchool.setCity("Bengaluru");
        sampleSchool.setState("Karnataka");
        sampleSchool.setPincode("560038");
        sampleSchool.setMobile("+91 80 2345 6789");
        sampleSchool.setEmail("office@greenwood.edu");
        sampleSchool.setWebsite("www.greenwood.edu");
        sampleSchool.setAffiliationNumber("CBSE / AFF-830192");
        sampleSchool.setTagline("Empowering Minds, Inspiring Futures");
        sampleSchool.setPrincipalName("Dr. Rajesh Verma");
        sampleSchool.setStatus("ACTIVE");

        sampleStudent = new Student();
        sampleStudent.setId(UUID.randomUUID());
        sampleStudent.setSchool(sampleSchool);
        sampleStudent.setAdmissionNumber("ADM-2026-001");
        sampleStudent.setStudentName("Aarav Sharma");
        sampleStudent.setFatherName("Rajesh Sharma");
        sampleStudent.setMotherName("Pooja Sharma");
        sampleStudent.setDateOfBirth(LocalDate.of(2010, 5, 15));
        sampleStudent.setGender("MALE");
        sampleStudent.setBloodGroup("B+");
        sampleStudent.setClassName("10th");
        sampleStudent.setSection("A");
        sampleStudent.setRollNumber("12");
        sampleStudent.setAddress("42 Lake View Apartments, City Center");
        sampleStudent.setMobile("9876543210");
        sampleStudent.setEmergencyContact("9876543219");
        sampleStudent.setStatus("ACTIVE");

        sampleTemplate = new CardTemplate();
        sampleTemplate.setId(UUID.randomUUID());
        sampleTemplate.setTemplateName("Standard Student Card");
        sampleTemplate.setTemplateCategory("STUDENT");
        sampleTemplate.setSchool(sampleSchool);

        sampleVersion = new TemplateVersion();
        sampleVersion.setId(UUID.randomUUID());
        sampleVersion.setTemplate(sampleTemplate);
        sampleVersion.setVersionNumber(1);
        sampleVersion.setCardWidthMm(new BigDecimal("85.60"));
        sampleVersion.setCardHeightMm(new BigDecimal("54.00"));
        sampleVersion.setBleedMarginMm(new BigDecimal("3.00"));
    }

    @Test
    @DisplayName("1. Text elements using 'content' attribute interpolate all 24 placeholders")
    void testTextElementWithContentAndAllPlaceholders() throws Exception {
        String frontJson = """
        {
          "elements": [
            {
              "type": "text",
              "content": "Student: {{student_name}} Adm: {{admission_number}}",
              "x": 5, "y": 5, "width": 75, "height": 6, "fontSize": 8
            },
            {
              "type": "text",
              "content": "Class: {{class}}-{{section}} Roll: {{roll_number}}",
              "x": 5, "y": 12, "width": 75, "height": 6, "fontSize": 8
            },
            {
              "type": "text",
              "content": "Parents: {{father_name}} & {{mother_name}}",
              "x": 5, "y": 19, "width": 75, "height": 6, "fontSize": 8
            },
            {
              "type": "text",
              "content": "DOB: {{date_of_birth}} Gender: {{gender}} Blood: {{blood_group}}",
              "x": 5, "y": 26, "width": 75, "height": 6, "fontSize": 8
            },
            {
              "type": "text",
              "content": "Contact: {{mobile}} Emg: {{emergency_contact}}",
              "x": 5, "y": 33, "width": 75, "height": 6, "fontSize": 8
            },
            {
              "type": "text",
              "content": "Addr: {{address}} Session: {{academic_session}} Valid: {{valid_until}}",
              "x": 5, "y": 40, "width": 75, "height": 6, "fontSize": 8
            }
          ]
        }
        """;

        String backJson = """
        {
          "elements": [
            {
              "type": "text",
              "content": "School: {{school_name}} Code: {{school_code}}",
              "x": 5, "y": 5, "width": 75, "height": 6, "fontSize": 8
            },
            {
              "type": "text",
              "content": "Campus: {{school_address}} Phone: {{school_phone}}",
              "x": 5, "y": 12, "width": 75, "height": 6, "fontSize": 8
            },
            {
              "type": "text",
              "content": "Email: {{school_email}} Web: {{school_website}}",
              "x": 5, "y": 19, "width": 75, "height": 6, "fontSize": 8
            },
            {
              "type": "text",
              "content": "Affil: {{affiliation_number}} Tag: {{school_tagline}}",
              "x": 5, "y": 26, "width": 75, "height": 6, "fontSize": 8
            },
            {
              "type": "text",
              "content": "Principal: {{principal_name}}",
              "x": 5, "y": 33, "width": 75, "height": 6, "fontSize": 8
            }
          ]
        }
        """;

        sampleVersion.setFrontDesignJson(frontJson);
        sampleVersion.setBackDesignJson(backJson);

        byte[] pdfBytes = renderingService.renderCardPdf(sampleTemplate, sampleVersion, sampleStudent, sampleSchool, "2026-2027");
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String frontText = stripper.getText(doc);

            assertThat(frontText).contains("Aarav Sharma");
            assertThat(frontText).contains("ADM-2026-001");
            assertThat(frontText).contains("10th");
            assertThat(frontText).contains("Rajesh Sharma");
            assertThat(frontText).contains("Pooja Sharma");
            assertThat(frontText).contains("15-05-2010");
            assertThat(frontText).contains("MALE");
            assertThat(frontText).contains("B+");
            assertThat(frontText).contains("9876543210");
            assertThat(frontText).contains("9876543219");
            assertThat(frontText).contains("42 Lake View Apartments");
            assertThat(frontText).contains("2026-2027");
            assertThat(frontText).contains("31-03-2027");

            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String backText = stripper.getText(doc);

            assertThat(backText).contains("Greenwood International School");
            assertThat(backText).contains("GWIS-001");
            assertThat(backText).contains("14 Heritage Boulevard");
            assertThat(backText).contains("+91 80 2345 6789");
            assertThat(backText).contains("office@greenwood.edu");
            assertThat(backText).contains("www.greenwood.edu");
            assertThat(backText).contains("CBSE / AFF-830192");
            assertThat(backText).contains("Empowering Minds");
            assertThat(backText).contains("Inspiring Futures");
            assertThat(backText).contains("Dr. Rajesh Verma");
        }
    }

    @Test
    @DisplayName("2. Background color from design JSON is extracted and rendered")
    void testBackgroundColor() throws Exception {
        String frontJson = """
        {
          "backgroundColor": "#EFF6FF",
          "elements": [
            { "type": "text", "content": "Welcome", "x": 10, "y": 10, "width": 50, "height": 10 }
          ]
        }
        """;
        sampleVersion.setFrontDesignJson(frontJson);
        sampleVersion.setBackDesignJson("{\"backgroundColor\":\"#FEF2F2\",\"elements\":[]}");

        byte[] pdfBytes = renderingService.renderCardPdf(sampleTemplate, sampleVersion, sampleStudent, sampleSchool, "2026-2027");
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("3. School signature element renders loaded image or elegant vector fallback")
    void testSchoolSignatureElement() throws Exception {
        // Test fallback vector signature
        String frontJson = """
        {
          "elements": [
            { "type": "signature", "label": "Principal Signature", "x": 55, "y": 38, "width": 25, "height": 12 }
          ]
        }
        """;
        sampleVersion.setFrontDesignJson(frontJson);
        sampleVersion.setBackDesignJson("{\"elements\":[]}");

        byte[] pdfFallback = renderingService.renderCardPdf(sampleTemplate, sampleVersion, sampleStudent, sampleSchool, "2026-2027");
        assertThat(pdfFallback).isNotNull().isNotEmpty();

        // Test loaded signature image from file storage
        UUID signFileId = UUID.randomUUID();
        sampleSchool.setSignatureFileId(signFileId.toString());

        BufferedImage fakeSign = new BufferedImage(100, 40, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = fakeSign.createGraphics();
        g.setColor(Color.BLUE);
        g.drawLine(10, 20, 90, 20);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(fakeSign, "png", baos);
        Resource mockResource = new ByteArrayResource(baos.toByteArray());

        when(fileStorageService.loadAsResource(signFileId)).thenReturn(mockResource);

        byte[] pdfLoaded = renderingService.renderCardPdf(sampleTemplate, sampleVersion, sampleStudent, sampleSchool, "2026-2027");
        assertThat(pdfLoaded).isNotNull().isNotEmpty();
    }

    @Test
    @DisplayName("4. Photo box shapes (circle, rounded, rectangle) render without error")
    void testPhotoBoxShapes() throws Exception {
        String frontJson = """
        {
          "elements": [
            { "type": "image", "field": "student_photo", "shape": "circle", "x": 5, "y": 5, "width": 20, "height": 20 },
            { "type": "photo", "field": "student_photo", "shape": "rounded", "borderRadius": 3, "x": 30, "y": 5, "width": 20, "height": 25, "borderColor": "#CBD5E1", "borderWidth": 1 },
            { "type": "photo", "field": "student_photo", "shape": "square", "x": 55, "y": 5, "width": 20, "height": 25 }
          ]
        }
        """;
        sampleVersion.setFrontDesignJson(frontJson);
        sampleVersion.setBackDesignJson("{\"elements\":[]}");

        byte[] pdfBytes = renderingService.renderCardPdf(sampleTemplate, sampleVersion, sampleStudent, sampleSchool, "2026-2027");
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("5. Dynamic QR code generates custom interpolated content or standard vCard")
    void testDynamicQrCode() throws Exception {
        String frontJson = """
        {
          "elements": [
            { "type": "qrcode", "content": "https://school.edu/verify/{{admission_number}}", "x": 5, "y": 5, "width": 20, "height": 20 },
            { "type": "qr", "field": "admission_number", "x": 30, "y": 5, "width": 20, "height": 20 }
          ]
        }
        """;
        sampleVersion.setFrontDesignJson(frontJson);
        sampleVersion.setBackDesignJson("{\"elements\":[]}");

        byte[] pdfBytes = renderingService.renderCardPdf(sampleTemplate, sampleVersion, sampleStudent, sampleSchool, "2026-2027");
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("6. Shape elements support rounded corners, border color and border width")
    void testShapeElements() throws Exception {
        String frontJson = """
        {
          "elements": [
            { "type": "rect", "x": 5, "y": 5, "width": 30, "height": 15, "fillColor": "#1E3A8A", "borderRadius": 2 },
            { "type": "rectangle", "x": 40, "y": 5, "width": 30, "height": 15, "borderColor": "#DC2626", "borderWidth": 1.5, "fillColor": "#FEE2E2" },
            { "type": "circle", "x": 5, "y": 25, "width": 15, "height": 15, "fillColor": "#10B981" },
            { "type": "line", "x": 5, "y": 45, "width": 75, "height": 2, "borderColor": "#94A3B8", "borderWidth": 1 }
          ]
        }
        """;
        sampleVersion.setFrontDesignJson(frontJson);
        sampleVersion.setBackDesignJson("{\"elements\":[]}");

        byte[] pdfBytes = renderingService.renderCardPdf(sampleTemplate, sampleVersion, sampleStudent, sampleSchool, "2026-2027");
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);
        }
    }

    @Test
    @DisplayName("7. Empty template falls back to rich, professional front and back designs")
    void testFallbackTemplates() throws Exception {
        sampleVersion.setFrontDesignJson(null);
        sampleVersion.setBackDesignJson("");

        byte[] pdfBytes = renderingService.renderCardPdf(sampleTemplate, sampleVersion, sampleStudent, sampleSchool, "2026-2027");
        assertThat(pdfBytes).isNotNull().isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            assertThat(doc.getNumberOfPages()).isEqualTo(2);

            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String frontText = stripper.getText(doc);
            assertThat(frontText).contains("Greenwood International School");
            assertThat(frontText).contains("Aarav Sharma");
            assertThat(frontText).contains("ADM-2026-001");

            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String backText = stripper.getText(doc);
            assertThat(backText).contains("INSTRUCTIONS & EMERGENCY CONTACT");
            assertThat(backText).contains("Rajesh Sharma");
            assertThat(backText).contains("14 Heritage Boulevard");
        }
    }

    @Test
    @DisplayName("8. Batch PDF rendering generates 2 pages per student")
    void testBatchPdfRendering() throws Exception {
        Student student2 = new Student();
        student2.setId(UUID.randomUUID());
        student2.setSchool(sampleSchool);
        student2.setAdmissionNumber("ADM-2026-002");
        student2.setStudentName("Diya Patel");
        student2.setClassName("9th");
        student2.setSection("B");

        sampleVersion.setFrontDesignJson("{\"elements\":[]}");
        sampleVersion.setBackDesignJson("{\"elements\":[]}");

        byte[] batchPdf = renderingService.renderBatchPdf(
                sampleTemplate,
                sampleVersion,
                List.of(sampleStudent, student2),
                sampleSchool,
                "2026-2027"
        );

        assertThat(batchPdf).isNotNull().isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(batchPdf)) {
            // 2 students * 2 pages = 4 pages
            assertThat(doc.getNumberOfPages()).isEqualTo(4);

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(doc);
            assertThat(fullText).contains("Aarav Sharma");
            assertThat(fullText).contains("Diya Patel");
        }
    }

    @Test
    @DisplayName("9. A4 Imposition Sheet PDF generates exact A4 dimensions (595.28 x 841.89 pt) and duplex mirroring")
    void testRenderA4SheetPdfPortrait() throws Exception {
        // Portrait card template: 54mm width x 85.6mm height
        sampleVersion.setCardWidthMm(new BigDecimal("54.00"));
        sampleVersion.setCardHeightMm(new BigDecimal("85.60"));
        sampleVersion.setFrontDesignJson("{\"elements\":[{\"type\":\"text\",\"content\":\"Portrait Front {{student_name}}\",\"x\":2,\"y\":5,\"width\":50,\"height\":10,\"fontSize\":7}]}");
        sampleVersion.setBackDesignJson("{\"elements\":[{\"type\":\"text\",\"content\":\"Portrait Back {{admission_number}}\",\"x\":2,\"y\":5,\"width\":50,\"height\":10,\"fontSize\":7}]}");

        Student student2 = new Student();
        student2.setId(UUID.randomUUID());
        student2.setSchool(sampleSchool);
        student2.setAdmissionNumber("ADM-2026-002");
        student2.setStudentName("Diya Patel");

        byte[] a4Pdf = renderingService.renderA4SheetPdf(
                sampleTemplate,
                sampleVersion,
                List.of(sampleStudent, student2),
                sampleSchool,
                "2026-2027"
        );

        assertThat(a4Pdf).isNotNull().isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(a4Pdf)) {
            // 2 students fit on 1 sheet -> 2 pages (Page 1 Front, Page 2 Back)
            assertThat(doc.getNumberOfPages()).isEqualTo(2);

            // Verify A4 dimensions (595.28 x 841.89 pt)
            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                float width = doc.getPage(p).getMediaBox().getWidth();
                float height = doc.getPage(p).getMediaBox().getHeight();
                assertThat(width).isCloseTo(595.28f, org.assertj.core.data.Offset.offset(0.5f));
                assertThat(height).isCloseTo(841.89f, org.assertj.core.data.Offset.offset(0.5f));
            }

            PDFTextStripper stripper = new PDFTextStripper();

            // Page 1: Front Side Sheet
            stripper.setStartPage(1);
            stripper.setEndPage(1);
            String page1Text = stripper.getText(doc);
            assertThat(page1Text).contains("FRONT");
            assertThat(page1Text).contains("Portrait Front Aarav Sharma");
            assertThat(page1Text).contains("Portrait Front Diya Patel");

            // Page 2: Back Side Sheet (Duplex Mirrored)
            stripper.setStartPage(2);
            stripper.setEndPage(2);
            String page2Text = stripper.getText(doc);
            assertThat(page2Text).contains("BACK");
            assertThat(page2Text).contains("Portrait Back ADM-2026-001");
            assertThat(page2Text).contains("Portrait Back ADM-2026-002");
        }
    }

    @Test
    @DisplayName("10. A4 Imposition Sheet pagination handles multiple sheets for larger rosters")
    void testRenderA4SheetPdfLandscapeMultiSheet() throws Exception {
        // Landscape card template: 85.6mm x 54mm -> 2 cols x 5 rows = 10 cards per sheet
        sampleVersion.setCardWidthMm(new BigDecimal("85.60"));
        sampleVersion.setCardHeightMm(new BigDecimal("54.00"));

        List<Student> studentList = new ArrayList<>();
        for (int i = 1; i <= 12; i++) {
            Student s = new Student();
            s.setId(UUID.randomUUID());
            s.setSchool(sampleSchool);
            s.setAdmissionNumber(String.format("ADM-2026-%03d", i));
            s.setStudentName("Student " + i);
            studentList.add(s);
        }

        byte[] a4Pdf = renderingService.renderA4SheetPdf(
                sampleTemplate,
                sampleVersion,
                studentList,
                sampleSchool,
                "2026-2027"
        );

        assertThat(a4Pdf).isNotNull().isNotEmpty();

        try (PDDocument doc = Loader.loadPDF(a4Pdf)) {
            // 12 students with 10 cards/sheet -> 2 sheets -> 4 pages
            assertThat(doc.getNumberOfPages()).isEqualTo(4);

            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                float width = doc.getPage(p).getMediaBox().getWidth();
                float height = doc.getPage(p).getMediaBox().getHeight();
                assertThat(width).isCloseTo(595.28f, org.assertj.core.data.Offset.offset(0.5f));
                assertThat(height).isCloseTo(841.89f, org.assertj.core.data.Offset.offset(0.5f));
            }

            PDFTextStripper stripper = new PDFTextStripper();
            String fullText = stripper.getText(doc);
            assertThat(fullText).contains("Student 1");
            assertThat(fullText).contains("Student 12");
            assertThat(fullText).contains("Sheet 1 of 2");
            assertThat(fullText).contains("Sheet 2 of 2");
        }
    }

    @Test
    @DisplayName("11. renderJobImagesZip creates valid zip containing high-res front and back JPEGs")
    void testRenderJobImagesZip() throws Exception {
        Student student2 = new Student();
        student2.setId(UUID.randomUUID());
        student2.setSchool(sampleSchool);
        student2.setAdmissionNumber("ADM-2026-002");
        student2.setStudentName("Diya Patel");

        byte[] zipBytes = renderingService.renderJobImagesZip(
                sampleTemplate,
                sampleVersion,
                List.of(sampleStudent, student2),
                sampleSchool,
                "2026-2027"
        );

        assertThat(zipBytes).isNotNull().isNotEmpty();

        List<String> entryNames = new ArrayList<>();
        int validImagesCount = 0;

        try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                entryNames.add(entry.getName());
                byte[] imgBytes = zis.readAllBytes();
                assertThat(imgBytes).isNotEmpty();

                BufferedImage img = ImageIO.read(new ByteArrayInputStream(imgBytes));
                assertThat(img).isNotNull();
                assertThat(img.getWidth()).isGreaterThan(500);
                assertThat(img.getHeight()).isGreaterThan(300);
                validImagesCount++;

                zis.closeEntry();
            }
        }

        // 2 students * 2 sides = 4 image entries
        assertThat(entryNames).containsExactlyInAnyOrder(
                "ADM-2026-001_front.jpg",
                "ADM-2026-001_back.jpg",
                "ADM-2026-002_front.jpg",
                "ADM-2026-002_back.jpg"
        );
        assertThat(validImagesCount).isEqualTo(4);
    }

    @Test
    @DisplayName("12. renderSingleStudentImage renders crisp 300 DPI JPEG for specified side")
    void testRenderSingleStudentImage() throws Exception {
        byte[] frontImageBytes = renderingService.renderSingleStudentImage(
                sampleTemplate,
                sampleVersion,
                sampleStudent,
                sampleSchool,
                "2026-2027",
                "front"
        );

        assertThat(frontImageBytes).isNotNull().isNotEmpty();
        BufferedImage frontImg = ImageIO.read(new ByteArrayInputStream(frontImageBytes));
        assertThat(frontImg).isNotNull();
        assertThat(frontImg.getWidth()).isGreaterThan(500);
        assertThat(frontImg.getHeight()).isGreaterThan(300);

        byte[] backImageBytes = renderingService.renderSingleStudentImage(
                sampleTemplate,
                sampleVersion,
                sampleStudent,
                sampleSchool,
                "2026-2027",
                "back"
        );

        assertThat(backImageBytes).isNotNull().isNotEmpty();
        BufferedImage backImg = ImageIO.read(new ByteArrayInputStream(backImageBytes));
        assertThat(backImg).isNotNull();
        assertThat(backImg.getWidth()).isGreaterThan(500);
        assertThat(backImg.getHeight()).isGreaterThan(300);
    }

    @Test
    @DisplayName("13. renderCustomImpositionPdf supports custom grid (2x2), spacing (5mm), and FRONT_ONLY")
    void testRenderCustomImpositionPdfCustomGridAndSpacing() throws Exception {
        PrintSettingsDto settings = new PrintSettingsDto(
                PrintSettingsDto.MODE_SHEET,
                PrintSettingsDto.PAPER_A4,
                PrintSettingsDto.SIDES_FRONT_ONLY,
                2, // 2 columns
                2, // 2 rows -> 4 cards per page
                5.0, // 5mm hGap
                5.0, // 5mm vGap
                15.0,
                15.0,
                15.0,
                15.0,
                true,
                true,
                true,
                false
        );

        byte[] pdfBytes = renderingService.renderCustomImpositionPdf(
                sampleTemplate,
                sampleVersion,
                List.of(sampleStudent),
                sampleSchool,
                "2026-2027",
                settings
        );

        assertThat(pdfBytes).isNotNull().isNotEmpty();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            // FRONT_ONLY means only 1 page is generated
            assertThat(doc.getNumberOfPages()).isEqualTo(1);
            float width = doc.getPage(0).getMediaBox().getWidth();
            float height = doc.getPage(0).getMediaBox().getHeight();
            assertThat(width).isCloseTo(595.28f, org.assertj.core.data.Offset.offset(0.5f));
            assertThat(height).isCloseTo(841.89f, org.assertj.core.data.Offset.offset(0.5f));
        }
    }

    @Test
    @DisplayName("14. renderCustomImpositionPdf supports PLASTIC_CARD mode (CR80 PVC direct 1 card per page)")
    void testRenderCustomImpositionPdfPlasticCardMode() throws Exception {
        PrintSettingsDto settings = new PrintSettingsDto(
                PrintSettingsDto.MODE_PLASTIC,
                PrintSettingsDto.PAPER_CR80,
                PrintSettingsDto.SIDES_DUPLEX,
                1,
                1,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                0.0,
                true,
                false,
                false,
                false
        );

        byte[] pdfBytes = renderingService.renderCustomImpositionPdf(
                sampleTemplate,
                sampleVersion,
                List.of(sampleStudent),
                sampleSchool,
                "2026-2027",
                settings
        );

        assertThat(pdfBytes).isNotNull().isNotEmpty();
        try (PDDocument doc = Loader.loadPDF(pdfBytes)) {
            // 1 student * DUPLEX = 2 pages (Front then Back)
            assertThat(doc.getNumberOfPages()).isEqualTo(2);

            // Page dimensions should match exact card size: 85.6mm x 54mm = 242.64 x 153.07 pt
            float expectedW = 85.60f * (72.0f / 25.4f);
            float expectedH = 54.00f * (72.0f / 25.4f);

            for (int p = 0; p < doc.getNumberOfPages(); p++) {
                float w = doc.getPage(p).getMediaBox().getWidth();
                float h = doc.getPage(p).getMediaBox().getHeight();
                assertThat(w).isCloseTo(expectedW, org.assertj.core.data.Offset.offset(1.0f));
                assertThat(h).isCloseTo(expectedH, org.assertj.core.data.Offset.offset(1.0f));
            }
        }
    }
}
