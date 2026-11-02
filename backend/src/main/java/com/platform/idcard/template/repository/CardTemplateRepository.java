package com.platform.idcard.template.repository;

import com.platform.idcard.template.entity.CardTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface CardTemplateRepository extends JpaRepository<CardTemplate, UUID> {

    boolean existsByTemplateNameIgnoreCase(String templateName);

    @Query(
            value = "SELECT t FROM CardTemplate t WHERE " +
                    "(:search IS NULL OR LOWER(t.templateName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                    "(:category IS NULL OR t.templateCategory = :category) AND " +
                    "(:status IS NULL OR t.status = :status)",
            countQuery = "SELECT count(t) FROM CardTemplate t WHERE " +
                    "(:search IS NULL OR LOWER(t.templateName) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
                    "(:category IS NULL OR t.templateCategory = :category) AND " +
                    "(:status IS NULL OR t.status = :status)"
    )
    Page<CardTemplate> findWithFilters(
            @Param("search") String search,
            @Param("category") String category,
            @Param("status") String status,
            Pageable pageable
    );

    @Query(
            value = "SELECT DISTINCT t FROM CardTemplate t " +
                    "WHERE t.status = 'ACTIVE' " +
                    "AND (t.school.id = :schoolId OR t.school IS NULL OR EXISTS (SELECT a FROM TemplateAssignment a WHERE a.template = t AND a.school.id = :schoolId AND a.active = true)) " +
                    "AND EXISTS (SELECT v FROM TemplateVersion v WHERE v.template = t AND v.approvalStatus = 'APPROVED')",
            countQuery = "SELECT count(DISTINCT t) FROM CardTemplate t " +
                    "WHERE t.status = 'ACTIVE' " +
                    "AND (t.school.id = :schoolId OR t.school IS NULL OR EXISTS (SELECT a FROM TemplateAssignment a WHERE a.template = t AND a.school.id = :schoolId AND a.active = true)) " +
                    "AND EXISTS (SELECT v FROM TemplateVersion v WHERE v.template = t AND v.approvalStatus = 'APPROVED')"
    )
    Page<CardTemplate> findAvailableForSchool(@Param("schoolId") UUID schoolId, Pageable pageable);

    @Query("SELECT CASE WHEN count(t) > 0 THEN true ELSE false END FROM CardTemplate t " +
            "WHERE t.id = :templateId " +
            "AND t.status = 'ACTIVE' " +
            "AND (t.school.id = :schoolId OR t.school IS NULL OR EXISTS (SELECT a FROM TemplateAssignment a WHERE a.template = t AND a.school.id = :schoolId AND a.active = true))")
    boolean isTemplateAvailableToSchool(@Param("templateId") UUID templateId, @Param("schoolId") UUID schoolId);
}
