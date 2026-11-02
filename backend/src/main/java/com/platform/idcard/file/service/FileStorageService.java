package com.platform.idcard.file.service;

import com.platform.idcard.file.entity.FileEntity;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.file.Path;
import java.util.UUID;

public interface FileStorageService {

    FileEntity storeFile(MultipartFile file, UUID schoolId, UUID createdBy);

    FileEntity storeFile(byte[] content, String originalFilename, String contentType, UUID schoolId, UUID createdBy);

    FileEntity storeFile(InputStream inputStream, String originalFilename, String contentType, long fileSize, UUID schoolId, UUID createdBy);

    Resource loadAsResource(FileEntity fileEntity);

    Resource loadAsResource(UUID fileId);

    FileEntity getFile(UUID fileId);

    Path getStorageDirectory();

    Path resolveSafePath(String filename);

    void validateZipEntryPath(String entryName, Path targetDirectory);
}
