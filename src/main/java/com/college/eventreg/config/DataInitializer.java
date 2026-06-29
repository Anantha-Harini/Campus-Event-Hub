package com.college.eventreg.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.college.eventreg.model.User;
import com.college.eventreg.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepo;
    private final PasswordEncoder encoder;

    public DataInitializer(UserRepository userRepo, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.encoder = encoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Create default admin account only if no admin exists
        if (userRepo.findByUsername("admin@college.edu").isEmpty()) {
            User admin = new User();
            admin.setUsername("admin@college.edu");
            admin.setPassword(encoder.encode("admin123"));
            admin.setName("System Administrator");
            admin.setRole("ROLE_ADMIN");
            userRepo.save(admin);
        }
    }
}
