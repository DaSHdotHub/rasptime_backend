package io.github.dashdothub.rasptime_backend.repository;

import io.github.dashdothub.rasptime_backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByRfidTag(String rfidTag);

    Optional<User> findByRfidTagAndActiveTrue(String rfidTag);

    List<User> findAllByActiveTrue();

    List<User> findAllByClockedInTrue();

    boolean existsByRfidTag(String rfidTag);
}