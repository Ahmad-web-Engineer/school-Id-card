package com.platform.idcard.student.entity;

import com.platform.idcard.auth.entity.User;
import com.platform.idcard.file.entity.FileEntity;
import com.platform.idcard.organization.entity.School;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "photo_import_batches")
public class PhotoImportBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private User uploadedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "original_file_id")
    private FileEntity originalFile;

    @Column(name = "selected_class", length = 100)
    private String selectedClass;

    @Column(name = "selected_section", length = 50)
    private String selectedSection;

    @Column(name = "total_files", nullable = false)
    private int totalFiles = 0;

    @Column(name = "matched_files", nullable = false)
    private int matchedFiles = 0;

    @Column(name = "unmatched_files", nullable = false)
    private int unmatchedFiles = 0;

    @Column(name = "duplicate_files", nullable = false)
    private int duplicateFiles = 0;

    @Column(name = "import_status", nullable = false, length = 50)
    private String importStatus = "PROCESSING";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public PhotoImportBatch() {}

    @PrePersist
    public void onPrePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public School getSchool() {
        return school;
    }

    public void setSchool(School school) {
        this.school = school;
    }

    public User getUploadedBy() {
        return uploadedBy;
    }

    public void setUploadedBy(User uploadedBy) {
        this.uploadedBy = uploadedBy;
    }

    public FileEntity getOriginalFile() {
        return originalFile;
    }

    public void setOriginalFile(FileEntity originalFile) {
        this.originalFile = originalFile;
    }

    public String getSelectedClass() {
        return selectedClass;
    }

    public void setSelectedClass(String selectedClass) {
        this.selectedClass = selectedClass;
    }

    public String getSelectedSection() {
        return selectedSection;
    }

    public void setSelectedSection(String selectedSection) {
        this.selectedSection = selectedSection;
    }

    public int getTotalFiles() {
        return totalFiles;
    }

    public void setTotalFiles(int totalFiles) {
        this.totalFiles = totalFiles;
    }

    public int getMatchedFiles() {
        return matchedFiles;
    }

    public void setMatchedFiles(int matchedFiles) {
        this.matchedFiles = matchedFiles;
    }

    public int getUnmatchedFiles() {
        return unmatchedFiles;
    }

    public void setUnmatchedFiles(int unmatchedFiles) {
        this.unmatchedFiles = unmatchedFiles;
    }

    public int getDuplicateFiles() {
        return duplicateFiles;
    }

    public void setDuplicateFiles(int duplicateFiles) {
        this.duplicateFiles = duplicateFiles;
    }

    public String getImportStatus() {
        return importStatus;
    }

    public void setImportStatus(String importStatus) {
        this.importStatus = importStatus;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
