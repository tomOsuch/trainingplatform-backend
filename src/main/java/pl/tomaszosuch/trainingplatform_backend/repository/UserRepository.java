package pl.tomaszosuch.trainingplatform_backend.repository;

import java.util.Collection;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pl.tomaszosuch.trainingplatform_backend.entity.User;
import pl.tomaszosuch.trainingplatform_backend.enums.Role;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    @Query("""
            SELECT u FROM User u
            WHERE (lower(u.lastName) LIKE :pattern ESCAPE '!'
                   OR lower(u.email) LIKE :pattern ESCAPE '!')
            AND u.isActive IN :activeStates
            """)
    Page<User> findForAdmin(@Param("pattern") String pattern,
                            @Param("activeStates") Collection<Boolean> activeStates,
                            Pageable pageable);

    @Query("SELECT COUNT(u) FROM User u WHERE u.role = :role AND u.isActive = true")
    long countActiveByRole(@Param("role") Role role);

}
