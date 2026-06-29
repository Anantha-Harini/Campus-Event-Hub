package com.college.eventreg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.college.eventreg.model.Event;
import com.college.eventreg.model.EventRegistration;
import com.college.eventreg.model.RegistrationStatus;
import com.college.eventreg.model.User;

import java.util.Collection;
import java.util.List;

public interface EventRegistrationRepository extends JpaRepository<EventRegistration, Long> {
    
    List<EventRegistration> findByStudent(User student);

    boolean existsByStudentAndEvent(User student, Event event);

    boolean existsByStudentAndEventAndStatusNot(User student, Event event, RegistrationStatus status);

    long countByEventAndStatusIn(Event event, Collection<RegistrationStatus> statuses);

    @Query("SELECT r FROM EventRegistration r WHERE " +
           "(:eventName IS NULL OR :eventName = '' OR r.event.name = :eventName) AND " +
           "(:searchName IS NULL OR :searchName = '' OR LOWER(r.student.name) LIKE LOWER(CONCAT('%', :searchName, '%'))) AND " +
           "(:registerNumber IS NULL OR :registerNumber = '' OR r.student.registerNumber = :registerNumber) AND " +
           "(:department IS NULL OR :department = '' OR LOWER(r.student.department) = LOWER(:department))")
    List<EventRegistration> searchRegistrations(
            @Param("eventName") String eventName,
            @Param("searchName") String searchName,
            @Param("registerNumber") String registerNumber,
            @Param("department") String department
    );

    @Query("SELECT COUNT(DISTINCT r.student.department) FROM EventRegistration r")
    long countDistinctStudentDepartments();

    @org.springframework.transaction.annotation.Transactional
    void deleteByEvent(Event event);
}
