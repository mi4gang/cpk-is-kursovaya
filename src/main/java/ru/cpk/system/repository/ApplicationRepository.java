package ru.cpk.system.repository;

import java.util.List;
import java.util.Optional;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.DocumentStatus;

public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findByListenerFullNameContainingIgnoreCase(String listenerFullName, Sort sort);

    List<Application> findByStudentIdOrderByApplicationDateDesc(Long studentId);

    List<Application> findByAssignedTeacherIdOrderByApplicationDateDesc(Long teacherId);

    long countByDocStatus(DocumentStatus docStatus);

    long countByDocStatusAndAccessStatus(DocumentStatus docStatus, AccessStatus accessStatus);

    long countByDocStatusAndAccessStatusNot(DocumentStatus docStatus, AccessStatus accessStatus);

    long countByAccessStatus(AccessStatus accessStatus);

    long countByAccessStatusAndStatusNot(AccessStatus accessStatus, ApplicationStatus status);

    @Query("""
        select count(a) from Application a
        join a.assessmentResult ar
        where ar.status = ru.cpk.system.model.AssessmentStatus.PASSED
          and a.teacherCompleted = true
          and a.certificate is null
        """)
    long countReadyForCertificate();

    @Query("""
        select count(a) from Application a
        where a.assignedTeacher.id = :teacherId
          and a.teacherCompleted = false
        """)
    long countCurrentStudentsForTeacher(Long teacherId);

    @Query("""
        select count(distinct a.program.id) from Application a
        where a.assignedTeacher.id = :teacherId
          and a.teacherCompleted = false
        """)
    long countCurrentProgramsForTeacher(Long teacherId);

    Optional<Application> findByIdAndStudentId(Long id, Long studentId);

    Optional<Application> findByIdAndAssignedTeacherId(Long id, Long teacherId);

    List<Application> findByDocStatusOrderByApplicationDateAsc(DocumentStatus docStatus);

    List<Application> findByDocStatusAndAccessStatusOrderByApplicationDateAsc(DocumentStatus docStatus, AccessStatus accessStatus);

    List<Application> findByDocStatusAndAccessStatusNotOrderByApplicationDateAsc(DocumentStatus docStatus, AccessStatus accessStatus);

    List<Application> findByAccessStatusAndStatusNotOrderByApplicationDateAsc(AccessStatus accessStatus, ApplicationStatus status);

    List<Application> findByAccessStatusOrderByApplicationDateAsc(AccessStatus accessStatus);

    @Query("""
        select a from Application a
        join a.assessmentResult ar
        where ar.status = ru.cpk.system.model.AssessmentStatus.PASSED
          and a.teacherCompleted = true
          and a.certificate is null
        order by a.completedAt asc
        """)
    List<Application> findReadyForCertificate();

    @Query("""
        select a from Application a
        where a.assignedTeacher is not null
        order by a.applicationDate desc
        """)
    List<Application> findAssignedApplications();

    List<Application> findByStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
        ApplicationStatus status,
        LocalDate completedAtStart,
        LocalDate completedAtEnd
    );

    long countByStatusAndCompletedAtBetween(
        ApplicationStatus status,
        LocalDate completedAtStart,
        LocalDate completedAtEnd
    );

    long countByStatusAndCompletedAtBetweenAndCertificateIsNotNull(
        ApplicationStatus status,
        LocalDate completedAtStart,
        LocalDate completedAtEnd
    );

    long countByStatusAndCompletedAtBetweenAndCertificateIsNull(
        ApplicationStatus status,
        LocalDate completedAtStart,
        LocalDate completedAtEnd
    );

    @Query("""
        select a from Application a
        where a.payment is null
        order by a.id asc
        """)
    List<Application> findWithoutPayment();

    @Query("""
        select count(a) from Application a
        where a.docStatus = ru.cpk.system.model.DocumentStatus.APPROVED
          and (a.payment is null or a.payment.status <> ru.cpk.system.model.PaymentStatus.PAID)
        """)
    long countApprovedDocsWithoutPaidPayment();

    @Query("""
        select a from Application a
        where a.docStatus = ru.cpk.system.model.DocumentStatus.APPROVED
          and (a.payment is null or a.payment.status <> ru.cpk.system.model.PaymentStatus.PAID)
        order by a.applicationDate asc
        """)
    List<Application> findApprovedDocsWithoutPaidPayment();

    List<Application> findByAssignedTeacherIdAndAssessmentResultIsNullOrderByApplicationDateDesc(Long teacherId);
}
