package com.platform.idcard.file.service;

import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.file.entity.FileEntity;
import com.platform.idcard.file.repository.FileRepository;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.repository.SchoolRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Objects;
import java.util.UUID;

@Service
public class LocalFileStorageService implements FileStorageService {

    private final Path storageDirectory;
    private final FileRepository fileRepository;
    private final SchoolRepository schoolRepository;
    private final UserRepository userRepository;

    public LocalFileStorageService(
            @Value("${app.storage.location:uploads}") String storageLocation,
            FileRepository fileRepository,
            SchoolRepository schoolRepository,
            UserRepository userRepository
    ) {
        this.storageDirectory = Paths.get(storageLocation).toAbsolutePath().normalize();
        this.fileRepository = fileRepository;
        this.schoolRepository = schoolRepository;
        this.userRepository = userRepository;
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(this.storageDirectory);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Could not initialize file storage directory.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Path getStorageDirectory() {
        return storageDirectory;
    }

    @Override
    public Path resolveSafePath(String filename) {
        String cleanName = StringUtils.cleanPath(Objects.requireNonNullElse(filename, "file"));
        Path target = this.storageDirectory.resolve(cleanName).normalize();
        if (!target.startsWith(this.storageDirectory)) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Invalid storage path navigation attempt.", HttpStatus.BAD_REQUEST);
        }
        return target;
    }

    @Override
    public void validateZipEntryPath(String entryName, Path targetDirectory) {
        Path targetNormalized = targetDirectory.toAbsolutePath().normalize();
        Path entryNormalized = targetNormalized.resolve(entryName).normalize();
        if (!entryNormalized.startsWith(targetNormalized)) {
            throw new ApiException(
                    ErrorCode.VALIDATION_FAILED,
                    "Zip traversal vulnerability detected (Zip Slip): " + entryName,
                    HttpStatus.BAD_REQUEST
            );
        }
    }

    @Override
    @Transactional
    public FileEntity storeFile(MultipartFile file, UUID schoolId, UUID createdBy) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Uploaded file cannot be empty.", HttpStatus.BAD_REQUEST);
        }
        String originalFilename = StringUtils.cleanPath(Objects.requireNonNullElse(file.getOriginalFilename(), "unnamed_file"));
        String contentType = Objects.requireNonNullElse(file.getContentType(), "application/octet-stream");
        try {
            return storeFile(file.getInputStream(), originalFilename, contentType, file.getSize(), schoolId, createdBy);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to read uploaded file contents.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    @Transactional
    public FileEntity storeFile(byte[] content, String originalFilename, String contentType, UUID schoolId, UUID createdBy) {
        if (content == null || content.length == 0) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "File content cannot be empty.", HttpStatus.BAD_REQUEST);
        }
        ByteArrayInputStream bais = new ByteArrayInputStream(content);
        return storeFile(bais, originalFilename, contentType, content.length, schoolId, createdBy);
    }

    @Override
    @Transactional
    public FileEntity storeFile(InputStream inputStream, String originalFilename, String contentType, long fileSize, UUID schoolId, UUID createdBy) {
        String cleanedName = StringUtils.cleanPath(Objects.requireNonNullElse(originalFilename, "file"));
        String uniqueFileName = UUID.randomUUID() + "_" + cleanedName;
        Path destination = resolveSafePath(uniqueFileName);

        try {
            Files.copy(inputStream, destination, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Failed to persist file to disk.", HttpStatus.INTERNAL_SERVER_ERROR);
        }

        FileEntity fileEntity = new FileEntity();
        fileEntity.setFileName(uniqueFileName);
        fileEntity.setOriginalFileName(cleanedName);
        fileEntity.setContentType(contentType);
        fileEntity.setFileSize(fileSize);
        fileEntity.setStoragePath(destination.toString());

        if (schoolId != null) {
            School school = schoolRepository.findById(schoolId).orElse(null);
            fileEntity.setSchool(school);
        }

        if (createdBy != null) {
            User user = userRepository.findById(createdBy).orElse(null);
            fileEntity.setCreatedBy(user);
        }

        return fileRepository.save(fileEntity);
    }

    @Override
    public Resource loadAsResource(FileEntity fileEntity) {
        try {
            Path filePath = Paths.get(fileEntity.getStoragePath()).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File with ID " + fileEntity.getId() + " is missing on disk.");
            }
        } catch (MalformedURLException e) {
            throw new ApiException(ErrorCode.INTERNAL_SERVER_ERROR, "Could not read file from storage path.", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public Resource loadAsResource(UUID fileId) {
        FileEntity entity = getFile(fileId);
        return loadAsResource(entity);
    }

    @Override
    public FileEntity getFile(UUID fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> new ResourceNotFoundException("File with ID " + fileId + " not found."));
    }
}
