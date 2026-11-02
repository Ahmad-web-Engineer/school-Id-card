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
import com.platform.idcard.organization.dto.CreateOrganizationUserRequest;
import com.platform.idcard.organization.dto.SchoolRequest;
import com.platform.idcard.organization.dto.SchoolResponse;
import com.platform.idcard.organization.dto.UpdateSchoolSettingsRequest;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.entity.SchoolUser;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.organization.repository.SchoolUserRepository;
import com.platform.idcard.file.entity.FileEntity;
import com.platform.idcard.file.service.FileStorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class SchoolService {

    private final SchoolRepository schoolRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final FileStorageService fileStorageService;

    public SchoolService(
            SchoolRepository schoolRepository,
            SchoolUserRepository schoolUserRepository,
            UserRepository userRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            FileStorageService fileStorageService
    ) {
        this.schoolRepository = schoolRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.fileStorageService = fileStorageService;
    }

    public PagedResponse<SchoolResponse> getSchools(int page, int size, String search, String status) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        String searchTrimmed = (search != null && !search.isBlank()) ? search.trim() : null;
        String statusTrimmed = (status != null && !status.isBlank()) ? status.trim().toUpperCase() : null;

        Page<School> pageResult = schoolRepository.searchSchools(searchTrimmed, statusTrimmed, pageable);

        var data = pageResult.getContent().stream()
                .map(SchoolResponse::fromEntity)
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
    public SchoolResponse createSchool(SchoolRequest request) {
        if (schoolRepository.existsBySchoolCode(request.schoolCode().trim())) {
            throw new BusinessConflictException(ErrorCode.RESOURCE_CONFLICT,
                    "School with code '" + request.schoolCode() + "' already exists.");
        }
        if (schoolRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessConflictException(ErrorCode.RESOURCE_CONFLICT,
                    "School with email '" + request.email() + "' already exists.");
        }

        School school = new School();
        school.setSchoolName(request.schoolName().trim());
        school.setSchoolCode(request.schoolCode().trim().toUpperCase());
        school.setAddress(request.address());
        school.setCity(request.city());
        school.setState(request.state());
        school.setPincode(request.pincode());
        school.setEmail(request.email().trim().toLowerCase());
        school.setMobile(request.mobile());
        if (request.status() != null && !request.status().isBlank()) {
            school.setStatus(request.status().toUpperCase());
        }

        School saved = schoolRepository.save(school);
        return SchoolResponse.fromEntity(saved);
    }

    public SchoolResponse getSchoolById(UUID id) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + id + " not found."));
        return SchoolResponse.fromEntity(school);
    }

    @Transactional
    public SchoolResponse updateSchool(UUID id, SchoolRequest request) {
        School school = schoolRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + id + " not found."));

        if (!school.getEmail().equalsIgnoreCase(request.email().trim()) &&
            schoolRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessConflictException(ErrorCode.RESOURCE_CONFLICT,
                    "School with email '" + request.email() + "' already exists.");
        }

        school.setSchoolName(request.schoolName().trim());
        school.setAddress(request.address());
        school.setCity(request.city());
        school.setState(request.state());
        school.setPincode(request.pincode());
        school.setEmail(request.email().trim().toLowerCase());
        school.setMobile(request.mobile());
        if (request.status() != null && !request.status().isBlank()) {
            school.setStatus(request.status().toUpperCase());
        }

        School updated = schoolRepository.save(school);
        return SchoolResponse.fromEntity(updated);
    }

    @Transactional
    public SchoolResponse updateSchoolSettings(UUID schoolId, UpdateSchoolSettingsRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + schoolId + " not found."));

        if (request.email() != null && !request.email().isBlank() &&
            !school.getEmail().equalsIgnoreCase(request.email().trim()) &&
            schoolRepository.existsByEmail(request.email().trim().toLowerCase())) {
            throw new BusinessConflictException(ErrorCode.RESOURCE_CONFLICT,
                    "School with email '" + request.email() + "' already exists.");
        }

        school.setSchoolName(request.schoolName().trim());
        if (request.address() != null) school.setAddress(request.address().trim());
        if (request.city() != null) school.setCity(request.city().trim());
        if (request.state() != null) school.setState(request.state().trim());
        if (request.pincode() != null) school.setPincode(request.pincode().trim());
        if (request.email() != null && !request.email().isBlank()) school.setEmail(request.email().trim().toLowerCase());
        if (request.mobile() != null) school.setMobile(request.mobile().trim());
        if (request.logoFileId() != null) school.setLogoFileId(request.logoFileId().trim());
        if (request.signatureFileId() != null) school.setSignatureFileId(request.signatureFileId().trim());
        if (request.website() != null) school.setWebsite(request.website().trim());
        if (request.affiliationNumber() != null) school.setAffiliationNumber(request.affiliationNumber().trim());
        if (request.tagline() != null) school.setTagline(request.tagline().trim());
        if (request.principalName() != null) school.setPrincipalName(request.principalName().trim());

        School updated = schoolRepository.save(school);
        return SchoolResponse.fromEntity(updated);
    }

    @Transactional
    public UserSummaryResponse createSchoolUser(UUID schoolId, CreateOrganizationUserRequest request) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + schoolId + " not found."));

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new BusinessConflictException(ErrorCode.RESOURCE_CONFLICT,
                    "User with email '" + email + "' is already registered.");
        }

        Role schoolRole = roleRepository.findByRoleName("SCHOOL")
                .orElseThrow(() -> new ResourceNotFoundException("Role 'SCHOOL' not configured."));

        User user = new User();
        user.setFullName(request.fullName().trim());
        user.setEmail(email);
        user.setMobile(request.mobile());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus("ACTIVE");
        user.setRoles(Set.of(schoolRole));
        User savedUser = userRepository.save(user);

        SchoolUser schoolUser = new SchoolUser(savedUser, school, request.designation());
        schoolUserRepository.save(schoolUser);

        return new UserSummaryResponse(
                savedUser.getId(),
                savedUser.getFullName(),
                savedUser.getEmail(),
                savedUser.getMobile(),
                "SCHOOL",
                school.getId(),
                null,
                request.designation(),
                savedUser.getStatus()
        );
    }

    public List<UserSummaryResponse> getSchoolUsers(UUID schoolId) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + schoolId + " not found."));
        return schoolUserRepository.findBySchoolId(schoolId).stream()
                .map(su -> new UserSummaryResponse(
                        su.getUser().getId(),
                        su.getUser().getFullName(),
                        su.getUser().getEmail(),
                        su.getUser().getMobile(),
                        "SCHOOL",
                        school.getId(),
                        null,
                        su.getDesignation(),
                        su.getUser().getStatus()
                ))
                .toList();
    }

    @Transactional
    public void resetUserPassword(UUID userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with ID: " + userId));
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public SchoolResponse uploadSchoolLogo(UUID schoolId, MultipartFile file, UUID uploadedBy) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + schoolId + " not found."));

        FileEntity stored = fileStorageService.storeFile(file, schoolId, uploadedBy);
        school.setLogoFileId(stored.getId().toString());
        School updated = schoolRepository.save(school);
        return SchoolResponse.fromEntity(updated);
    }

    @Transactional
    public SchoolResponse uploadSchoolSignature(UUID schoolId, MultipartFile file, UUID uploadedBy) {
        School school = schoolRepository.findById(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School with ID " + schoolId + " not found."));

        FileEntity stored = fileStorageService.storeFile(file, schoolId, uploadedBy);
        school.setSignatureFileId(stored.getId().toString());
        School updated = schoolRepository.save(school);
        return SchoolResponse.fromEntity(updated);
    }
}
