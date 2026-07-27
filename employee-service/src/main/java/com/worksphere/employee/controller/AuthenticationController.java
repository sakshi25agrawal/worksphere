package com.worksphere.employee.controller;

import com.worksphere.employee.dto.AuthenticationRequest;
import com.worksphere.employee.dto.AuthenticationResponse;
import com.worksphere.employee.service.AuthenticationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthenticationController {

    private final AuthenticationService authenticationService;

    public AuthenticationController(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthenticationResponse> login(
            @RequestBody AuthenticationRequest request) {

        return ResponseEntity.ok(
                authenticationService.authenticate(request)
        );
    }

}