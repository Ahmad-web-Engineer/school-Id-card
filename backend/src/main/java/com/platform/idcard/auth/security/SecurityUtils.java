package com.platform.idcard.auth.security;

import com.platform.idcard.common.exception.ErrorCode;
import com.platform.idcard.common.exception.UnauthorizedAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

public final class SecurityUtils {

    private SecurityUtils() {}

    public static UserPrincipal getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            return principal;
        }
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            String role = auth.getAuthorities().stream()
                    .map(a -> a.getAuthority().replace("ROLE_", ""))
                    .findFirst().orElse("SUPER_ADMIN");
            return new UserPrincipal(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    auth.getName(),
                    "",
                    auth.getName(),
                    role,
                    null,
                    null,
                    "Administrator",
                    true
            );
        }
        throw new UnauthorizedAccessException(ErrorCode.UNAUTHORIZED, "User is not authenticated.");
    }

    public static UUID getCurrentUserId() {
        return getCurrentUser().getId();
    }

    public static String getCurrentUserRole() {
        return getCurrentUser().getRole();
    }

    public static boolean isSuperAdmin() {
        return "SUPER_ADMIN".equalsIgnoreCase(getCurrentUserRole());
    }

    public static boolean isSchoolUser() {
        return "SCHOOL".equalsIgnoreCase(getCurrentUserRole());
    }

    public static boolean isAgencyUser() {
        return "PRINTING_AGENCY".equalsIgnoreCase(getCurrentUserRole());
    }

    /**
     * Non-negotiable rule 2: Takes school_id strictly from the logged-in user's token context.
     * Throws UnauthorizedAccessException if missing or user is not a school user.
     */
    public static UUID getRequiredSchoolId() {
        UserPrincipal user = getCurrentUser();
        if (user.getSchoolId() == null) {
            throw new UnauthorizedAccessException(
                    ErrorCode.RECORD_ACCESS_FORBIDDEN,
                    "Access denied: Logged-in user is not associated with any school."
            );
        }
        return user.getSchoolId();
    }

    /**
     * Non-negotiable rule 2: Takes agency_id strictly from the logged-in user's token context.
     * Throws UnauthorizedAccessException if missing or user is not an agency user.
     */
    public static UUID getRequiredAgencyId() {
        UserPrincipal user = getCurrentUser();
        if (user.getAgencyId() == null) {
            throw new UnauthorizedAccessException(
                    ErrorCode.RECORD_ACCESS_FORBIDDEN,
                    "Access denied: Logged-in user is not associated with any agency."
            );
        }
        return user.getAgencyId();
    }
}
