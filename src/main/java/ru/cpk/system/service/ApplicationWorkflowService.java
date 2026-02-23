package ru.cpk.system.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.AssessmentStatus;
import ru.cpk.system.model.DocumentStatus;
import ru.cpk.system.model.Payment;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.AssessmentResultRepository;
import ru.cpk.system.repository.PaymentRepository;

@Service
public class ApplicationWorkflowService {

    public static final int PASS_THRESHOLD = 75;
    private static final int TRIAL_DAYS = 3;

    private final ApplicationRepository applicationRepository;
    private final PaymentRepository paymentRepository;
    private final AssessmentResultRepository assessmentResultRepository;

    public ApplicationWorkflowService(ApplicationRepository applicationRepository,
                                      PaymentRepository paymentRepository,
                                      AssessmentResultRepository assessmentResultRepository) {
        this.applicationRepository = applicationRepository;
        this.paymentRepository = paymentRepository;
        this.assessmentResultRepository = assessmentResultRepository;
    }

    public void initializeStudentApplication(Application application, User student) {
        application.setStudent(student);
        if (application.getApplicationDate() == null) {
            application.setApplicationDate(LocalDate.now());
        }
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setDocStatus(DocumentStatus.PENDING);
        application.setAccessStatus(AccessStatus.NO_ACCESS);
        application.setTrialEndsAt(null);
        application.setAssignedTeacher(null);
        application.setProgressPercent(0);
        application.setTeacherCompleted(false);
        application.setCompletedAt(null);
    }

    public void validateAssignedTeacher(User teacher) {
        if (teacher != null && teacher.getRole() != RoleName.TEACHER) {
            throw new IllegalArgumentException("Назначить можно только пользователя с ролью преподавателя");
        }
    }

    public Application assignTeacherOnly(Application current, User teacher) {
        validateAssignedTeacher(teacher);
        current.setAssignedTeacher(teacher);
        if (current.getDocStatus() == DocumentStatus.REJECTED) {
            current.setAssignedTeacher(null);
            return applicationRepository.save(current);
        }

        if (isPaymentPaid(current)
            && current.getAccessStatus() == AccessStatus.TRIAL_ACCESS
            && current.getAssignedTeacher() != null) {
            current.setAccessStatus(AccessStatus.FULL_ACCESS);
            current.setTrialEndsAt(null);
            if (current.getStatus() != ApplicationStatus.COMPLETED) {
                current.setStatus(ApplicationStatus.PAID);
            }
        }
        return applicationRepository.save(current);
    }

    public Application openTrialAccess(Application current) {
        if (current.getDocStatus() != DocumentStatus.APPROVED) {
            throw new IllegalArgumentException("Пробный доступ возможен только после подтверждения документов");
        }
        if (isPaymentPaid(current)) {
            throw new IllegalArgumentException("После полной оплаты пробный доступ не требуется");
        }
        current.setAccessStatus(AccessStatus.TRIAL_ACCESS);
        current.setStatus(ApplicationStatus.TRIAL_ACCESS);
        current.setTrialEndsAt(LocalDate.now().plusDays(TRIAL_DAYS));
        return applicationRepository.save(current);
    }

    public Application openFullAccess(Application current) {
        if (current.getDocStatus() != DocumentStatus.APPROVED) {
            throw new IllegalArgumentException("Полный доступ возможен только после подтверждения документов");
        }
        if (!isPaymentPaid(current)) {
            throw new IllegalArgumentException("Полный доступ можно открыть только после оплаты");
        }
        if (current.getAssignedTeacher() == null) {
            throw new IllegalArgumentException("Перед открытием полного доступа назначьте преподавателя");
        }
        current.setAccessStatus(AccessStatus.FULL_ACCESS);
        current.setTrialEndsAt(null);
        if (current.getStatus() != ApplicationStatus.COMPLETED) {
            current.setStatus(ApplicationStatus.PAID);
        }
        return applicationRepository.save(current);
    }

    public Application updateByMethodist(Application current, Application incoming) {
        if (incoming.getDocStatus() != null) {
            current.setDocStatus(incoming.getDocStatus());
        }
        current.setAssignedTeacher(incoming.getAssignedTeacher());
        validateAssignedTeacher(current.getAssignedTeacher());

        if (current.getDocStatus() == DocumentStatus.REJECTED) {
            current.setStatus(ApplicationStatus.REJECTED);
            current.setAccessStatus(AccessStatus.NO_ACCESS);
            current.setTrialEndsAt(null);
            current.setAssignedTeacher(null);
            return applicationRepository.save(current);
        }

        if (current.getDocStatus() == DocumentStatus.APPROVED
            && current.getStatus() == ApplicationStatus.SUBMITTED) {
            current.setStatus(ApplicationStatus.DOCS_APPROVED);
        }

        if (incoming.getAccessStatus() != null && incoming.getAccessStatus() != current.getAccessStatus()) {
            if (incoming.getAccessStatus() == AccessStatus.TRIAL_ACCESS) {
                return openTrialAccess(current);
            }
            if (incoming.getAccessStatus() == AccessStatus.FULL_ACCESS) {
                return openFullAccess(current);
            }
            if (incoming.getAccessStatus() == AccessStatus.NO_ACCESS && !isPaymentPaid(current)) {
                current.setAccessStatus(AccessStatus.NO_ACCESS);
                current.setTrialEndsAt(null);
            }
        }

        return applicationRepository.save(current);
    }

    public Application syncAfterPayment(Payment payment) {
        Application application = payment.getApplication();
        if (payment.getStatus() == PaymentStatus.PAID && application.getDocStatus() == DocumentStatus.APPROVED) {
            if (application.getAccessStatus() == AccessStatus.TRIAL_ACCESS
                && application.getAssignedTeacher() != null) {
                application.setAccessStatus(AccessStatus.FULL_ACCESS);
                application.setTrialEndsAt(null);
            }
            if (application.getStatus() != ApplicationStatus.COMPLETED
                && application.getStatus() != ApplicationStatus.REJECTED) {
                application.setStatus(ApplicationStatus.PAID);
            }
        }
        return applicationRepository.save(application);
    }

    public Application applyAssessmentStatus(Application application, int score) {
        if (score < PASS_THRESHOLD) {
            application.setTeacherCompleted(false);
            application.setCompletedAt(null);
            if (application.getStatus() != ApplicationStatus.REJECTED) {
                application.setStatus(ApplicationStatus.IN_PROGRESS);
            }
            return applicationRepository.save(application);
        }

        if (application.isTeacherCompleted()) {
            application.setStatus(ApplicationStatus.COMPLETED);
            if (application.getCompletedAt() == null) {
                application.setCompletedAt(LocalDate.now());
            }
        } else if (application.getStatus() != ApplicationStatus.REJECTED) {
            application.setStatus(ApplicationStatus.IN_PROGRESS);
        }
        return applicationRepository.save(application);
    }

    public Application updateTeacherProgress(Application application, int progressPercent) {
        int normalizedProgress = Math.max(0, Math.min(100, progressPercent));
        application.setProgressPercent(normalizedProgress);
        if (normalizedProgress > 0
            && application.getStatus() != ApplicationStatus.COMPLETED
            && application.getStatus() != ApplicationStatus.REJECTED) {
            application.setStatus(ApplicationStatus.IN_PROGRESS);
        }
        return applicationRepository.save(application);
    }

    public Application markTeacherCompleted(Application application) {
        boolean passed = assessmentResultRepository.findByApplicationId(application.getId())
            .map(result -> result.getStatus() == AssessmentStatus.PASSED)
            .orElse(false);
        if (!passed) {
            throw new IllegalArgumentException("Завершение возможно только после успешной аттестации (>= 75 баллов)");
        }

        application.setTeacherCompleted(true);
        application.setCompletedAt(LocalDate.now());
        application.setStatus(ApplicationStatus.COMPLETED);
        if (application.getProgressPercent() < 100) {
            application.setProgressPercent(100);
        }
        return applicationRepository.save(application);
    }

    public Application applyTrialExpiration(Application application) {
        if (application.getAccessStatus() == AccessStatus.TRIAL_ACCESS
            && application.getTrialEndsAt() != null
            && application.getTrialEndsAt().isBefore(LocalDate.now())
            && !isPaymentPaid(application)) {
            application.setAccessStatus(AccessStatus.NO_ACCESS);
            application.setTrialEndsAt(null);
            if (application.getDocStatus() == DocumentStatus.APPROVED
                && application.getStatus() != ApplicationStatus.REJECTED
                && application.getStatus() != ApplicationStatus.COMPLETED) {
                application.setStatus(ApplicationStatus.DOCS_APPROVED);
            }
            return applicationRepository.save(application);
        }
        return application;
    }

    public boolean canIssueCertificate(Application application) {
        boolean passed = assessmentResultRepository.findByApplicationId(application.getId())
            .map(result -> result.getStatus() == AssessmentStatus.PASSED)
            .orElse(false);
        return passed && application.isTeacherCompleted();
    }

    private boolean isPaymentPaid(Application application) {
        if (application.getPayment() != null) {
            return application.getPayment().getStatus() == PaymentStatus.PAID;
        }
        return paymentRepository.findByApplicationId(application.getId())
            .map(payment -> payment.getStatus() == PaymentStatus.PAID)
            .orElse(false);
    }
}
