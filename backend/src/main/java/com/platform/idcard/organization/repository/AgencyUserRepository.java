package com.platform.idcard.organization.repository;

import com.platform.idcard.auth.entity.User;
import com.platform.idcard.organization.entity.Agency;
import com.platform.idcard.organization.entity.AgencyUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgencyUserRepository extends JpaRepository<AgencyUser, UUID> {
    Optional<AgencyUser> findByUser(User user);
    Optional<AgencyUser> findByUserId(UUID userId);
    List<AgencyUser> findByAgency(Agency agency);
    List<AgencyUser> findByAgencyId(UUID agencyId);
}
