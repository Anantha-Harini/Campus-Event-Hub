package com.college.eventreg.service;

import com.college.eventreg.model.Event;
import com.college.eventreg.model.EventRegistration;
import com.college.eventreg.model.RegistrationStatus;
import com.college.eventreg.model.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExportServiceTest {

    private final ExportService exportService = new ExportService();

    private EventRegistration createMockRegistration() {
        User student = new User();
        student.setName("John Doe");
        student.setUsername("john@college.edu");
        student.setRegisterNumber("2024001");
        student.setDepartment("Computer Science");

        Event event = new Event();
        event.setName("Tech Summit 2025");
        event.setVenue("Main Auditorium");
        event.setEventDate(LocalDateTime.of(2025, 11, 15, 10, 0));

        EventRegistration reg = new EventRegistration();
        reg.setId(1L);
        reg.setStudent(student);
        reg.setEvent(event);
        reg.setRegistrationDate(LocalDateTime.now());
        reg.setStatus(RegistrationStatus.CONFIRMED);
        return reg;
    }

    @Test
    @DisplayName("Should generate a non-empty PDF event pass")
    void testGenerateEventPass() {
        EventRegistration reg = createMockRegistration();
        ByteArrayInputStream pdf = exportService.generateEventPass(reg);

        assertNotNull(pdf);
        byte[] pdfBytes = pdf.readAllBytes();
        assertTrue(pdfBytes.length > 0, "PDF should not be empty");
        // PDF files always start with %PDF
        String header = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", header, "Generated file should be a valid PDF");
    }

    @Test
    @DisplayName("Should generate CSV export with correct headers")
    void testExportCsv() {
        EventRegistration reg = createMockRegistration();
        ByteArrayInputStream csv = exportService.exportToCsv(java.util.List.of(reg));

        String content = new String(csv.readAllBytes());
        assertTrue(content.contains("Registration ID"), "CSV should contain headers");
        assertTrue(content.contains("John Doe"), "CSV should contain student name");
        assertTrue(content.contains("Tech Summit 2025"), "CSV should contain event name");
    }

    @Test
    @DisplayName("Should generate PDF export report with content")
    void testExportPdf() {
        EventRegistration reg = createMockRegistration();
        ByteArrayInputStream pdf = exportService.exportToPdf(java.util.List.of(reg));

        byte[] pdfBytes = pdf.readAllBytes();
        assertTrue(pdfBytes.length > 0);
        String header = new String(pdfBytes, 0, 4);
        assertEquals("%PDF", header);
    }
}
