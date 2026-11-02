package com.platform.idcard.student.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "student_import_rows")
public class StudentImportRow {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "batch_id", nullable = false)
    private StudentImportBatch batch;

    @Column(name = "row_number", nullable = false)
    private int rowNumber;

    @Column(name = "admission_number", length = 100)
    private String admissionNumber;

    @Column(name = "student_name", length = 255)
    private String studentName;

    @Column(name = "father_name", length = 255)
    private String fatherName;

    @Column(name = "mother_name", length = 255)
    private String motherName;

    @Column(name = "date_of_birth", length = 50)
    private String dateOfBirth;

    @Column(name = "gender", length = 20)
    private String gender;

    @Column(name = "blood_group", length = 10)
    private String bloodGroup;

    @Column(name = "class_name", length = 100)
    private String className;

    @Column(name = "section", length = 50)
    private String section;

    @Column(name = "roll_number", length = 50)
    private String rollNumber;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "mobile", length = 20)
    private String mobile;

    @Column(name = "emergency_contact", length = 20)
    private String emergencyContact;

    @Column(name = "raw_data_json", columnDefinition = "TEXT")
    private String rawDataJson;

    @Column(name = "validation_status", nullable = false, length = 50)
    private String validationStatus = "PENDING";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public StudentImportRow() {}

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

    public String getAdmissionNumber() {
        return admissionNumber;
    }

    public void setAdmissionNumber(String admissionNumber) {
        this.admissionNumber = admissionNumber;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getFatherName() {
        return fatherName;
    }

    public void setFatherName(String fatherName) {
        this.fatherName = fatherName;
    }

    public String getMotherName() {
        return motherName;
    }

    public void setMotherName(String motherName) {
        this.motherName = motherName;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getBloodGroup() {
        return bloodGroup;
    }

    public void setBloodGroup(String bloodGroup) {
        this.bloodGroup = bloodGroup;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public String getSection() {
        return section;
    }

    public void setSection(String section) {
        this.section = section;
    }

    public String getRollNumber() {
        return rollNumber;
    }

    public void setRollNumber(String rollNumber) {
        this.rollNumber = rollNumber;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getRawDataJson() {
        return rawDataJson;
    }

    public void setRawDataJson(String rawDataJson) {
        this.rawDataJson = rawDataJson;
    }

    public String getValidationStatus() {
        return validationStatus;
    }

    public void setValidationStatus(String validationStatus) {
        this.validationStatus = validationStatus;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
