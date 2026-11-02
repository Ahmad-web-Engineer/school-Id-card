package com.platform.idcard.order.service;

import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.common.response.PaginationMeta;
import com.platform.idcard.order.dto.*;
import com.platform.idcard.order.entity.*;
import com.platform.idcard.order.repository.*;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.student.entity.Student;
import com.platform.idcard.student.repository.StudentRepository;
import com.platform.idcard.template.entity.CardTemplate;
import com.platform.idcard.template.entity.TemplateApprovalStatus;
import com.platform.idcard.template.entity.TemplateVersion;
import com.platform.idcard.template.repository.CardTemplateRepository;
import com.platform.idcard.template.repository.TemplateVersionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Year;
import java.util.*;

@Service
public class OrderService {

    private final PrintingOrderRepository orderRepository;
    private final PrintingOrderItemRepository orderItemRepository;
    private final OrderStudentItemRepository orderStudentItemRepository;
    private final QuotationRepository quotationRepository;
    private final QuotationItemRepository quotationItemRepository;
    private final SchoolRepository schoolRepository;
    private final StudentRepository studentRepository;
    private final CardTemplateRepository cardTemplateRepository;
    private final TemplateVersionRepository templateVersionRepository;

    public OrderService(
            PrintingOrderRepository orderRepository,
            PrintingOrderItemRepository orderItemRepository,
            OrderStudentItemRepository orderStudentItemRepository,
            QuotationRepository quotationRepository,
            QuotationItemRepository quotationItemRepository,
            SchoolRepository schoolRepository,
            StudentRepository studentRepository,
            CardTemplateRepository cardTemplateRepository,
            TemplateVersionRepository templateVersionRepository
    ) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.orderStudentItemRepository = orderStudentItemRepository;
        this.quotationRepository = quotationRepository;
        this.quotationItemRepository = quotationItemRepository;
        this.schoolRepository = schoolRepository;
        this.studentRepository = studentRepository;
        this.cardTemplateRepository = cardTemplateRepository;
        this.templateVersionRepository = templateVersionRepository;
    }

    /**
     * School: Create a new draft order with student snapshots.
     * Non-negotiable Rule 2: schoolId comes strictly from authenticated token.
     */
    @Transactional
    public SchoolOrderDetailResponse createOrder(UUID schoolId, UUID userId, CreateOrderRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School not found with ID: " + schoolId));

        TemplateVersion templateVersion = templateVersionRepository.findById(request.templateVersionId())
                .orElseThrow(() -> new ResourceNotFoundException("Template version not found with ID: " + request.templateVersionId()));

        if (!TemplateApprovalStatus.APPROVED.name().equalsIgnoreCase(templateVersion.getApprovalStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Only APPROVED template versions can be used to place orders");
        }

        CardTemplate cardTemplate = templateVersion.getTemplate();
        if (cardTemplate.getSchool() != null && !cardTemplate.getSchool().getId().equals(schoolId)) {
            throw new ResourceNotFoundException("Template is not assigned to this school");
        }

        List<UUID> studentIds = request.studentIds();
        if (studentIds == null || studentIds.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "At least one student must be selected for the order");
        }

        // Verify and load students belonging strictly to caller's school
        List<Student> students = new ArrayList<>();
        for (UUID sId : studentIds) {
            Student s = studentRepository.findByIdAndSchoolId(sId, schoolId)
                    .orElseThrow(() -> new ResourceNotFoundException("Student with ID " + sId + " does not belong to this school"));
            students.add(s);
        }

        String orderNumber = generateUniqueOrderNumber();

        PrintingOrder order = new PrintingOrder();
        order.setOrderNumber(orderNumber);
        order.setSchool(school);
        order.setAcademicSession(request.academicSession() != null && !request.academicSession().isBlank()
                ? request.academicSession().trim() : "2026-2027");
        order.setTemplateVersion(templateVersion);
        order.setTotalStudents(students.size());

        int additionalQty = 0;
        if (request.additionalItems() != null) {
            for (AdditionalItemRequest itemReq : request.additionalItems()) {
                if (itemReq.quantity() != null && itemReq.quantity() > 0) {
                    additionalQty += itemReq.quantity();
                }
            }
        }
        order.setTotalQuantity(students.size() + additionalQty);
        order.setOrderStatus(OrderStatus.DRAFT.name());
        order.setDeliveryModel(request.deliveryModel() != null && !request.deliveryModel().isBlank()
                ? request.deliveryModel().trim() : "DIRECT_TO_SCHOOL");
        order.setDeliveryAddress(request.deliveryAddress() != null ? request.deliveryAddress().trim() : school.getAddress());
        order.setContactPerson(request.contactPerson() != null ? request.contactPerson().trim() : school.getSchoolName());
        order.setContactMobile(request.contactMobile() != null ? request.contactMobile().trim() : school.getMobile());
        order.setRemarks(request.remarks() != null ? request.remarks().trim() : null);
        order.setCreatedBy(userId);

        order = orderRepository.save(order);

        // Add default PVC ID Card item
        PrintingOrderItem cardItem = new PrintingOrderItem(
                order,
                "PVC_ID_CARD",
                students.size(),
                BigDecimal.ZERO,
                BigDecimal.ZERO
        );
        orderItemRepository.save(cardItem);
        order.addItem(cardItem);

        // Add additional items
        if (request.additionalItems() != null) {
            for (AdditionalItemRequest itemReq : request.additionalItems()) {
                if (itemReq.quantity() != null && itemReq.quantity() > 0) {
                    PrintingOrderItem addlItem = new PrintingOrderItem(
                            order,
                            itemReq.productType().trim().toUpperCase(),
                            itemReq.quantity(),
                            BigDecimal.ZERO,
                            BigDecimal.ZERO
                    );
                    orderItemRepository.save(addlItem);
                    order.addItem(addlItem);
                }
            }
        }

        // Create immutable snapshots of student data
        for (Student s : students) {
            OrderStudentItem snapshot = new OrderStudentItem();
            snapshot.setOrder(order);
            snapshot.setStudent(s);
            snapshot.setAdmissionNumber(s.getAdmissionNumber());
            snapshot.setStudentName(s.getStudentName());
            snapshot.setFatherName(s.getFatherName());
            snapshot.setMotherName(s.getMotherName());
            snapshot.setDateOfBirth(s.getDateOfBirth());
            snapshot.setGender(s.getGender());
            snapshot.setBloodGroup(s.getBloodGroup());
            snapshot.setClassName(s.getClassName());
            snapshot.setSection(s.getSection());
            snapshot.setRollNumber(s.getRollNumber());
            snapshot.setAddress(s.getAddress());
            snapshot.setEmergencyContact(s.getEmergencyContact());
            snapshot.setPhotoFileId(s.getPhotoFileId());
            snapshot.setTemplateVersion(templateVersion);
            orderStudentItemRepository.save(snapshot);
            order.addStudentItem(snapshot);
        }

        return mapToSchoolOrderDetail(order);
    }

    /**
     * School: List own orders with pagination and filtering.
     */
    @Transactional(readOnly = true)
    public PagedResponse<SchoolOrderSummaryResponse> getSchoolOrders(
            UUID schoolId,
            int page,
            int size,
            String search,
            String status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        Page<PrintingOrder> paged = orderRepository.findWithFilters(schoolId, cleanSearch, cleanStatus, pageable);

        List<SchoolOrderSummaryResponse> list = paged.getContent().stream().map(order -> {
            BigDecimal quoteAmount = order.getQuotation() != null ? order.getQuotation().getTotalAmount() : null;
            return new SchoolOrderSummaryResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getAcademicSession(),
                    order.getTemplateVersion().getTemplate().getTemplateName(),
                    order.getTemplateVersion().getVersionNumber(),
                    order.getTotalStudents(),
                    order.getTotalQuantity(),
                    order.getOrderStatus(),
                    quoteAmount,
                    order.getSubmittedAt(),
                    order.getCreatedAt()
            );
        }).toList();

        PaginationMeta meta = new PaginationMeta(
                paged.getNumber(),
                paged.getSize(),
                paged.getTotalElements(),
                paged.getTotalPages(),
                paged.isLast()
        );

        return new PagedResponse<>(true, list, meta);
    }

    /**
     * School: Get order details by ID. Strictly verifies school ownership.
     */
    @Transactional(readOnly = true)
    public SchoolOrderDetailResponse getSchoolOrderById(UUID orderId, UUID schoolId) {
        PrintingOrder order = orderRepository.findByIdAndSchoolId(orderId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        return mapToSchoolOrderDetail(order);
    }

    /**
     * School: Submit draft order for admin review.
     * Named business action: DRAFT / CORRECTION_REQUIRED -> SUBMITTED.
     */
    @Transactional
    public SchoolOrderDetailResponse submitOrder(UUID orderId, UUID schoolId) {
        PrintingOrder order = orderRepository.findByIdAndSchoolId(orderId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!OrderStatus.DRAFT.name().equalsIgnoreCase(order.getOrderStatus())
                && !OrderStatus.CORRECTION_REQUIRED.name().equalsIgnoreCase(order.getOrderStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Only DRAFT or CORRECTION_REQUIRED orders can be submitted");
        }

        order.setOrderStatus(OrderStatus.SUBMITTED.name());
        order.setSubmittedAt(Instant.now());
        order = orderRepository.save(order);

        return mapToSchoolOrderDetail(order);
    }

    /**
     * School: Cancel draft or submitted order.
     */
    @Transactional
    public SchoolOrderDetailResponse cancelOrder(UUID orderId, UUID schoolId) {
        PrintingOrder order = orderRepository.findByIdAndSchoolId(orderId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (OrderStatus.APPROVED.name().equalsIgnoreCase(order.getOrderStatus())
                || OrderStatus.AWAITING_ASSIGNMENT.name().equalsIgnoreCase(order.getOrderStatus())
                || OrderStatus.CANCELLED.name().equalsIgnoreCase(order.getOrderStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Cannot cancel order with status: " + order.getOrderStatus());
        }

        order.setOrderStatus(OrderStatus.CANCELLED.name());
        order = orderRepository.save(order);

        return mapToSchoolOrderDetail(order);
    }

    /**
     * Admin: List all orders across all schools with pagination and filtering.
     */
    @Transactional(readOnly = true)
    public PagedResponse<AdminOrderSummaryResponse> getAdminOrders(
            int page,
            int size,
            UUID schoolId,
            String search,
            String status
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        String cleanSearch = (search != null && !search.isBlank()) ? search.trim() : null;
        String cleanStatus = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        Page<PrintingOrder> paged = orderRepository.findWithFilters(schoolId, cleanSearch, cleanStatus, pageable);

        List<AdminOrderSummaryResponse> list = paged.getContent().stream().map(order -> {
            BigDecimal quoteAmount = order.getQuotation() != null ? order.getQuotation().getTotalAmount() : null;
            return new AdminOrderSummaryResponse(
                    order.getId(),
                    order.getOrderNumber(),
                    order.getSchool().getId(),
                    order.getSchool().getSchoolName(),
                    order.getSchool().getSchoolCode(),
                    order.getAcademicSession(),
                    order.getTemplateVersion().getTemplate().getTemplateName(),
                    order.getTemplateVersion().getVersionNumber(),
                    order.getTotalStudents(),
                    order.getTotalQuantity(),
                    order.getOrderStatus(),
                    quoteAmount,
                    order.getSubmittedAt(),
                    order.getCreatedAt()
            );
        }).toList();

        PaginationMeta meta = new PaginationMeta(
                paged.getNumber(),
                paged.getSize(),
                paged.getTotalElements(),
                paged.getTotalPages(),
                paged.isLast()
        );

        return new PagedResponse<>(true, list, meta);
    }

    /**
     * Admin: Get order details by ID.
     */
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getAdminOrderById(UUID orderId) {
        PrintingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));
        return mapToAdminOrderDetail(order);
    }

    /**
     * Admin: Start review on submitted order.
     * Named business action: SUBMITTED -> UNDER_REVIEW.
     */
    @Transactional
    public AdminOrderDetailResponse startReview(UUID orderId, UUID adminUserId) {
        PrintingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!OrderStatus.SUBMITTED.name().equalsIgnoreCase(order.getOrderStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Only SUBMITTED orders can be moved to UNDER_REVIEW");
        }

        order.setOrderStatus(OrderStatus.UNDER_REVIEW.name());
        order = orderRepository.save(order);
        return mapToAdminOrderDetail(order);
    }

    /**
     * Admin: Verify order student data and readiness.
     * Named business action: SUBMITTED / UNDER_REVIEW -> VERIFIED.
     */
    @Transactional
    public AdminOrderDetailResponse verifyOrder(UUID orderId, UUID adminUserId) {
        PrintingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!OrderStatus.SUBMITTED.name().equalsIgnoreCase(order.getOrderStatus())
                && !OrderStatus.UNDER_REVIEW.name().equalsIgnoreCase(order.getOrderStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Only SUBMITTED or UNDER_REVIEW orders can be VERIFIED");
        }

        order.setOrderStatus(OrderStatus.VERIFIED.name());
        order.setVerifiedAt(Instant.now());
        order.setVerifiedBy(adminUserId);
        order.setRejectionRemarks(null);
        order = orderRepository.save(order);

        return mapToAdminOrderDetail(order);
    }

    /**
     * Admin: Request corrections from school.
     * Named business action: SUBMITTED / UNDER_REVIEW -> CORRECTION_REQUIRED.
     */
    @Transactional
    public AdminOrderDetailResponse requestCorrection(UUID orderId, UUID adminUserId, RequestCorrectionRequest request) {
        PrintingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!OrderStatus.SUBMITTED.name().equalsIgnoreCase(order.getOrderStatus())
                && !OrderStatus.UNDER_REVIEW.name().equalsIgnoreCase(order.getOrderStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Only SUBMITTED or UNDER_REVIEW orders can have corrections requested");
        }

        order.setOrderStatus(OrderStatus.CORRECTION_REQUIRED.name());
        order.setRejectionRemarks(request.remarks().trim());
        order = orderRepository.save(order);

        return mapToAdminOrderDetail(order);
    }

    /**
     * Admin: Generate commercial quotation for verified order.
     * Named business action: VERIFIED / QUOTATION_GENERATED -> QUOTATION_GENERATED.
     * Money uses NUMERIC(14,2) with explicit rounding (Non-negotiable Rule 3).
     */
    @Transactional
    public QuotationResponse generateQuotation(UUID orderId, UUID adminUserId, GenerateQuotationRequest request) {
        PrintingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        if (!OrderStatus.VERIFIED.name().equalsIgnoreCase(order.getOrderStatus())
                && !OrderStatus.QUOTATION_GENERATED.name().equalsIgnoreCase(order.getOrderStatus())
                && !OrderStatus.AWAITING_APPROVAL.name().equalsIgnoreCase(order.getOrderStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Quotation can only be generated for VERIFIED orders");
        }

        if (request.items() == null || request.items().isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, "Quotation must have at least one line item");
        }

        Quotation quotation = quotationRepository.findByOrderId(orderId)
                .orElseGet(() -> {
                    Quotation q = new Quotation();
                    q.setOrder(order);
                    q.setQuotationNumber(generateUniqueQuotationNumber());
                    return q;
                });

        quotation.getItems().clear();

        BigDecimal subtotal = BigDecimal.ZERO;
        for (QuotationItemRequest itemReq : request.items()) {
            BigDecimal qty = BigDecimal.valueOf(itemReq.quantity());
            BigDecimal itemAmt = itemReq.unitPrice().multiply(qty).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(itemAmt);

            QuotationItem item = new QuotationItem(
                    quotation,
                    itemReq.productDescription().trim(),
                    itemReq.quantity(),
                    itemReq.unitPrice().setScale(2, RoundingMode.HALF_UP),
                    itemAmt
            );
            quotation.addItem(item);
        }

        BigDecimal discount = (request.discount() != null) ? request.discount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        BigDecimal taxableAmount = subtotal.subtract(discount).max(BigDecimal.ZERO);

        BigDecimal taxRate = (request.taxRate() != null) ? request.taxRate() : new BigDecimal("18.00");
        BigDecimal taxAmount = taxableAmount.multiply(taxRate).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        BigDecimal totalAmount = taxableAmount.add(taxAmount).setScale(2, RoundingMode.HALF_UP);

        int validDays = request.validDays() != null && request.validDays() > 0 ? request.validDays() : 15;
        LocalDate validUntil = LocalDate.now().plusDays(validDays);

        quotation.setSubtotal(subtotal);
        quotation.setDiscount(discount);
        quotation.setTaxRate(taxRate);
        quotation.setTaxAmount(taxAmount);
        quotation.setTotalAmount(totalAmount);
        quotation.setValidUntil(validUntil);
        quotation.setQuotationStatus(QuotationStatus.GENERATED.name());
        quotation.setGeneratedBy(adminUserId);
        quotation.setNotes(request.notes() != null ? request.notes().trim() : null);

        quotation = quotationRepository.save(quotation);

        order.setOrderStatus(OrderStatus.QUOTATION_GENERATED.name());
        orderRepository.save(order);

        return mapToQuotationResponse(quotation);
    }

    /**
     * School: Approve quotation.
     * Named business action: QUOTATION_GENERATED / AWAITING_APPROVAL -> APPROVED.
     */
    @Transactional
    public SchoolOrderDetailResponse approveQuotation(UUID orderId, UUID schoolId, UUID schoolUserId) {
        PrintingOrder order = orderRepository.findByIdAndSchoolId(orderId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with ID: " + orderId));

        Quotation quotation = quotationRepository.findByOrderId(orderId)
                .orElseThrow(() -> new ApiException(ErrorCode.INVALID_ACTION, "No quotation found for this order"));

        if (!QuotationStatus.GENERATED.name().equalsIgnoreCase(quotation.getQuotationStatus())) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Quotation is already " + quotation.getQuotationStatus());
        }

        quotation.setQuotationStatus(QuotationStatus.APPROVED.name());
        quotation.setApprovedBy(schoolUserId);
        quotation.setApprovedAt(Instant.now());
        quotationRepository.save(quotation);

        order.setOrderStatus(OrderStatus.APPROVED.name());
        order = orderRepository.save(order);

        return mapToSchoolOrderDetail(order);
    }

    // --- Helper Mappers ---

    private SchoolOrderDetailResponse mapToSchoolOrderDetail(PrintingOrder order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(it -> new OrderItemResponse(it.getId(), it.getProductType(), it.getQuantity(), it.getUnitSellingPrice(), it.getItemTotal()))
                .toList();

        List<OrderStudentSnapshotResponse> studentResponses = order.getStudentItems().stream()
                .map(s -> new OrderStudentSnapshotResponse(
                        s.getId(),
                        s.getStudent().getId(),
                        s.getAdmissionNumber(),
                        s.getStudentName(),
                        s.getFatherName(),
                        s.getMotherName(),
                        s.getDateOfBirth(),
                        s.getGender(),
                        s.getBloodGroup(),
                        s.getClassName(),
                        s.getSection(),
                        s.getRollNumber(),
                        s.getPhotoFileId()
                ))
                .toList();

        QuotationResponse qResp = order.getQuotation() != null ? mapToQuotationResponse(order.getQuotation()) : null;

        return new SchoolOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getAcademicSession(),
                order.getTemplateVersion().getId(),
                order.getTemplateVersion().getTemplate().getTemplateName(),
                order.getTemplateVersion().getVersionNumber(),
                order.getTotalStudents(),
                order.getTotalQuantity(),
                order.getOrderStatus(),
                order.getDeliveryModel(),
                order.getDeliveryAddress(),
                order.getContactPerson(),
                order.getContactMobile(),
                order.getRemarks(),
                order.getRejectionRemarks(),
                order.getSubmittedAt(),
                order.getVerifiedAt(),
                order.getCreatedAt(),
                itemResponses,
                studentResponses,
                qResp
        );
    }

    private AdminOrderDetailResponse mapToAdminOrderDetail(PrintingOrder order) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(it -> new OrderItemResponse(it.getId(), it.getProductType(), it.getQuantity(), it.getUnitSellingPrice(), it.getItemTotal()))
                .toList();

        List<OrderStudentSnapshotResponse> studentResponses = order.getStudentItems().stream()
                .map(s -> new OrderStudentSnapshotResponse(
                        s.getId(),
                        s.getStudent().getId(),
                        s.getAdmissionNumber(),
                        s.getStudentName(),
                        s.getFatherName(),
                        s.getMotherName(),
                        s.getDateOfBirth(),
                        s.getGender(),
                        s.getBloodGroup(),
                        s.getClassName(),
                        s.getSection(),
                        s.getRollNumber(),
                        s.getPhotoFileId()
                ))
                .toList();

        QuotationResponse qResp = order.getQuotation() != null ? mapToQuotationResponse(order.getQuotation()) : null;

        return new AdminOrderDetailResponse(
                order.getId(),
                order.getOrderNumber(),
                order.getSchool().getId(),
                order.getSchool().getSchoolName(),
                order.getSchool().getSchoolCode(),
                order.getAcademicSession(),
                order.getTemplateVersion().getId(),
                order.getTemplateVersion().getTemplate().getTemplateName(),
                order.getTemplateVersion().getVersionNumber(),
                order.getTotalStudents(),
                order.getTotalQuantity(),
                order.getOrderStatus(),
                order.getDeliveryModel(),
                order.getDeliveryAddress(),
                order.getContactPerson(),
                order.getContactMobile(),
                order.getRemarks(),
                order.getRejectionRemarks(),
                order.getSubmittedAt(),
                order.getVerifiedAt(),
                order.getVerifiedBy(),
                order.getCreatedBy(),
                order.getCreatedAt(),
                itemResponses,
                studentResponses,
                qResp
        );
    }

    private QuotationResponse mapToQuotationResponse(Quotation q) {
        List<QuotationItemResponse> itemResponses = q.getItems().stream()
                .map(it -> new QuotationItemResponse(it.getId(), it.getProductDescription(), it.getQuantity(), it.getUnitPrice(), it.getAmount()))
                .toList();

        return new QuotationResponse(
                q.getId(),
                q.getQuotationNumber(),
                q.getOrder().getId(),
                q.getSubtotal(),
                q.getDiscount(),
                q.getTaxRate(),
                q.getTaxAmount(),
                q.getTotalAmount(),
                q.getQuotationStatus(),
                q.getValidUntil(),
                q.getApprovedAt(),
                q.getNotes(),
                q.getCreatedAt(),
                itemResponses
        );
    }

    private String generateUniqueOrderNumber() {
        int year = Year.now().getValue();
        for (int i = 0; i < 10; i++) {
            String candidate = "ORD-" + year + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!orderRepository.existsByOrderNumber(candidate)) {
                return candidate;
            }
        }
        return "ORD-" + year + "-" + System.currentTimeMillis();
    }

    private String generateUniqueQuotationNumber() {
        int year = Year.now().getValue();
        for (int i = 0; i < 10; i++) {
            String candidate = "QUO-" + year + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            if (!quotationRepository.existsByQuotationNumber(candidate)) {
                return candidate;
            }
        }
        return "QUO-" + year + "-" + System.currentTimeMillis();
    }
}
