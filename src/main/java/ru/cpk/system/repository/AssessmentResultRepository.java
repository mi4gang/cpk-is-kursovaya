package ru.cpk.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.cpk.system.model.AssessmentResult;

public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, Long> {
}
