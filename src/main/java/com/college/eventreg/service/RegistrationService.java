package com.college.eventreg.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.college.eventreg.model.Event;
import com.college.eventreg.model.EventRegistration;
import com.college.eventreg.model.RegistrationStatus;
import com.college.eventreg.model.User;
import com.college.eventreg.repository.EventRegistrationRepository;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class RegistrationService {

    private final EventRegistrationRepository registrationRepo;

    public RegistrationService(EventRegistrationRepository registrationRepo) {
        this.registrationRepo = registrationRepo;
    }

    @Transactional
    public EventRegistration registerStudentToEvent(User student, Event event) {
        // 1. Check duplicate registration
        if (registrationRepo.existsByStudentAndEventAndStatusNot(student, event, RegistrationStatus.CANCELLED)) {
            throw new IllegalStateException("Already registered!");
        }

        // 2. Check event capacity
        long activeCount = getActiveRegistrationsCount(event);
        if (activeCount >= event.getCapacity()) {
            throw new IllegalStateException("Registration Closed! Event is full.");
        }

        // 3. Create registration
        EventRegistration registration = new EventRegistration();
        registration.setStudent(student);
        registration.setEvent(event);
        registration.setRegistrationDate(LocalDateTime.now());
        registration.setStatus(RegistrationStatus.CONFIRMED); // Default status

        EventRegistration saved = registrationRepo.save(registration);

        return saved;
    }

    @Transactional
    public void updateRegistrationStatus(Long id, RegistrationStatus status) {
        EventRegistration registration = registrationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        
        // Update status
        registration.setStatus(status);
        registrationRepo.save(registration);
    }

    @Transactional
    public void cancelRegistration(Long id, User student) {
        EventRegistration registration = registrationRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found"));
        
        if (!registration.getStudent().getId().equals(student.getId())) {
            throw new SecurityException("Unauthorized action");
        }

        registration.setStatus(RegistrationStatus.CANCELLED);
        registrationRepo.save(registration);
    }

    public long getActiveRegistrationsCount(Event event) {
        return registrationRepo.countByEventAndStatusIn(event, Arrays.asList(RegistrationStatus.CONFIRMED));
    }
}
