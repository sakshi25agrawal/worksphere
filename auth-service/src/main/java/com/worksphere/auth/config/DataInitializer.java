package com.worksphere.auth.config;

import com.worksphere.auth.entity.AppUser;
import com.worksphere.auth.repository.AppUserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            AppUserRepository appUserRepository,
            PasswordEncoder passwordEncoder) {

        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (appUserRepository.findByUsername("admin").isEmpty()) {

            AppUser admin = new AppUser();

            admin.setUsername("admin");

            admin.setPassword(
                    passwordEncoder.encode("Admin@123")
            );

            admin.setRole("ADMIN");

            admin.setEnabled(true);

            appUserRepository.save(admin);

            System.out.println("Default Admin User Created");
        }
    }
}