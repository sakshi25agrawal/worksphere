package com.worksphere.auth.service;

import com.worksphere.auth.dto.request.AuthenticationRequest;
import com.worksphere.auth.dto.response.AuthenticationResponse;
import com.worksphere.auth.entity.AppUser;
import com.worksphere.auth.repository.AppUserRepository;
import com.worksphere.auth.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;

    public AuthenticationService(
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            AppUserRepository appUserRepository) {

        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.appUserRepository = appUserRepository;
    }

    public AuthenticationResponse authenticate(
            AuthenticationRequest request) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.username(),
                        request.password()
                )
        );

        AppUser user = appUserRepository
                .findByUsername(request.username())
                .orElseThrow();

        String token =
                jwtService.generateToken(user.getUsername());

        return new AuthenticationResponse(token);
    }
}