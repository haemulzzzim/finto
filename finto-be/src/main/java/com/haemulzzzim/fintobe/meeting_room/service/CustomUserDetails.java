package com.haemulzzzim.fintobe.meeting_room.service;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// 커스텀 UserDetails
public class CustomUserDetails implements UserDetails {
    private final Long userSeq;
    private final String email;
    private final String password;
    private final Collection<? extends GrantedAuthority> authorities;

    public CustomUserDetails(Long userSeq, String email, String password,
            Collection<? extends GrantedAuthority> authorities) {
        this.userSeq = userSeq;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    public Long getUserSeq() {
        return userSeq;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}