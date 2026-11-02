package com.platform.idcard.student.service;

import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.file.entity.FileEntity;
import com.platform.idcard.file.service.FileStorageService;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.student.dto.PhotoImportBatchResponse;
import com.platform.idcard.student.dto.PhotoImportResultResponse;
import com.platform.idcard.student.dto.StudentPhotoUploadResponse;
import com.platform.idcard.student.entity.PhotoImportBatch;
import com.platform.idcard.student.entity.PhotoImportResult;
import com.platform.idcard.student.entity.Student;
import com.platform.idcard.student.repository.PhotoImportBatchRepository;
import com.platform.idcard.student.repository.PhotoImportResultRepository;
import com.platform.idcard.student.repository.StudentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Service
public class PhotoImportService {

    private static final Logger log = LoggerFactory.getLogger(PhotoImportService.class);

    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    private final PhotoImportBatchRepository batchRepository;
    private final PhotoImportResultRepository resultRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;

    public PhotoImportService(
            PhotoImportBatchRepository batchRepository,
            PhotoImportResultRepository resultRepository,
            StudentRepository studentRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository,
            FileStorageService fileStorageService
    ) {
        this.batchRepository = batchRepository;
        this.resultRepository = resultRepository;
        this.studentRepository = studentRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public PhotoImportBatchResponse processBulkPhotoUpload(
            UUID schoolId,
            MultipartFile zipFile,
            String selectedClass,
            String selectedSection
    ) {
        if (zipFile == null || zipFile.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Uploaded file is empty", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = zipFile.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".zip")) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Only .zip files are supported", HttpStatus.BAD_REQUEST);
        }

        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + schoolId + " not found."));

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        User currentUser = userRepository.findById(currentUserId).orElse(null);

        // Store original uploaded ZIP archive
        FileEntity originalZipEntity = fileStorageService.storeFile(zipFile, schoolId, currentUserId);

        // Create Batch
        PhotoImportBatch batch = new PhotoImportBatch();
        batch.setSchool(school);
        batch.setUploadedBy(currentUser);
        batch.setOriginalFile(originalZipEntity);
        batch.setSelectedClass(selectedClass != null ? selectedClass.trim() : null);
        batch.setSelectedSection(selectedSection != null ? selectedSection.trim().toUpperCase() : null);
        batch.setImportStatus("PROCESSING");
        batch = batchRepository.save(batch);

        List<Student> allSchoolStudents = studentRepository.findBySchoolIdOrderByAdmissionNumberAsc(schoolId);
        String dominantPrefix = determineDominantAdmissionPrefix(allSchoolStudents);

        List<PhotoImportResult> results = new ArrayList<>();
        Set<UUID> matchedStudentIdsInBatch = new HashSet<>();
        int matchedCount = 0;
        int unmatchedCount = 0;
        int duplicateCount = 0;

        Path tempDir = fileStorageService.getStorageDirectory();

        try (ZipInputStream zis = new ZipInputStream(zipFile.getInputStream())) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                String entryName = entry.getName();

                // Zip-Slip defense verification
                fileStorageService.validateZipEntryPath(entryName, tempDir);

                // Ignore directories and OS metadata files
                if (entry.isDirectory() || isSystemFile(entryName)) {
                    zis.closeEntry();
                    continue;
                }

                String simpleName = getSimpleFileName(entryName);
                String extension = getFileExtension(simpleName).toLowerCase();

                if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
                    PhotoImportResult res = new PhotoImportResult();
                    res.setBatch(batch);
                    res.setFileName(simpleName);
                    res.setMatchStatus("BAD_IMAGE");
                    res.setMessage("Unsupported image format: " + extension);
                    results.add(res);
                    unmatchedCount++;
                    zis.closeEntry();
                    continue;
                }

                // Read image bytes
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    baos.write(buffer, 0, len);
                }
                byte[] imageBytes = baos.toByteArray();
                zis.closeEntry();

                if (imageBytes.length == 0) {
                    PhotoImportResult res = new PhotoImportResult();
                    res.setBatch(batch);
                    res.setFileName(simpleName);
                    res.setMatchStatus("BAD_IMAGE");
                    res.setMessage("Image file is empty or corrupt");
                    results.add(res);
                    unmatchedCount++;
                    continue;
                }

                // Always store the extracted photo image file so it can be assigned manually if needed
                String mimeType = "image/" + ("png".equals(extension) ? "png" : "jpeg");
                FileEntity storedImage = fileStorageService.storeFile(
                        new ByteArrayInputStream(imageBytes),
                        simpleName,
                        mimeType,
                        imageBytes.length,
                        schoolId,
                        currentUserId
                );

                // Parse filename
                ParsedFilename parsed = parseFileName(simpleName, selectedClass, selectedSection);

                // Perform multi-strategy matching against enrolled students
                MatchOutcome outcome = matchStudentForPhoto(
                        parsed,
                        allSchoolStudents,
                        matchedStudentIdsInBatch,
                        dominantPrefix
                );

                PhotoImportResult result = new PhotoImportResult();
                result.setBatch(batch);
                result.setFileName(simpleName);
                result.setParsedClass(parsed.className());
                result.setParsedSection(parsed.section());
                result.setParsedRollNumber(parsed.rollNumber());
                result.setPhotoFile(storedImage);
                result.setMatchStatus(outcome.status());
                result.setMessage(outcome.message());

                if ("MATCHED".equals(outcome.status())) {
                    Student matchedStudent = outcome.student();
                    matchedStudent.setPhotoFileId(storedImage.getId());
                    studentRepository.save(matchedStudent);

                    matchedStudentIdsInBatch.add(matchedStudent.getId());
                    result.setMatchedStudent(matchedStudent);
                    matchedCount++;
                } else if ("DUPLICATE_PHOTO".equals(outcome.status())) {
                    result.setMatchedStudent(outcome.student());
                    duplicateCount++;
                } else {
                    unmatchedCount++;
                }

                results.add(result);
            }
        } catch (ApiException ae) {
            throw ae;
        } catch (Exception e) {
            log.error("Failed to extract or process ZIP archive", e);
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "ZIP archive extraction failed: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST
            );
        }

        if (results.isEmpty()) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "ZIP archive contains no valid photo files",
                    HttpStatus.BAD_REQUEST
            );
        }

        resultRepository.saveAll(results);

        batch.setTotalFiles(results.size());
        batch.setMatchedFiles(matchedCount);
        batch.setUnmatchedFiles(unmatchedCount);
        batch.setDuplicateFiles(duplicateCount);
        batch.setImportStatus("COMPLETED");
        batchRepository.save(batch);

        List<PhotoImportResultResponse> resultDtos = results.stream()
                .map(PhotoImportResultResponse::fromEntity)
                .toList();

        return new PhotoImportBatchResponse(
                batch.getId(),
                batch.getTotalFiles(),
                batch.getMatchedFiles(),
                batch.getUnmatchedFiles(),
                batch.getDuplicateFiles(),
                batch.getImportStatus(),
                resultDtos
        );
    }

    @Transactional
    public PhotoImportBatchResponse assignPhotoResultToStudent(UUID schoolId, UUID resultId, UUID studentId) {
        PhotoImportResult result = resultRepository.findById(resultId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo import result not found"));

        PhotoImportBatch batch = result.getBatch();
        if (!batch.getSchool().getId().equals(schoolId)) {
            throw new ApiException(
                    ErrorCode.ACCESS_DENIED,
                    "You do not have permission to modify this photo batch.",
                    HttpStatus.FORBIDDEN
            );
        }

        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found in this school"));

        if (result.getPhotoFile() == null) {
            throw new ApiException(
                    ErrorCode.RESOURCE_NOT_FOUND,
                    "No photo file is associated with this result.",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Link photo file to student record
        student.setPhotoFileId(result.getPhotoFile().getId());
        studentRepository.save(student);

        // Update result
        String previousStatus = result.getMatchStatus();
        result.setMatchedStudent(student);
        result.setMatchStatus("MATCHED");
        result.setMessage("Manually assigned to " + student.getStudentName() + " (" + student.getAdmissionNumber() + ")");
        resultRepository.save(result);

        // Adjust batch counters if moving from unmatched or duplicate to matched
        if (!"MATCHED".equals(previousStatus)) {
            batch.setMatchedFiles(batch.getMatchedFiles() + 1);
            if ("DUPLICATE_PHOTO".equals(previousStatus)) {
                batch.setDuplicateFiles(Math.max(0, batch.getDuplicateFiles() - 1));
            } else {
                batch.setUnmatchedFiles(Math.max(0, batch.getUnmatchedFiles() - 1));
            }
            batchRepository.save(batch);
        }

        return getPhotoBatchReport(schoolId, batch.getId());
    }

    @Transactional(readOnly = true)
    public PhotoImportBatchResponse getPhotoBatchReport(UUID schoolId, UUID batchId) {
        PhotoImportBatch batch = batchRepository.findByIdAndSchoolId(batchId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Photo import batch not found"));

        List<PhotoImportResult> results = resultRepository.findByBatchIdOrderByIdAsc(batchId);
        List<PhotoImportResultResponse> resultDtos = results.stream()
                .map(PhotoImportResultResponse::fromEntity)
                .toList();

        return new PhotoImportBatchResponse(
                batch.getId(),
                batch.getTotalFiles(),
                batch.getMatchedFiles(),
                batch.getUnmatchedFiles(),
                batch.getDuplicateFiles(),
                batch.getImportStatus(),
                resultDtos
        );
    }

    @Transactional
    public StudentPhotoUploadResponse updateStudentPhoto(UUID schoolId, UUID studentId, MultipartFile file) {
        Student student = studentRepository.findByIdAndSchoolId(studentId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Student not found"));

        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Image file is required", HttpStatus.BAD_REQUEST);
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getFileExtension(Objects.requireNonNullElse(originalFilename, "")).toLowerCase();
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unsupported image format or corrupt file", HttpStatus.BAD_REQUEST);
        }

        // Validate content type
        String contentType = file.getContentType();
        if (contentType != null && !contentType.startsWith("image/")) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Unsupported image format or corrupt file", HttpStatus.BAD_REQUEST);
        }

        UUID currentUserId = SecurityUtils.getCurrentUserId();
        FileEntity fileEntity = fileStorageService.storeFile(file, schoolId, currentUserId);

        student.setPhotoFileId(fileEntity.getId());
        studentRepository.save(student);

        String photoUrl = "/api/v1/files/" + fileEntity.getId();
        return new StudentPhotoUploadResponse(
                student.getId(),
                fileEntity.getId(),
                photoUrl,
                "Photo uploaded and assigned successfully"
        );
    }

    private MatchOutcome matchStudentForPhoto(
            ParsedFilename parsed,
            List<Student> allStudents,
            Set<UUID> matchedStudentIds,
            String dominantPrefix
    ) {
        // Strategy 1: Explicit or parsed Class + Section + Roll Number
        if (parsed.className() != null && parsed.section() != null && parsed.rollNumber() != null) {
            String normReqClass = normalizeClassName(parsed.className());
            String normReqRoll = normalizeRoll(parsed.rollNumber());

            for (Student s : allStudents) {
                if (normalizeClassName(s.getClassName()).equalsIgnoreCase(normReqClass)
                        && s.getSection() != null && s.getSection().trim().equalsIgnoreCase(parsed.section().trim())
                        && normalizeRoll(s.getRollNumber()).equals(normReqRoll)) {
                    if (matchedStudentIds.contains(s.getId())) {
                        return new MatchOutcome(
                                "DUPLICATE_PHOTO",
                                s,
                                "Duplicate photo in archive for student: " + s.getStudentName() + " (" + s.getAdmissionNumber() + ")"
                        );
                    }
                    return new MatchOutcome(
                            "MATCHED",
                            s,
                            "Matched by Class " + s.getClassName() + "-" + s.getSection() + " roll number " + s.getRollNumber()
                    );
                }
            }
            // If class-sec-roll did not match, fall through to Strategy 2
            // because admission numbers like "ADM-P-001" also split into 3 parts.
        }

        // Strategy 2: Full Admission Number Match (Exact or Alphanumeric)
        if (parsed.admissionCandidate() != null && !parsed.admissionCandidate().isBlank()) {
            String candidate = parsed.admissionCandidate().trim();
            for (Student s : allStudents) {
                if (s.getAdmissionNumber().equalsIgnoreCase(candidate)) {
                    if (matchedStudentIds.contains(s.getId())) {
                        return new MatchOutcome(
                                "DUPLICATE_PHOTO",
                                s,
                                "Duplicate photo in archive for student: " + s.getStudentName() + " (" + s.getAdmissionNumber() + ")"
                        );
                    }
                    return new MatchOutcome(
                            "MATCHED",
                            s,
                            "Matched by admission number " + s.getAdmissionNumber()
                    );
                }
            }

            // Alphanumeric clean match (e.g. ADM2026001 vs ADM-2026-001)
            String cleanCandidate = candidate.replaceAll("[^a-zA-Z0-9]", "");
            if (cleanCandidate.length() >= 3) {
                for (Student s : allStudents) {
                    String cleanAdm = s.getAdmissionNumber().replaceAll("[^a-zA-Z0-9]", "");
                    if (cleanAdm.equalsIgnoreCase(cleanCandidate)) {
                        if (matchedStudentIds.contains(s.getId())) {
                            return new MatchOutcome(
                                    "DUPLICATE_PHOTO",
                                    s,
                                    "Duplicate photo in archive for student: " + s.getStudentName() + " (" + s.getAdmissionNumber() + ")"
                            );
                        }
                        return new MatchOutcome(
                                "MATCHED",
                                s,
                                "Matched by admission number " + s.getAdmissionNumber()
                        );
                    }
                }
            }
        }

        // Strategy 3: Numeric photo sequence / trailing numeric suffix of admission number
        // (e.g. 1.jpg -> ADM-2026-001, 50.jpg -> ADM-2026-050)
        Integer num = tryParseInt(parsed.rollNumber());
        if (num == null) {
            num = tryParseInt(parsed.admissionCandidate());
        }

        if (num != null) {
            final int targetNum = num;
            List<Student> suffixMatches = allStudents.stream()
                    .filter(s -> {
                        Integer sInt = extractTrailingInteger(s.getAdmissionNumber());
                        return sInt != null && sInt == targetNum;
                    })
                    .toList();

            if (!suffixMatches.isEmpty()) {
                Student chosen;
                if (suffixMatches.size() == 1) {
                    chosen = suffixMatches.get(0);
                } else {
                    // Prefer candidate not yet matched in this batch
                    List<Student> unMatched = suffixMatches.stream()
                            .filter(s -> !matchedStudentIds.contains(s.getId()))
                            .toList();
                    List<Student> pool = unMatched.isEmpty() ? suffixMatches : unMatched;

                    // Prefer candidate matching dominant admission prefix (e.g. ADM-2026- over GW-2026-)
                    chosen = pool.stream()
                            .filter(s -> extractPrefix(s.getAdmissionNumber()).equalsIgnoreCase(dominantPrefix))
                            .findFirst()
                            .orElse(pool.get(0));
                }

                if (matchedStudentIds.contains(chosen.getId())) {
                    return new MatchOutcome(
                            "DUPLICATE_PHOTO",
                            chosen,
                            "Duplicate photo in archive for student: " + chosen.getStudentName() + " (" + chosen.getAdmissionNumber() + ")"
                    );
                }

                return new MatchOutcome(
                        "MATCHED",
                        chosen,
                        "Matched by admission number " + chosen.getAdmissionNumber() + " (suffix #" + num + ")"
                );
            }

            // Strategy 4: Unique Roll Number Across School
            List<Student> rollMatches = allStudents.stream()
                    .filter(s -> normalizeRoll(s.getRollNumber()).equals(String.valueOf(targetNum)))
                    .toList();

            if (rollMatches.size() == 1) {
                Student s = rollMatches.get(0);
                if (matchedStudentIds.contains(s.getId())) {
                    return new MatchOutcome(
                            "DUPLICATE_PHOTO",
                            s,
                            "Duplicate photo in archive for student: " + s.getStudentName() + " (" + s.getAdmissionNumber() + ")"
                    );
                }
                return new MatchOutcome(
                        "MATCHED",
                        s,
                        "Matched by roll number " + s.getRollNumber() + " (Class " + s.getClassName() + "-" + s.getSection() + ")"
                );
            } else if (rollMatches.size() > 1) {
                String studentList = rollMatches.stream()
                        .map(s -> s.getStudentName() + " (" + s.getClassName() + "-" + s.getSection() + ")")
                        .collect(Collectors.joining(", "));
                return new MatchOutcome(
                        "AMBIGUOUS_MATCH",
                        null,
                        "Roll #" + targetNum + " matches multiple students: " + studentList + ". Use Target Class & Section or Assign manually."
                );
            }
        }

        return new MatchOutcome(
                "NO_STUDENT_FOUND",
                null,
                "No student found matching filename " + parsed.admissionCandidate()
        );
    }

    private ParsedFilename parseFileName(String fileName, String requestClass, String requestSection) {
        String baseName = getBaseName(fileName);

        // Normalize roll number: strip leading zeros, e.g. "07" -> "7"
        String strippedBase = stripLeadingZeros(baseName);

        // Case 1: Class and section supplied in request (Option A)
        if (requestClass != null && !requestClass.isBlank() && requestSection != null && !requestSection.isBlank()) {
            return new ParsedFilename(
                    requestClass.trim(),
                    requestSection.trim().toUpperCase(),
                    strippedBase,
                    baseName
            );
        }

        // Case 2: Filename format class_sec_roll (e.g. 10th_A_07 or 10_A_7 or 10th-A-07) (Option B)
        String[] parts = baseName.split("[_\\-]");
        if (parts.length >= 3) {
            String c = parts[0].trim();
            String s = parts[1].trim().toUpperCase();
            String r = stripLeadingZeros(parts[2].trim());
            return new ParsedFilename(c, s, r, baseName);
        }

        // Case 3: Admission number or pure roll number (Option C)
        return new ParsedFilename(
                null,
                null,
                strippedBase,
                baseName
        );
    }

    private Integer tryParseInt(String text) {
        if (text == null || text.isBlank()) return null;
        try {
            return Integer.parseInt(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer extractTrailingInteger(String text) {
        if (text == null || text.isBlank()) return null;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(".*?(\\d+)$").matcher(text.trim());
        if (m.matches()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private String extractPrefix(String admissionNumber) {
        if (admissionNumber == null) return "";
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(.*?)(?:\\d+)$").matcher(admissionNumber.trim());
        if (m.matches()) {
            return m.group(1).toUpperCase();
        }
        return admissionNumber.toUpperCase();
    }

    private String determineDominantAdmissionPrefix(List<Student> students) {
        if (students == null || students.isEmpty()) return "";
        Map<String, Long> prefixCounts = students.stream()
                .map(s -> extractPrefix(s.getAdmissionNumber()))
                .filter(p -> !p.isBlank())
                .collect(Collectors.groupingBy(p -> p, Collectors.counting()));

        return prefixCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("");
    }

    private String normalizeClassName(String className) {
        if (className == null) return "";
        String s = className.trim().toLowerCase();
        s = s.replaceAll("^(class|grade|std|standard)\\s*", "");
        s = s.replaceAll("(?<=\\d)(st|nd|rd|th)\\b", "");
        return s.trim();
    }

    private String normalizeRoll(String roll) {
        if (roll == null) return "";
        String stripped = roll.trim().replaceFirst("^0+(?!$)", "");
        return stripped.isEmpty() ? roll.trim() : stripped;
    }

    private String stripLeadingZeros(String s) {
        if (s == null) return null;
        String trimmed = s.trim();
        String stripped = trimmed.replaceFirst("^0+(?!$)", "");
        return stripped.isEmpty() ? trimmed : stripped;
    }

    private String getSimpleFileName(String fullPath) {
        int lastSlash = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
        return lastSlash >= 0 ? fullPath.substring(lastSlash + 1) : fullPath;
    }

    private String getBaseName(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx > 0 ? fileName.substring(0, dotIdx) : fileName;
    }

    private String getFileExtension(String fileName) {
        int dotIdx = fileName.lastIndexOf('.');
        return dotIdx >= 0 ? fileName.substring(dotIdx + 1) : "";
    }

    private boolean isSystemFile(String entryName) {
        String lower = entryName.toLowerCase();
        return lower.contains("__macosx") ||
                lower.endsWith(".ds_store") ||
                lower.endsWith("thumbs.db") ||
                lower.startsWith(".");
    }

    private record ParsedFilename(
            String className,
            String section,
            String rollNumber,
            String admissionCandidate
    ) {}

    private record MatchOutcome(
            String status,
            Student student,
            String message
    ) {}
}
