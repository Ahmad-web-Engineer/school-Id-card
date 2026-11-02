package com.platform.idcard.template.repository;

import com.platform.idcard.template.entity.TemplateVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TemplateVersionRepository extends JpaRepository<TemplateVersion, UUID> {

    Optional<TemplateVersion> findByIdAndTemplateId(UUID id, UUID templateId);

    List<TemplateVersion> findByTemplateIdOrderByVersionNumberAsc(UUID templateId);

    Optional<TemplateVersion> findFirstByTemplateIdOrderByVersionNumberDesc(UUID templateId);

    Optional<TemplateVersion> findFirstByTemplateIdAndApprovalStatus(UUID templateId, String approvalStatus);

    Optional<TemplateVersion> findFirstByTemplateIdAndApprovalStatusOrderByVersionNumberDesc(UUID templateId, String approvalStatus);

    List<TemplateVersion> findByTemplateIdAndApprovalStatus(UUID templateId, String approvalStatus);
}
