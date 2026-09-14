package com.worksphere.auth.config;

import com.worksphere.auth.entity.AppUser;
import com.worksphere.auth.entity.Role;
import com.worksphere.auth.repository.AppUserRepository;
import com.worksphere.auth.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DataInitializer implements CommandLineRunner {

    private final AppUserRepository appUserRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(
            AppUserRepository appUserRepository,
            RoleRepository roleRepository,
            PasswordEncoder passwordEncoder) {

        this.appUserRepository = appUserRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }


    @Override
    public void run(String... args) {

        /*
         * Create default ADMIN user if it does not exist.
         */
        if (appUserRepository.findByUsername("admin").isEmpty()) {

            Role adminRole = roleRepository
                    .findByName("ADMIN")
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "ADMIN role not found"));

            AppUser admin = new AppUser();

            admin.setUsername("admin");

            admin.setPassword(
                    passwordEncoder.encode("Admin@123")
            );

            admin.setRoles(List.of(adminRole));

            admin.setEnabled(true);

            appUserRepository.save(admin);

            System.out.println("Default Admin User Created");
        }

        /*
         * Create default EMPLOYEE user if it does not exist.
         *
         * This user is used to test RBAC authorization.
         */
        if (appUserRepository.findByUsername("employee").isEmpty()) {

            Role employeeRole = roleRepository
                    .findByName("EMPLOYEE")
                    .orElseThrow(() ->
                            new IllegalStateException(
                                    "EMPLOYEE role not found"));

            AppUser employee = new AppUser();

            employee.setUsername("employee");

            employee.setPassword(
                    passwordEncoder.encode("Employee@123")
            );

            employee.setRoles(List.of(employeeRole));

            employee.setEnabled(true);

            appUserRepository.save(employee);

            System.out.println("Default Employee User Created");
        }
    }
}