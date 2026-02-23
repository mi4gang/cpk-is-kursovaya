package ru.cpk.system.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.AssessmentResult;
import ru.cpk.system.model.AssessmentStatus;
import ru.cpk.system.model.Certificate;
import ru.cpk.system.model.CertificateStatus;
import ru.cpk.system.model.DocumentStatus;
import ru.cpk.system.model.Payment;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.model.Program;
import ru.cpk.system.model.ProgramType;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.AssessmentResultRepository;
import ru.cpk.system.repository.CertificateRepository;
import ru.cpk.system.repository.PaymentRepository;
import ru.cpk.system.repository.ProgramRepository;
import ru.cpk.system.repository.UserRepository;

@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final ApplicationRepository applicationRepository;
    private final PaymentRepository paymentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final CertificateRepository certificateRepository;
    private final PasswordEncoder passwordEncoder;
    private final String seedMode;

    public DataInitializer(UserRepository userRepository,
                           ProgramRepository programRepository,
                           ApplicationRepository applicationRepository,
                           PaymentRepository paymentRepository,
                           AssessmentResultRepository assessmentResultRepository,
                           CertificateRepository certificateRepository,
                           PasswordEncoder passwordEncoder,
                           @Value("${app.seed.mode:once}") String seedMode) {
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.applicationRepository = applicationRepository;
        this.paymentRepository = paymentRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.certificateRepository = certificateRepository;
        this.passwordEncoder = passwordEncoder;
        this.seedMode = seedMode;
    }

    @Override
    public void run(String... args) {
        SeedMode mode = SeedMode.from(seedMode);
        if (mode == SeedMode.OFF) {
            log.info("Seed disabled (app.seed.mode=off)");
            return;
        }

        if (mode == SeedMode.RESET) {
            resetDatabaseState();
        }

        // В reset-режиме поддерживаем полностью предсказуемый стенд для ручных прогонов.
        // В once-режиме только досоздаём отсутствующие demo-аккаунты без перезаписи существующих.
        boolean refreshDemoCredentials = mode == SeedMode.RESET;
        User admin = ensureUser("admin", "admin123", "Администратор системы", "admin@cpk.local", "+7-900-000-00-01", RoleName.ADMIN, refreshDemoCredentials);
        User methodist = ensureUser("methodist", "method123", "Методист центра", "methodist@cpk.local", "+7-900-000-00-02", RoleName.METHODIST, refreshDemoCredentials);
        User teacher = ensureUser("teacher", "teacher123", "Ильин Павел Сергеевич", "teacher@cpk.local", "+7-900-000-00-03", RoleName.TEACHER, refreshDemoCredentials);
        User teacher2 = ensureUser("teacher2", "teacher223", "Соколова Мария Андреевна", "teacher2@cpk.local", "+7-900-000-00-06", RoleName.TEACHER, refreshDemoCredentials);
        User teacher3 = ensureUser("teacher3", "teacher323", "Егоров Дмитрий Олегович", "teacher3@cpk.local", "+7-900-000-00-07", RoleName.TEACHER, refreshDemoCredentials);
        User student = ensureUser("student", "student123", "Смирнов Алексей Павлович", "student@cpk.local", "+7-900-000-00-04", RoleName.STUDENT, refreshDemoCredentials);

        Program p1 = ensureProgram(
            "Управление проектами в ИТ",
            "Программа повышения квалификации для руководителей ИТ-проектов.",
            ProgramType.ADVANCED_TRAINING,
            "Менеджмент",
            "Паспорт, диплом о высшем образовании.",
            72,
            7,
            45
        );

        Program p2 = ensureProgram(
            "Аналитика данных для бизнеса",
            "Курс по работе с BI-инструментами и данными.",
            ProgramType.ADVANCED_TRAINING,
            "Аналитика",
            "Паспорт, заявление, согласие на обработку данных.",
            48,
            10,
            35
        );

        Program p3 = ensureProgram(
            "Цифровая трансформация предприятия",
            "Программа профпереподготовки для управленческих команд.",
            ProgramType.PROFESSIONAL_RETRAINING,
            "Менеджмент",
            "Паспорт, диплом, резюме профессионального опыта.",
            256,
            14,
            120
        );

        if (mode == SeedMode.ONCE && applicationRepository.count() > 0) {
            log.info("Seed users/programs ensured, workflow seed skipped (app.seed.mode=once, applications already exist)");
            return;
        }

        Application docsPending = newApplication(student, p1, ApplicationStatus.SUBMITTED, DocumentStatus.PENDING, AccessStatus.NO_ACCESS);
        docsPending.setApplicationDate(LocalDate.now().minusDays(1));
        applicationRepository.save(docsPending);

        Application trialApp = newApplication(student, p2, ApplicationStatus.TRIAL_ACCESS, DocumentStatus.APPROVED, AccessStatus.TRIAL_ACCESS);
        trialApp.setApplicationDate(LocalDate.now().minusDays(4));
        trialApp.setTrialEndsAt(LocalDate.now().plusDays(2));
        trialApp.setAssignedTeacher(teacher2);
        applicationRepository.save(trialApp);

        Application paidInProgress = newApplication(student, p3, ApplicationStatus.IN_PROGRESS, DocumentStatus.APPROVED, AccessStatus.FULL_ACCESS);
        paidInProgress.setApplicationDate(LocalDate.now().minusDays(15));
        paidInProgress.setAssignedTeacher(teacher2);
        paidInProgress.setProgressPercent(60);
        paidInProgress = applicationRepository.save(paidInProgress);

        Payment payment = new Payment();
        payment.setApplication(paidInProgress);
        payment.setAmount(new BigDecimal("25000.00"));
        payment.setPaymentDate(LocalDate.now().minusDays(14));
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        AssessmentResult result = new AssessmentResult();
        result.setApplication(paidInProgress);
        result.setScore(88);
        result.setStatus(AssessmentStatus.PASSED);
        result.setResultDate(LocalDate.now().minusDays(2));
        result.setComment("Итоговая аттестация пройдена успешно");
        assessmentResultRepository.save(result);

        Application completedWithCertificate = newApplication(student, p1, ApplicationStatus.COMPLETED, DocumentStatus.APPROVED, AccessStatus.FULL_ACCESS);
        completedWithCertificate.setAssignedTeacher(teacher);
        completedWithCertificate.setProgressPercent(100);
        completedWithCertificate.setTeacherCompleted(true);
        completedWithCertificate.setCompletedAt(LocalDate.now().minusDays(1));
        completedWithCertificate = applicationRepository.save(completedWithCertificate);

        Payment payment2 = new Payment();
        payment2.setApplication(completedWithCertificate);
        payment2.setAmount(new BigDecimal("18000.00"));
        payment2.setPaymentDate(LocalDate.now().minusDays(5));
        payment2.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment2);

        AssessmentResult result2 = new AssessmentResult();
        result2.setApplication(completedWithCertificate);
        result2.setScore(90);
        result2.setStatus(AssessmentStatus.PASSED);
        result2.setResultDate(LocalDate.now().minusDays(1));
        result2.setComment("Обучение завершено");
        assessmentResultRepository.save(result2);

        Application completedReadyForCertificate = newApplication(student, p2, ApplicationStatus.COMPLETED, DocumentStatus.APPROVED, AccessStatus.FULL_ACCESS);
        completedReadyForCertificate.setAssignedTeacher(teacher3);
        completedReadyForCertificate.setProgressPercent(100);
        completedReadyForCertificate.setTeacherCompleted(true);
        completedReadyForCertificate.setCompletedAt(LocalDate.now());
        completedReadyForCertificate = applicationRepository.save(completedReadyForCertificate);

        Payment payment3 = new Payment();
        payment3.setApplication(completedReadyForCertificate);
        payment3.setAmount(new BigDecimal("22000.00"));
        payment3.setPaymentDate(LocalDate.now().minusDays(3));
        payment3.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment3);

        AssessmentResult result3 = new AssessmentResult();
        result3.setApplication(completedReadyForCertificate);
        result3.setScore(84);
        result3.setStatus(AssessmentStatus.PASSED);
        result3.setResultDate(LocalDate.now());
        result3.setComment("Готово к выдаче удостоверения");
        assessmentResultRepository.save(result3);

        Certificate certificate = new Certificate();
        certificate.setApplication(completedWithCertificate);
        certificate.setNumber("CPK-2025-0001");
        certificate.setIssueDate(LocalDate.now());
        certificate.setStatus(CertificateStatus.ISSUED);
        certificate.setDocumentLink("https://example.com/certificates/CPK-2025-0001.pdf");
        certificateRepository.save(certificate);

        // Suppress "unused" warnings for seed users that are part of role setup.
        if (admin == null || methodist == null || teacher2 == null || teacher3 == null) {
            throw new IllegalStateException("Не удалось создать обязательных пользователей");
        }

        log.info("Seed completed (app.seed.mode={})", mode.name().toLowerCase());
    }

    private Program ensureProgram(String title,
                                  String description,
                                  ProgramType type,
                                  String category,
                                  String requiredDocsNote,
                                  int durationHours,
                                  int startOffsetDays,
                                  int endOffsetDays) {
        return programRepository.findByTitleContainingIgnoreCase(title, Sort.by(Sort.Direction.ASC, "id")).stream()
            .filter(existing -> title.equalsIgnoreCase(existing.getTitle()))
            .findFirst()
            .orElseGet(() -> {
                Program program = new Program();
                program.setTitle(title);
                program.setDescription(description);
                program.setProgramType(type);
                program.setCategory(category);
                program.setRequiredDocsNote(requiredDocsNote);
                program.setDurationHours(durationHours);
                program.setStartDate(LocalDate.now().plusDays(startOffsetDays));
                program.setEndDate(LocalDate.now().plusDays(endOffsetDays));
                program.setActive(true);
                return programRepository.save(program);
            });
    }

    private Application newApplication(User student,
                                       Program program,
                                       ApplicationStatus status,
                                       DocumentStatus docStatus,
                                       AccessStatus accessStatus) {
        Application application = new Application();
        application.setStudent(student);
        application.setProgram(program);
        application.setListenerFullName(safeStudentName(student));
        application.setListenerEmail(safeStudentEmail(student));
        application.setListenerPhone(safeStudentPhone(student));
        application.setApplicationDate(LocalDate.now());
        application.setStatus(status);
        application.setDocStatus(docStatus);
        application.setAccessStatus(accessStatus);
        application.setProgressPercent(0);
        application.setTeacherCompleted(false);
        return application;
    }

    private User ensureUser(String username,
                            String password,
                            String fullName,
                            String email,
                            String phone,
                            RoleName roleName,
                            boolean overwriteExisting) {
        return userRepository.findByUsername(username)
            .map(existing -> {
                if (!overwriteExisting) {
                    return existing;
                }
                existing.setPassword(passwordEncoder.encode(password));
                existing.setFullName(fullName);
                existing.setEmail(email);
                existing.setPhone(phone);
                existing.setRole(roleName);
                existing.setEnabled(true);
                return userRepository.save(existing);
            })
            .orElseGet(() -> {
                User user = new User();
                user.setUsername(username);
                user.setPassword(passwordEncoder.encode(password));
                user.setFullName(fullName);
                user.setEmail(email);
                user.setPhone(phone);
                user.setRole(roleName);
                user.setEnabled(true);
                return userRepository.save(user);
            });
    }

    private String safeStudentName(User student) {
        if (student.getFullName() != null && !student.getFullName().isBlank()) {
            return student.getFullName().trim();
        }
        return student.getUsername();
    }

    private String safeStudentEmail(User student) {
        if (student.getEmail() != null && !student.getEmail().isBlank()) {
            return student.getEmail().trim().toLowerCase();
        }
        return student.getUsername() + "@cpk.local";
    }

    private String safeStudentPhone(User student) {
        if (student.getPhone() != null && !student.getPhone().isBlank()) {
            return student.getPhone().trim();
        }
        return "+7-900-000-00-00";
    }

    private void resetDatabaseState() {
        certificateRepository.deleteAllInBatch();
        assessmentResultRepository.deleteAllInBatch();
        paymentRepository.deleteAllInBatch();
        applicationRepository.deleteAllInBatch();
        programRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
    }

    private enum SeedMode {
        RESET,
        ONCE,
        OFF;

        private static SeedMode from(String rawValue) {
            if (rawValue == null || rawValue.isBlank()) {
                return ONCE;
            }
            return switch (rawValue.trim().toLowerCase()) {
                case "reset" -> RESET;
                case "off" -> OFF;
                default -> ONCE;
            };
        }
    }
}
