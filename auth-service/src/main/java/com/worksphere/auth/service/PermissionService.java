package com.worksphere.auth.service;

import com.worksphere.auth.entity.Role;
import com.worksphere.auth.repository.RolePermissionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PermissionService {

    private final RolePermissionRepository rolePermissionRepository;

    public PermissionService(
            RolePermissionRepository rolePermissionRepository) {

        this.rolePermissionRepository = rolePermissionRepository;
    }

    public List<String> getPermissionsForRoles(
            List<Role> roles) {

        return roles.stream()
                .flatMap(role ->
                        rolePermissionRepository
                                .findPermissionCodesByRoleName(
                                        role.getName()
                                )
                                .stream()
                )
                .distinct()
                .toList();
    }
}