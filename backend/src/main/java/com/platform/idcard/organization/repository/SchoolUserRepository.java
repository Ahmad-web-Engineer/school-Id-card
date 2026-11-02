package com.platform.idcard.organization.repository;

import com.platform.idcard.auth.entity.User;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.entity.SchoolUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SchoolUserRepository extends JpaRepository<SchoolUser, UUID> {
    Optional<SchoolUser> findByUser(User user);
    Optional<SchoolUser> findByUserId(UUID userId);
    List<SchoolUser> findBySchool(School school);
    List<SchoolUser> findBySchoolId(UUID schoolId);
}
