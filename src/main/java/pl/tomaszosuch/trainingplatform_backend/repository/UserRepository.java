package pl.tomaszosuch.trainingplatform_backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import pl.tomaszosuch.trainingplatform_backend.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

}
