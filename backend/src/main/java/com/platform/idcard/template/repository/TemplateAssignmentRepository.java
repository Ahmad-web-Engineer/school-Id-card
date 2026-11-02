package com.platform.idcard.template.repository;

import com.platform.idcard.template.entity.TemplateAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateAssignmentRepository extends JpaRepository<TemplateAssignment, UUID> {

    Optional<TemplateAssignment> findByTemplateIdAndSchoolIdAndAcademicSession(UUID templateId, UUID schoolId, String academicSession);

    List<TemplateAssignment> findByTemplateId(UUID templateId);

    List<TemplateAssignment> findBySchoolIdAndActiveTrue(UUID schoolId);
}
