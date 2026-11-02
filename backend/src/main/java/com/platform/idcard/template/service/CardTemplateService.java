package com.platform.idcard.template.service;

import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.BusinessConflictException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.common.response.PaginationMeta;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.student.entity.Student;
import com.platform.idcard.student.repository.StudentRepository;
import com.platform.idcard.template.dto.*;
import com.platform.idcard.template.entity.CardTemplate;
import com.platform.idcard.template.entity.TemplateApprovalStatus;
import com.platform.idcard.template.entity.TemplateAssignment;
import com.platform.idcard.template.entity.TemplateVersion;
import com.platform.idcard.template.repository.CardTemplateRepository;
import com.platform.idcard.template.repository.TemplateAssignmentRepository;
import com.platform.idcard.template.repository.TemplateVersionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class CardTemplateService {

    private final CardTemplateRepository cardTemplateRepository;
    private final TemplateVersionRepository templateVersionRepository;
    private final TemplateAssignmentRepository templateAssignmentRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final CardPdfRenderingService cardPdfRenderingService;

    public CardTemplateService(
            CardTemplateRepository cardTemplateRepository,
            TemplateVersionRepository templateVersionRepository,
            TemplateAssignmentRepository templateAssignmentRepository,
            SchoolRepository schoolRepository,
            StudentRepository studentRepository,
            CardPdfRenderingService cardPdfRenderingService
    ) {
        this.cardTemplateRepository = cardTemplateRepository;
        this.templateVersionRepository = templateVersionRepository;
        this.templateAssignmentRepository = templateAssignmentRepository;
        this.schoolRepository = schoolRepository;
        this.studentRepository = studentRepository;
        this.cardPdfRenderingService = cardPdfRenderingService;
    }

    /**
     * Admin: List templates with pagination, search and filters.
     */
    @Transactional(readOnly = true)
    public PagedResponse<CardTemplateResponse> getTemplates(
            int page,
            int size,
            String search,
            String category,
            String status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanCategory = (category != null && !category.isBlank()) ? category.trim().toUpperCase() : null;
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        Page<CardTemplate> paged = cardTemplateRepository.findWithFilters(cleanSearch, cleanCategory, cleanStatus, pageable);

        List<CardTemplateResponse> content = paged.getContent().stream().map(template -> {
            Optional<TemplateVersion> latestVersion = templateVersionRepository.findFirstByTemplateIdOrderByVersionNumberDesc(template.getId());
            Optional<TemplateVersion> approvedVersion = templateVersionRepository.findFirstByTemplateIdAndApprovalStatusOrderByVersionNumberDesc(
                    template.getId(), TemplateApprovalStatus.APPROVED.name()
            );

            Integer latestVersionNumber = latestVersion.map(TemplateVersion::getVersionNumber).orElse(0);
            String activeStatus = approvedVersion
                    .map(TemplateVersion::getApprovalStatus)
                    .orElseGet(() -> latestVersion.map(TemplateVersion::getApprovalStatus).orElse(null));

            UUID schoolId = template.getSchool() != null ? template.getSchool().getId() : null;
            String schoolName = template.getSchool() != null ? template.getSchool().getSchoolName() : "Global Platform Template";

            return new CardTemplateResponse(
                    template.getId(),
                    template.getTemplateName(),
                    template.getTemplateCategory(),
                    schoolId,
                    schoolName,
                    template.getStatus(),
                    latestVersionNumber,
                    activeStatus,
                    template.getCreatedAt()
            );
        }).toList();

        PaginationMeta meta = new PaginationMeta(
                paged.getNumber(),
                paged.getSize(),
                paged.getTotalElements(),
                paged.getTotalPages(),
                paged.isLast()
        );

        return new PagedResponse<>(true, content, meta);
    }

    /**
     * Admin: Create a new template master and initial version 1 (in DRAFT status).
     */
    @Transactional
    public CardTemplateDetailResponse createTemplate(CreateCardTemplateRequest request) {
        String cleanName = request.templateName().trim();
        if (cardTemplateRepository.existsByTemplateNameIgnoreCase(cleanName)) {
            throw new BusinessConflictException("Template with name '" + cleanName + "' already exists.");
        }

        School school = null;
        if (request.schoolId() != null) {
            school = schoolRepository.findById(request.schoolId())
                    .orElseThrow(() -> new ResourceNotFoundException("School not found with ID: " + request.schoolId()));
        }

        CardTemplate template = new CardTemplate();
        template.setTemplateName(cleanName);
        template.setTemplateCategory(
                (request.templateCategory() != null && !request.templateCategory().isBlank())
                        ? request.templateCategory().trim().toUpperCase()
                        : "STUDENT"
        );
        template.setSchool(school);
        template.setStatus("ACTIVE");
        template = cardTemplateRepository.save(template);

        TemplateVersion version1 = new TemplateVersion();
        version1.setTemplate(template);
        version1.setVersionNumber(1);
        version1.setCardWidthMm(request.cardWidthMm());
        version1.setCardHeightMm(request.cardHeightMm());
        version1.setBleedMarginMm(request.bleedMarginMm() != null ? request.bleedMarginMm() : new BigDecimal("3.00"));
        version1.setFrontDesignJson(
                (request.frontDesignJson() != null && !request.frontDesignJson().isBlank())
                        ? request.frontDesignJson()
                        : "{\"elements\":[]}"
        );
        version1.setBackDesignJson(
                (request.backDesignJson() != null && !request.backDesignJson().isBlank())
                        ? request.backDesignJson()
                        : "{\"elements\":[]}"
        );
        version1.setApprovalStatus(TemplateApprovalStatus.DRAFT.name());
        version1.setCreatedBy(SecurityUtils.getCurrentUserId());
        version1 = templateVersionRepository.save(version1);

        TemplateVersionResponse vResp = mapVersionResponse(version1);
        return new CardTemplateDetailResponse(
                template.getId(),
                template.getTemplateName(),
                template.getTemplateCategory(),
                template.getSchool() != null ? template.getSchool().getId() : null,
                template.getStatus(),
                List.of(vResp)
        );
    }

    /**
     * Admin: Get template details by ID including complete version history.
     */
    @Transactional(readOnly = true)
    public CardTemplateDetailResponse getTemplateById(UUID id) {
        CardTemplate template = cardTemplateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card template not found with ID: " + id));

        List<TemplateVersion> versions = templateVersionRepository.findByTemplateIdOrderByVersionNumberAsc(id);
        List<TemplateVersionResponse> versionResponses = versions.stream().map(this::mapVersionResponse).toList();

        return new CardTemplateDetailResponse(
                template.getId(),
                template.getTemplateName(),
                template.getTemplateCategory(),
                template.getSchool() != null ? template.getSchool().getId() : null,
                template.getStatus(),
                versionResponses
        );
    }

    /**
     * Admin: Save a new version for an existing template (versionNumber = max + 1, status = DRAFT).
     */
    @Transactional
    public TemplateVersionResponse createVersion(UUID templateId, CreateTemplateVersionRequest request) {
        CardTemplate template = cardTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Card template not found with ID: " + templateId));

        int nextVersionNumber = templateVersionRepository.findFirstByTemplateIdOrderByVersionNumberDesc(templateId)
                .map(v -> v.getVersionNumber() + 1)
                .orElse(1);

        TemplateVersion version = new TemplateVersion();
        version.setTemplate(template);
        version.setVersionNumber(nextVersionNumber);
        version.setCardWidthMm(request.cardWidthMm());
        version.setCardHeightMm(request.cardHeightMm());
        version.setBleedMarginMm(request.bleedMarginMm() != null ? request.bleedMarginMm() : new BigDecimal("3.00"));
        version.setFrontDesignJson(request.frontDesignJson());
        version.setBackDesignJson(request.backDesignJson());
        version.setApprovalStatus(TemplateApprovalStatus.DRAFT.name());
        version.setCreatedBy(SecurityUtils.getCurrentUserId());

        version = templateVersionRepository.save(version);
        return mapVersionResponse(version);
    }

    /**
     * Admin: Submit version for approval (DRAFT -> PENDING_APPROVAL).
     */
    @Transactional
    public TemplateVersionResponse submitVersion(UUID templateId, UUID versionId) {
        TemplateVersion version = templateVersionRepository.findByIdAndTemplateId(versionId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template version not found with ID: " + versionId));

        if (!TemplateApprovalStatus.DRAFT.name().equalsIgnoreCase(version.getApprovalStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Only DRAFT versions can be submitted");
        }

        version.setApprovalStatus(TemplateApprovalStatus.PENDING_APPROVAL.name());
        version = templateVersionRepository.save(version);
        return mapVersionResponse(version);
    }

    /**
     * Admin: Approve and lock template version.
     * Named business action: Moves target version to APPROVED.
     * Any previously APPROVED version for this template is moved to ARCHIVED.
     */
    @Transactional
    public TemplateVersionResponse approveVersion(UUID templateId, UUID versionId, UUID approverUserId) {
        TemplateVersion version = templateVersionRepository.findByIdAndTemplateId(versionId, templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Template version not found with ID: " + versionId));

        if (TemplateApprovalStatus.APPROVED.name().equalsIgnoreCase(version.getApprovalStatus())
                || TemplateApprovalStatus.ARCHIVED.name().equalsIgnoreCase(version.getApprovalStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Version is already approved or archived");
        }

        // Archive previously approved versions for this template
        List<TemplateVersion> previouslyApproved = templateVersionRepository.findByTemplateIdAndApprovalStatus(
                templateId, TemplateApprovalStatus.APPROVED.name()
        );
        for (TemplateVersion prev : previouslyApproved) {
            prev.setApprovalStatus(TemplateApprovalStatus.ARCHIVED.name());
            templateVersionRepository.save(prev);
        }

        // Set target version to APPROVED
        version.setApprovalStatus(TemplateApprovalStatus.APPROVED.name());
        version.setApprovedBy(approverUserId);
        version.setApprovedAt(Instant.now());
        version = templateVersionRepository.save(version);

        return mapVersionResponse(version);
    }

    /**
     * Admin: Assign an approved template to a school for an academic session.
     */
    @Transactional
    public TemplateAssignmentResponse assignTemplateToSchool(UUID templateId, AssignTemplateRequest request) {
        CardTemplate template = cardTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Card template not found with ID: " + templateId));

        boolean hasApprovedVersion = templateVersionRepository
                .findFirstByTemplateIdAndApprovalStatus(templateId, TemplateApprovalStatus.APPROVED.name())
                .isPresent();
        if (!hasApprovedVersion) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Template must have an approved version before assigning to schools");
        }

        School school = schoolRepository.findById(request.schoolId())
                .orElseThrow(() -> new ResourceNotFoundException("School not found with ID: " + request.schoolId()));

        String session = request.academicSession().trim();
        TemplateAssignment assignment = templateAssignmentRepository
                .findByTemplateIdAndSchoolIdAndAcademicSession(templateId, school.getId(), session)
                .orElseGet(() -> {
                    TemplateAssignment a = new TemplateAssignment();
                    a.setTemplate(template);
                    a.setSchool(school);
                    a.setAcademicSession(session);
                    return a;
                });

        assignment.setActive(request.active() != null ? request.active() : true);
        assignment = templateAssignmentRepository.save(assignment);

        return new TemplateAssignmentResponse(
                assignment.getId(),
                template.getId(),
                school.getId(),
                assignment.getAcademicSession(),
                "Template assigned to school successfully"
        );
    }

    /**
     * School: List assigned templates that have an APPROVED version.
     * Non-negotiable Rule 1: No agency data visible.
     */
    @Transactional(readOnly = true)
    public PagedResponse<SchoolTemplateResponse> getSchoolTemplates(UUID schoolId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<CardTemplate> pagedTemplates = cardTemplateRepository.findAvailableForSchool(schoolId, pageable);

        List<SchoolTemplateResponse> responses = new ArrayList<>();
        for (CardTemplate template : pagedTemplates.getContent()) {
            Optional<TemplateVersion> approvedOpt = templateVersionRepository
                    .findFirstByTemplateIdAndApprovalStatusOrderByVersionNumberDesc(
                            template.getId(), TemplateApprovalStatus.APPROVED.name()
                    );

            if (approvedOpt.isPresent()) {
                TemplateVersion approved = approvedOpt.get();
                responses.add(new SchoolTemplateResponse(
                        template.getId(),
                        template.getTemplateName(),
                        template.getTemplateCategory(),
                        approved.getId(),
                        approved.getVersionNumber(),
                        approved.getCardWidthMm(),
                        approved.getCardHeightMm(),
                        approved.getFrontDesignJson(),
                        approved.getBackDesignJson()
                ));
            }
        }

        PaginationMeta meta = new PaginationMeta(
                pagedTemplates.getNumber(),
                pagedTemplates.getSize(),
                pagedTemplates.getTotalElements(),
                pagedTemplates.getTotalPages(),
                pagedTemplates.isLast()
        );

        return new PagedResponse<>(true, responses, meta);
    }

    /**
     * Render high-resolution PDF preview for a template version with sample or actual student.
     */
    @Transactional(readOnly = true)
    public byte[] renderPreviewPdf(
            UUID templateId,
            UUID versionId,
            UUID optionalStudentId,
            UUID optionalSchoolId
    ) {
        CardTemplate template = cardTemplateRepository.findById(templateId)
                .orElseThrow(() -> new ResourceNotFoundException("Card template not found with ID: " + templateId));

        if (optionalSchoolId != null && !cardTemplateRepository.isTemplateAvailableToSchool(templateId, optionalSchoolId)) {
            throw new ResourceNotFoundException("Template is not assigned to this school");
        }

        TemplateVersion version;
        if (versionId != null) {
            version = templateVersionRepository.findByIdAndTemplateId(versionId, templateId)
                    .orElseThrow(() -> new ResourceNotFoundException("Template version not found with ID: " + versionId));
        } else {
            version = templateVersionRepository
                    .findFirstByTemplateIdAndApprovalStatusOrderByVersionNumberDesc(templateId, TemplateApprovalStatus.APPROVED.name())
                    .orElseGet(() -> templateVersionRepository.findFirstByTemplateIdOrderByVersionNumberDesc(templateId)
                            .orElseThrow(() -> new ResourceNotFoundException("No version available for this template")));
        }

        Student student = null;
        if (optionalStudentId != null) {
            if (optionalSchoolId != null) {
                student = studentRepository.findByIdAndSchoolId(optionalStudentId, optionalSchoolId)
                        .orElseThrow(() -> new ResourceNotFoundException("Student not found in this school"));
            } else {
                student = studentRepository.findById(optionalStudentId)
                        .orElseThrow(() -> new ResourceNotFoundException("Student not found with ID: " + optionalStudentId));
            }
        } else if (optionalSchoolId != null) {
            Page<Student> activeStudents = studentRepository.searchStudents(
                    optionalSchoolId, null, null, null, "ACTIVE", PageRequest.of(0, 1)
            );
            if (!activeStudents.isEmpty()) {
                student = activeStudents.getContent().get(0);
            }
        }

        School school = null;
        if (optionalSchoolId != null) {
            school = schoolRepository.findById(optionalSchoolId).orElse(null);
        } else if (student != null && student.getSchool() != null) {
            school = student.getSchool();
        } else if (template.getSchool() != null) {
            school = template.getSchool();
        }

        return cardPdfRenderingService.renderCardPdf(template, version, student, school, "2026-2027");
    }

    private TemplateVersionResponse mapVersionResponse(TemplateVersion version) {
        return new TemplateVersionResponse(
                version.getId(),
                version.getVersionNumber(),
                version.getCardWidthMm(),
                version.getCardHeightMm(),
                version.getBleedMarginMm(),
                version.getFrontDesignJson(),
                version.getBackDesignJson(),
                version.getApprovalStatus(),
                version.getCreatedBy(),
                version.getApprovedBy(),
                version.getApprovedAt(),
                version.getCreatedAt()
        );
    }
}
