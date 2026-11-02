package com.platform.idcard.organization.service;

import com.platform.idcard.auth.dto.UserSummaryResponse;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.common.exception.BusinessConflictException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.ResourceNotFoundException;
import com.platform.idcard.common.response.PagedResponse;
import com.platform.idcard.common.response.PaginationMeta;
import com.platform.idcard.organization.dto.AgencyRequest;
import com.platform.idcard.organization.dto.AgencyResponse;
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.entity.Agency;
import com.platform.idcard.organization.entity.AgencyUser;
import com.platform.idcard.organization.repository.AgencyRepository;
import com.platform.idcard.organization.repository.AgencyUserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class AgencyService {

    private final AgencyRepository agencyRepository;
    private final AgencyUserRepository agencyUserRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AgencyService(
            AgencyRepository agencyRepository,
            AgencyUserRepository agencyUserRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.agencyRepository = agencyRepository;
        this.agencyUserRepository = agencyUserRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public PagedResponse<AgencyResponse> getAgencies(int page, int size, String search, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String searchTrimmed = (search != null && !search.isBlank()) ? search.trim() : null;
        String statusTrimmed = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        Page<Agency> pageResult = agencyRepository.searchAgencies(searchTrimmed, statusTrimmed, pageable);

        var data = pageResult.getContent().stream()
                .map(AgencyResponse::fromEntity)
                .toList();

        PaginationMeta meta = PaginationMeta.of(
                pageResult.getNumber(),
                pageResult.getSize(),
                pageResult.getTotalElements(),
                pageResult.getTotalPages(),
                pageResult.isLast()
        );

        return PagedResponse.of(data, meta);
    }

    @Transactional
    public AgencyResponse createAgency(AgencyRequest request) {
        if (agencyRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessConflictException(ErrorCode.RESOURCE_CONFLICT,
                    "Printing agency with email '" + request.email() + "' already exists.");
        }

        Agency agency = new Agency();
        agency.setAgencyName(request.agencyName().trim());
        agency.setContactPerson(request.contactPerson().trim());
        agency.setEmail(request.email().trim().toLowerCase());
        agency.setMobile(request.mobile().trim());
        agency.setAddress(request.address());
        if (request.productionCapacity() != null) {
            agency.setProductionCapacity(request.productionCapacity());
        }
        agency.setCapabilities(request.capabilities());
        if (request.status() != null && !request.status().isBlank()) {
            agency.setStatus(request.status().toUpperCase());
        }

        Agency saved = agencyRepository.save(agency);
        return AgencyResponse.fromEntity(saved);
    }

    public AgencyResponse getAgencyById(UUID id) {
        Agency agency = agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agency with ID " + id + " not found."));
        return AgencyResponse.fromEntity(agency);
    }

    @Transactional
    public AgencyResponse updateAgency(UUID id, AgencyRequest request) {
        Agency agency = agencyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Agency with ID " + id + " not found."));

        if (!agency.getEmail().equalsIgnoreCase(request.email().trim()) &&
            agencyRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessConflictException(ErrorCode.RESOURCE_CONFLICT,
                    "Printing agency with email '" + request.email() + "' already exists.");
        }

        agency.setAgencyName(request.agencyName().trim());
        agency.setContactPerson(request.contactPerson().trim());
        agency.setEmail(request.email().trim().toLowerCase());
        agency.setMobile(request.mobile().trim());
        agency.setAddress(request.address());
        if (request.productionCapacity() != null) {
            agency.setProductionCapacity(request.productionCapacity());
        }
        agency.setCapabilities(request.capabilities());
        if (request.status() != null && !request.status().isBlank()) {
            agency.setStatus(request.status().toUpperCase());
        }

        Agency updated = agencyRepository.save(agency);
        return AgencyResponse.fromEntity(updated);
    }

    @Transactional
    public UserSummaryResponse createAgencyUser(UUID agencyId, CreateOrganizationUserRequest request) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency with ID " + agencyId + " not found."));

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessConflictException(ErrorCode.RESOURCE_CONFLICT,
                    "User with email '" + email + "' is already registered.");
        }

        Role agencyRole = roleRepository.findByRoleName("PRINTING_AGENCY")
                .orElseThrow(() -> new ResourceNotFoundException("Role 'PRINTING_AGENCY' not configured."));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setMobile(request.mobile());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("ACTIVE");
        user.setRoles(Set.of(agencyRole));
        User savedUser = userRepository.save(user);

        AgencyUser agencyUser = new AgencyUser(savedUser, agency, request.designation());
        agencyUserRepository.save(agencyUser);

        return new UserSummaryResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getMobile(),
                "PRINTING_AGENCY",
                null,
                agency.getId(),
                request.designation(),
                savedUser.getStatus()
        );
    }

    public List<UserSummaryResponse> getAgencyUsers(UUID agencyId) {
        Agency agency = agencyRepository.findById(agencyId)
                .orElseThrow(() -> new ResourceNotFoundException("Agency with ID " + agencyId + " not found."));
        return agencyUserRepository.findByAgencyId(agencyId).stream()
                .map(au -> new UserSummaryResponse(
                        au.getUser().getId(),
                        au.getUser().getFullName(),
                        au.getUser().getEmail(),
                        au.getUser().getMobile(),
                        "PRINTING_AGENCY",
                        null,
                        agency.getId(),
                        au.getDesignation(),
                        au.getUser().getStatus()
                ))
                .toList();
    }
}
