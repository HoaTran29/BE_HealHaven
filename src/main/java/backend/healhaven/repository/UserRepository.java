package backend.healhaven.repository;

import backend.healhaven.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Integer> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    org.springframework.data.domain.Page<User> findByRole(backend.healhaven.enums.UserRole role,
            org.springframework.data.domain.Pageable pageable);

    java.util.List<User> findByRole(backend.healhaven.enums.UserRole role);
}
