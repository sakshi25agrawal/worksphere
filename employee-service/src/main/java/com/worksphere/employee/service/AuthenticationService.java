package com.worksphere.employee.service;

import org.springframework.stereotype.Service;

import com.worksphere.employee.dto.request.AuthenticationRequest;
import com.worksphere.employee.dto.response.AuthenticationResponse;
import com.worksphere.employee.security.JwtService;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.worksphere.employee.entity.AppUser;
import com.worksphere.employee.repository.AppUserRepository;

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

        AppUser user = appUserRepository.findByUsername(request.username())
                .orElseThrow();

        String token = jwtService.generateToken(user.getUsername());

        return new AuthenticationResponse(token);

    }

}