package com.platform.idcard.student.entity;

import com.platform.idcard.file.entity.FileEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "photo_import_results")
public class PhotoImportResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private PhotoImportBatch batch;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "parsed_class", length = 100)
    private String parsedClass;

    @Column(name = "parsed_section", length = 50)
    private String parsedSection;

    @Column(name = "parsed_roll_number", length = 50)
    private String parsedRollNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "matched_student_id")
    private Student matchedStudent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "photo_file_id")
    private FileEntity photoFile;

    @Column(name = "match_status", nullable = false, length = 50)
    private String matchStatus;

    @Column(name = "message", columnDefinition = "TEXT")
    private String message;

    public PhotoImportResult() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public PhotoImportBatch getBatch() {
        return batch;
    }

    public void setBatch(PhotoImportBatch batch) {
        this.batch = batch;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getParsedClass() {
        return parsedClass;
    }

    public void setParsedClass(String parsedClass) {
        this.parsedClass = parsedClass;
    }

    public String getParsedSection() {
        return parsedSection;
    }

    public void setParsedSection(String parsedSection) {
        this.parsedSection = parsedSection;
    }

    public String getParsedRollNumber() {
        return parsedRollNumber;
    }

    public void setParsedRollNumber(String parsedRollNumber) {
        this.parsedRollNumber = parsedRollNumber;
    }

    public Student getMatchedStudent() {
        return matchedStudent;
    }

    public void setMatchedStudent(Student matchedStudent) {
        this.matchedStudent = matchedStudent;
    }

    public FileEntity getPhotoFile() {
        return photoFile;
    }

    public void setPhotoFile(FileEntity photoFile) {
        this.photoFile = photoFile;
    }

    public String getMatchStatus() {
        return matchStatus;
    }

    public void setMatchStatus(String matchStatus) {
        this.matchStatus = matchStatus;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
