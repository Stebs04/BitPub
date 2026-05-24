package com.bitpub.common.security.context;

import com.bitpub.common.security.enums.Permission;
import com.bitpub.common.security.enums.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JwtClaims {
    private String userId;
    private String username;
    private Role role;
    private List<Permission> permissions;
    private List<String> localeIds;
    private Integer tokenVersion;
    private String traceId;
}
