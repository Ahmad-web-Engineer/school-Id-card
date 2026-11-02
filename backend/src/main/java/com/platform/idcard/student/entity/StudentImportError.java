package com.platform.idcard.student.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "student_import_errors")
public class StudentImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private StudentImportBatch batch;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "field_name", length = 100)
    private String fieldName;

    @Column(name = "error_message", nullable = false, columnDefinition = "TEXT")
    private String errorMessage;

    public StudentImportError() {}

    public StudentImportError(StudentImportBatch batch, int rowNumber, String fieldName, String errorMessage) {
        this.batch = batch;
        this.rowNumber = rowNumber;
        this.fieldName = fieldName;
        this.errorMessage = errorMessage;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public StudentImportBatch getBatch() {
        return batch;
    }

    public void setBatch(StudentImportBatch batch) {
        this.batch = batch;
    }

    public int getRowNumber() {
        return rowNumber;
    }

    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
