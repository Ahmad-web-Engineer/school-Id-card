package com.platform.idcard.student.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.common.response.PaginationMeta;
import com.platform.idcard.file.entity.FileEntity;
import com.platform.idcard.file.service.FileStorageService;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.student.dto.StudentImportBatchDetailResponse;
import com.platform.idcard.student.dto.StudentImportBatchResponse;
import com.platform.idcard.student.dto.StudentImportConfirmResponse;
import com.platform.idcard.student.dto.StudentImportRowResponse;
import com.platform.idcard.student.entity.Student;
import com.platform.idcard.student.entity.StudentImportBatch;
import com.platform.idcard.student.entity.StudentImportError;
import com.platform.idcard.student.entity.StudentImportRow;
import com.platform.idcard.student.repository.StudentImportBatchRepository;
import com.platform.idcard.student.repository.StudentImportErrorRepository;
import com.platform.idcard.student.repository.StudentImportRowRepository;
import com.platform.idcard.student.repository.StudentRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
public class StudentImportService {

    private static final Logger log = LoggerFactory.getLogger(StudentImportService.class);

    private static final Set<String> VALID_GENDERS = Set.of("MALE", "FEMALE", "OTHER");
    private static final Set<String> VALID_BLOOD_GROUPS = Set.of("A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-");

    private static final List<DateTimeFormatter> DATE_FORMATTERS = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("d/M/yyyy")
    );

    private final StudentImportBatchRepository batchRepository;
    private final StudentImportRowRepository rowRepository;
    private final StudentImportErrorRepository errorRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public StudentImportService(
            StudentImportBatchRepository batchRepository,
            StudentImportRowRepository rowRepository,
            StudentImportErrorRepository errorRepository,
            StudentRepository studentRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService,
            ObjectMapper objectMapper
    ) {
        this.batchRepository = batchRepository;
        this.rowRepository = rowRepository;
        this.errorRepository = errorRepository;
        this.studentRepository = studentRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public StudentImportBatchResponse processImport(UUID schoolId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Uploaded file is empty", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Only .xlsx and .csv files are supported", HttpStatus.BAD_REQUEST);
        }

        String lowerName = originalFilename.toLowerCase();
        String fileType;
        if (lowerName.endsWith(".xlsx")) {
            fileType = "XLSX";
        } else if (lowerName.endsWith(".csv")) {
            fileType = "CSV";
        } else {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Only .xlsx and .csv files are supported", HttpStatus.BAD_REQUEST);
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + schoolId + " not found."));

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId).orElse(null);

        // Store original uploaded file
        FileEntity originalFile = fileStorageService.storeFile(file, schoolId, currentUserId);

        // Parse rows from file
        List<RawStudentRow> rawRows;
        try (InputStream is = file.getInputStream()) {
            if ("XLSX".equals(fileType)) {
                rawRows = parseXlsx(is);
            } else {
                rawRows = parseCsv(is);
            }
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INVALID_REQUEST_PAYLOAD, "Failed to read file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }

        if (rawRows.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Uploaded file contains no data rows", HttpStatus.BAD_REQUEST);
        }

        // Initialize batch
        StudentImportBatch batch = new StudentImportBatch();
        batch.setSchool(school);
        batch.setUploadedBy(currentUser);
        batch.setOriginalFile(originalFile);
        batch.setFileType(fileType);
        batch.setTotalRows(rawRows.size());
        batch.setImportStatus("VALIDATED");
        batch = batchRepository.save(batch);

        // Validate rows and keep track of in-file uniqueness
        Set<String> seenAdmissionsInFile = new HashSet<>();
        Set<String> seenClassSecRollInFile = new HashSet<>();

        int validCount = 0;
        int errorCount = 0;
        List<StudentImportRow> entityRows = new ArrayList<>(rawRows.size());
        List<StudentImportError> entityErrors = new ArrayList<>();

        for (RawStudentRow raw : rawRows) {
            List<FieldErrorDetail> rowErrors = new ArrayList<>();

            // 1. Mandatory value validations
            if (raw.admissionNumber == null || raw.admissionNumber.isBlank()) {
                rowErrors.add(new FieldErrorDetail("Admission Number", "Admission number is required"));
            }
            if (raw.studentName == null || raw.studentName.isBlank()) {
                rowErrors.add(new FieldErrorDetail("Student Name", "Student name is required"));
            }
            if (raw.className == null || raw.className.isBlank()) {
                rowErrors.add(new FieldErrorDetail("Class", "Class is required"));
            }
            if (raw.section == null || raw.section.isBlank()) {
                rowErrors.add(new FieldErrorDetail("Section", "Section is required"));
            }
            if (raw.rollNumber == null || raw.rollNumber.isBlank()) {
                rowErrors.add(new FieldErrorDetail("Roll Number", "Roll number is required"));
            }

            // 2. Date of birth validation
            if (raw.dateOfBirth != null && !raw.dateOfBirth.isBlank()) {
                LocalDate parsedDate = parseDate(raw.dateOfBirth.trim());
                if (parsedDate == null) {
                    rowErrors.add(new FieldErrorDetail("Date of Birth", "Invalid date of birth format. Use YYYY-MM-DD or DD-MM-YYYY"));
                }
            }

            // 3. Gender validation
            if (raw.gender != null && !raw.gender.isBlank()) {
                if (!VALID_GENDERS.contains(raw.gender.trim().toUpperCase())) {
                    rowErrors.add(new FieldErrorDetail("Gender", "Invalid gender"));
                }
            }

            // 4. Blood group validation
            if (raw.bloodGroup != null && !raw.bloodGroup.isBlank()) {
                if (!VALID_BLOOD_GROUPS.contains(raw.bloodGroup.trim().toUpperCase())) {
                    rowErrors.add(new FieldErrorDetail("Blood Group", "Invalid blood group"));
                }
            }

            // 5. In-file duplicate admission number
            if (raw.admissionNumber != null && !raw.admissionNumber.isBlank()) {
                String normalizedAdm = raw.admissionNumber.trim().toUpperCase();
                if (seenAdmissionsInFile.contains(normalizedAdm)) {
                    rowErrors.add(new FieldErrorDetail("Admission Number", "Duplicate admission number in file: " + raw.admissionNumber.trim()));
                } else {
                    seenAdmissionsInFile.add(normalizedAdm);
                }
            }

            // 6. In-file duplicate class + section + roll number
            if (raw.className != null && !raw.className.isBlank() &&
                raw.section != null && !raw.section.isBlank() &&
                raw.rollNumber != null && !raw.rollNumber.isBlank()) {
                String key = raw.className.trim().toUpperCase() + "_" + raw.section.trim().toUpperCase() + "_" + raw.rollNumber.trim();
                if (seenClassSecRollInFile.contains(key)) {
                    rowErrors.add(new FieldErrorDetail("Roll Number",
                            "Roll number " + raw.rollNumber.trim() + " is duplicate in Class " + raw.className.trim() + "-" + raw.section.trim()));
                } else {
                    seenClassSecRollInFile.add(key);
                }

                // 7. School-level roll number conflict with a different student
                if (raw.admissionNumber != null && !raw.admissionNumber.isBlank()) {
                    Optional<Student> existingWithRoll = studentRepository
                            .findBySchoolIdAndClassNameIgnoreCaseAndSectionIgnoreCaseAndRollNumber(
                                    schoolId, raw.className.trim(), raw.section.trim(), raw.rollNumber.trim()
                            );
                    if (existingWithRoll.isPresent() &&
                        !existingWithRoll.get().getAdmissionNumber().equalsIgnoreCase(raw.admissionNumber.trim())) {
                        rowErrors.add(new FieldErrorDetail("Roll Number",
                                "Roll number " + raw.rollNumber.trim() + " already assigned to student (" +
                                existingWithRoll.get().getAdmissionNumber() + ") in Class " + raw.className.trim() + "-" + raw.section.trim()));
                    }
                }
            }

            // Determine status
            boolean isValid = rowErrors.isEmpty();
            if (isValid) {
                validCount++;
            } else {
                errorCount++;
            }

            String errorMessage = null;
            if (!rowErrors.isEmpty()) {
                errorMessage = String.join("; ", rowErrors.stream().map(FieldErrorDetail::message).toList());
                for (FieldErrorDetail err : rowErrors) {
                    entityErrors.add(new StudentImportError(batch, raw.rowNumber, err.fieldName(), err.message()));
                }
            }

            StudentImportRow entityRow = new StudentImportRow();
            entityRow.setBatch(batch);
            entityRow.setRowNumber(raw.rowNumber);
            entityRow.setAdmissionNumber(raw.admissionNumber != null ? raw.admissionNumber.trim() : null);
            entityRow.setStudentName(raw.studentName != null ? raw.studentName.trim() : null);
            entityRow.setFatherName(raw.fatherName != null ? raw.fatherName.trim() : null);
            entityRow.setMotherName(raw.motherName != null ? raw.motherName.trim() : null);
            entityRow.setDateOfBirth(raw.dateOfBirth != null ? raw.dateOfBirth.trim() : null);
            entityRow.setGender(raw.gender != null ? raw.gender.trim().toUpperCase() : null);
            entityRow.setBloodGroup(raw.bloodGroup != null ? raw.bloodGroup.trim().toUpperCase() : null);
            entityRow.setClassName(raw.className != null ? raw.className.trim() : null);
            entityRow.setSection(raw.section != null ? raw.section.trim().toUpperCase() : null);
            entityRow.setRollNumber(raw.rollNumber != null ? raw.rollNumber.trim() : null);
            entityRow.setAddress(raw.address != null ? raw.address.trim() : null);
            entityRow.setMobile(raw.mobile != null ? raw.mobile.trim() : null);
            entityRow.setEmergencyContact(raw.emergencyContact != null ? raw.emergencyContact.trim() : null);
            entityRow.setValidationStatus(isValid ? "VALID" : "ERROR");
            entityRow.setErrorMessage(errorMessage);

            try {
                entityRow.setRawDataJson(objectMapper.writeValueAsString(raw));
            } catch (JsonProcessingException e) {
                entityRow.setRawDataJson("{}");
            }

            entityRows.add(entityRow);
        }

        rowRepository.saveAll(entityRows);
        if (!entityErrors.isEmpty()) {
            errorRepository.saveAll(entityErrors);
        }

        batch.setValidRows(validCount);
        batch.setErrorRows(errorCount);
        batch = batchRepository.save(batch);

        List<StudentImportRowResponse> preview = entityRows.stream()
                .limit(20)
                .map(StudentImportRowResponse::fromEntity)
                .toList();

        return StudentImportBatchResponse.ofUpload(batch, originalFilename, preview);
    }

    @Transactional(readOnly = true)
    public StudentImportBatchDetailResponse getBatchDetails(UUID schoolId, UUID batchId, int page, int size, String statusFilter) {
        StudentImportBatch batch = batchRepository.findByIdAndSchoolId(batchId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Import batch not found"));

        Pageable pageable = PageRequest.of(page, size, Sort.by("rowNumber").ascending());
        Page<StudentImportRow> pageRows;

        if (statusFilter != null && ("VALID".equalsIgnoreCase(statusFilter) || "ERROR".equalsIgnoreCase(statusFilter))) {
            pageRows = rowRepository.findByBatchIdAndValidationStatus(batchId, statusFilter.toUpperCase(), pageable);
        } else {
            pageRows = rowRepository.findByBatchId(batchId, pageable);
        }

        List<StudentImportRowResponse> rows = pageRows.getContent().stream()
                .map(StudentImportRowResponse::fromEntity)
                .toList();

        String originalFilename = batch.getOriginalFile() != null ? batch.getOriginalFile().getOriginalFileName() : "import_file";
        StudentImportBatchResponse batchResponse = StudentImportBatchResponse.ofDetail(batch, originalFilename, rows);

        PaginationMeta meta = PaginationMeta.of(
                pageRows.getNumber(),
                pageRows.getSize(),
                pageRows.getTotalElements(),
                pageRows.getTotalPages(),
                pageRows.isLast()
        );

        return StudentImportBatchDetailResponse.of(batchResponse, meta);
    }

    @Transactional
    public StudentImportConfirmResponse confirmBatchImport(UUID schoolId, UUID batchId) {
        StudentImportBatch batch = batchRepository.findByIdAndSchoolId(batchId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Import batch not found"));

        if (!"VALIDATED".equals(batch.getImportStatus())) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Batch is already imported or no valid rows available",
                    HttpStatus.BAD_REQUEST
            );
        }

        List<StudentImportRow> validRows = rowRepository.findByBatchIdAndValidationStatus(batchId, "VALID");
        if (validRows.isEmpty()) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Batch has no valid rows to import",
                    HttpStatus.BAD_REQUEST
            );
        }

        School school = batch.getSchool();
        int importedCount = 0;

        for (StudentImportRow row : validRows) {
            String admNo = row.getAdmissionNumber().trim();
            Optional<Student> existingOpt = studentRepository.findBySchoolIdAndAdmissionNumber(schoolId, admNo);

            Student student = existingOpt.orElseGet(Student::new);
            if (student.getId() == null) {
                student.setSchool(school);
                student.setAdmissionNumber(admNo);
            }

            student.setStudentName(row.getStudentName());
            student.setFatherName(row.getFatherName());
            student.setMotherName(row.getMotherName());
            if (row.getDateOfBirth() != null && !row.getDateOfBirth().isBlank()) {
                LocalDate dob = parseDate(row.getDateOfBirth().trim());
                student.setDateOfBirth(dob);
            }
            student.setGender(row.getGender());
            student.setBloodGroup(row.getBloodGroup());
            student.setClassName(row.getClassName());
            student.setSection(row.getSection());
            student.setRollNumber(row.getRollNumber());
            student.setAddress(row.getAddress());
            student.setMobile(row.getMobile());
            student.setEmergencyContact(row.getEmergencyContact());
            student.setStatus("ACTIVE");

            studentRepository.save(student);
            importedCount++;
        }

        batch.setImportedRows(importedCount);
        batch.setCompletedAt(Instant.now());
        batch.setImportStatus(batch.getErrorRows() > 0 ? "COMPLETED_WITH_ERRORS" : "COMPLETED");
        batchRepository.save(batch);

        String msg = "Successfully imported " + importedCount + " students." +
                (batch.getErrorRows() > 0 ? " " + batch.getErrorRows() + " rows were skipped due to errors." : "");

        return new StudentImportConfirmResponse(
                batch.getId(),
                batch.getTotalRows(),
                importedCount,
                batch.getErrorRows(),
                batch.getImportStatus(),
                msg
        );
    }

    @Transactional(readOnly = true)
    public byte[] generateErrorReportCsv(UUID schoolId, UUID batchId) {
        StudentImportBatch batch = batchRepository.findByIdAndSchoolId(batchId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Import batch not found"));

        List<StudentImportError> errors = errorRepository.findByBatchIdOrderByRowNumberAsc(batchId);
        List<StudentImportRow> allRows = rowRepository.findByBatchIdOrderByRowNumberAsc(batchId);
        Map<Integer, StudentImportRow> rowMap = new HashMap<>();
        for (StudentImportRow r : allRows) {
            rowMap.put(r.getRowNumber(), r);
        }

        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out, StandardCharsets.UTF_8);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.builder()
                     .setHeader("Row Number", "Admission Number", "Student Name", "Field Name", "Error Description")
                     .build())) {

            for (StudentImportError error : errors) {
                StudentImportRow row = rowMap.get(error.getRowNumber());
                String adm = row != null && row.getAdmissionNumber() != null ? row.getAdmissionNumber() : "";
                String name = row != null && row.getStudentName() != null ? row.getStudentName() : "";
                csvPrinter.printRecord(
                        error.getRowNumber(),
                        adm,
                        name,
                        error.getFieldName(),
                        error.getErrorMessage()
                );
            }
            csvPrinter.flush();
            return out.toByteArray();
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to generate error report CSV", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private LocalDate parseDate(String rawDate) {
        if (rawDate == null || rawDate.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(rawDate, formatter);
            } catch (DateTimeParseException ignored) {
            }
        }
        return null;
    }

    private List<RawStudentRow> parseXlsx(InputStream inputStream) {
        List<RawStudentRow> rows = new ArrayList<>();
        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Uploaded file contains no sheets", HttpStatus.BAD_REQUEST);
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new ApiException(ErrorCode.VALIDATION_FAILED, "Uploaded file contains no header row", HttpStatus.BAD_REQUEST);
            }

            Map<String, Integer> headerMap = new HashMap<>();
            for (Cell cell : headerRow) {
                String header = getCellValueAsString(cell).trim();
                String cleanHeader = cleanHeader(header);
                if (!cleanHeader.isEmpty()) {
                    headerMap.put(cleanHeader, cell.getColumnIndex());
                }
            }

            validateRequiredHeaders(headerMap);

            int lastRowNum = sheet.getLastRowNum();
            for (int r = 1; r <= lastRowNum; r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                RawStudentRow studentRow = new RawStudentRow();
                studentRow.rowNumber = r + 1; // 1-indexed for human readability
                studentRow.admissionNumber = getColumnValue(row, headerMap, "ADMISSION NUMBER");
                studentRow.studentName = getColumnValue(row, headerMap, "STUDENT NAME");
                studentRow.className = getColumnValue(row, headerMap, "CLASS");
                studentRow.section = getColumnValue(row, headerMap, "SECTION");
                studentRow.rollNumber = getColumnValue(row, headerMap, "ROLL NUMBER");
                studentRow.fatherName = getColumnValue(row, headerMap, "FATHER'S NAME", "FATHER NAME");
                studentRow.motherName = getColumnValue(row, headerMap, "MOTHER'S NAME", "MOTHER NAME");
                studentRow.dateOfBirth = getColumnValue(row, headerMap, "DATE OF BIRTH", "DOB");
                studentRow.gender = getColumnValue(row, headerMap, "GENDER");
                studentRow.bloodGroup = getColumnValue(row, headerMap, "BLOOD GROUP");
                studentRow.mobile = getColumnValue(row, headerMap, "MOBILE");
                studentRow.emergencyContact = getColumnValue(row, headerMap, "EMERGENCY CONTACT");
                studentRow.address = getColumnValue(row, headerMap, "ADDRESS");
                rows.add(studentRow);
            }
        } catch (IOException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Could not parse Excel spreadsheet: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return rows;
    }

    private List<RawStudentRow> parseCsv(InputStream inputStream) {
        List<RawStudentRow> rows = new ArrayList<>();
        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build().parse(reader)) {

            Map<String, Integer> headerMap = new HashMap<>();
            for (Map.Entry<String, Integer> entry : parser.getHeaderMap().entrySet()) {
                String clean = cleanHeader(entry.getKey());
                if (!clean.isEmpty()) {
                    headerMap.put(clean, entry.getValue());
                }
            }

            validateRequiredHeaders(headerMap);

            int rowIdx = 2; // header is row 1
            for (CSVRecord record : parser) {
                if (isCsvRecordEmpty(record)) {
                    rowIdx++;
                    continue;
                }
                RawStudentRow studentRow = new RawStudentRow();
                studentRow.rowNumber = rowIdx++;
                studentRow.admissionNumber = getCsvValue(record, headerMap, "ADMISSION NUMBER");
                studentRow.studentName = getCsvValue(record, headerMap, "STUDENT NAME");
                studentRow.className = getCsvValue(record, headerMap, "CLASS");
                studentRow.section = getCsvValue(record, headerMap, "SECTION");
                studentRow.rollNumber = getCsvValue(record, headerMap, "ROLL NUMBER");
                studentRow.fatherName = getCsvValue(record, headerMap, "FATHER'S NAME", "FATHER NAME");
                studentRow.motherName = getCsvValue(record, headerMap, "MOTHER'S NAME", "MOTHER NAME");
                studentRow.dateOfBirth = getCsvValue(record, headerMap, "DATE OF BIRTH", "DOB");
                studentRow.gender = getCsvValue(record, headerMap, "GENDER");
                studentRow.bloodGroup = getCsvValue(record, headerMap, "BLOOD GROUP");
                studentRow.mobile = getCsvValue(record, headerMap, "MOBILE");
                studentRow.emergencyContact = getCsvValue(record, headerMap, "EMERGENCY CONTACT");
                studentRow.address = getCsvValue(record, headerMap, "ADDRESS");
                rows.add(studentRow);
            }
        } catch (IOException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Could not parse CSV file: " + e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return rows;
    }

    private void validateRequiredHeaders(Map<String, Integer> headerMap) {
        List<String> missing = new ArrayList<>();
        if (!headerMap.containsKey("ADMISSION NUMBER")) missing.add("Admission Number");
        if (!headerMap.containsKey("STUDENT NAME")) missing.add("Student Name");
        if (!headerMap.containsKey("CLASS")) missing.add("Class");
        if (!headerMap.containsKey("SECTION")) missing.add("Section");
        if (!headerMap.containsKey("ROLL NUMBER")) missing.add("Roll Number");

        if (!missing.isEmpty()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "Required column missing: " + String.join(", ", missing),
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    private String cleanHeader(String header) {
        if (header == null) return "";
        return header.replace("*", "").trim().toUpperCase();
    }

    private String getColumnValue(Row row, Map<String, Integer> headerMap, String... headerAliases) {
        for (String alias : headerAliases) {
            Integer colIdx = headerMap.get(alias);
            if (colIdx != null) {
                Cell cell = row.getCell(colIdx);
                if (cell != null) {
                    return getCellValueAsString(cell).trim();
                }
            }
        }
        return null;
    }

    private String getCsvValue(CSVRecord record, Map<String, Integer> headerMap, String... headerAliases) {
        for (String alias : headerAliases) {
            Integer colIdx = headerMap.get(alias);
            if (colIdx != null && colIdx < record.size()) {
                String val = record.get(colIdx);
                return val != null ? val.trim() : null;
            }
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    LocalDate d = cell.getLocalDateTimeCellValue().toLocalDate();
                    yield d.format(DateTimeFormatter.ISO_LOCAL_DATE);
                } else {
                    double num = cell.getNumericCellValue();
                    if (num == (long) num) {
                        yield String.valueOf((long) num);
                    } else {
                        yield String.valueOf(num);
                    }
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK && !getCellValueAsString(cell).isBlank()) {
                return false;
            }
        }
        return true;
    }

    private boolean isCsvRecordEmpty(CSVRecord record) {
        for (String val : record) {
            if (val != null && !val.isBlank()) {
                return false;
            }
        }
        return true;
    }

    public static class RawStudentRow {
        public int rowNumber;
        public String admissionNumber;
        public String studentName;
        public String className;
        public String section;
        public String rollNumber;
        public String fatherName;
        public String motherName;
        public String dateOfBirth;
        public String gender;
        public String bloodGroup;
        public String mobile;
        public String emergencyContact;
        public String address;
    }

    private record FieldErrorDetail(String fieldName, String message) {}
}
