package com.platform.idcard.agencyjob.repository;

import com.platform.idcard.agencyjob.entity.AgencyAssignment;
import com.platform.idcard.agencyjob.entity.AssignmentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencyAssignmentRepository extends JpaRepository<AgencyAssignment, UUID> {

    List<AgencyAssignment> findAllByOrderId(UUID orderId);

    List<AgencyAssignment> findAllByAgencyId(UUID agencyId);

    long countByAgencyId(UUID agencyId);

    Optional<AgencyAssignment> findByIdAndAgencyId(UUID id, UUID agencyId);

    boolean existsByAssignmentNumber(String assignmentNumber);

    @Query("SELECT COUNT(a) > 0 FROM AgencyAssignment a WHERE a.agency.id = :agencyId AND a.order.school.id = :schoolId")
    boolean existsByAgencyIdAndSchoolId(@Param("agencyId") UUID agencyId, @Param("schoolId") UUID schoolId);

    @Query("""
        SELECT a FROM AgencyAssignment a
        WHERE (:orderId IS NULL OR a.order.id = :orderId)
          AND (:agencyId IS NULL OR a.agency.id = :agencyId)
          AND (:status IS NULL OR a.assignmentStatus = :status)
          AND (:search IS NULL OR (
                LOWER(a.assignmentNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(a.agency.agencyName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
             OR LOWER(a.order.orderNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          ))
        ORDER BY a.createdAt DESC
    """)
    Page<AgencyAssignment> searchAdminAssignments(
            @Param("orderId") UUID orderId,
            @Param("agencyId") UUID agencyId,
            @Param("status") AssignmentStatus status,
            @Param("search") String search,
            Pageable pageable
    );

    @Query("""
        SELECT a FROM AgencyAssignment a
        WHERE a.agency.id = :agencyId
          AND (:status IS NULL OR a.assignmentStatus = :status)
          AND (:search IS NULL OR (
                LOWER(a.assignmentNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
          ))
        ORDER BY a.createdAt DESC
    """)
    Page<AgencyAssignment> searchAgencyJobs(
            @Param("agencyId") UUID agencyId,
            @Param("status") AssignmentStatus status,
            @Param("search") String search,
            Pageable pageable
    );
}
