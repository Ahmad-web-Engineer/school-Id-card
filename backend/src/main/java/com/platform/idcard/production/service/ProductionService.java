package com.platform.idcard.production.service;

import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.agencyjob.entity.AgencyJobItem;
import com.platform.idcard.agencyjob.entity.AssignmentStatus;
import com.platform.idcard.agencyjob.repository.AgencyAssignmentRepository;
import com.platform.idcard.agencyjob.repository.AgencyJobItemRepository;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.common.exception.ValidationException;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.common.response.PaginationMeta;
import com.platform.idcard.order.entity.OrderStatus;
import com.platform.idcard.order.entity.PrintingOrder;
import com.platform.idcard.order.repository.PrintingOrderRepository;
import com.platform.idcard.production.dto.*;
import com.platform.idcard.production.entity.*;
import com.platform.idcard.production.repository.ProductionBatchRepository;
import com.platform.idcard.production.repository.ProductionUpdateRepository;
import com.platform.idcard.production.repository.QualityCheckRepository;
import com.platform.idcard.production.repository.ReprintRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class ProductionService {

    private static final Logger log = LoggerFactory.getLogger(ProductionService.class);

    private final ProductionBatchRepository productionBatchRepository;
    private final ProductionUpdateRepository productionUpdateRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final ReprintRequestRepository reprintRequestRepository;
    private final AgencyAssignmentRepository agencyAssignmentRepository;
    private final AgencyJobItemRepository agencyJobItemRepository;
    private final PrintingOrderRepository printingOrderRepository;
    private final UserRepository userRepository;

    public ProductionService(
            ProductionBatchRepository productionBatchRepository,
            ProductionUpdateRepository productionUpdateRepository,
            QualityCheckRepository qualityCheckRepository,
            ReprintRequestRepository reprintRequestRepository,
            AgencyAssignmentRepository agencyAssignmentRepository,
            AgencyJobItemRepository agencyJobItemRepository,
            PrintingOrderRepository printingOrderRepository,
            UserRepository userRepository
    ) {
        this.productionBatchRepository = productionBatchRepository;
        this.productionUpdateRepository = productionUpdateRepository;
        this.qualityCheckRepository = qualityCheckRepository;
        this.reprintRequestRepository = reprintRequestRepository;
        this.agencyAssignmentRepository = agencyAssignmentRepository;
        this.agencyJobItemRepository = agencyJobItemRepository;
        this.printingOrderRepository = printingOrderRepository;
        this.userRepository = userRepository;
    }

    /**
     * Printing Agency: Transition accepted assignment into IN_PRODUCTION.
     */
    @Transactional
    public StartProductionResponse startProduction(UUID assignmentId, UUID agencyId, UUID userId) {
        AgencyAssignment assignment = agencyAssignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + assignmentId));

        if (assignment.getAssignmentStatus() != AssignmentStatus.AGENCY_ACCEPTED) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Only AGENCY_ACCEPTED jobs can start production. Current: " + assignment.getAssignmentStatus()
            );
        }

        assignment.setAssignmentStatus(AssignmentStatus.IN_PRODUCTION);
        agencyAssignmentRepository.save(assignment);

        // Update parent order status to IN_PRODUCTION if not already
        PrintingOrder parentOrder = assignment.getOrder();
        if (!OrderStatus.IN_PRODUCTION.name().equalsIgnoreCase(parentOrder.getOrderStatus())) {
            parentOrder.setOrderStatus(OrderStatus.IN_PRODUCTION.name());
            printingOrderRepository.save(parentOrder);
        }

        // Auto-create initial production batch if none exists
        List<ProductionBatch> existingBatches = productionBatchRepository.findAllByAssignmentId(assignmentId);
        if (existingBatches.isEmpty()) {
            int totalPlanned = assignment.getItems().stream().mapToInt(AgencyJobItem::getAssignedQuantity).sum();
            ProductionBatch initialBatch = new ProductionBatch();
            initialBatch.setAssignment(assignment);
            initialBatch.setBatchNumber(generateBatchNumber());
            initialBatch.setPlannedQuantity(totalPlanned > 0 ? totalPlanned : 1);
            initialBatch.setActualQuantity(0);
            initialBatch.setProductionStatus(ProductionBatchStatus.IN_PROGRESS);
            initialBatch.setStartedAt(OffsetDateTime.now());
            initialBatch.setNotes("Initial production batch");
            productionBatchRepository.save(initialBatch);
        }

        return new StartProductionResponse(
                assignment.getId(),
                assignment.getAssignmentNumber(),
                assignment.getAssignmentStatus().name(),
                "Production started successfully"
        );
    }

    /**
     * Printing Agency: Create planned or sub-batch for an assignment.
     */
    @Transactional
    public AgencyProductionBatchResponse createBatch(
            UUID assignmentId,
            UUID agencyId,
            UUID userId,
            CreateBatchRequest request
    ) {
        AgencyAssignment assignment = agencyAssignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + assignmentId));

        if (assignment.getAssignmentStatus() != AssignmentStatus.IN_PRODUCTION
                && assignment.getAssignmentStatus() != AssignmentStatus.AGENCY_ACCEPTED) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Cannot create batch for assignment with status: " + assignment.getAssignmentStatus()
            );
        }

        if (assignment.getAssignmentStatus() == AssignmentStatus.AGENCY_ACCEPTED) {
            assignment.setAssignmentStatus(AssignmentStatus.IN_PRODUCTION);
            agencyAssignmentRepository.save(assignment);
        }

        ProductionBatch batch = new ProductionBatch();
        batch.setAssignment(assignment);
        batch.setBatchNumber(generateBatchNumber());
        batch.setPlannedQuantity(request.plannedQuantity());
        batch.setActualQuantity(0);
        batch.setProductionStatus(ProductionBatchStatus.IN_PROGRESS);
        batch.setStartedAt(OffsetDateTime.now());
        batch.setNotes(request.notes());

        ProductionBatch saved = productionBatchRepository.save(batch);
        return mapToAgencyBatchResponse(saved);
    }

    /**
     * Printing Agency: List batches for an assigned job.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AgencyProductionBatchResponse> getAgencyBatches(
            UUID assignmentId,
            UUID agencyId,
            Pageable pageable
    ) {
        // Enforce agency access check
        agencyAssignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + assignmentId));

        Page<ProductionBatch> page = productionBatchRepository.findAllByAssignmentId(assignmentId, pageable);
        List<AgencyProductionBatchResponse> content = page.getContent().stream()
                .map(this::mapToAgencyBatchResponse)
                .toList();

        return PagedResponse.of(
                content,
                PaginationMeta.of(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isLast()
                )
        );
    }

    /**
     * Printing Agency: Post production progress update.
     */
    @Transactional
    public AgencyProductionBatchResponse addProductionUpdate(
            UUID batchId,
            UUID agencyId,
            UUID userId,
            ProductionUpdateRequest request
    ) {
        ProductionBatch batch = productionBatchRepository.findByIdAndAgencyId(batchId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Production batch not found with ID: " + batchId));

        if (batch.getProductionStatus() == ProductionBatchStatus.CANCELLED) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Cannot add progress updates to a CANCELLED batch"
            );
        }

        User updater = userId != null ? userRepository.findById(userId).orElse(null) : null;

        // Persist update log
        ProductionUpdate update = new ProductionUpdate();
        update.setBatch(batch);
        update.setUpdatedBy(updater);
        update.setPrintedQuantity(request.printedQuantity());
        update.setRejectedQuantity(request.rejectedQuantity());
        update.setRemarks(request.remarks());
        update.setEvidenceFileId(request.evidenceFileId());

        int pendingInBatch = Math.max(0, batch.getPlannedQuantity() - (batch.getActualQuantity() + request.printedQuantity()));
        update.setPendingQuantity(pendingInBatch);

        productionUpdateRepository.save(update);

        // Update batch actual quantity
        int newActual = batch.getActualQuantity() + request.printedQuantity();
        batch.setActualQuantity(newActual);

        if (batch.getProductionStatus() == ProductionBatchStatus.PLANNED) {
            batch.setProductionStatus(ProductionBatchStatus.IN_PROGRESS);
            batch.setStartedAt(OffsetDateTime.now());
        }

        if (newActual >= batch.getPlannedQuantity()) {
            batch.setProductionStatus(ProductionBatchStatus.PRINTING_COMPLETED);
            batch.setCompletedAt(OffsetDateTime.now());
        }

        ProductionBatch savedBatch = productionBatchRepository.save(batch);

        // Update AgencyJobItems printed/rejected quantities
        AgencyAssignment assignment = batch.getAssignment();
        int remainingToDistribute = request.printedQuantity();
        int rejectedToDistribute = request.rejectedQuantity();

        for (AgencyJobItem item : assignment.getItems()) {
            if (remainingToDistribute > 0) {
                int capacity = Math.max(0, item.getAssignedQuantity() - item.getPrintedQuantity());
                int toAdd = Math.min(capacity, remainingToDistribute);
                item.setPrintedQuantity(item.getPrintedQuantity() + toAdd);
                item.setPendingQuantity(Math.max(0, item.getAssignedQuantity() - item.getPrintedQuantity()));
                remainingToDistribute -= toAdd;
            }
            if (rejectedToDistribute > 0) {
                item.setRejectedQuantity(item.getRejectedQuantity() + rejectedToDistribute);
                rejectedToDistribute = 0;
            }
            agencyJobItemRepository.save(item);
        }

        // Check if all items in assignment are printed
        boolean allAssignmentPrinted = assignment.getItems().stream()
                .allMatch(i -> i.getPrintedQuantity() >= i.getAssignedQuantity());

        if (allAssignmentPrinted) {
            assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
            agencyAssignmentRepository.save(assignment);
        }

        // Recalculate parent order status
        PrintingOrder parentOrder = assignment.getOrder();
        List<AgencyAssignment> allAssignments = agencyAssignmentRepository.findAllByOrderId(parentOrder.getId());

        boolean allAssignmentsCompleted = allAssignments.stream()
                .filter(a -> a.getAssignmentStatus() != AssignmentStatus.REASSIGNED && a.getAssignmentStatus() != AssignmentStatus.CANCELLED)
                .allMatch(a -> a.getAssignmentStatus() == AssignmentStatus.COMPLETED);

        if (allAssignmentsCompleted) {
            parentOrder.setOrderStatus(OrderStatus.PRINTING_COMPLETED.name());
        } else {
            parentOrder.setOrderStatus(OrderStatus.PARTIALLY_COMPLETED.name());
        }
        printingOrderRepository.save(parentOrder);

        return mapToAgencyBatchResponse(savedBatch);
    }

    /**
     * Printing Agency: Submit quality check inspection report.
     */
    @Transactional
    public QualityCheckResponse submitQualityCheck(
            UUID assignmentId,
            UUID agencyId,
            UUID userId,
            QualityCheckRequest request
    ) {
        AgencyAssignment assignment = agencyAssignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + assignmentId));

        return processQualityCheck(assignment, userId, request);
    }

    /**
     * Super Admin: Perform Quality Check (Model B delivery or quality audit).
     */
    @Transactional
    public AdminQualityCheckResponse performAdminQualityCheck(
            UUID assignmentId,
            UUID adminUserId,
            QualityCheckRequest request
    ) {
        AgencyAssignment assignment = agencyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));

        QualityCheckResponse qc = processQualityCheck(assignment, adminUserId, request);
        return mapToAdminQualityCheckResponse(
                qualityCheckRepository.findById(qc.id()).orElseThrow()
        );
    }

    private QualityCheckResponse processQualityCheck(
            AgencyAssignment assignment,
            UUID userId,
            QualityCheckRequest request
    ) {
        if (request.inspectedQuantity() != request.acceptedQuantity() + request.rejectedQuantity()) {
            throw new ValidationException("Inspected quantity must equal sum of accepted and rejected quantities");
        }

        User inspector = userId != null ? userRepository.findById(userId).orElse(null) : null;

        QualityCheckStatus status;
        if (request.rejectedQuantity() == 0) {
            status = QualityCheckStatus.PASSED;
        } else if (request.acceptedQuantity() > 0) {
            status = QualityCheckStatus.CONDITIONALLY_PASSED;
        } else {
            status = QualityCheckStatus.FAILED;
        }

        QualityCheck qc = new QualityCheck();
        qc.setAssignment(assignment);
        qc.setCheckNumber(generateCheckNumber());
        qc.setInspectedQuantity(request.inspectedQuantity());
        qc.setAcceptedQuantity(request.acceptedQuantity());
        qc.setRejectedQuantity(request.rejectedQuantity());
        qc.setDefectCategory(request.defectCategory());
        qc.setDefectDescription(request.defectDescription());
        qc.setInspectionStatus(status);
        qc.setInspectedBy(inspector);
        qc.setNotes(request.notes());
        qc.setInspectedAt(OffsetDateTime.now());

        QualityCheck savedQc = qualityCheckRepository.save(qc);

        // Update AgencyJobItem accepted quantities
        int acceptedRemaining = request.acceptedQuantity();
        for (AgencyJobItem item : assignment.getItems()) {
            if (acceptedRemaining > 0) {
                int toAccept = Math.min(item.getPrintedQuantity() - item.getAcceptedQuantity(), acceptedRemaining);
                if (toAccept > 0) {
                    item.setAcceptedQuantity(item.getAcceptedQuantity() + toAccept);
                    agencyJobItemRepository.save(item);
                    acceptedRemaining -= toAccept;
                }
            }
        }

        // If defects were found, automatically create a reprint request for Admin review
        if (request.rejectedQuantity() > 0) {
            ReprintRequest reprint = new ReprintRequest();
            reprint.setAssignment(assignment);
            reprint.setQualityCheck(savedQc);
            reprint.setRequestNumber(generateReprintNumber());
            reprint.setReplacementQuantity(request.rejectedQuantity());
            reprint.setReason(request.defectDescription() != null && !request.defectDescription().isBlank()
                    ? request.defectDescription()
                    : "Quality defect detected during inspection: " + request.defectCategory());
            reprint.setCostBearer(CostBearer.AGENCY);
            reprint.setApprovalStatus(ReprintStatus.PENDING);
            reprintRequestRepository.save(reprint);

            // Transition parent order to REPRINT_REQUIRED
            PrintingOrder parentOrder = assignment.getOrder();
            parentOrder.setOrderStatus(OrderStatus.REPRINT_REQUIRED.name());
            printingOrderRepository.save(parentOrder);
        } else {
            // Quality check passed: if all printed cards are accepted, mark QC as complete
            PrintingOrder parentOrder = assignment.getOrder();
            if (!OrderStatus.REPRINT_REQUIRED.name().equalsIgnoreCase(parentOrder.getOrderStatus())) {
                parentOrder.setOrderStatus(OrderStatus.QUALITY_CHECK.name());
                printingOrderRepository.save(parentOrder);
            }
        }

        return mapToQualityCheckResponse(savedQc);
    }

    /**
     * Printing Agency: List quality checks for an assigned job.
     */
    @Transactional(readOnly = true)
    public PagedResponse<QualityCheckResponse> getAgencyQualityChecks(
            UUID assignmentId,
            UUID agencyId,
            Pageable pageable
    ) {
        // Enforce tenant boundary
        agencyAssignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + assignmentId));

        Page<QualityCheck> page = qualityCheckRepository.findAllByAssignmentId(assignmentId, pageable);
        List<QualityCheckResponse> content = page.getContent().stream()
                .map(this::mapToQualityCheckResponse)
                .toList();

        return PagedResponse.of(
                content,
                PaginationMeta.of(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isLast()
                )
        );
    }

    /**
     * Super Admin: List all production batches with filters.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminProductionBatchResponse> getAdminBatches(
            Pageable pageable,
            UUID assignmentId,
            UUID agencyId,
            String statusStr
    ) {
        ProductionBatchStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ProductionBatchStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<ProductionBatch> page = productionBatchRepository.searchBatches(
                assignmentId, agencyId, status, pageable
        );

        List<AdminProductionBatchResponse> content = page.getContent().stream()
                .map(this::mapToAdminBatchResponse)
                .toList();

        return PagedResponse.of(
                content,
                PaginationMeta.of(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isLast()
                )
        );
    }

    /**
     * Super Admin: List all quality checks with filters.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminQualityCheckResponse> getAdminQualityChecks(
            Pageable pageable,
            UUID assignmentId,
            UUID agencyId,
            String statusStr
    ) {
        QualityCheckStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = QualityCheckStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<QualityCheck> page = qualityCheckRepository.searchQualityChecks(
                assignmentId, agencyId, status, pageable
        );

        List<AdminQualityCheckResponse> content = page.getContent().stream()
                .map(this::mapToAdminQualityCheckResponse)
                .toList();

        return PagedResponse.of(
                content,
                PaginationMeta.of(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isLast()
                )
        );
    }

    /**
     * Super Admin: List all reprint requests with filters.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminReprintRequestResponse> getAdminReprintRequests(
            Pageable pageable,
            UUID assignmentId,
            String statusStr
    ) {
        ReprintStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = ReprintStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<ReprintRequest> page = reprintRequestRepository.searchReprintRequests(
                assignmentId, status, pageable
        );

        List<AdminReprintRequestResponse> content = page.getContent().stream()
                .map(this::mapToAdminReprintResponse)
                .toList();

        return PagedResponse.of(
                content,
                PaginationMeta.of(
                        page.getNumber(),
                        page.getSize(),
                        page.getTotalElements(),
                        page.getTotalPages(),
                        page.isLast()
                )
        );
    }

    /**
     * Super Admin: Approve reprint request and assign cost bearer.
     */
    @Transactional
    public AdminReprintRequestResponse approveReprintRequest(
            UUID reprintId,
            UUID adminUserId,
            ApproveReprintRequest request
    ) {
        ReprintRequest reprint = reprintRequestRepository.findById(reprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Reprint request not found with ID: " + reprintId));

        if (reprint.getApprovalStatus() != ReprintStatus.PENDING) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Only PENDING reprint requests can be approved. Current: " + reprint.getApprovalStatus()
            );
        }

        User adminUser = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;
        reprint.setApprovalStatus(ReprintStatus.APPROVED);
        reprint.setCostBearer(request.costBearer());
        reprint.setApprovedBy(adminUser);
        reprint.setApprovedAt(OffsetDateTime.now());

        ReprintRequest saved = reprintRequestRepository.save(reprint);

        // Put assignment back to IN_PRODUCTION for re-printing
        AgencyAssignment assignment = reprint.getAssignment();
        assignment.setAssignmentStatus(AssignmentStatus.IN_PRODUCTION);
        agencyAssignmentRepository.save(assignment);

        // Parent order moves back to IN_PRODUCTION
        PrintingOrder parentOrder = assignment.getOrder();
        parentOrder.setOrderStatus(OrderStatus.IN_PRODUCTION.name());
        printingOrderRepository.save(parentOrder);

        return mapToAdminReprintResponse(saved);
    }

    /**
     * Super Admin: Reject reprint request.
     */
    @Transactional
    public AdminReprintRequestResponse rejectReprintRequest(
            UUID reprintId,
            UUID adminUserId,
            RejectReprintRequest request
    ) {
        ReprintRequest reprint = reprintRequestRepository.findById(reprintId)
                .orElseThrow(() -> new ResourceNotFoundException("Reprint request not found with ID: " + reprintId));

        if (reprint.getApprovalStatus() != ReprintStatus.PENDING) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Only PENDING reprint requests can be rejected. Current: " + reprint.getApprovalStatus()
            );
        }

        reprint.setApprovalStatus(ReprintStatus.REJECTED);
        reprint.setRejectionReason(request.rejectionReason());

        ReprintRequest saved = reprintRequestRepository.save(reprint);
        return mapToAdminReprintResponse(saved);
    }

    /**
     * School: Track order progress (cards printed, verified, stages).
     * Non-Negotiable Rule 1: Zero agency details leaked to the school.
     */
    @Transactional(readOnly = true)
    public SchoolOrderTrackingResponse getSchoolOrderTracking(UUID orderId, UUID schoolId) {
        PrintingOrder order = printingOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!order.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Order not found with ID: " + orderId);
        }

        List<AgencyAssignment> assignments = agencyAssignmentRepository.findAllByOrderId(orderId);

        int totalCards = order.getTotalQuantity();
        int cardsPrinted = 0;
        int cardsVerified = 0;

        for (AgencyAssignment a : assignments) {
            if (a.getAssignmentStatus() != AssignmentStatus.REASSIGNED && a.getAssignmentStatus() != AssignmentStatus.CANCELLED) {
                for (AgencyJobItem item : a.getItems()) {
                    cardsPrinted += item.getPrintedQuantity();
                    cardsVerified += item.getAcceptedQuantity();
                }
            }
        }

        double percent = totalCards > 0 ? Math.min(100.0, (cardsPrinted * 100.0) / totalCards) : 0.0;

        String currentStage = mapStatusToStage(order.getOrderStatus());
        String stageDesc = mapStatusToDescription(order.getOrderStatus(), cardsPrinted, totalCards);

        return new SchoolOrderTrackingResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getOrderStatus(),
                totalCards,
                cardsPrinted,
                cardsVerified,
                Math.round(percent * 10.0) / 10.0,
                currentStage,
                stageDesc
        );
    }

    // --- Private Helper & Mapping Methods ---

    private synchronized String generateBatchNumber() {
        int year = Year.now().getValue();
        long count = productionBatchRepository.count() + 1;
        String number = String.format("BATCH-%d-%04d", year, count);
        while (productionBatchRepository.existsByBatchNumber(number)) {
            count++;
            number = String.format("BATCH-%d-%04d", year, count);
        }
        return number;
    }

    private synchronized String generateCheckNumber() {
        int year = Year.now().getValue();
        long count = qualityCheckRepository.count() + 1;
        String number = String.format("QC-%d-%04d", year, count);
        while (qualityCheckRepository.existsByCheckNumber(number)) {
            count++;
            number = String.format("QC-%d-%04d", year, count);
        }
        return number;
    }

    private synchronized String generateReprintNumber() {
        int year = Year.now().getValue();
        long count = reprintRequestRepository.count() + 1;
        String number = String.format("REP-%d-%04d", year, count);
        while (reprintRequestRepository.existsByRequestNumber(number)) {
            count++;
            number = String.format("REP-%d-%04d", year, count);
        }
        return number;
    }

    private AgencyProductionBatchResponse mapToAgencyBatchResponse(ProductionBatch pb) {
        return new AgencyProductionBatchResponse(
                pb.getId(),
                pb.getAssignment().getId(),
                pb.getBatchNumber(),
                pb.getPlannedQuantity(),
                pb.getActualQuantity(),
                pb.getProductionStatus(),
                pb.getStartedAt(),
                pb.getCompletedAt(),
                pb.getNotes(),
                pb.getCreatedAt()
        );
    }

    private AdminProductionBatchResponse mapToAdminBatchResponse(ProductionBatch pb) {
        return new AdminProductionBatchResponse(
                pb.getId(),
                pb.getAssignment().getId(),
                pb.getAssignment().getAssignmentNumber(),
                pb.getAssignment().getAgency().getId(),
                pb.getAssignment().getAgency().getAgencyName(),
                pb.getAssignment().getOrder().getId(),
                pb.getAssignment().getOrder().getOrderNumber(),
                pb.getBatchNumber(),
                pb.getPlannedQuantity(),
                pb.getActualQuantity(),
                pb.getProductionStatus(),
                pb.getStartedAt(),
                pb.getCompletedAt(),
                pb.getNotes(),
                pb.getCreatedAt()
        );
    }

    private QualityCheckResponse mapToQualityCheckResponse(QualityCheck qc) {
        return new QualityCheckResponse(
                qc.getId(),
                qc.getAssignment().getId(),
                qc.getCheckNumber(),
                qc.getInspectedQuantity(),
                qc.getAcceptedQuantity(),
                qc.getRejectedQuantity(),
                qc.getDefectCategory(),
                qc.getDefectDescription(),
                qc.getInspectionStatus(),
                qc.getInspectedBy() != null ? qc.getInspectedBy().getFullName() : "Quality Officer",
                qc.getNotes(),
                qc.getInspectedAt()
        );
    }

    private AdminQualityCheckResponse mapToAdminQualityCheckResponse(QualityCheck qc) {
        return new AdminQualityCheckResponse(
                qc.getId(),
                qc.getAssignment().getId(),
                qc.getAssignment().getAssignmentNumber(),
                qc.getAssignment().getAgency().getId(),
                qc.getAssignment().getAgency().getAgencyName(),
                qc.getAssignment().getOrder().getId(),
                qc.getAssignment().getOrder().getOrderNumber(),
                qc.getCheckNumber(),
                qc.getInspectedQuantity(),
                qc.getAcceptedQuantity(),
                qc.getRejectedQuantity(),
                qc.getDefectCategory(),
                qc.getDefectDescription(),
                qc.getInspectionStatus(),
                qc.getInspectedBy() != null ? qc.getInspectedBy().getFullName() : "Quality Officer",
                qc.getNotes(),
                qc.getInspectedAt()
        );
    }

    private AdminReprintRequestResponse mapToAdminReprintResponse(ReprintRequest rr) {
        return new AdminReprintRequestResponse(
                rr.getId(),
                rr.getAssignment().getId(),
                rr.getAssignment().getAssignmentNumber(),
                rr.getQualityCheck() != null ? rr.getQualityCheck().getId() : null,
                rr.getRequestNumber(),
                rr.getReplacementQuantity(),
                rr.getReason(),
                rr.getCostBearer(),
                rr.getApprovalStatus(),
                rr.getApprovedBy() != null ? rr.getApprovedBy().getFullName() : null,
                rr.getApprovedAt(),
                rr.getRejectionReason(),
                rr.getCreatedAt()
        );
    }

    private String mapStatusToStage(String status) {
        if (status == null) return "PREPARING";
        if (status.contains("DRAFT") || status.contains("SUBMITTED") || status.contains("REVIEW")) return "VERIFICATION";
        if (status.contains("QUOTATION") || status.contains("APPROVAL")) return "PAYMENT_AND_APPROVAL";
        if (status.contains("ASSIGNED") || status.contains("ACCEPTED")) return "QUEUE";
        if (status.contains("PRODUCTION") || status.contains("PARTIALLY")) return "PRINTING";
        if (status.contains("QUALITY") || status.contains("REPRINT")) return "QUALITY_INSPECTION";
        if (status.contains("DISPATCH") || status.contains("DELIVER")) return "DELIVERY";
        return status;
    }

    private String mapStatusToDescription(String status, int printed, int total) {
        if (status == null) return "Order is being prepared.";
        if (OrderStatus.IN_PRODUCTION.name().equalsIgnoreCase(status)
                || OrderStatus.PARTIALLY_COMPLETED.name().equalsIgnoreCase(status)) {
            return String.format("ID cards are actively being printed (%d of %d cards printed).", printed, total);
        }
        if (OrderStatus.PRINTING_COMPLETED.name().equalsIgnoreCase(status)) {
            return "Printing is 100% complete. Undergoing technical quality verification.";
        }
        if (OrderStatus.QUALITY_CHECK.name().equalsIgnoreCase(status)) {
            return "Cards are undergoing visual and dimensional quality inspection.";
        }
        if (OrderStatus.REPRINT_REQUIRED.name().equalsIgnoreCase(status)) {
            return "Defective cards identified during inspection. Fast-track reprint in progress.";
        }
        return "Order status: " + status;
    }
}
