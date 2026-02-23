package ru.cpk.system.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.cpk.system.model.Certificate;
import ru.cpk.system.model.CertificateStatus;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
    Optional<Certificate> findByApplicationId(Long applicationId);

    List<Certificate> findByApplicationStudentIdOrderByIssueDateDesc(Long studentId);

    List<Certificate> findByStatusAndIssueDateBetweenOrderByIssueDateDesc(
        CertificateStatus status,
        LocalDate dateFrom,
        LocalDate dateTo
    );

    long countByStatusAndIssueDateBetween(
        CertificateStatus status,
        LocalDate dateFrom,
        LocalDate dateTo
    );
}
