package io.github.dashdothub.rasptime_backend.repository;


import io.github.dashdothub.rasptime_backend.entity.TimeEntry;
import io.github.dashdothub.rasptime_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface TimeEntryRepository extends JpaRepository<TimeEntry, Long> {

    List<TimeEntry> findByUserAndWorkDate(User user, LocalDate workDate);

    List<TimeEntry> findByUserAndWorkDateBetween(User user, LocalDate start, LocalDate end);

    Optional<TimeEntry> findByUserAndPunchOutIsNull(User user);

    List<TimeEntry> findAllByPunchOutIsNull();

    List<TimeEntry> findByWorkDate(LocalDate workDate);
}