package ru.cpk.system.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.cpk.system.model.Certificate;

public interface CertificateRepository extends JpaRepository<Certificate, Long> {
}
