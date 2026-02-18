package ru.cpk.system.repository;

import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.cpk.system.model.Program;

public interface ProgramRepository extends JpaRepository<Program, Long> {
    List<Program> findByTitleContainingIgnoreCase(String title, Sort sort);

    long countByActiveTrue();
}
