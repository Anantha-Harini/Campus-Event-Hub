package com.college.eventreg.service;

import com.college.eventreg.model.Event;
import com.college.eventreg.model.EventRegistration;
import com.college.eventreg.model.RegistrationStatus;
import com.college.eventreg.model.User;
import com.college.eventreg.repository.EventRegistrationRepository;
import com.college.eventreg.repository.EventRepository;
import com.college.eventreg.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class RegistrationServiceTest {

    @Autowired
    private RegistrationService registrationService;

    @Autowired
    private UserRepository userRepo;

    @Autowired
    private EventRepository eventRepo;

    @Autowired
    private EventRegistrationRepository registrationRepo;

    @Autowired
    private PasswordEncoder encoder;

    private User testStudent;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        registrationRepo.deleteAll();
        eventRepo.deleteAll();
        userRepo.deleteAll();

        testStudent = new User();
        testStudent.setUsername("test@student.edu");
        testStudent.setPassword(encoder.encode("test123"));
        testStudent.setRole("ROLE_USER");
        testStudent.setName("Test Student");
        testStudent.setRegisterNumber("2024TEST");
        testStudent.setDepartment("Computer Science");
        testStudent = userRepo.save(testStudent);

        testEvent = new Event();
        testEvent.setName("Test Workshop");
        testEvent.setDescription("A test event");
        testEvent.setVenue("Test Hall");
        testEvent.setEventDate(LocalDateTime.now().plusDays(5));
        testEvent.setCapacity(2);
        testEvent.setBannerUrl("https://example.com/banner.jpg");
        testEvent = eventRepo.save(testEvent);
    }

    @Test
    @DisplayName("Should register a student to an event successfully")
    void testSuccessfulRegistration() {
        EventRegistration reg = registrationService.registerStudentToEvent(testStudent, testEvent);

        assertNotNull(reg.getId());
        assertEquals(RegistrationStatus.CONFIRMED, reg.getStatus());
        assertEquals(testStudent.getId(), reg.getStudent().getId());
        assertEquals(testEvent.getId(), reg.getEvent().getId());
    }

    @Test
    @DisplayName("Should prevent duplicate registration for same event")
    void testDuplicateRegistrationBlocked() {
        registrationService.registerStudentToEvent(testStudent, testEvent);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            registrationService.registerStudentToEvent(testStudent, testEvent);
        });
        assertEquals("Already registered!", exception.getMessage());
    }

    @Test
    @DisplayName("Should block registration when event is full")
    void testCapacityLimitEnforced() {
        // Fill the event (capacity = 2)
        User student2 = new User();
        student2.setUsername("student2@test.edu");
        student2.setPassword(encoder.encode("pass"));
        student2.setRole("ROLE_USER");
        student2.setName("Student Two");
        student2.setRegisterNumber("2024002");
        student2.setDepartment("IT");
        student2 = userRepo.save(student2);

        registrationService.registerStudentToEvent(testStudent, testEvent);
        registrationService.registerStudentToEvent(student2, testEvent);

        // Third student should be blocked
        User student3 = new User();
        student3.setUsername("student3@test.edu");
        student3.setPassword(encoder.encode("pass"));
        student3.setRole("ROLE_USER");
        student3.setName("Student Three");
        student3.setRegisterNumber("2024003");
        student3.setDepartment("ECE");
        student3 = userRepo.save(student3);

        User finalStudent3 = student3;
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            registrationService.registerStudentToEvent(finalStudent3, testEvent);
        });
        assertTrue(exception.getMessage().contains("full"));
    }

    @Test
    @DisplayName("Should allow re-registration after cancellation")
    void testReRegistrationAfterCancel() {
        EventRegistration reg = registrationService.registerStudentToEvent(testStudent, testEvent);
        registrationService.cancelRegistration(reg.getId(), testStudent);

        // Should not throw — re-registration allowed
        EventRegistration reReg = registrationService.registerStudentToEvent(testStudent, testEvent);
        assertEquals(RegistrationStatus.CONFIRMED, reReg.getStatus());
    }

    @Test
    @DisplayName("Should cancel a registration successfully")
    void testCancelRegistration() {
        EventRegistration reg = registrationService.registerStudentToEvent(testStudent, testEvent);
        registrationService.cancelRegistration(reg.getId(), testStudent);

        EventRegistration cancelled = registrationRepo.findById(reg.getId()).orElseThrow();
        assertEquals(RegistrationStatus.CANCELLED, cancelled.getStatus());
    }

    @Test
    @DisplayName("Should block unauthorized cancellation")
    void testUnauthorizedCancellation() {
        EventRegistration reg = registrationService.registerStudentToEvent(testStudent, testEvent);

        User otherStudent = new User();
        otherStudent.setUsername("other@test.edu");
        otherStudent.setPassword(encoder.encode("pass"));
        otherStudent.setRole("ROLE_USER");
        otherStudent.setName("Other Student");
        otherStudent.setRegisterNumber("2024999");
        otherStudent.setDepartment("Mech");
        otherStudent = userRepo.save(otherStudent);

        User finalOther = otherStudent;
        assertThrows(SecurityException.class, () -> {
            registrationService.cancelRegistration(reg.getId(), finalOther);
        });
    }

    @Test
    @DisplayName("Should count only confirmed registrations as active")
    void testActiveRegistrationCount() {
        registrationService.registerStudentToEvent(testStudent, testEvent);
        assertEquals(1, registrationService.getActiveRegistrationsCount(testEvent));
    }
}
