package com.platform.idcard.agencyjob.service;

import com.platform.idcard.agencyjob.dto.*;
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
import com.platform.idcard.order.entity.PrintingOrderItem;
import com.platform.idcard.order.repository.PrintingOrderRepository;
import com.platform.idcard.organization.entity.Agency;
import com.platform.idcard.organization.repository.AgencyRepository;
import com.platform.idcard.template.entity.TemplateVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.*;

@Service
public class AgencyJobService {

    private static final Logger log = LoggerFactory.getLogger(AgencyJobService.class);

    private final AgencyAssignmentRepository agencyAssignmentRepository;
    private final AgencyJobItemRepository agencyJobItemRepository;
    private final PrintingOrderRepository printingOrderRepository;
    private final AgencyRepository agencyRepository;
    private final UserRepository userRepository;

    public AgencyJobService(
            AgencyAssignmentRepository agencyAssignmentRepository,
            AgencyJobItemRepository agencyJobItemRepository,
            PrintingOrderRepository printingOrderRepository,
            AgencyRepository agencyRepository,
            UserRepository userRepository
    ) {
        this.agencyAssignmentRepository = agencyAssignmentRepository;
        this.agencyJobItemRepository = agencyJobItemRepository;
        this.printingOrderRepository = printingOrderRepository;
        this.agencyRepository = agencyRepository;
        this.userRepository = userRepository;
    }

    /**
     * Super Admin: Assign an order to one or more printing agencies with quantity allocations.
     */
    @Transactional
    public List<AdminAgencyAssignmentResponse> assignOrderToAgencies(
            UUID orderId,
            UUID adminUserId,
            AssignOrderRequest request
    ) {
        PrintingOrder order = printingOrderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!OrderStatus.APPROVED.name().equalsIgnoreCase(order.getOrderStatus())
                && !OrderStatus.AWAITING_ASSIGNMENT.name().equalsIgnoreCase(order.getOrderStatus())) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Only APPROVED or AWAITING_ASSIGNMENT orders can be assigned. Current status: " + order.getOrderStatus()
            );
        }

        User adminUser = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;

        // Map existing allocated quantities per order item (excluding rejected/cancelled/reassigned)
        List<AgencyAssignment> existingAssignments = agencyAssignmentRepository.findAllByOrderId(orderId);
        Map<UUID, Integer> currentlyAllocated = new HashMap<>();
        for (AgencyAssignment ea : existingAssignments) {
            if (ea.getAssignmentStatus() != AssignmentStatus.REJECTED
                    && ea.getAssignmentStatus() != AssignmentStatus.CANCELLED
                    && ea.getAssignmentStatus() != AssignmentStatus.REASSIGNED) {
                for (AgencyJobItem item : ea.getItems()) {
                    UUID oiId = item.getOrderItem().getId();
                    currentlyAllocated.put(oiId, currentlyAllocated.getOrDefault(oiId, 0) + item.getAssignedQuantity());
                }
            }
        }

        // Map new requested quantities per order item
        Map<UUID, Integer> newlyRequested = new HashMap<>();
        for (AgencyAssignmentItemRequest aReq : request.assignments()) {
            for (JobItemAllocationRequest jReq : aReq.jobItems()) {
                newlyRequested.put(jReq.orderItemId(), newlyRequested.getOrDefault(jReq.orderItemId(), 0) + jReq.assignedQuantity());
            }
        }

        // Validate that total allocated does not exceed order item quantities
        Map<UUID, PrintingOrderItem> orderItemMap = new HashMap<>();
        for (PrintingOrderItem oi : order.getItems()) {
            orderItemMap.put(oi.getId(), oi);
        }

        for (Map.Entry<UUID, Integer> entry : newlyRequested.entrySet()) {
            UUID oiId = entry.getKey();
            PrintingOrderItem oi = orderItemMap.get(oiId);
            if (oi == null) {
                throw new ResourceNotFoundException("Order item not found with ID: " + oiId);
            }
            int totalAlloc = currentlyAllocated.getOrDefault(oiId, 0) + entry.getValue();
            if (totalAlloc > oi.getQuantity()) {
                throw new ValidationException(
                        "Allocated quantity (" + totalAlloc + ") exceeds total order item quantity (" +
                                oi.getQuantity() + ") for product: " + oi.getProductType()
                );
            }
        }

        // Persist each assignment
        List<AdminAgencyAssignmentResponse> createdResponses = new ArrayList<>();
        for (AgencyAssignmentItemRequest aReq : request.assignments()) {
            Agency agency = agencyRepository.findById(aReq.agencyId())
                    .orElseThrow(() -> new ResourceNotFoundException("Agency not found with ID: " + aReq.agencyId()));

            AgencyAssignment assignment = new AgencyAssignment();
            assignment.setAssignmentNumber(generateAssignmentNumber());
            assignment.setOrder(order);
            assignment.setAgency(agency);
            assignment.setAssignedBy(adminUser);
            assignment.setAssignedAt(OffsetDateTime.now());
            assignment.setExpectedDeliveryDate(aReq.expectedDeliveryDate());
            assignment.setAgreedRate(aReq.agreedRate());
            assignment.setAssignmentStatus(AssignmentStatus.ASSIGNED);
            assignment.setNotes(aReq.notes());

            int totalAssignedQty = 0;
            for (JobItemAllocationRequest jReq : aReq.jobItems()) {
                PrintingOrderItem oi = orderItemMap.get(jReq.orderItemId());
                AgencyJobItem jobItem = new AgencyJobItem();
                jobItem.setAssignment(assignment);
                jobItem.setOrderItem(oi);
                jobItem.setAssignedQuantity(jReq.assignedQuantity());
                jobItem.setPendingQuantity(jReq.assignedQuantity());
                jobItem.setPrintedQuantity(0);
                jobItem.setRejectedQuantity(0);
                jobItem.setAcceptedQuantity(0);

                assignment.getItems().add(jobItem);
                totalAssignedQty += jReq.assignedQuantity();
            }

            BigDecimal totalAgreed = aReq.agreedRate()
                    .multiply(BigDecimal.valueOf(totalAssignedQty))
                    .setScale(2, RoundingMode.HALF_UP);
            assignment.setTotalAgreedAmount(totalAgreed);

            AgencyAssignment saved = agencyAssignmentRepository.save(assignment);
            createdResponses.add(mapToAdminResponse(saved));
        }

        // Update Parent Order Status
        boolean allFullyAllocated = true;
        for (PrintingOrderItem oi : order.getItems()) {
            int allocated = currentlyAllocated.getOrDefault(oi.getId(), 0) + newlyRequested.getOrDefault(oi.getId(), 0);
            if (allocated < oi.getQuantity()) {
                allFullyAllocated = false;
                break;
            }
        }

        if (allFullyAllocated) {
            order.setOrderStatus(OrderStatus.ASSIGNED.name());
        } else {
            order.setOrderStatus(OrderStatus.AWAITING_ASSIGNMENT.name());
        }
        printingOrderRepository.save(order);

        return createdResponses;
    }

    /**
     * Super Admin: List all agency assignments with pagination and search.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminAgencyAssignmentResponse> getAdminAssignments(
            Pageable pageable,
            UUID orderId,
            UUID agencyId,
            String statusStr,
            String search
    ) {
        AssignmentStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = AssignmentStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }

        String searchPattern = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<AgencyAssignment> page = agencyAssignmentRepository.searchAdminAssignments(
                orderId, agencyId, status, searchPattern, pageable
        );

        List<AdminAgencyAssignmentResponse> content = page.getContent().stream()
                .map(this::mapToAdminResponse)
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
     * Super Admin: Get assignment details by ID.
     */
    @Transactional(readOnly = true)
    public AdminAgencyAssignmentDetailResponse getAdminAssignmentById(UUID assignmentId) {
        AgencyAssignment assignment = agencyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));

        return mapToAdminDetailResponse(assignment);
    }

    /**
     * Super Admin: Reassign job when rejected or pending.
     */
    @Transactional
    public AdminAgencyAssignmentResponse reassignJob(
            UUID assignmentId,
            UUID adminUserId,
            ReassignJobRequest request
    ) {
        AgencyAssignment oldAssignment = agencyAssignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found with ID: " + assignmentId));

        if (oldAssignment.getAssignmentStatus() != AssignmentStatus.REJECTED
                && oldAssignment.getAssignmentStatus() != AssignmentStatus.ASSIGNED) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Only REJECTED or pending ASSIGNED assignments can be reassigned. Current: " + oldAssignment.getAssignmentStatus()
            );
        }

        Agency newAgency = agencyRepository.findById(request.newAgencyId())
                .orElseThrow(() -> new ResourceNotFoundException("Agency not found with ID: " + request.newAgencyId()));

        User adminUser = adminUserId != null ? userRepository.findById(adminUserId).orElse(null) : null;

        // Mark old assignment as REASSIGNED
        oldAssignment.setAssignmentStatus(AssignmentStatus.REASSIGNED);
        oldAssignment.setReassignmentReason(request.reassignmentReason());
        agencyAssignmentRepository.save(oldAssignment);

        // Create new assignment linking to old assignment
        AgencyAssignment newAssignment = new AgencyAssignment();
        newAssignment.setAssignmentNumber(generateAssignmentNumber());
        newAssignment.setOrder(oldAssignment.getOrder());
        newAssignment.setAgency(newAgency);
        newAssignment.setAssignedBy(adminUser);
        newAssignment.setAssignedAt(OffsetDateTime.now());
        newAssignment.setExpectedDeliveryDate(request.expectedDeliveryDate() != null ? request.expectedDeliveryDate() : oldAssignment.getExpectedDeliveryDate());
        newAssignment.setAgreedRate(request.agreedRate());
        newAssignment.setAssignmentStatus(AssignmentStatus.ASSIGNED);
        newAssignment.setReassignedFrom(oldAssignment);
        newAssignment.setReassignmentReason(request.reassignmentReason());
        newAssignment.setNotes(oldAssignment.getNotes());

        int totalAssignedQty = 0;
        for (AgencyJobItem oldItem : oldAssignment.getItems()) {
            AgencyJobItem newItem = new AgencyJobItem();
            newItem.setAssignment(newAssignment);
            newItem.setOrderItem(oldItem.getOrderItem());
            newItem.setAssignedQuantity(oldItem.getAssignedQuantity());
            newItem.setPendingQuantity(oldItem.getAssignedQuantity());
            newItem.setPrintedQuantity(0);
            newItem.setRejectedQuantity(0);
            newItem.setAcceptedQuantity(0);

            newAssignment.getItems().add(newItem);
            totalAssignedQty += oldItem.getAssignedQuantity();
        }

        BigDecimal totalAgreed = request.agreedRate()
                .multiply(BigDecimal.valueOf(totalAssignedQty))
                .setScale(2, RoundingMode.HALF_UP);
        newAssignment.setTotalAgreedAmount(totalAgreed);

        AgencyAssignment saved = agencyAssignmentRepository.save(newAssignment);

        // Update Parent order to ASSIGNED
        PrintingOrder parentOrder = oldAssignment.getOrder();
        parentOrder.setOrderStatus(OrderStatus.ASSIGNED.name());
        printingOrderRepository.save(parentOrder);

        return mapToAdminResponse(saved);
    }

    /**
     * Printing Agency: List jobs assigned to the caller's agency.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AgencyJobSummaryResponse> getAgencyJobs(
            UUID agencyId,
            Pageable pageable,
            String statusStr,
            String search
    ) {
        AssignmentStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try {
                status = AssignmentStatus.valueOf(statusStr.toUpperCase().trim());
            } catch (IllegalArgumentException ignored) {}
        }

        String searchPattern = (search != null && !search.isBlank()) ? search.trim() : null;
        Page<AgencyAssignment> page = agencyAssignmentRepository.searchAgencyJobs(
                agencyId, status, searchPattern, pageable
        );

        List<AgencyJobSummaryResponse> content = page.getContent().stream()
                .map(this::mapToAgencySummaryResponse)
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
     * Printing Agency: Get job detail by ID with technical specs.
     */
    @Transactional(readOnly = true)
    public AgencyJobDetailResponse getAgencyJobById(UUID assignmentId, UUID agencyId) {
        AgencyAssignment assignment = agencyAssignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + assignmentId));

        return mapToAgencyDetailResponse(assignment);
    }

    /**
     * Printing Agency: Accept assigned job.
     */
    @Transactional
    public AgencyJobDetailResponse acceptJob(UUID assignmentId, UUID agencyId, UUID userId) {
        AgencyAssignment assignment = agencyAssignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + assignmentId));

        if (assignment.getAssignmentStatus() != AssignmentStatus.ASSIGNED) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Only ASSIGNED jobs can be accepted. Current: " + assignment.getAssignmentStatus()
            );
        }

        assignment.setAssignmentStatus(AssignmentStatus.AGENCY_ACCEPTED);
        agencyAssignmentRepository.save(assignment);

        // Check sibling assignments for parent order
        PrintingOrder parentOrder = assignment.getOrder();
        List<AgencyAssignment> allAssignments = agencyAssignmentRepository.findAllByOrderId(parentOrder.getId());
        boolean allActiveAccepted = allAssignments.stream()
                .filter(a -> a.getAssignmentStatus() != AssignmentStatus.REASSIGNED && a.getAssignmentStatus() != AssignmentStatus.CANCELLED)
                .allMatch(a -> a.getAssignmentStatus() == AssignmentStatus.AGENCY_ACCEPTED || a.getAssignmentStatus() == AssignmentStatus.IN_PRODUCTION);

        if (allActiveAccepted) {
            parentOrder.setOrderStatus(OrderStatus.AGENCY_ACCEPTED.name());
            printingOrderRepository.save(parentOrder);
        }

        return mapToAgencyDetailResponse(assignment);
    }

    /**
     * Printing Agency: Reject assigned job.
     */
    @Transactional
    public AgencyJobDetailResponse rejectJob(
            UUID assignmentId,
            UUID agencyId,
            UUID userId,
            RejectJobRequest request
    ) {
        AgencyAssignment assignment = agencyAssignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job not found with ID: " + assignmentId));

        if (assignment.getAssignmentStatus() != AssignmentStatus.ASSIGNED) {
            throw new ApiException(
                    ErrorCode.INVALID_ACTION,
                    "Only ASSIGNED jobs can be rejected. Current: " + assignment.getAssignmentStatus()
            );
        }

        assignment.setAssignmentStatus(AssignmentStatus.REJECTED);
        assignment.setRejectionReason(request.rejectionReason());
        agencyAssignmentRepository.save(assignment);

        // Parent order moves back to AWAITING_ASSIGNMENT
        PrintingOrder parentOrder = assignment.getOrder();
        parentOrder.setOrderStatus(OrderStatus.AWAITING_ASSIGNMENT.name());
        printingOrderRepository.save(parentOrder);

        return mapToAgencyDetailResponse(assignment);
    }

    // --- Private Helper Methods & Response Mappers ---

    private synchronized String generateAssignmentNumber() {
        int year = Year.now().getValue();
        long count = agencyAssignmentRepository.count() + 1;
        String number = String.format("JOB-%d-%04d", year, count);
        while (agencyAssignmentRepository.existsByAssignmentNumber(number)) {
            count++;
            number = String.format("JOB-%d-%04d", year, count);
        }
        return number;
    }

    private AdminAgencyAssignmentResponse mapToAdminResponse(AgencyAssignment a) {
        int totalQty = a.getItems().stream().mapToInt(AgencyJobItem::getAssignedQuantity).sum();
        List<AdminAgencyJobItemResponse> jobItems = a.getItems().stream()
                .map(i -> new AdminAgencyJobItemResponse(
                        i.getId(),
                        i.getOrderItem().getId(),
                        i.getOrderItem().getProductType(),
                        i.getAssignedQuantity(),
                        i.getPrintedQuantity(),
                        i.getRejectedQuantity(),
                        i.getAcceptedQuantity(),
                        i.getPendingQuantity()
                ))
                .toList();

        return new AdminAgencyAssignmentResponse(
                a.getId(),
                a.getAssignmentNumber(),
                a.getOrder().getId(),
                a.getOrder().getOrderNumber(),
                a.getOrder().getSchool() != null ? a.getOrder().getSchool().getSchoolName() : "Unknown School",
                a.getAgency().getId(),
                a.getAgency().getAgencyName(),
                a.getAssignedBy() != null ? a.getAssignedBy().getFullName() : "System Admin",
                a.getAssignedAt(),
                a.getExpectedDeliveryDate(),
                a.getAgreedRate(),
                a.getTotalAgreedAmount(),
                a.getAssignmentStatus(),
                a.getReassignedFrom() != null ? a.getReassignedFrom().getId() : null,
                a.getReassignmentReason(),
                a.getRejectionReason(),
                a.getNotes(),
                totalQty,
                jobItems
        );
    }

    private AdminAgencyAssignmentDetailResponse mapToAdminDetailResponse(AgencyAssignment a) {
        int totalQty = a.getItems().stream().mapToInt(AgencyJobItem::getAssignedQuantity).sum();
        List<AdminAgencyJobItemResponse> jobItems = a.getItems().stream()
                .map(i -> new AdminAgencyJobItemResponse(
                        i.getId(),
                        i.getOrderItem().getId(),
                        i.getOrderItem().getProductType(),
                        i.getAssignedQuantity(),
                        i.getPrintedQuantity(),
                        i.getRejectedQuantity(),
                        i.getAcceptedQuantity(),
                        i.getPendingQuantity()
                ))
                .toList();

        return new AdminAgencyAssignmentDetailResponse(
                a.getId(),
                a.getAssignmentNumber(),
                a.getOrder().getId(),
                a.getOrder().getOrderNumber(),
                a.getOrder().getSchool() != null ? a.getOrder().getSchool().getSchoolName() : "Unknown School",
                a.getAgency().getId(),
                a.getAgency().getAgencyName(),
                a.getAgency().getContactPerson(),
                a.getAgency().getEmail(),
                a.getAgency().getMobile(),
                a.getAssignedBy() != null ? a.getAssignedBy().getFullName() : "System Admin",
                a.getAssignedAt(),
                a.getExpectedDeliveryDate(),
                a.getAgreedRate(),
                a.getTotalAgreedAmount(),
                a.getAssignmentStatus(),
                a.getReassignedFrom() != null ? a.getReassignedFrom().getId() : null,
                a.getReassignmentReason(),
                a.getRejectionReason(),
                a.getNotes(),
                totalQty,
                jobItems
        );
    }

    private AgencyJobSummaryResponse mapToAgencySummaryResponse(AgencyAssignment a) {
        int totalQty = a.getItems().stream().mapToInt(AgencyJobItem::getAssignedQuantity).sum();
        return new AgencyJobSummaryResponse(
                a.getId(),
                a.getAssignmentNumber(),
                a.getOrder().getAcademicSession(),
                totalQty,
                a.getExpectedDeliveryDate(),
                a.getAssignmentStatus(),
                a.getAssignedAt()
        );
    }

    private AgencyJobDetailResponse mapToAgencyDetailResponse(AgencyAssignment a) {
        int totalQty = a.getItems().stream().mapToInt(AgencyJobItem::getAssignedQuantity).sum();
        List<AgencyJobItemResponse> items = a.getItems().stream()
                .map(i -> new AgencyJobItemResponse(
                        i.getId(),
                        i.getOrderItem().getProductType(),
                        i.getAssignedQuantity(),
                        i.getPrintedQuantity(),
                        i.getRejectedQuantity(),
                        i.getAcceptedQuantity(),
                        i.getPendingQuantity()
                ))
                .toList();

        TemplateVersion tv = a.getOrder().getTemplateVersion();
        AgencyCardSpecificationsResponse specs = tv != null
                ? new AgencyCardSpecificationsResponse(
                        tv.getCardWidthMm(),
                        tv.getCardHeightMm(),
                        tv.getBleedMarginMm(),
                        tv.getTemplate() != null ? tv.getTemplate().getTemplateName() : "Standard ID Card"
                  )
                : new AgencyCardSpecificationsResponse(
                        new BigDecimal("85.60"),
                        new BigDecimal("54.00"),
                        new BigDecimal("3.00"),
                        "Standard ID Card"
                  );

        return new AgencyJobDetailResponse(
                a.getId(),
                a.getAssignmentNumber(),
                a.getOrder().getAcademicSession(),
                totalQty,
                a.getExpectedDeliveryDate(),
                a.getAssignmentStatus(),
                a.getAssignedAt(),
                a.getNotes(),
                a.getRejectionReason(),
                specs,
                items
        );
    }
}
