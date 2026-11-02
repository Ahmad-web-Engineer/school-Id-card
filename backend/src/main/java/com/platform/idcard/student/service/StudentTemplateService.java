package com.platform.idcard.student.service;

import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class StudentTemplateService {

    public static final String[] HEADERS = {
            "Admission Number*",
            "Student Name*",
            "Class*",
            "Section*",
            "Roll Number*",
            "Father's Name",
            "Mother's Name",
            "Date of Birth",
            "Gender",
            "Blood Group",
            "Mobile",
            "Emergency Contact",
            "Address"
    };

    private static final List<List<String>> SAMPLE_ROWS = List.of(
            List.of(
                    "ADM-2026-001",
                    "Aarav Sharma",
                    "10th",
                    "A",
                    "1",
                    "Rajesh Sharma",
                    "Pooja Sharma",
                    "2010-05-15",
                    "MALE",
                    "B+",
                    "9876543210",
                    "9876543219",
                    "42 Lake View Apartments"
            ),
            List.of(
                    "ADM-2026-002",
                    "Diya Patel",
                    "10th",
                    "A",
                    "2",
                    "Suresh Patel",
                    "Meena Patel",
                    "2010-08-22",
                    "FEMALE",
                    "O+",
                    "9876543211",
                    "9876543218",
                    "15 Green Valley Residency"
            )
    );

    public byte[] generateXlsxTemplate() {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Students Template");

            // Header Style
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());

            CellStyle headerCellStyle = workbook.createCellStyle();
            headerCellStyle.setFont(headerFont);
            headerCellStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerCellStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerCellStyle.setAlignment(HorizontalAlignment.CENTER);

            // Create Header Row
            Row headerRow = sheet.createRow(0);
            for (int col = 0; col < HEADERS.length; col++) {
                Cell cell = headerRow.createCell(col);
                cell.setCellValue(HEADERS[col]);
                cell.setCellStyle(headerCellStyle);
            }

            // Create Sample Data Rows
            int rowIdx = 1;
            for (List<String> sampleRow : SAMPLE_ROWS) {
                Row row = sheet.createRow(rowIdx++);
                for (int col = 0; col < sampleRow.size(); col++) {
                    row.createCell(col).setCellValue(sampleRow.get(col));
                }
            }

            // Auto-size columns
            for (int col = 0; col < HEADERS.length; col++) {
                sheet.autoSizeColumn(col);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate XLSX template.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    public byte[] generateCsvTemplate() {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(HEADERS).build())) {

            for (List<String> sampleRow : SAMPLE_ROWS) {
                csvPrinter.printRecord(sampleRow);
            }
            csvPrinter.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate CSV template.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
