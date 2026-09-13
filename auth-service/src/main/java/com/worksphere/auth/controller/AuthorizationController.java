package com.worksphere.auth.controller;

import com.worksphere.auth.entity.Resource;
import com.worksphere.auth.service.ResourceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/authorization")
public class AuthorizationController {

    private final ResourceService resourceService;

    public AuthorizationController(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    @GetMapping("/resource")
    public ResponseEntity<String> getRequiredPermission(
            @RequestParam String method,
            @RequestParam String path) {

        return resourceService.findResource(method, path)
                .map(Resource::getPermission)
                .map(permission -> ResponseEntity.ok(permission.getCode()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}