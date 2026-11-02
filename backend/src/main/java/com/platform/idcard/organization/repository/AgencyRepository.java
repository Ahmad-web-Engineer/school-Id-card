package com.platform.idcard.organization.repository;

import com.platform.idcard.organization.entity.Agency;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencyRepository extends JpaRepository<Agency, UUID> {
    Optional<Agency> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT a FROM Agency a WHERE " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:search IS NULL OR LOWER(a.agencyName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')) OR " +
           " LOWER(a.contactPerson) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))")
    Page<Agency> searchAgencies(@Param("search") String search, @Param("status") String status, Pageable pageable);
}
