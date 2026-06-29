package com.college.eventreg.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.college.eventreg.model.Event;
import java.time.LocalDateTime;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {
    List<Event> findByEventDateAfter(LocalDateTime dateTime);
}
