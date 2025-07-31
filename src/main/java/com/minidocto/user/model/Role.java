package com.minidocto.user.model;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.Set;
import java.util.Collections;

public enum Role {
    USER(Collections.singleton(new SimpleGrantedAuthority("ROLE_USER"))),
    PRO(Collections.singleton(new SimpleGrantedAuthority("ROLE_PRO")));

    private final Set<GrantedAuthority> authorities;

    Role(Set<GrantedAuthority> authorities) {
        this.authorities = authorities;
    }

    public Set<GrantedAuthority> getAuthorities() {
        return authorities;
    }
} 