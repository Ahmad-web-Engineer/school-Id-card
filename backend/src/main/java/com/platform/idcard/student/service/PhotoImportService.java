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

                // Parse filename
                ParsedFilename parsed = parseFileName(simpleName, selectedClass, selectedSection);

                // Match against student
                Student matchedStudent = findStudentForPhoto(schoolId, parsed);

                PhotoImportResult result = new PhotoImportResult();
                result.setBatch(batch);
                result.setFileName(simpleName);
                result.setParsedClass(parsed.className());
                result.setParsedSection(parsed.section());
                result.setParsedRollNumber(parsed.rollNumber());

                if (matchedStudent == null) {
                    result.setMatchStatus("NO_STUDENT_FOUND");
                    String desc = buildNoMatchMessage(parsed);
                    result.setMessage(desc);
                    unmatchedCount++;
                } else if (matchedStudentIdsInBatch.contains(matchedStudent.getId())) {
                    result.setMatchedStudent(matchedStudent);
                    result.setMatchStatus("DUPLICATE_PHOTO");
                    result.setMessage("Duplicate photo in archive for student: " + matchedStudent.getStudentName() +
                            " (" + matchedStudent.getAdmissionNumber() + ")");
                    duplicateCount++;
                } else {
                    // Store photo image file
                    String mimeType = "image/" + ("png".equals(extension) ? "png" : "jpeg");
                    FileEntity storedImage = fileStorageService.storeFile(
                            new ByteArrayInputStream(imageBytes),
                            simpleName,
                            mimeType,
                            imageBytes.length,
                            schoolId,
                            currentUserId
                    );

                    // Update student
                    matchedStudent.setPhotoFileId(storedImage.getId());
                    studentRepository.save(matchedStudent);

                    matchedStudentIdsInBatch.add(matchedStudent.getId());
                    result.setMatchedStudent(matchedStudent);
                    result.setPhotoFile(storedImage);
                    result.setMatchStatus("MATCHED");
                    result.setMessage(buildMatchedMessage(parsed, matchedStudent));
                    matchedCount++;
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

    private Student findStudentForPhoto(UUID schoolId, ParsedFilename parsed) {
        // Priority 1: Match by class + section + rollNumber
        if (parsed.className() != null && parsed.section() != null && parsed.rollNumber() != null) {
            Optional<Student> match = studentRepository.findBySchoolIdAndClassNameIgnoreCaseAndSectionIgnoreCaseAndRollNumber(
                    schoolId, parsed.className(), parsed.section(), parsed.rollNumber()
            );
            if (match.isPresent()) {
                return match.get();
            }
        }

        // Priority 2: Match by admission number
        if (parsed.admissionCandidate() != null) {
            Optional<Student> matchAdm = studentRepository.findBySchoolIdAndAdmissionNumberIgnoreCase(
                    schoolId, parsed.admissionCandidate()
            );
            if (matchAdm.isPresent()) {
                return matchAdm.get();
            }
        }

        // Priority 3: If roll number was parsed and there's a unique match by roll number in school (only if roll is provided)
        if (parsed.className() == null && parsed.section() == null && parsed.rollNumber() != null) {
            List<Student> students = studentRepository.findBySchoolIdAndRollNumber(schoolId, parsed.rollNumber());
            if (students.size() == 1) {
                return students.get(0);
            }
        }

        return null;
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

    private String buildMatchedMessage(ParsedFilename parsed, Student student) {
        if (parsed.className() != null && parsed.section() != null && parsed.rollNumber() != null) {
            return "Matched by class " + parsed.className() + "-" + parsed.section() + " roll number " + parsed.rollNumber();
        }
        return "Matched by admission number " + student.getAdmissionNumber();
    }

    private String buildNoMatchMessage(ParsedFilename parsed) {
        if (parsed.className() != null && parsed.section() != null && parsed.rollNumber() != null) {
            return "No student found with roll number " + parsed.rollNumber() + " in Class " + parsed.className() + "-" + parsed.section();
        }
        return "No student found matching filename " + parsed.admissionCandidate();
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
}
