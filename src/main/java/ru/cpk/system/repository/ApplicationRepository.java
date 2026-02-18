package ru.cpk.system.repository;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.cpk.system.model.Application;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByListenerFullNameContainingIgnoreCase(String listenerFullName, Sort sort);
}
