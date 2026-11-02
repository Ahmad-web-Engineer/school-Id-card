package com.platform.idcard.auth.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final String fullName;
    private final String role;
    private final UUID schoolId;
    private final UUID agencyId;
    private final String designation;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(
            UUID id,
            String email,
            String password,
            String fullName,
            String role,
            UUID schoolId,
            UUID agencyId,
            String designation,
            boolean active
    ) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
        this.schoolId = schoolId;
        this.agencyId = agencyId;
        this.designation = designation;
        this.active = active;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role));
    }

    public UUID getId() {
        return id;
    }

    public String getFullName() {
        return fullName;
    }

    public String getRole() {
        return role;
    }

    public UUID getSchoolId() {
        return schoolId;
    }

    public UUID getAgencyId() {
        return agencyId;
    }

    public String getDesignation() {
        return designation;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
