package com.college.eventreg.controller;

import com.college.eventreg.model.User;
import com.college.eventreg.repository.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class EventControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private PasswordEncoder encoder;

    @Test
    @DisplayName("Login page should be publicly accessible")
    void testLoginPageAccessible() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Signup page should be publicly accessible")
    void testSignupPageAccessible() throws Exception {
        mockMvc.perform(get("/signup"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Protected pages should redirect unauthenticated users to login")
    void testProtectedPagesRedirect() throws Exception {
        mockMvc.perform(get("/user/home"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/admin/home"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("Should register a new student via signup form")
    void testStudentSignup() throws Exception {
        mockMvc.perform(post("/signup")
                        .param("username", "newstudent@test.edu")
                        .param("password", "password123")
                        .param("name", "New Student")
                        .param("registerNumber", "2024NEW")
                        .param("department", "Computer Science")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?registered=true"));

        // Verify user was created in the database
        assertTrue(userRepo.findByUsername("newstudent@test.edu").isPresent());
    }

    @Test
    @DisplayName("Should reject signup with duplicate email")
    void testDuplicateSignupRejected() throws Exception {
        // Create existing user
        User existing = new User();
        existing.setUsername("existing@test.edu");
        existing.setPassword(encoder.encode("pass"));
        existing.setRole("ROLE_USER");
        existing.setName("Existing");
        existing.setRegisterNumber("2024EX");
        existing.setDepartment("IT");
        userRepo.save(existing);

        mockMvc.perform(post("/signup")
                        .param("username", "existing@test.edu")
                        .param("password", "password123")
                        .param("name", "Duplicate")
                        .param("registerNumber", "2024DUP")
                        .param("department", "IT")
                        .with(csrf()))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Schedule page should be accessible to all authenticated users")
    void testScheduleAccessible() throws Exception {
        mockMvc.perform(get("/schedule"))
                .andExpect(status().is3xxRedirection()); // Redirects to login for unauthenticated
    }
}
