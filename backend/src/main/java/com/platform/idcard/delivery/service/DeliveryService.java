package com.platform.idcard.delivery.service;

import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.agencyjob.entity.AgencyJobItem;
import com.platform.idcard.agencyjob.entity.AssignmentStatus;
import com.platform.idcard.agencyjob.repository.AgencyAssignmentRepository;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.common.exception.ValidationException;
import com.platform.idcard.common.response.PaginationMeta;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.delivery.dto.*;
import com.platform.idcard.delivery.entity.*;
import com.platform.idcard.delivery.repository.DeliveryRecordRepository;
import com.platform.idcard.delivery.repository.DispatchRecordRepository;
import com.platform.idcard.order.entity.OrderStatus;
import com.platform.idcard.order.entity.PrintingOrder;
import com.platform.idcard.order.repository.PrintingOrderRepository;
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
public class DeliveryService {

    private static final Logger log = LoggerFactory.getLogger(DeliveryService.class);

    private final DispatchRecordRepository dispatchRepository;
    private final DeliveryRecordRepository deliveryRepository;
    private final AgencyAssignmentRepository assignmentRepository;
    private final PrintingOrderRepository orderRepository;
    private final UserRepository userRepository;

    public DeliveryService(
            DispatchRecordRepository dispatchRepository,
            DeliveryRecordRepository deliveryRepository,
            AgencyAssignmentRepository assignmentRepository,
            PrintingOrderRepository orderRepository,
            UserRepository userRepository
    ) {
        this.dispatchRepository = dispatchRepository;
        this.deliveryRepository = deliveryRepository;
        this.assignmentRepository = assignmentRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // 1. Agency Dispatch
    @Transactional
    public AgencyDispatchResponse agencyCreateDispatch(
            UUID assignmentId,
            UUID agencyId,
            UUID userId,
            CreateDispatchPayload request
    ) {
        AgencyAssignment assignment = assignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found or access denied"));

        if (assignment.getAssignmentStatus() == AssignmentStatus.REJECTED ||
                assignment.getAssignmentStatus() == AssignmentStatus.CANCELLED) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Cannot dispatch a cancelled or rejected assignment");
        }

        int totalAssigned = assignment.getItems() != null && !assignment.getItems().isEmpty()
                ? assignment.getItems().stream().mapToInt(AgencyJobItem::getAssignedQuantity).sum()
                : (assignment.getOrder() != null ? assignment.getOrder().getTotalQuantity() : 0);

        if (totalAssigned > 0 && request.dispatchQuantity() > totalAssigned) {
            throw new ValidationException("Dispatch quantity (" + request.dispatchQuantity() +
                    ") exceeds assigned job quantity (" + totalAssigned + ")");
        }

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        PrintingOrder order = assignment.getOrder();
        String currentYear = String.valueOf(Year.now().getValue());
        long count = dispatchRepository.countByDispatchNumberStartingWith("DSP-" + currentYear) + 1;
        String dispatchNumber = String.format("DSP-%s-%04d", currentYear, count);

        DispatchRecord record = new DispatchRecord();
        record.setAssignment(assignment);
        record.setOrder(order);
        record.setDispatchNumber(dispatchNumber);
        record.setCourierName(request.courierName());
        record.setTrackingNumber(request.trackingNumber());
        record.setDispatchQuantity(request.dispatchQuantity());
        record.setDispatchDate(OffsetDateTime.now());
        record.setDispatchStatus(DispatchStatus.IN_TRANSIT);
        record.setDispatchedBy(user);
        record.setDispatchNotes(request.dispatchNotes());
        record.setProofFileId(request.proofFileId());

        boolean isModelA = "DIRECT_TO_SCHOOL".equalsIgnoreCase(order.getDeliveryModel()) ||
                "MODEL_A".equalsIgnoreCase(order.getDeliveryModel());

        if (isModelA) {
            record.setDispatchType(DispatchType.AGENCY_TO_SCHOOL);
            assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
            order.setOrderStatus(OrderStatus.DISPATCHED.name());
        } else {
            record.setDispatchType(DispatchType.AGENCY_TO_HUB);
            assignment.setAssignmentStatus(AssignmentStatus.COMPLETED);
        }

        assignmentRepository.save(assignment);
        orderRepository.save(order);
        DispatchRecord saved = dispatchRepository.save(record);

        log.info("Dispatch created {} for assignment {} under model {}", dispatchNumber, assignmentId, order.getDeliveryModel());
        return mapToAgencyDispatch(saved);
    }

    // 2. Agency View Dispatches
    @Transactional(readOnly = true)
    public PagedResponse<AgencyDispatchResponse> getAgencyDispatches(UUID agencyId, Pageable pageable) {
        Page<DispatchRecord> page = dispatchRepository.findByAssignmentAgencyId(agencyId, pageable);
        List<AgencyDispatchResponse> content = page.getContent().stream().map(this::mapToAgencyDispatch).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AgencyDispatchResponse> getAgencyJobDispatches(UUID assignmentId, UUID agencyId, Pageable pageable) {
        assignmentRepository.findByIdAndAgencyId(assignmentId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Job assignment not found or access denied"));

        Page<DispatchRecord> page = dispatchRepository.findByAssignmentId(assignmentId, pageable);
        List<AgencyDispatchResponse> content = page.getContent().stream().map(this::mapToAgencyDispatch).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    // 3. Admin View Global Dispatches
    @Transactional(readOnly = true)
    public PagedResponse<AdminDispatchResponse> getAdminDispatches(Pageable pageable, UUID agencyId, String status) {
        DispatchStatus dispatchStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                dispatchStatus = DispatchStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<DispatchRecord> page = dispatchRepository.searchAdminDispatches(agencyId, dispatchStatus, pageable);
        List<AdminDispatchResponse> content = page.getContent().stream().map(this::mapToAdminDispatch).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    // 4. Admin Receive Consignment at Central Hub (Model B)
    @Transactional
    public AdminDispatchResponse adminReceiveAtHub(UUID dispatchId, UUID adminUserId, HubReceiveRequest request) {
        DispatchRecord dispatch = dispatchRepository.findById(dispatchId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispatch record not found: " + dispatchId));

        if (dispatch.getDispatchType() != DispatchType.AGENCY_TO_HUB) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Only consignments dispatched to central hub can be received here");
        }

        dispatch.setDispatchStatus(DispatchStatus.DELIVERED_TO_HUB);
        String updatedNotes = (dispatch.getDispatchNotes() != null ? dispatch.getDispatchNotes() + " | " : "") +
                "Hub Received (" + request.receivedQuantity() + " cards): " +
                (request.hubNotes() != null ? request.hubNotes() : "Condition: " + request.conditionStatus());
        dispatch.setDispatchNotes(updatedNotes);

        DispatchRecord saved = dispatchRepository.save(dispatch);
        log.info("Admin hub received dispatch {}", dispatch.getDispatchNumber());
        return mapToAdminDispatch(saved);
    }

    // 5. Admin Secondary Dispatch to School (Model B)
    @Transactional
    public AdminDispatchResponse adminSecondaryDispatch(
            UUID orderId,
            UUID adminUserId,
            AdminSecondaryDispatchRequest request
    ) {
        PrintingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Printing order not found: " + orderId));

        User user = null;
        if (adminUserId != null) {
            user = userRepository.findById(adminUserId).orElse(null);
        }

        String currentYear = String.valueOf(Year.now().getValue());
        long count = dispatchRepository.countByDispatchNumberStartingWith("DSP-" + currentYear) + 1;
        String dispatchNumber = String.format("DSP-%s-%04d", currentYear, count);

        DispatchRecord record = new DispatchRecord();
        record.setOrder(order);
        record.setDispatchNumber(dispatchNumber);
        record.setCourierName(request.courierName());
        record.setTrackingNumber(request.trackingNumber());
        record.setDispatchQuantity(request.dispatchQuantity());
        record.setDispatchDate(OffsetDateTime.now());
        record.setDispatchStatus(DispatchStatus.IN_TRANSIT);
        record.setDispatchType(DispatchType.HUB_TO_SCHOOL);
        record.setDispatchedBy(user);
        record.setDispatchNotes(request.notes());

        order.setOrderStatus(OrderStatus.DISPATCHED.name());
        orderRepository.save(order);
        DispatchRecord saved = dispatchRepository.save(record);

        log.info("Secondary hub dispatch created {} for order {}", dispatchNumber, order.getOrderNumber());
        return mapToAdminDispatch(saved);
    }

    // 6. School Confirm Delivery Receipt
    @Transactional
    public SchoolDeliveryResponse schoolConfirmDelivery(
            UUID orderId,
            UUID schoolId,
            ConfirmDeliveryRequest request
    ) {
        PrintingOrder order = orderRepository.findByIdAndSchoolId(orderId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied"));

        String currentYear = String.valueOf(Year.now().getValue());
        long count = deliveryRepository.countByDeliveryNumberStartingWith("DEL-" + currentYear) + 1;
        String deliveryNumber = String.format("DEL-%s-%04d", currentYear, count);

        // Find associated active dispatch if any
        List<DispatchRecord> dispatches = dispatchRepository.findByOrderId(orderId);
        DispatchRecord dispatch = dispatches.isEmpty() ? null : dispatches.get(dispatches.size() - 1);
        if (dispatch != null) {
            dispatch.setDispatchStatus(DispatchStatus.DELIVERED_TO_SCHOOL);
            dispatchRepository.save(dispatch);
        }

        DeliveryModel model = DeliveryModel.DIRECT_TO_SCHOOL;
        if ("VIA_ADMIN_HUB".equalsIgnoreCase(order.getDeliveryModel()) ||
                "MODEL_B".equalsIgnoreCase(order.getDeliveryModel())) {
            model = DeliveryModel.VIA_ADMIN_HUB;
        }

        DeliveryRecord delivery = new DeliveryRecord();
        delivery.setOrder(order);
        delivery.setDispatch(dispatch);
        delivery.setDeliveryNumber(deliveryNumber);
        delivery.setDeliveryModel(model);
        delivery.setDeliveryQuantity(request.receivedQuantity());
        delivery.setReceivedBy(request.receivedBy());
        delivery.setReceivedMobile(request.receivedMobile());
        delivery.setDeliveryNotes(request.deliveryNotes());
        delivery.setProofOfDeliveryFileId(request.proofOfDeliveryFileId());
        delivery.setDeliveryStatus(DeliveryStatus.CONFIRMED);
        delivery.setDeliveredAt(OffsetDateTime.now());

        DeliveryRecord saved = deliveryRepository.save(delivery);

        // Calculate total delivered across deliveries
        List<DeliveryRecord> allDeliveries = deliveryRepository.findByOrderId(orderId);
        int totalDelivered = allDeliveries.stream().mapToInt(DeliveryRecord::getDeliveryQuantity).sum();

        if (totalDelivered >= order.getTotalQuantity()) {
            order.setOrderStatus(OrderStatus.DELIVERED.name());
        } else {
            order.setOrderStatus(OrderStatus.PARTIALLY_DELIVERED.name());
        }
        orderRepository.save(order);

        log.info("School {} confirmed delivery {} (quantity: {}) for order {}",
                schoolId, deliveryNumber, request.receivedQuantity(), order.getOrderNumber());
        return mapToSchoolDelivery(saved);
    }

    // 7. School View Deliveries
    @Transactional(readOnly = true)
    public PagedResponse<SchoolDeliveryResponse> getSchoolDeliveries(UUID orderId, UUID schoolId, Pageable pageable) {
        orderRepository.findByIdAndSchoolId(orderId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied"));

        Page<DeliveryRecord> page = deliveryRepository.findByOrderId(orderId, pageable);
        List<SchoolDeliveryResponse> content = page.getContent().stream().map(this::mapToSchoolDelivery).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    // 8. Admin View Deliveries
    @Transactional(readOnly = true)
    public PagedResponse<AdminDeliveryResponse> getAdminDeliveries(Pageable pageable, UUID schoolId, String status) {
        DeliveryStatus deliveryStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                deliveryStatus = DeliveryStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        Page<DeliveryRecord> page = deliveryRepository.searchAdminDeliveries(schoolId, deliveryStatus, pageable);
        List<AdminDeliveryResponse> content = page.getContent().stream().map(this::mapToAdminDelivery).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    // 9. Admin Close Delivery
    @Transactional
    public AdminDeliveryResponse adminCloseDelivery(UUID deliveryId, UUID adminUserId, CloseDeliveryRequest request) {
        DeliveryRecord delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new ResourceNotFoundException("Delivery record not found: " + deliveryId));

        User user = null;
        if (adminUserId != null) {
            user = userRepository.findById(adminUserId).orElse(null);
        }

        delivery.setDeliveryStatus(DeliveryStatus.CLOSED);
        delivery.setClosedBy(user);
        delivery.setClosedAt(OffsetDateTime.now());
        if (request != null && request.closureNotes() != null) {
            delivery.setClosureNotes(request.closureNotes());
        }

        DeliveryRecord saved = deliveryRepository.save(delivery);
        log.info("Admin closed delivery record {}", delivery.getDeliveryNumber());
        return mapToAdminDelivery(saved);
    }

    // Mapping Helpers
    private AgencyDispatchResponse mapToAgencyDispatch(DispatchRecord d) {
        return new AgencyDispatchResponse(
                d.getId(),
                d.getAssignment() != null ? d.getAssignment().getId() : null,
                d.getAssignment() != null ? d.getAssignment().getAssignmentNumber() : null,
                d.getDispatchNumber(),
                d.getCourierName(),
                d.getTrackingNumber(),
                d.getDispatchQuantity(),
                d.getDispatchStatus(),
                d.getDispatchType(),
                d.getDispatchDate(),
                d.getDispatchNotes()
        );
    }

    private AdminDispatchResponse mapToAdminDispatch(DispatchRecord d) {
        return new AdminDispatchResponse(
                d.getId(),
                d.getAssignment() != null ? d.getAssignment().getId() : null,
                d.getAssignment() != null ? d.getAssignment().getAssignmentNumber() : null,
                d.getAssignment() != null && d.getAssignment().getAgency() != null ? d.getAssignment().getAgency().getId() : null,
                d.getAssignment() != null && d.getAssignment().getAgency() != null ? d.getAssignment().getAgency().getAgencyName() : null,
                d.getOrder() != null ? d.getOrder().getId() : null,
                d.getOrder() != null ? d.getOrder().getOrderNumber() : null,
                d.getOrder() != null && d.getOrder().getSchool() != null ? d.getOrder().getSchool().getSchoolName() : null,
                d.getDispatchNumber(),
                d.getCourierName(),
                d.getTrackingNumber(),
                d.getDispatchQuantity(),
                d.getDispatchStatus(),
                d.getDispatchType(),
                d.getDispatchDate(),
                d.getDispatchNotes(),
                d.getDispatchedBy() != null ? d.getDispatchedBy().getFullName() : null
        );
    }

    private SchoolDeliveryResponse mapToSchoolDelivery(DeliveryRecord d) {
        return new SchoolDeliveryResponse(
                d.getId(),
                d.getOrder() != null ? d.getOrder().getId() : null,
                d.getOrder() != null ? d.getOrder().getOrderNumber() : null,
                d.getDeliveryNumber(),
                d.getDeliveryModel(),
                d.getDeliveryQuantity(),
                d.getReceivedBy(),
                d.getReceivedMobile(),
                d.getDeliveryStatus(),
                d.getDeliveredAt(),
                d.getDeliveryNotes()
        );
    }

    private AdminDeliveryResponse mapToAdminDelivery(DeliveryRecord d) {
        return new AdminDeliveryResponse(
                d.getId(),
                d.getOrder() != null ? d.getOrder().getId() : null,
                d.getOrder() != null ? d.getOrder().getOrderNumber() : null,
                d.getOrder() != null && d.getOrder().getSchool() != null ? d.getOrder().getSchool().getId() : null,
                d.getOrder() != null && d.getOrder().getSchool() != null ? d.getOrder().getSchool().getSchoolName() : null,
                d.getDispatch() != null ? d.getDispatch().getId() : null,
                d.getDispatch() != null ? d.getDispatch().getDispatchNumber() : null,
                d.getDeliveryNumber(),
                d.getDeliveryModel(),
                d.getDeliveryQuantity(),
                d.getReceivedBy(),
                d.getReceivedMobile(),
                d.getDeliveryStatus(),
                d.getDeliveredAt(),
                d.getDeliveryNotes(),
                d.getClosedBy() != null ? d.getClosedBy().getFullName() : null,
                d.getClosedAt(),
                d.getClosureNotes()
        );
    }
}
