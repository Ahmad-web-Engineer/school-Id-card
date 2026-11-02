package com.platform.idcard.student.controller;

import com.platform.idcard.common.response.ApiResponse;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.student.dto.StudentResponse;
import com.platform.idcard.student.service.StudentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/schools/{schoolId}/students")
@PreAuthorize("hasRole('SUPER_ADMIN')")
@Tag(name = "Admin Student Oversight", description = "Admin read-only verification of school student rosters")
public class AdminStudentController {

    private final StudentService studentService;

    public AdminStudentController(StudentService studentService) {
        this.studentService = studentService;
    }

    @GetMapping
    @Operation(summary = "List school students (Admin)", description = "Super admin inspection of a specific school's student roster")
    public ResponseEntity<PagedResponse<StudentResponse>> listStudents(
            @PathVariable UUID schoolId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String className,
            @RequestParam(required = false) String section,
            @RequestParam(required = false) String status
    ) {
        return ResponseEntity.ok(studentService.getStudents(schoolId, page, size, search, className, section, status));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get school student details (Admin)", description = "Super admin inspection of a specific student record")
    public ResponseEntity<ApiResponse<StudentResponse>> getStudent(
            @PathVariable UUID schoolId,
            @PathVariable UUID id
    ) {
        return ResponseEntity.ok(ApiResponse.ok(studentService.getStudentById(schoolId, id)));
    }
}
