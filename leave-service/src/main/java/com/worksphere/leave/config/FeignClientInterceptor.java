package com.worksphere.leave.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignClientInterceptor {

    @Bean
    public RequestInterceptor authorizationRequestInterceptor() {
        return requestTemplate -> {

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            if (attributes == null) {
                return;
            }

            HttpServletRequest request = attributes.getRequest();

            String authorizationHeader = request.getHeader("Authorization");

            if (authorizationHeader != null && !authorizationHeader.isBlank()) {
                requestTemplate.header("Authorization", authorizationHeader);
            }
        };
    }
}
