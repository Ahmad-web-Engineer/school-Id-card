package com.platform.idcard.auth.service;

import com.platform.idcard.auth.dto.*;
import com.platform.idcard.auth.entity.RefreshToken;
import com.platform.idcard.auth.entity.Role;
import com.platform.idcard.auth.entity.User;
import com.platform.idcard.auth.repository.RefreshTokenRepository;
import com.platform.idcard.auth.repository.RoleRepository;
import com.platform.idcard.auth.repository.UserRepository;
import com.platform.idcard.auth.security.JwtService;
import com.platform.idcard.auth.security.SecurityUtils;
import com.platform.idcard.auth.security.UserPrincipal;
import com.platform.idcard.common.exception.ApiException;
import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.organization.entity.School;
import com.platform.idcard.organization.entity.SchoolUser;
import com.platform.idcard.organization.repository.AgencyUserRepository;
import com.platform.idcard.organization.repository.SchoolRepository;
import com.platform.idcard.organization.repository.SchoolUserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final SchoolUserRepository schoolUserRepository;
    private final AgencyUserRepository agencyUserRepository;
    private final SchoolRepository schoolRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            SchoolUserRepository schoolUserRepository,
            AgencyUserRepository agencyUserRepository,
            SchoolRepository schoolRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.schoolUserRepository = schoolUserRepository;
        this.agencyUserRepository = agencyUserRepository;
        this.schoolRepository = schoolRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.email().trim().toLowerCase())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Invalid email or password.", HttpStatus.UNAUTHORIZED));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Invalid email or password.", HttpStatus.UNAUTHORIZED);
        }

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Account is deactivated. Please contact support.", HttpStatus.UNAUTHORIZED);
        }

        return createLoginResponse(user);
    }

    @Transactional
    public LoginResponse registerSchool(RegisterSchoolRequest request) {
        String schoolCode = request.schoolCode().trim().toUpperCase();
        if (schoolRepository.existsBySchoolCode(schoolCode)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "School with code '" + schoolCode + "' already exists.", HttpStatus.CONFLICT);
        }

        String schoolEmail = request.schoolEmail().trim().toLowerCase();
        if (schoolRepository.existsByEmail(schoolEmail)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "School with email '" + schoolEmail + "' already exists.", HttpStatus.CONFLICT);
        }

        String adminEmail = request.adminEmail().trim().toLowerCase();
        if (userRepository.existsByEmail(adminEmail)) {
            throw new ApiException(ErrorCode.RESOURCE_CONFLICT, "User with email '" + adminEmail + "' already exists.", HttpStatus.CONFLICT);
        }

        Role schoolRole = roleRepository.findByRoleName("SCHOOL")
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "Role 'SCHOOL' not configured.", HttpStatus.INTERNAL_SERVER_ERROR));

        // 1. Create School
        School school = new School();
        school.setSchoolName(request.schoolName().trim());
        school.setSchoolCode(schoolCode);
        school.setAddress(request.address());
        school.setCity(request.city());
        school.setState(request.state());
        school.setPincode(request.pincode());
        school.setEmail(schoolEmail);
        school.setMobile(request.schoolMobile());
        school.setStatus("ACTIVE");
        School savedSchool = schoolRepository.save(school);

        // 2. Create School Admin User
        User user = new User();
        user.setFullName(request.adminFullName().trim());
        user.setEmail(adminEmail);
        user.setMobile(request.adminMobile());
        user.setPasswordHash(passwordEncoder.encode(request.adminPassword()));
        user.setStatus("ACTIVE");
        user.setRoles(Set.of(schoolRole));
        User savedUser = userRepository.save(user);

        // 3. Map School User
        String designation = (request.adminDesignation() != null && !request.adminDesignation().isBlank())
                ? request.adminDesignation().trim()
                : "Principal";
        SchoolUser schoolUser = new SchoolUser(savedUser, savedSchool, designation);
        schoolUserRepository.save(schoolUser);

        return createLoginResponse(savedUser);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found.", HttpStatus.NOT_FOUND));

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Current password does not match.", HttpStatus.BAD_REQUEST);
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
    }

    @Transactional
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        RefreshToken token = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "Invalid or expired refresh token.", HttpStatus.UNAUTHORIZED));

        if (token.isRevoked() || token.isExpired()) {
            refreshTokenRepository.delete(token);
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Refresh token is expired or revoked.", HttpStatus.UNAUTHORIZED);
        }

        User user = token.getUser();
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "Account is deactivated.", HttpStatus.UNAUTHORIZED);
        }

        // Rotate token
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        String primaryRole = user.getRoles().stream().map(Role::getRoleName).findFirst().orElse("USER");
        UUID schoolId = null;
        UUID agencyId = null;
        String designation = null;

        if ("SCHOOL".equalsIgnoreCase(primaryRole)) {
            var schoolUser = schoolUserRepository.findByUserId(user.getId()).orElse(null);
            if (schoolUser != null) {
                schoolId = schoolUser.getSchool().getId();
                designation = schoolUser.getDesignation();
            }
        } else if ("PRINTING_AGENCY".equalsIgnoreCase(primaryRole)) {
            var agencyUser = agencyUserRepository.findByUserId(user.getId()).orElse(null);
            if (agencyUser != null) {
                agencyId = agencyUser.getAgency().getId();
                designation = agencyUser.getDesignation();
            }
        }

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                primaryRole,
                schoolId,
                agencyId,
                designation,
                true
        );

        String newAccessToken = jwtService.generateAccessToken(principal);
        RefreshToken newRefreshToken = createRefreshToken(user);

        return TokenResponse.of(newAccessToken, newRefreshToken.getToken(), jwtService.getExpirationSeconds());
    }

    @Transactional
    public void logout(String refreshTokenStr) {
        if (refreshTokenStr != null && !refreshTokenStr.isBlank()) {
            refreshTokenRepository.findByToken(refreshTokenStr).ifPresent(token -> {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            });
        }
    }

    public UserSummaryResponse getCurrentUserProfile() {
        UserPrincipal principal = SecurityUtils.getCurrentUser();
        User user = userRepository.findById(principal.getId())
                .orElseThrow(() -> new ApiException(ErrorCode.RESOURCE_NOT_FOUND, "User not found.", HttpStatus.NOT_FOUND));

        return new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                principal.getRole(),
                principal.getSchoolId(),
                principal.getAgencyId(),
                principal.getDesignation(),
                user.getStatus()
        );
    }

    private LoginResponse createLoginResponse(User user) {
        String primaryRole = user.getRoles().stream()
                .map(Role::getRoleName)
                .findFirst()
                .orElse("USER");

        UUID schoolId = null;
        UUID agencyId = null;
        String designation = null;

        if ("SCHOOL".equalsIgnoreCase(primaryRole)) {
            var schoolUser = schoolUserRepository.findByUserId(user.getId()).orElse(null);
            if (schoolUser != null) {
                schoolId = schoolUser.getSchool().getId();
                designation = schoolUser.getDesignation();
            }
        } else if ("PRINTING_AGENCY".equalsIgnoreCase(primaryRole)) {
            var agencyUser = agencyUserRepository.findByUserId(user.getId()).orElse(null);
            if (agencyUser != null) {
                agencyId = agencyUser.getAgency().getId();
                designation = agencyUser.getDesignation();
            }
        }

        UserPrincipal principal = new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getFullName(),
                primaryRole,
                schoolId,
                agencyId,
                designation,
                true
        );

        String accessToken = jwtService.generateAccessToken(principal);
        RefreshToken refreshToken = createRefreshToken(user);

        UserSummaryResponse summary = new UserSummaryResponse(
                user.getId(),
                user.getFullName(),
                user.getEmail(),
                user.getMobile(),
                primaryRole,
                schoolId,
                agencyId,
                designation,
                user.getStatus()
        );

        return LoginResponse.of(accessToken, refreshToken.getToken(), jwtService.getExpirationSeconds(), summary);
    }

    private RefreshToken createRefreshToken(User user) {
        RefreshToken token = new RefreshToken(
                user,
                UUID.randomUUID().toString(),
                Instant.now().plus(7, ChronoUnit.DAYS)
        );
        return refreshTokenRepository.save(token);
    }
}
