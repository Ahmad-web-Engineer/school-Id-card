package com.platform.idcard.reporting.service;

import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.agencyjob.entity.AgencyJobItem;
import com.platform.idcard.agencyjob.repository.AgencyAssignmentRepository;
import com.platform.idcard.delivery.entity.DeliveryRecord;
import com.platform.idcard.delivery.entity.DispatchRecord;
import com.platform.idcard.delivery.repository.DeliveryRecordRepository;
import com.platform.idcard.delivery.repository.DispatchRecordRepository;
import com.platform.idcard.finance.repository.AgencySettlementRepository;
import com.platform.idcard.finance.repository.InvoiceRepository;
import com.platform.idcard.finance.repository.PaymentRepository;
import com.platform.idcard.order.entity.PrintingOrder;
import com.platform.idcard.order.entity.PrintingOrderItem;
import com.platform.idcard.order.repository.PrintingOrderRepository;
import com.platform.idcard.organization.repository.AgencyRepository;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.production.entity.ProductionBatch;
import com.platform.idcard.production.entity.QualityCheck;
import com.platform.idcard.production.repository.ProductionBatchRepository;
import com.platform.idcard.production.repository.QualityCheckRepository;
import com.platform.idcard.reporting.dto.AdminFinancialReportResponse;
import com.platform.idcard.reporting.dto.AdminOperationalReportResponse;
import com.platform.idcard.reporting.dto.AgencyPerformanceReportResponse;
import com.platform.idcard.reporting.dto.SchoolReportSummaryResponse;
import com.platform.idcard.student.repository.StudentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ReportingService {

    private final PrintingOrderRepository orderRepository;
    private final StudentRepository studentRepository;
    private final SchoolRepository schoolRepository;
    private final AgencyRepository agencyRepository;
    private final AgencyAssignmentRepository assignmentRepository;
    private final ProductionBatchRepository productionBatchRepository;
    private final QualityCheckRepository qualityCheckRepository;
    private final DispatchRecordRepository dispatchRecordRepository;
    private final DeliveryRecordRepository deliveryRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final AgencySettlementRepository settlementRepository;

    public ReportingService(
            PrintingOrderRepository orderRepository,
            StudentRepository studentRepository,
            SchoolRepository schoolRepository,
            AgencyRepository agencyRepository,
            AgencyAssignmentRepository assignmentRepository,
            ProductionBatchRepository productionBatchRepository,
            QualityCheckRepository qualityCheckRepository,
            DispatchRecordRepository dispatchRecordRepository,
            DeliveryRecordRepository deliveryRecordRepository,
            InvoiceRepository invoiceRepository,
            PaymentRepository paymentRepository,
            AgencySettlementRepository settlementRepository
    ) {
        this.orderRepository = orderRepository;
        this.studentRepository = studentRepository;
        this.schoolRepository = schoolRepository;
        this.agencyRepository = agencyRepository;
        this.assignmentRepository = assignmentRepository;
        this.productionBatchRepository = productionBatchRepository;
        this.qualityCheckRepository = qualityCheckRepository;
        this.dispatchRecordRepository = dispatchRecordRepository;
        this.deliveryRecordRepository = deliveryRecordRepository;
        this.invoiceRepository = invoiceRepository;
        this.paymentRepository = paymentRepository;
        this.settlementRepository = settlementRepository;
    }

    @Transactional(readOnly = true)
    public AdminOperationalReportResponse getAdminOperationalReport() {
        List<PrintingOrder> allOrders = orderRepository.findAll();
        long totalOrders = allOrders.size();

        Map<String, Long> ordersByStatus = allOrders.stream()
                .collect(Collectors.groupingBy(PrintingOrder::getOrderStatus, Collectors.counting()));

        long totalStudents = studentRepository.count();

        List<ProductionBatch> allBatches = productionBatchRepository.findAll();
        long totalCardsPrinted = allBatches.stream()
                .mapToLong(b -> b.getActualQuantity() != null && b.getActualQuantity() > 0 ? b.getActualQuantity() : b.getPlannedQuantity())
                .sum();

        List<DeliveryRecord> allDeliveries = deliveryRecordRepository.findAll();
        long totalCardsDelivered = allDeliveries.stream()
                .mapToLong(d -> d.getDeliveryQuantity() != null ? d.getDeliveryQuantity() : 0)
                .sum();

        long activeAgencies = agencyRepository.findAll().stream()
                .filter(a -> "ACTIVE".equalsIgnoreCase(a.getStatus()))
                .count();

        long activeSchools = schoolRepository.findAll().stream()
                .filter(s -> "ACTIVE".equalsIgnoreCase(s.getStatus()))
                .count();

        List<QualityCheck> allQcs = qualityCheckRepository.findAll();
        long totalInspected = allQcs.stream().mapToLong(q -> q.getInspectedQuantity() != null ? q.getInspectedQuantity() : 0).sum();
        long totalAccepted = allQcs.stream().mapToLong(q -> q.getAcceptedQuantity() != null ? q.getAcceptedQuantity() : 0).sum();
        double passRate = totalInspected > 0 ? (totalAccepted * 100.0) / totalInspected : 100.0;
        passRate = Math.round(passRate * 10.0) / 10.0;

        return new AdminOperationalReportResponse(
                totalOrders,
                ordersByStatus,
                totalStudents,
                totalCardsPrinted,
                totalCardsDelivered,
                activeAgencies,
                activeSchools,
                passRate
        );
    }

    @Transactional(readOnly = true)
    public AdminFinancialReportResponse getAdminFinancialReport() {
        BigDecimal grossRevenue = invoiceRepository.sumAllInvoicedAmount();
        BigDecimal collectedRevenue = paymentRepository.sumAllCompletedPayments();
        BigDecimal outstandingReceivables = invoiceRepository.sumAllDueAmount();

        BigDecimal agencyDirectCosts = settlementRepository.sumAllPayableAmount();
        BigDecimal agencySettledPayouts = settlementRepository.sumAllPaidAmount();
        BigDecimal agencyPendingPayables = settlementRepository.sumAllOutstandingAmount();

        BigDecimal grossContribution = grossRevenue.subtract(agencyDirectCosts);
        BigDecimal grossMarginPercentage = BigDecimal.ZERO;
        if (grossRevenue.compareTo(BigDecimal.ZERO) > 0) {
            grossMarginPercentage = grossContribution
                    .multiply(new BigDecimal("100"))
                    .divide(grossRevenue, 2, RoundingMode.HALF_UP);
        }

        return new AdminFinancialReportResponse(
                grossRevenue,
                collectedRevenue,
                agencyDirectCosts,
                agencySettledPayouts,
                outstandingReceivables,
                agencyPendingPayables,
                grossContribution,
                grossMarginPercentage
        );
    }

    @Transactional(readOnly = true)
    public SchoolReportSummaryResponse getSchoolReportSummary(UUID schoolId) {
        long totalStudents = studentRepository.countBySchoolId(schoolId);
        long totalOrders = orderRepository.countBySchoolId(schoolId);

        List<PrintingOrder> schoolOrders = orderRepository.findAll().stream()
                .filter(o -> schoolId.equals(o.getSchool().getId()))
                .toList();

        long cardsOrdered = schoolOrders.stream()
                .flatMap(o -> o.getItems().stream())
                .mapToLong(PrintingOrderItem::getQuantity)
                .sum();

        long cardsDelivered = schoolOrders.stream()
                .flatMap(o -> deliveryRecordRepository.findByOrderId(o.getId()).stream())
                .mapToLong(d -> d.getDeliveryQuantity() != null ? d.getDeliveryQuantity() : 0)
                .sum();

        BigDecimal totalBilled = invoiceRepository.sumTotalInvoicedBySchoolId(schoolId);
        BigDecimal totalPaid = paymentRepository.sumCompletedPaymentsBySchoolId(schoolId);
        BigDecimal outstandingDues = invoiceRepository.sumDueAmountBySchoolId(schoolId);

        return new SchoolReportSummaryResponse(
                totalStudents,
                totalOrders,
                cardsOrdered,
                cardsDelivered,
                totalBilled,
                totalPaid,
                outstandingDues
        );
    }

    @Transactional(readOnly = true)
    public AgencyPerformanceReportResponse getAgencyPerformanceReport(UUID agencyId) {
        List<AgencyAssignment> assignments = assignmentRepository.findAllByAgencyId(agencyId);
        long totalJobsAssigned = assignments.size();

        long totalCardsAssigned = assignments.stream()
                .flatMap(a -> a.getItems().stream())
                .mapToLong(i -> i.getAssignedQuantity() != null ? i.getAssignedQuantity() : 0)
                .sum();

        long totalCardsProduced = assignments.stream()
                .flatMap(a -> productionBatchRepository.findAllByAssignmentId(a.getId()).stream())
                .mapToLong(b -> b.getActualQuantity() != null && b.getActualQuantity() > 0 ? b.getActualQuantity() : b.getPlannedQuantity())
                .sum();

        long totalCardsDispatched = dispatchRecordRepository.findByAssignmentAgencyId(agencyId, org.springframework.data.domain.Pageable.unpaged())
                .getContent().stream()
                .mapToLong(d -> d.getDispatchQuantity() != null ? d.getDispatchQuantity() : 0)
                .sum();

        List<QualityCheck> agencyQcs = assignments.stream()
                .flatMap(a -> qualityCheckRepository.findAllByAssignmentId(a.getId()).stream())
                .toList();

        long qcPassed = agencyQcs.stream().mapToLong(q -> q.getAcceptedQuantity() != null ? q.getAcceptedQuantity() : 0).sum();
        long qcFailed = agencyQcs.stream().mapToLong(q -> q.getRejectedQuantity() != null ? q.getRejectedQuantity() : 0).sum();
        double qcPassPercentage = (qcPassed + qcFailed) > 0 ? (qcPassed * 100.0) / (qcPassed + qcFailed) : 100.0;
        qcPassPercentage = Math.round(qcPassPercentage * 10.0) / 10.0;

        BigDecimal totalEarnings = settlementRepository.sumAgreedAmountByAgencyId(agencyId);
        BigDecimal settledEarnings = settlementRepository.sumPaidAmountByAgencyId(agencyId);
        BigDecimal pendingPayout = settlementRepository.sumOutstandingAmountByAgencyId(agencyId);

        return new AgencyPerformanceReportResponse(
                totalJobsAssigned,
                totalCardsAssigned,
                totalCardsProduced,
                totalCardsDispatched,
                qcPassed,
                qcFailed,
                qcPassPercentage,
                totalEarnings,
                settledEarnings,
                pendingPayout
        );
    }
}
