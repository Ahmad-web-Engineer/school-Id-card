package com.platform.idcard.finance.service;

import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.agencyjob.repository.AgencyAssignmentRepository;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.BusinessConflictException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.common.response.PaginationMeta;
import com.platform.idcard.finance.dto.*;
import com.platform.idcard.finance.entity.*;
import com.platform.idcard.finance.repository.AgencySettlementRepository;
import com.platform.idcard.finance.repository.CostEntryRepository;
import com.platform.idcard.finance.repository.InvoiceRepository;
import com.platform.idcard.finance.repository.PaymentRepository;
import com.platform.idcard.order.entity.OrderStatus;
import com.platform.idcard.order.entity.PrintingOrder;
import com.platform.idcard.order.entity.Quotation;
import com.platform.idcard.order.repository.PrintingOrderRepository;
import com.platform.idcard.order.repository.QuotationRepository;
import com.platform.idcard.organization.entity.Agency;
import com.platform.idcard.organization.entity.School;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.List;
import java.util.UUID;

@Service
public class FinanceService {

    private static final Logger log = LoggerFactory.getLogger(FinanceService.class);

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AgencySettlementRepository settlementRepository;
    private final CostEntryRepository costEntryRepository;
    private final PrintingOrderRepository orderRepository;
    private final QuotationRepository quotationRepository;
    private final AgencyAssignmentRepository assignmentRepository;
    private final UserRepository userRepository;

    public FinanceService(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            AgencySettlementRepository settlementRepository,
            CostEntryRepository costEntryRepository,
            PrintingOrderRepository orderRepository,
            QuotationRepository quotationRepository,
            AgencyAssignmentRepository assignmentRepository,
            UserRepository userRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.settlementRepository = settlementRepository;
        this.costEntryRepository = costEntryRepository;
        this.orderRepository = orderRepository;
        this.quotationRepository = quotationRepository;
        this.assignmentRepository = assignmentRepository;
        this.userRepository = userRepository;
    }

    // =========================================================================
    // 1. SCHOOL FINANCE WORKFLOWS
    // =========================================================================

    @Transactional
    public SchoolPaymentResponse recordSchoolPayment(
            UUID orderId,
            UUID schoolId,
            UUID userId,
            RecordPaymentRequest request
    ) {
        PrintingOrder order = orderRepository.findByIdAndSchoolId(orderId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found or access denied"));

        return processPaymentRecord(order, order.getSchool(), userId, request);
    }

    @Transactional(readOnly = true)
    public SchoolFinanceSummaryResponse getSchoolFinanceSummary(UUID schoolId) {
        BigDecimal totalInvoiced = invoiceRepository.sumTotalInvoicedBySchoolId(schoolId);
        BigDecimal totalPaid = paymentRepository.sumCompletedPaymentsBySchoolId(schoolId);
        BigDecimal due = invoiceRepository.sumDueAmountBySchoolId(schoolId);
        BigDecimal outstanding = due.compareTo(BigDecimal.ZERO) > 0 ? due : BigDecimal.ZERO;

        Page<Invoice> invPage = invoiceRepository.findBySchoolId(schoolId, Pageable.ofSize(1));
        Page<Payment> payPage = paymentRepository.findBySchoolId(schoolId, Pageable.ofSize(1));

        return new SchoolFinanceSummaryResponse(
                totalInvoiced,
                totalPaid,
                outstanding,
                invPage.getTotalElements(),
                payPage.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<SchoolInvoiceResponse> getSchoolInvoices(UUID schoolId, Pageable pageable) {
        Page<Invoice> page = invoiceRepository.findBySchoolId(schoolId, pageable);
        List<SchoolInvoiceResponse> content = page.getContent().stream().map(this::mapToSchoolInvoice).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    @Transactional(readOnly = true)
    public SchoolInvoiceResponse getSchoolInvoiceById(UUID invoiceId, UUID schoolId) {
        Invoice invoice = invoiceRepository.findByIdAndSchoolId(invoiceId, schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("Invoice not found or access denied"));
        return mapToSchoolInvoice(invoice);
    }

    @Transactional(readOnly = true)
    public PagedResponse<SchoolPaymentResponse> getSchoolPayments(UUID schoolId, Pageable pageable) {
        Page<Payment> page = paymentRepository.findBySchoolId(schoolId, pageable);
        List<SchoolPaymentResponse> content = page.getContent().stream().map(this::mapToSchoolPayment).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    // =========================================================================
    // 2. AGENCY FINANCE WORKFLOWS
    // =========================================================================

    @Transactional(readOnly = true)
    public AgencyFinanceSummaryResponse getAgencyFinanceSummary(UUID agencyId) {
        BigDecimal totalAgreed = settlementRepository.sumAgreedAmountByAgencyId(agencyId);
        BigDecimal totalDeductions = settlementRepository.sumDeductionsByAgencyId(agencyId);
        BigDecimal totalPayable = settlementRepository.sumPayableAmountByAgencyId(agencyId);
        BigDecimal totalPaid = settlementRepository.sumPaidAmountByAgencyId(agencyId);
        BigDecimal outstanding = settlementRepository.sumOutstandingAmountByAgencyId(agencyId);

        Page<AgencySettlement> page = settlementRepository.findByAgencyId(agencyId, Pageable.ofSize(1));

        return new AgencyFinanceSummaryResponse(
                totalAgreed,
                totalDeductions,
                totalPayable,
                totalPaid,
                outstanding,
                page.getTotalElements()
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AgencySettlementResponse> getAgencySettlements(UUID agencyId, Pageable pageable) {
        Page<AgencySettlement> page = settlementRepository.findByAgencyId(agencyId, pageable);
        List<AgencySettlementResponse> content = page.getContent().stream().map(this::mapToAgencySettlement).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    @Transactional(readOnly = true)
    public AgencySettlementResponse getAgencySettlementById(UUID settlementId, UUID agencyId) {
        AgencySettlement settlement = settlementRepository.findByIdAndAgencyId(settlementId, agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found or access denied"));
        return mapToAgencySettlement(settlement);
    }

    // =========================================================================
    // 3. ADMIN FINANCE WORKFLOWS
    // =========================================================================

    @Transactional
    public AdminInvoiceResponse generateInvoice(UUID orderId, UUID adminUserId, GenerateInvoiceRequest request) {
        PrintingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        if (invoiceRepository.findByOrderId(orderId).isPresent()) {
            throw new BusinessConflictException("An invoice already exists for order: " + order.getOrderNumber());
        }

        Quotation quotation = order.getQuotation();
        if (quotation == null) {
            quotation = quotationRepository.findByOrderId(orderId).orElse(null);
        }
        if (quotation == null) {
            throw new ApiException(ErrorCode.INVALID_ACTION, "Cannot generate invoice without an approved quotation");
        }

        String currentYear = String.valueOf(Year.now().getValue());
        long count = invoiceRepository.countByInvoiceNumberStartingWith("INV-" + currentYear) + 1;
        String invoiceNumber = String.format("INV-%s-%04d", currentYear, count);

        BigDecimal subtotal = quotation.getSubtotal();
        BigDecimal discount = quotation.getDiscount() != null ? quotation.getDiscount() : BigDecimal.ZERO;
        BigDecimal taxAmount = quotation.getTaxAmount();
        BigDecimal totalAmount = quotation.getTotalAmount();

        BigDecimal alreadyPaid = paymentRepository.sumCompletedPaymentsByOrderId(orderId);
        BigDecimal dueAmount = totalAmount.subtract(alreadyPaid).max(BigDecimal.ZERO);
        InvoiceStatus status = dueAmount.compareTo(BigDecimal.ZERO) == 0 ? InvoiceStatus.PAID :
                (alreadyPaid.compareTo(BigDecimal.ZERO) > 0 ? InvoiceStatus.PARTIALLY_PAID : InvoiceStatus.ISSUED);

        Invoice invoice = new Invoice();
        invoice.setOrder(order);
        invoice.setSchool(order.getSchool());
        invoice.setQuotation(quotation);
        invoice.setInvoiceNumber(invoiceNumber);
        invoice.setSubtotal(subtotal);
        invoice.setDiscount(discount);
        invoice.setTaxAmount(taxAmount);
        invoice.setTotalAmount(totalAmount);
        invoice.setPaidAmount(alreadyPaid);
        invoice.setDueAmount(dueAmount);
        invoice.setInvoiceStatus(status);
        invoice.setIssuedAt(OffsetDateTime.now());
        if (request != null && request.dueDate() != null) {
            invoice.setDueDate(request.dueDate());
        }
        if (request != null && request.notes() != null) {
            invoice.setNotes(request.notes());
        }

        Invoice saved = invoiceRepository.save(invoice);
        log.info("Invoice {} generated for order {}", invoiceNumber, order.getOrderNumber());
        return mapToAdminInvoice(saved);
    }

    @Transactional
    public AdminPaymentResponse adminRecordPayment(UUID orderId, UUID adminUserId, RecordPaymentRequest request) {
        PrintingOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));

        SchoolPaymentResponse schoolRes = processPaymentRecord(order, order.getSchool(), adminUserId, request);
        Payment payment = paymentRepository.findById(schoolRes.id()).orElseThrow();
        return mapToAdminPayment(payment);
    }

    @Transactional
    public AdminSettlementResponse generateAgencySettlement(
            UUID assignmentId,
            UUID adminUserId,
            GenerateSettlementRequest request
    ) {
        AgencyAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found: " + assignmentId));

        if (settlementRepository.findByAssignmentId(assignmentId).isPresent()) {
            throw new BusinessConflictException("Settlement already generated for this assignment");
        }

        String currentYear = String.valueOf(Year.now().getValue());
        long count = settlementRepository.countBySettlementNumberStartingWith("SET-" + currentYear) + 1;
        String settlementNumber = String.format("SET-%s-%04d", currentYear, count);

        BigDecimal agreedAmount = assignment.getTotalAgreedAmount();
        BigDecimal deductions = request != null && request.deductionAmount() != null ? request.deductionAmount() : BigDecimal.ZERO;
        BigDecimal penalty = request != null && request.penaltyAmount() != null ? request.penaltyAmount() : BigDecimal.ZERO;

        BigDecimal payable = agreedAmount.subtract(deductions).subtract(penalty).max(BigDecimal.ZERO);

        AgencySettlement settlement = new AgencySettlement();
        settlement.setAssignment(assignment);
        settlement.setAgency(assignment.getAgency());
        settlement.setSettlementNumber(settlementNumber);
        settlement.setAgreedAmount(agreedAmount);
        settlement.setDeductionAmount(deductions);
        settlement.setPenaltyAmount(penalty);
        settlement.setPayableAmount(payable);
        settlement.setPaidAmount(BigDecimal.ZERO);
        settlement.setOutstandingAmount(payable);
        settlement.setSettlementStatus(SettlementStatus.APPROVED);
        if (request != null && request.notes() != null) {
            settlement.setNotes(request.notes());
        }

        AgencySettlement saved = settlementRepository.save(settlement);

        // Record Cost Entry
        CostEntry cost = new CostEntry(
                assignment.getOrder(),
                assignment,
                CostCategory.AGENCY_PRINTING,
                "Agency printing cost: " + settlementNumber,
                payable
        );
        costEntryRepository.save(cost);

        log.info("Agency settlement {} generated for assignment {}", settlementNumber, assignment.getAssignmentNumber());
        return mapToAdminSettlement(saved);
    }

    @Transactional
    public AdminSettlementResponse payAgencySettlement(
            UUID settlementId,
            UUID adminUserId,
            PaySettlementRequest request
    ) {
        AgencySettlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new ResourceNotFoundException("Settlement not found: " + settlementId));

        User adminUser = null;
        if (adminUserId != null) {
            adminUser = userRepository.findById(adminUserId).orElse(null);
        }

        BigDecimal newPaid = settlement.getPaidAmount().add(request.amount());
        BigDecimal newOutstanding = settlement.getPayableAmount().subtract(newPaid).max(BigDecimal.ZERO);

        settlement.setPaidAmount(newPaid);
        settlement.setOutstandingAmount(newOutstanding);
        settlement.setPaymentReference(request.paymentReference());
        settlement.setSettledBy(adminUser);
        settlement.setSettledAt(OffsetDateTime.now());

        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            settlement.setSettlementStatus(SettlementStatus.SETTLED);
        } else {
            settlement.setSettlementStatus(SettlementStatus.PARTIALLY_PAID);
        }

        if (request.notes() != null) {
            settlement.setNotes((settlement.getNotes() != null ? settlement.getNotes() + " | " : "") + request.notes());
        }

        AgencySettlement saved = settlementRepository.save(settlement);
        log.info("Settlement {} payment recorded: {}", settlement.getSettlementNumber(), request.amount());
        return mapToAdminSettlement(saved);
    }

    @Transactional(readOnly = true)
    public AdminFinanceSummaryResponse getAdminFinanceSummary() {
        BigDecimal grossRevenue = invoiceRepository.sumAllInvoicedAmount();
        BigDecimal collected = paymentRepository.sumAllCompletedPayments();
        BigDecimal outstandingReceivables = invoiceRepository.sumAllDueAmount();

        BigDecimal agencyDirectCost = settlementRepository.sumAllPayableAmount();
        BigDecimal agencySettled = settlementRepository.sumAllPaidAmount();
        BigDecimal agencyPending = settlementRepository.sumAllOutstandingAmount();

        BigDecimal grossContribution = grossRevenue.subtract(agencyDirectCost);
        BigDecimal marginPct = BigDecimal.ZERO;
        if (grossRevenue.compareTo(BigDecimal.ZERO) > 0) {
            marginPct = grossContribution.multiply(BigDecimal.valueOf(100))
                    .divide(grossRevenue, 2, RoundingMode.HALF_UP);
        }

        return new AdminFinanceSummaryResponse(
                grossRevenue,
                collected,
                outstandingReceivables,
                agencyDirectCost,
                agencySettled,
                agencyPending,
                grossContribution,
                marginPct
        );
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminInvoiceResponse> getAdminInvoices(Pageable pageable, UUID schoolId, String status) {
        InvoiceStatus invoiceStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                invoiceStatus = InvoiceStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        Page<Invoice> page = invoiceRepository.searchAdminInvoices(schoolId, invoiceStatus, pageable);
        List<AdminInvoiceResponse> content = page.getContent().stream().map(this::mapToAdminInvoice).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminPaymentResponse> getAdminPayments(Pageable pageable, UUID schoolId, String status) {
        PaymentStatus paymentStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                paymentStatus = PaymentStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        Page<Payment> page = paymentRepository.searchAdminPayments(schoolId, paymentStatus, pageable);
        List<AdminPaymentResponse> content = page.getContent().stream().map(this::mapToAdminPayment).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    @Transactional(readOnly = true)
    public PagedResponse<AdminSettlementResponse> getAdminSettlements(Pageable pageable, UUID agencyId, String status) {
        SettlementStatus settlementStatus = null;
        if (status != null && !status.isBlank()) {
            try {
                settlementStatus = SettlementStatus.valueOf(status.toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }
        Page<AgencySettlement> page = settlementRepository.searchAdminSettlements(agencyId, settlementStatus, pageable);
        List<AdminSettlementResponse> content = page.getContent().stream().map(this::mapToAdminSettlement).toList();
        PaginationMeta meta = PaginationMeta.of(page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages(), page.isLast());
        return PagedResponse.of(content, meta);
    }

    // =========================================================================
    // INTERNAL PAYMENT PROCESSING
    // =========================================================================

    private SchoolPaymentResponse processPaymentRecord(
            PrintingOrder order,
            School school,
            UUID recordedById,
            RecordPaymentRequest request
    ) {
        String currentYear = String.valueOf(Year.now().getValue());
        long count = paymentRepository.countByPaymentReferenceStartingWith("PAY-" + currentYear) + 1;
        String paymentReference = String.format("PAY-%s-%04d", currentYear, count);

        User recordedBy = null;
        if (recordedById != null) {
            recordedBy = userRepository.findById(recordedById).orElse(null);
        }

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setSchool(school);
        payment.setPaymentReference(paymentReference);
        payment.setAmount(request.amount());
        payment.setPaymentMethod(request.paymentMethod());
        payment.setPaymentStatus(PaymentStatus.COMPLETED);
        payment.setTransactionReference(request.transactionReference());
        payment.setPaymentNotes(request.paymentNotes());
        payment.setRecordedBy(recordedBy);
        payment.setReceivedAt(OffsetDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Update corresponding Invoice if exists
        invoiceRepository.findByOrderId(order.getId()).ifPresent(invoice -> {
            BigDecimal updatedPaid = invoice.getPaidAmount().add(request.amount());
            BigDecimal updatedDue = invoice.getTotalAmount().subtract(updatedPaid).max(BigDecimal.ZERO);
            invoice.setPaidAmount(updatedPaid);
            invoice.setDueAmount(updatedDue);
            if (updatedDue.compareTo(BigDecimal.ZERO) == 0) {
                invoice.setInvoiceStatus(InvoiceStatus.PAID);
            } else {
                invoice.setInvoiceStatus(InvoiceStatus.PARTIALLY_PAID);
            }
            invoiceRepository.save(invoice);
        });

        log.info("Payment {} (amount: {}) recorded for order {}", paymentReference, request.amount(), order.getOrderNumber());
        return mapToSchoolPayment(savedPayment);
    }

    // =========================================================================
    // MAPPERS (STRICT NON-NEGOTIABLE ROLE ISOLATION)
    // =========================================================================

    private SchoolPaymentResponse mapToSchoolPayment(Payment p) {
        return new SchoolPaymentResponse(
                p.getId(),
                p.getOrder().getId(),
                p.getOrder().getOrderNumber(),
                p.getPaymentReference(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getPaymentStatus(),
                p.getTransactionReference(),
                p.getReceivedAt(),
                p.getPaymentNotes()
        );
    }

    private SchoolInvoiceResponse mapToSchoolInvoice(Invoice i) {
        return new SchoolInvoiceResponse(
                i.getId(),
                i.getOrder().getId(),
                i.getOrder().getOrderNumber(),
                i.getInvoiceNumber(),
                i.getSubtotal(),
                i.getDiscount(),
                i.getTaxAmount(),
                i.getTotalAmount(),
                i.getPaidAmount(),
                i.getDueAmount(),
                i.getInvoiceStatus(),
                i.getIssuedAt(),
                i.getDueDate(),
                i.getNotes()
        );
    }

    private AgencySettlementResponse mapToAgencySettlement(AgencySettlement s) {
        return new AgencySettlementResponse(
                s.getId(),
                s.getAssignment().getId(),
                s.getAssignment().getAssignmentNumber(),
                s.getSettlementNumber(),
                s.getAgreedAmount(),
                s.getDeductionAmount(),
                s.getPenaltyAmount(),
                s.getPayableAmount(),
                s.getPaidAmount(),
                s.getOutstandingAmount(),
                s.getSettlementStatus(),
                s.getPaymentReference(),
                s.getSettledAt(),
                s.getNotes()
        );
    }

    private AdminPaymentResponse mapToAdminPayment(Payment p) {
        return new AdminPaymentResponse(
                p.getId(),
                p.getOrder().getId(),
                p.getOrder().getOrderNumber(),
                p.getSchool().getId(),
                p.getSchool().getSchoolName(),
                p.getPaymentReference(),
                p.getAmount(),
                p.getPaymentMethod(),
                p.getPaymentStatus(),
                p.getTransactionReference(),
                p.getReceivedAt(),
                p.getRecordedBy() != null ? p.getRecordedBy().getFullName() : null,
                p.getPaymentNotes()
        );
    }

    private AdminInvoiceResponse mapToAdminInvoice(Invoice i) {
        return new AdminInvoiceResponse(
                i.getId(),
                i.getOrder().getId(),
                i.getOrder().getOrderNumber(),
                i.getSchool().getId(),
                i.getSchool().getSchoolName(),
                i.getQuotation() != null ? i.getQuotation().getId() : null,
                i.getQuotation() != null ? i.getQuotation().getQuotationNumber() : null,
                i.getInvoiceNumber(),
                i.getSubtotal(),
                i.getDiscount(),
                i.getTaxAmount(),
                i.getTotalAmount(),
                i.getPaidAmount(),
                i.getDueAmount(),
                i.getInvoiceStatus(),
                i.getIssuedAt(),
                i.getDueDate(),
                i.getNotes()
        );
    }

    private AdminSettlementResponse mapToAdminSettlement(AgencySettlement s) {
        return new AdminSettlementResponse(
                s.getId(),
                s.getAssignment().getId(),
                s.getAssignment().getAssignmentNumber(),
                s.getAgency().getId(),
                s.getAgency().getAgencyName(),
                s.getAssignment().getOrder() != null ? s.getAssignment().getOrder().getId() : null,
                s.getAssignment().getOrder() != null ? s.getAssignment().getOrder().getOrderNumber() : null,
                s.getSettlementNumber(),
                s.getAgreedAmount(),
                s.getDeductionAmount(),
                s.getPenaltyAmount(),
                s.getPayableAmount(),
                s.getPaidAmount(),
                s.getOutstandingAmount(),
                s.getSettlementStatus(),
                s.getPaymentReference(),
                s.getSettledAt(),
                s.getSettledBy() != null ? s.getSettledBy().getFullName() : null,
                s.getNotes()
        );
    }
}
