package ru.cpk.system.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.cpk.system.model.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}
