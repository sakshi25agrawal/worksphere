package com.worksphere.auth.repository;

import com.worksphere.auth.entity.RolePermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RolePermissionRepository
        extends JpaRepository<RolePermission, Long> {

    @Query("""
        SELECT rp.permission.code
        FROM RolePermission rp
        WHERE rp.role.name = :roleName
        """)
    List<String> findPermissionCodesByRoleName(
            @Param("roleName") String roleName
    );
}