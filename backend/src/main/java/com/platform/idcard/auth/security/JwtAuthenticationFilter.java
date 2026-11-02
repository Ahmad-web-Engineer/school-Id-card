package com.platform.idcard.auth.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    public JwtAuthenticationFilter(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String jwt = authHeader.substring(7).trim();

            if (jwtService.validateToken(jwt) && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    Claims claims = jwtService.extractAllClaims(jwt);
                    String email = claims.getSubject();
                    String userIdStr = claims.get("userId", String.class);
                    String role = claims.get("role", String.class);
                    String fullName = claims.get("fullName", String.class);
                    String schoolIdStr = claims.get("schoolId", String.class);
                    String agencyIdStr = claims.get("agencyId", String.class);
                    String designation = claims.get("designation", String.class);

                    UUID userId = userIdStr != null ? UUID.fromString(userIdStr) : null;
                    UUID schoolId = schoolIdStr != null ? UUID.fromString(schoolIdStr) : null;
                    UUID agencyId = agencyIdStr != null ? UUID.fromString(agencyIdStr) : null;

                    UserPrincipal principal = new UserPrincipal(
                            userId,
                            email,
                            "", // Password not needed in token session
                            fullName != null ? fullName : "",
                            role != null ? role : "",
                            schoolId,
                            agencyId,
                            designation,
                            true
                    );

                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            principal.getAuthorities()
                    );
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                } catch (Exception ignored) {
                    // Invalid claims or parsing failure: context remains unauthenticated
                }
            }
        }

        filterChain.doFilter(request, response);
    }
}
