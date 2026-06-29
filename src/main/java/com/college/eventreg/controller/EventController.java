package com.college.eventreg.controller;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.college.eventreg.model.Event;
import com.college.eventreg.model.EventRegistration;
import com.college.eventreg.model.RegistrationStatus;
import com.college.eventreg.model.User;
import com.college.eventreg.repository.EventRegistrationRepository;
import com.college.eventreg.repository.EventRepository;
import com.college.eventreg.repository.UserRepository;
import com.college.eventreg.service.ExportService;
import com.college.eventreg.service.RegistrationService;

import jakarta.servlet.http.HttpServletResponse;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class EventController {

    private final UserRepository userRepo;
    private final EventRepository eventRepo;
    private final EventRegistrationRepository registrationRepo;
    private final RegistrationService registrationService;
    private final ExportService exportService;
    private final PasswordEncoder encoder;

    public EventController(UserRepository userRepo, EventRepository eventRepo,
                           EventRegistrationRepository registrationRepo, RegistrationService registrationService,
                           ExportService exportService, PasswordEncoder encoder) {
        this.userRepo = userRepo;
        this.eventRepo = eventRepo;
        this.registrationRepo = registrationRepo;
        this.registrationService = registrationService;
        this.exportService = exportService;
        this.encoder = encoder;
    }

    private User getAuthenticatedUser(Principal principal) {
        if (principal == null) return null;
        return userRepo.findByUsername(principal.getName()).orElse(null);
    }

    @GetMapping("/")
    public String index(Principal principal) {
        if (principal != null) {
            User user = getAuthenticatedUser(principal);
            if (user != null) {
                if ("ROLE_ADMIN".equals(user.getRole())) {
                    return "redirect:/admin/home";
                } else {
                    return "redirect:/user/home";
                }
            }
        }
        return "index";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("user", new User());
        return "signup";
    }

    @PostMapping("/signup")
    public String signupSubmit(@ModelAttribute User user, Model model) {
        if (userRepo.existsByUsername(user.getUsername())) {
            model.addAttribute("errorMessage", "Email/Username already registered!");
            return "signup";
        }
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("ROLE_USER"); // default sign up role is Student/User
        userRepo.save(user);
        return "redirect:/login?registered=true";
    }

    @GetMapping("/user/home")
    public String userHome(Principal principal, Model model) {
        User student = getAuthenticatedUser(principal);
        if (student == null) return "redirect:/login";

        List<Event> events = eventRepo.findAll();
        List<EventRegistration> registrations = registrationRepo.findByStudent(student);

        model.addAttribute("student", student);
        model.addAttribute("events", events);
        model.addAttribute("registrations", registrations);
        
        List<Long> registeredEventIds = registrations.stream()
                .filter(r -> r.getStatus() != RegistrationStatus.CANCELLED)
                .map(r -> r.getEvent().getId())
                .toList();
        model.addAttribute("registeredEventIds", registeredEventIds);
        model.addAttribute("regService", registrationService); // exposes capacity checks directly

        return "user_home";
    }

    @PostMapping("/user/register/{eventId}")
    public String registerForEvent(@PathVariable Long eventId, Principal principal, RedirectAttributes redirectAttrs) {
        User student = getAuthenticatedUser(principal);
        if (student == null) return "redirect:/login";

        Event event = eventRepo.findById(eventId)
                .orElseThrow(() -> new IllegalArgumentException("Event not found"));

        try {
            registrationService.registerStudentToEvent(student, event);
            redirectAttrs.addFlashAttribute("successMessage", "Successfully registered for " + event.getName() + "! Confirmation email sent.");
        } catch (IllegalStateException e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/home";
    }

    @PostMapping("/user/cancel/{regId}")
    public String cancelRegistration(@PathVariable Long regId, Principal principal, RedirectAttributes redirectAttrs) {
        User student = getAuthenticatedUser(principal);
        if (student == null) return "redirect:/login";

        try {
            registrationService.cancelRegistration(regId, student);
            redirectAttrs.addFlashAttribute("successMessage", "Registration cancelled successfully.");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }

        return "redirect:/user/home";
    }

    @GetMapping("/admin/home")
    public String adminHome(Model model) {
        long totalEvents = eventRepo.count();
        long totalRegistrations = registrationRepo.count();
        long upcomingEvents = eventRepo.findAll().stream()
                .filter(e -> e.getEventDate().isAfter(LocalDateTime.now()))
                .count();
        long participatingDepts = registrationRepo.countDistinctStudentDepartments();

        model.addAttribute("totalEvents", totalEvents);
        model.addAttribute("totalRegistrations", totalRegistrations);
        model.addAttribute("upcomingEvents", upcomingEvents);
        model.addAttribute("participatingDepartments", participatingDepts);

        return "admin_home";
    }

    @GetMapping("/admin/registrations")
    public String viewRegistrations(@RequestParam(required = false) String eventName,
                                    @RequestParam(required = false) String searchName,
                                    @RequestParam(required = false) String registerNumber,
                                    @RequestParam(required = false) String department,
                                    Model model) {
        List<EventRegistration> registrations = registrationRepo.searchRegistrations(eventName, searchName, registerNumber, department);
        List<Event> events = eventRepo.findAll();

        model.addAttribute("registrations", registrations);
        model.addAttribute("events", events);
        
        // Retain inputs in search panel
        model.addAttribute("selectedEventName", eventName);
        model.addAttribute("searchName", searchName);
        model.addAttribute("registerNumber", registerNumber);
        model.addAttribute("department", department);

        return "admin_registrations";
    }

    @PostMapping("/admin/registrations/status/{regId}")
    public String updateStatus(@PathVariable Long regId, @RequestParam RegistrationStatus status, RedirectAttributes redirectAttrs) {
        try {
            registrationService.updateRegistrationStatus(regId, status);
            redirectAttrs.addFlashAttribute("successMessage", "Registration status updated to " + status + ".");
        } catch (Exception e) {
            redirectAttrs.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/admin/registrations";
    }

    @GetMapping("/admin/export/csv")
    public void exportCsv(@RequestParam(required = false) String eventName,
                          @RequestParam(required = false) String searchName,
                          @RequestParam(required = false) String registerNumber,
                          @RequestParam(required = false) String department,
                          HttpServletResponse response) throws IOException {
        List<EventRegistration> list = registrationRepo.searchRegistrations(eventName, searchName, registerNumber, department);
        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=registrations.csv");

        org.springframework.util.StreamUtils.copy(exportService.exportToCsv(list), response.getOutputStream());
    }

    @GetMapping("/admin/export/excel")
    public void exportExcel(@RequestParam(required = false) String eventName,
                            @RequestParam(required = false) String searchName,
                            @RequestParam(required = false) String registerNumber,
                            @RequestParam(required = false) String department,
                            HttpServletResponse response) throws IOException {
        List<EventRegistration> list = registrationRepo.searchRegistrations(eventName, searchName, registerNumber, department);
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename=registrations.xlsx");

        org.springframework.util.StreamUtils.copy(exportService.exportToExcel(list), response.getOutputStream());
    }

    @GetMapping("/admin/export/pdf")
    public void exportPdf(@RequestParam(required = false) String eventName,
                          @RequestParam(required = false) String searchName,
                          @RequestParam(required = false) String registerNumber,
                          @RequestParam(required = false) String department,
                          HttpServletResponse response) throws IOException {
        List<EventRegistration> list = registrationRepo.searchRegistrations(eventName, searchName, registerNumber, department);
        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=registrations.pdf");

        org.springframework.util.StreamUtils.copy(exportService.exportToPdf(list), response.getOutputStream());
    }

    @GetMapping("/schedule")
    public String schedule(Model model) {
        List<Event> events = eventRepo.findAll();
        model.addAttribute("events", events);
        return "schedule";
    }

    @GetMapping("/admin/events/new")
    public String showAddEventForm(Model model) {
        model.addAttribute("event", new Event());
        return "add_event";
    }

    @PostMapping("/admin/events/new")
    public String submitAddEventForm(@ModelAttribute Event event, RedirectAttributes redirectAttrs) {
        if (event.getBannerUrl() == null || event.getBannerUrl().trim().isEmpty()) {
            event.setBannerUrl("https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&auto=format&fit=crop");
        }
        eventRepo.save(event);
        redirectAttrs.addFlashAttribute("successMessage", "Event '" + event.getName() + "' created successfully!");
        return "redirect:/admin/home";
    }

    @GetMapping("/admin/events/edit/{id}")
    public String showEditEventForm(@PathVariable Long id, Model model) {
        Event event = eventRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid event Id:" + id));
        model.addAttribute("event", event);
        return "edit_event";
    }

    @PostMapping("/admin/events/edit/{id}")
    public String submitEditEventForm(@PathVariable Long id, @ModelAttribute Event event, RedirectAttributes redirectAttrs) {
        event.setId(id);
        if (event.getBannerUrl() == null || event.getBannerUrl().trim().isEmpty()) {
            event.setBannerUrl("https://images.unsplash.com/photo-1540575467063-178a50c2df87?w=800&auto=format&fit=crop");
        }
        eventRepo.save(event);
        redirectAttrs.addFlashAttribute("successMessage", "Event '" + event.getName() + "' updated successfully!");
        return "redirect:/schedule";
    }

    @PostMapping("/admin/events/delete/{id}")
    public String deleteEvent(@PathVariable Long id, RedirectAttributes redirectAttrs) {
        Event event = eventRepo.findById(id).orElseThrow(() -> new IllegalArgumentException("Invalid event Id:" + id));
        registrationRepo.deleteByEvent(event);
        eventRepo.delete(event);
        redirectAttrs.addFlashAttribute("successMessage", "Event '" + event.getName() + "' deleted successfully!");
        return "redirect:/schedule";
    }

    @GetMapping("/user/registrations/{regId}/pass")
    public void downloadEventPass(@PathVariable Long regId, Principal principal, HttpServletResponse response) throws IOException {
        EventRegistration registration = registrationRepo.findById(regId)
                .orElseThrow(() -> new IllegalArgumentException("Registration not found with ID: " + regId));
        
        // Security check: Student can only download their own pass
        User student = userRepo.findByUsername(principal.getName())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        boolean isAdmin = student.getRole().equals("ROLE_ADMIN");
        if (!isAdmin && !registration.getStudent().getId().equals(student.getId())) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Unauthorized to access this entry pass.");
            return;
        }

        response.setContentType("application/pdf");
        response.setHeader("Content-Disposition", "attachment; filename=EventPass_" + regId + ".pdf");

        ByteArrayInputStream passStream = exportService.generateEventPass(registration);
        org.springframework.util.StreamUtils.copy(passStream, response.getOutputStream());
    }
}
