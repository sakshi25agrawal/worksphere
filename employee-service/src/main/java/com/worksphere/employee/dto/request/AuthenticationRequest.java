package com.worksphere.employee.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public record AuthenticationRequest(

        @Schema(
                description = "Username",
                example = "sakshi"
        )
        @NotBlank(message = "Username is required")
        String username,

        @Schema(
                description = "Password",
                example = "Admin@123"
        )
        @NotBlank(message = "Password is required")
        String password

) {
}