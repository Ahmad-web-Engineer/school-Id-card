package com.platform.idcard.student.controller;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.student.dto.StudentRequest;
import com.platform.idcard.student.dto.StudentResponse;
import com.platform.idcard.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/school/students")
@PreAuthorize("hasRole('SCHOOL')")
@Tag(name = "School Students", description = "School self-service student roster management")
public class SchoolStudentController {

    private final StudentService studentService;

    public SchoolStudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @Operation(summary = "List school students", description = "Paginated list of students filtered by caller school token")
    public ResponseEntity<PagedResponse<StudentResponse>> listStudents(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String status
    ) {
        // Non-negotiable rule 2: schoolId derived strictly from authenticated token
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        return ResponseEntity.ok(studentService.getStudents(schoolId, page, size, search, className, section, status));
    }

    @PostMapping
    @Operation(summary = "Create student", description = "Creates a new student record for caller's school")
    public ResponseEntity<ApiResponse<StudentResponse>> createStudent(@Valid @RequestBody StudentRequest request) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        StudentResponse response = studentService.createStudent(schoolId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get student details", description = "Retrieves student record if owned by caller's school")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(@PathVariable UUID id) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        StudentResponse response = studentService.getStudentById(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update student", description = "Updates student record if owned by caller's school")
    public ResponseEntity<ApiResponse<StudentResponse>> updateStudent(
            @PathVariable UUID id,
            @Valid @RequestBody StudentRequest request
    ) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        StudentResponse response = studentService.updateStudent(schoolId, id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Deactivate student", description = "Soft deactivates student record")
    public ResponseEntity<ApiResponse<Map<String, String>>> deactivateStudent(@PathVariable UUID id) {
        UUID schoolId = SecurityUtils.getRequiredSchoolId();
        studentService.deactivateStudent(schoolId, id);
        return ResponseEntity.ok(ApiResponse.ok(Map.of("message", "Student record deactivated successfully")));
    }
}
