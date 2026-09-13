package com.worksphere.auth.repository;

import com.worksphere.auth.entity.Resource;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceRepository extends JpaRepository<Resource, Long> {

    Optional<Resource> findByHttpMethodAndPathPattern(
            String httpMethod,
            String pathPattern
    );
}
