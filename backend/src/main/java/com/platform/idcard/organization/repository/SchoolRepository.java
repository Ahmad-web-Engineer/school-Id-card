package com.platform.idcard.organization.repository;

import com.platform.idcard.organization.entity.School;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolRepository extends JpaRepository<School, UUID> {
    Optional<School> findBySchoolCode(String schoolCode);
    boolean existsBySchoolCode(String schoolCode);
    boolean existsByEmail(String email);

    @Query("SELECT s FROM School s WHERE " +
           "(:status IS NULL OR s.status = :status) AND " +
           "(:search IS NULL OR LOWER(s.schoolName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.schoolCode) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           " LOWER(s.city) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<School> searchSchools(@Param("search") String search, @Param("status") String status, Pageable pageable);
}
