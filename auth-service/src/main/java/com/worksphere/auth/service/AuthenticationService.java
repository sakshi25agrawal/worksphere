package com.worksphere.auth.service;

import com.worksphere.auth.dto.request.AuthenticationRequest;
import com.worksphere.auth.dto.response.AuthenticationResponse;
import com.worksphere.auth.entity.AppUser;
import com.worksphere.auth.repository.AppUserRepository;
import com.worksphere.auth.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final PermissionService permissionService;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AppUserRepository appUserRepository,
            PermissionService permissionService) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
        this.permissionService = permissionService;
    }

    public AuthenticationResponse authenticate(
            AuthenticationRequest request) {

        // 1. Authenticate username and password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        // 2. Load user with roles from database
        AppUser user = appUserRepository
                .findByUsername(request.username())
                .orElseThrow();

        // 3. Get permissions for all user's roles
        List<String> permissions =
                permissionService.getPermissionsForRoles(
                        user.getRoles()
                );

        // 4. Generate JWT containing roles + permissions
        String token =
                jwtService.generateToken(
                        user,
                        permissions
                );

        return new AuthenticationResponse(token);
    }
}

