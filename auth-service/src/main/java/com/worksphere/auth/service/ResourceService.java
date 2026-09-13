package com.worksphere.auth.service;

import com.worksphere.auth.entity.Resource;
import com.worksphere.auth.repository.ResourceRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ResourceService {

    private final ResourceRepository resourceRepository;

    public ResourceService(ResourceRepository resourceRepository) {
        this.resourceRepository = resourceRepository;
    }

    public Optional<Resource> findResource(
            String httpMethod,
            String path) {

        return resourceRepository.findAll()
                .stream()
                .filter(resource ->
                        resource.getHttpMethod()
                                .equalsIgnoreCase(httpMethod))
                .filter(resource ->
                        pathMatches(
                                resource.getPathPattern(),
                                path
                        ))
                .findFirst();
    }

    private boolean pathMatches(
            String pathPattern,
            String actualPath) {

        String[] patternParts =
                pathPattern.split("/");

        String[] actualParts =
                actualPath.split("/");

        if (patternParts.length != actualParts.length) {
            return false;
        }

        for (int i = 0; i < patternParts.length; i++) {

            String patternPart = patternParts[i];
            String actualPart = actualParts[i];

            if (patternPart.startsWith("{")
                    && patternPart.endsWith("}")) {
                continue;
            }

            if (!patternPart.equals(actualPart)) {
                return false;
            }
        }

        return true;
    }
}
