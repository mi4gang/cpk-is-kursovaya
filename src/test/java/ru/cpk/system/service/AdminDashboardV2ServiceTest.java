package ru.cpk.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.Certificate;
import ru.cpk.system.model.CertificateStatus;
import ru.cpk.system.model.DocumentStatus;
import ru.cpk.system.model.Payment;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.model.Program;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.CertificateRepository;

@ExtendWith(MockitoExtension.class)
class AdminDashboardV2ServiceTest {

    @Mock
    private ApplicationRepository applicationRepository;

    @Mock
    private CertificateRepository certificateRepository;

    @Mock
    private StatsService statsService;

    @InjectMocks
    private AdminDashboardV2Service service;

    @Test
    void buildDrilldownView_routesActionsToAdminCaseCard() {
        Application inTraining = application(
            101L,
            "Слушатель 1",
            4,
            DocumentStatus.APPROVED,
            AccessStatus.FULL_ACCESS,
            ApplicationStatus.IN_PROGRESS,
            11L,
            "Ильин Павел Сергеевич",
            PaymentStatus.PAID
        );

        when(applicationRepository.findAll()).thenReturn(List.of(inTraining));

        AdminDashboardV2Service.DrilldownView view = service.buildDrilldownView(
            AdminDashboardV2Service.QUEUE_IN_TRAINING,
            null,
            false,
            null,
            null,
            null
        );

        assertThat(view.rows()).hasSize(1);
        assertThat(view.rows().get(0).nextActionUrl())
            .isEqualTo("/admin/dashboard-v2/case/101");
    }

    @Test
    void buildDrilldownView_classifiesTrialAccessSeparatelyFromApprovedUnpaid() {
        Application trialUnpaid = application(
            201L,
            "Слушатель 2",
            3,
            DocumentStatus.APPROVED,
            AccessStatus.TRIAL_ACCESS,
            ApplicationStatus.TRIAL_ACCESS,
            12L,
            "Соколова Мария Андреевна",
            null
        );

        when(applicationRepository.findAll()).thenReturn(List.of(trialUnpaid));

        AdminDashboardV2Service.DrilldownView view = service.buildDrilldownView(
            AdminDashboardV2Service.QUEUE_TRIAL_ACCESS,
            null,
            false,
            0,
            null,
            "priority_desc"
        );

        assertThat(view.rows()).hasSize(1);
        assertThat(view.rows().get(0).queueType()).isEqualTo(AdminDashboardV2Service.QUEUE_TRIAL_ACCESS);
    }

    @Test
    void buildDrilldownView_appliesPriorityOnlyFilterForCriticalAndHigh() {
        Application docsOld = application(
            301L,
            "Слушатель 3",
            7,
            DocumentStatus.PENDING,
            AccessStatus.NO_ACCESS,
            ApplicationStatus.SUBMITTED,
            null,
            null,
            null
        );
        Application approvedUnpaid = application(
            302L,
            "Слушатель 4",
            1,
            DocumentStatus.APPROVED,
            AccessStatus.FULL_ACCESS,
            ApplicationStatus.IN_PROGRESS,
            null,
            null,
            null
        );

        when(applicationRepository.findAll()).thenReturn(List.of(approvedUnpaid, docsOld));

        AdminDashboardV2Service.DrilldownView view = service.buildDrilldownView(
            AdminDashboardV2Service.QUEUE_ALL,
            null,
            true,
            null,
            null,
            "age_desc"
        );

        assertThat(view.rows()).hasSize(2);
        assertThat(view.rows())
            .extracting(AdminDashboardV2Service.DrilldownRow::applicationId)
            .containsExactly(301L, 302L);
        assertThat(view.rows())
            .extracting(AdminDashboardV2Service.DrilldownRow::priorityLevelKey)
            .containsExactly("critical", "high");
    }

    @Test
    void buildCompletedDrilldownView_calculatesTotalsByPeriod() {
        Application completedWithCertificate = application(
            401L,
            "Слушатель 5",
            2,
            DocumentStatus.APPROVED,
            AccessStatus.FULL_ACCESS,
            ApplicationStatus.COMPLETED,
            14L,
            "Егоров Дмитрий Олегович",
            PaymentStatus.PAID
        );
        completedWithCertificate.setCompletedAt(LocalDate.now().minusDays(1));
        completedWithCertificate.setTeacherCompleted(true);

        Certificate certificate = new Certificate();
        certificate.setStatus(CertificateStatus.ISSUED);
        certificate.setNumber("CERT-001");
        certificate.setIssueDate(LocalDate.now());
        certificate.setApplication(completedWithCertificate);
        completedWithCertificate.setCertificate(certificate);

        Application completedWithoutCertificate = application(
            402L,
            "Слушатель 6",
            3,
            DocumentStatus.APPROVED,
            AccessStatus.FULL_ACCESS,
            ApplicationStatus.COMPLETED,
            15L,
            "Ильин Павел Сергеевич",
            PaymentStatus.PAID
        );
        completedWithoutCertificate.setCompletedAt(LocalDate.now().minusDays(1));
        completedWithoutCertificate.setTeacherCompleted(true);

        when(certificateRepository.findByStatusAndIssueDateBetweenOrderByIssueDateDesc(
            CertificateStatus.ISSUED,
            LocalDate.now().minusDays(7),
            LocalDate.now()
        )).thenReturn(List.of(certificate));
        when(applicationRepository.findByStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
            ApplicationStatus.COMPLETED,
            LocalDate.now().minusDays(7),
            LocalDate.now()
        )).thenReturn(List.of(completedWithCertificate, completedWithoutCertificate));

        AdminDashboardV2Service.CompletedDrilldownView view = service.buildCompletedDrilldownView(
            LocalDate.now().minusDays(7),
            LocalDate.now(),
            "all"
        );

        assertThat(view.totalCount()).isEqualTo(2);
        assertThat(view.issuedCount()).isEqualTo(1);
        assertThat(view.notIssuedCount()).isEqualTo(1);
        assertThat(view.paidAmount()).isEqualByComparingTo(BigDecimal.valueOf(2000));
    }

    private Application application(Long id,
                                    String listener,
                                    int ageDays,
                                    DocumentStatus docStatus,
                                    AccessStatus accessStatus,
                                    ApplicationStatus status,
                                    Long teacherId,
                                    String teacherName,
                                    PaymentStatus paymentStatus) {
        Application application = new Application();
        application.setId(id);
        application.setListenerFullName(listener);
        application.setApplicationDate(LocalDate.now().minusDays(ageDays));
        application.setDocStatus(docStatus);
        application.setAccessStatus(accessStatus);
        application.setStatus(status);
        application.setProgressPercent(40);

        Program program = new Program();
        program.setId(id + 5000);
        program.setTitle("Программа " + id);
        application.setProgram(program);

        if (teacherId != null) {
            User teacher = new User();
            teacher.setId(teacherId);
            teacher.setFullName(teacherName);
            application.setAssignedTeacher(teacher);
        }

        if (paymentStatus != null) {
            Payment payment = new Payment();
            payment.setStatus(paymentStatus);
            payment.setAmount(BigDecimal.valueOf(1000));
            payment.setApplication(application);
            application.setPayment(payment);
        }

        return application;
    }
}
