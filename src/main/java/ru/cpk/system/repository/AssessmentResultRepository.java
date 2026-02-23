package ru.cpk.system.repository;

import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.cpk.system.model.AssessmentResult;

public interface AssessmentResultRepository extends JpaRepository<AssessmentResult, Long> {
    Optional<AssessmentResult> findByApplicationId(Long applicationId);

    List<AssessmentResult> findByApplicationAssignedTeacherIdOrderByResultDateDesc(Long teacherId);
}
