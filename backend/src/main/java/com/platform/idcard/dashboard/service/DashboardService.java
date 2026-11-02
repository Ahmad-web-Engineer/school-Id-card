package com.platform.idcard.dashboard.service;

import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.agencyjob.entity.AgencyJobItem;
import com.platform.idcard.agencyjob.entity.AssignmentStatus;
import com.platform.idcard.agencyjob.repository.AgencyAssignmentRepository;
import com.platform.idcard.dashboard.dto.AdminDashboardStatsResponse;
import com.platform.idcard.dashboard.dto.AgencyDashboardStatsResponse;
import com.platform.idcard.dashboard.dto.RecentOrderSummary;
import com.platform.idcard.dashboard.dto.SchoolDashboardStatsResponse;
import com.platform.idcard.finance.repository.AgencySettlementRepository;
import com.platform.idcard.finance.repository.InvoiceRepository;
import com.platform.idcard.order.entity.PrintingOrder;
import com.platform.idcard.order.repository.PrintingOrderRepository;
import com.platform.idcard.organization.entity.Agency;
import com.platform.idcard.organization.repository.AgencyRepository;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.student.repository.StudentRepository;
import com.platform.idcard.template.repository.CardTemplateRepository;
import com.platform.idcard.template.repository.TemplateAssignmentRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class DashboardService {

    private final SchoolRepository schoolRepository;
    private final AgencyRepository agencyRepository;
    private final StudentRepository studentRepository;
    private final CardTemplateRepository cardTemplateRepository;
    private final PrintingOrderRepository printingOrderRepository;
    private final TemplateAssignmentRepository templateAssignmentRepository;
    private final AgencyAssignmentRepository agencyAssignmentRepository;
    private final InvoiceRepository invoiceRepository;
    private final AgencySettlementRepository agencySettlementRepository;

    private static final List<String> ACTIVE_ORDER_STATUSES = List.of(
            "SUBMITTED", "UNDER_REVIEW", "CORRECTION_REQUIRED", "VERIFIED",
            "QUOTATION_GENERATED", "APPROVED", "ASSIGNED", "IN_PRODUCTION",
            "PRINTED", "QC_PASSED", "DISPATCHED"
    );

    public DashboardService(
            SchoolRepository schoolRepository,
            AgencyRepository agencyRepository,
            StudentRepository studentRepository,
            CardTemplateRepository cardTemplateRepository,
            PrintingOrderRepository printingOrderRepository,
            TemplateAssignmentRepository templateAssignmentRepository,
            AgencyAssignmentRepository agencyAssignmentRepository,
            InvoiceRepository invoiceRepository,
            AgencySettlementRepository agencySettlementRepository
    ) {
        this.schoolRepository = schoolRepository;
        this.agencyRepository = agencyRepository;
        this.studentRepository = studentRepository;
        this.cardTemplateRepository = cardTemplateRepository;
        this.printingOrderRepository = printingOrderRepository;
        this.templateAssignmentRepository = templateAssignmentRepository;
        this.agencyAssignmentRepository = agencyAssignmentRepository;
        this.invoiceRepository = invoiceRepository;
        this.agencySettlementRepository = agencySettlementRepository;
    }

    public AdminDashboardStatsResponse getAdminDashboardStats() {
        long totalSchools = schoolRepository.count();
        long totalAgencies = agencyRepository.count();
        long totalStudents = studentRepository.count();
        long totalOrders = printingOrderRepository.count();
        long activeOrders = printingOrderRepository.countByOrderStatusIn(ACTIVE_ORDER_STATUSES);
        long completedOrders = printingOrderRepository.countByOrderStatusIn(List.of("DELIVERED", "COMPLETED"));

        BigDecimal totalRevenue = invoiceRepository.sumAllInvoicedAmount();
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        List<PrintingOrder> recentOrdersList = printingOrderRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        List<RecentOrderSummary> recentOrders = recentOrdersList.stream()
                .map(o -> new RecentOrderSummary(
                        o.getId(),
                        o.getOrderNumber(),
                        o.getSchool() != null ? o.getSchool().getSchoolName() : "Unknown School",
                        o.getTotalStudents(),
                        o.getTotalQuantity(),
                        o.getOrderStatus(),
                        o.getCreatedAt()
                ))
                .toList();

        Map<String, Long> statusDistribution = new HashMap<>();
        List<Object[]> counts = printingOrderRepository.countGroupByOrderStatus();
        for (Object[] row : counts) {
            String status = (String) row[0];
            Long count = (Long) row[1];
            statusDistribution.put(status, count);
        }

        return new AdminDashboardStatsResponse(
                totalSchools,
                totalAgencies,
                totalStudents,
                totalOrders,
                activeOrders,
                completedOrders,
                totalRevenue,
                recentOrders,
                statusDistribution
        );
    }

    public SchoolDashboardStatsResponse getSchoolDashboardStats(UUID schoolId) {
        long enrolledStudents = studentRepository.countBySchoolId(schoolId);
        long approvedTemplates = cardTemplateRepository.findAvailableForSchool(schoolId, PageRequest.of(0, 1)).getTotalElements();
        long totalOrders = printingOrderRepository.countBySchoolId(schoolId);
        long activeOrders = printingOrderRepository.countBySchoolIdAndOrderStatusIn(schoolId, ACTIVE_ORDER_STATUSES);
        long deliveredCards = printingOrderRepository.countBySchoolIdAndOrderStatusIn(schoolId, List.of("DELIVERED", "COMPLETED"));
        long pendingQuotations = printingOrderRepository.countBySchoolIdAndOrderStatus(schoolId, "QUOTATION_GENERATED");

        List<PrintingOrder> recentOrdersList = printingOrderRepository.findWithFilters(
                schoolId, null, null,
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent();

        List<RecentOrderSummary> recentOrders = recentOrdersList.stream()
                .map(o -> new RecentOrderSummary(
                        o.getId(),
                        o.getOrderNumber(),
                        o.getSchool() != null ? o.getSchool().getSchoolName() : "",
                        o.getTotalStudents(),
                        o.getTotalQuantity(),
                        o.getOrderStatus(),
                        o.getCreatedAt()
                ))
                .toList();

        return new SchoolDashboardStatsResponse(
                enrolledStudents,
                approvedTemplates,
                totalOrders,
                activeOrders,
                deliveredCards,
                pendingQuotations,
                recentOrders
        );
    }

    public AgencyDashboardStatsResponse getAgencyDashboardStats(UUID agencyId) {
        List<AgencyAssignment> allJobs = agencyAssignmentRepository.findAllByAgencyId(agencyId);
        long totalJobs = allJobs.size();

        long pendingJobs = allJobs.stream()
                .filter(j -> j.getAssignmentStatus() == AssignmentStatus.ASSIGNED)
                .count();

        long inProductionJobs = allJobs.stream()
                .filter(j -> j.getAssignmentStatus() == AssignmentStatus.IN_PRODUCTION || j.getAssignmentStatus() == AssignmentStatus.AGENCY_ACCEPTED)
                .count();

        long completedJobs = allJobs.stream()
                .filter(j -> j.getAssignmentStatus() == AssignmentStatus.COMPLETED)
                .count();

        long capacity = agencyRepository.findById(agencyId)
                .map(Agency::getProductionCapacity)
                .orElse(1000);

        BigDecimal totalEarnings = agencySettlementRepository.sumPayableAmountByAgencyId(agencyId);
        if (totalEarnings == null) {
            totalEarnings = BigDecimal.ZERO;
        }

        List<AgencyDashboardStatsResponse.RecentAgencyJobSummary> recentJobs = allJobs.stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .limit(5)
                .map(j -> new AgencyDashboardStatsResponse.RecentAgencyJobSummary(
                        j.getId(),
                        j.getAssignmentNumber(),
                        j.getItems().stream().mapToInt(AgencyJobItem::getAssignedQuantity).sum(),
                        j.getAssignmentStatus().name(),
                        j.getAgreedRate()
                ))
                .toList();

        return new AgencyDashboardStatsResponse(
                totalJobs,
                pendingJobs,
                inProductionJobs,
                completedJobs,
                capacity,
                totalEarnings,
                recentJobs
        );
    }
}
