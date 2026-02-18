package ru.cpk.system.config;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.AssessmentResult;
import ru.cpk.system.model.AssessmentStatus;
import ru.cpk.system.model.Certificate;
import ru.cpk.system.model.CertificateStatus;
import ru.cpk.system.model.Payment;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.model.Program;
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

    private final UserRepository userRepository;
    private final ProgramRepository programRepository;
    private final ApplicationRepository applicationRepository;
    private final PaymentRepository paymentRepository;
    private final AssessmentResultRepository assessmentResultRepository;
    private final CertificateRepository certificateRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(UserRepository userRepository,
                           ProgramRepository programRepository,
                           ApplicationRepository applicationRepository,
                           PaymentRepository paymentRepository,
                           AssessmentResultRepository assessmentResultRepository,
                           CertificateRepository certificateRepository,
                           PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.programRepository = programRepository;
        this.applicationRepository = applicationRepository;
        this.paymentRepository = paymentRepository;
        this.assessmentResultRepository = assessmentResultRepository;
        this.certificateRepository = certificateRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (userRepository.count() == 0) {
            userRepository.save(createUser("admin", "admin123", "Администратор системы", "admin@cpk.local", "+7-900-000-00-01", RoleName.ADMIN));
            userRepository.save(createUser("methodist", "method123", "Методист центра", "methodist@cpk.local", "+7-900-000-00-02", RoleName.METHODIST));
            userRepository.save(createUser("teacher", "teacher123", "Преподаватель", "teacher@cpk.local", "+7-900-000-00-03", RoleName.TEACHER));
            userRepository.save(createUser("student", "student123", "Слушатель", "student@cpk.local", "+7-900-000-00-04", RoleName.STUDENT));
        }

        if (programRepository.count() > 0) {
            return;
        }

        Program p1 = new Program();
        p1.setTitle("Управление проектами в ИТ");
        p1.setDescription("Программа повышения квалификации для руководителей ИТ-проектов.");
        p1.setDurationHours(72);
        p1.setStartDate(LocalDate.now().plusDays(7));
        p1.setEndDate(LocalDate.now().plusDays(45));
        p1.setActive(true);
        p1 = programRepository.save(p1);

        Program p2 = new Program();
        p2.setTitle("Аналитика данных для бизнеса");
        p2.setDescription("Курс по работе с BI-инструментами и данными.");
        p2.setDurationHours(48);
        p2.setStartDate(LocalDate.now().plusDays(10));
        p2.setEndDate(LocalDate.now().plusDays(35));
        p2.setActive(true);
        p2 = programRepository.save(p2);

        Application app = new Application();
        app.setListenerFullName("Иванов Сергей Петрович");
        app.setListenerEmail("ivanov.sp@example.com");
        app.setListenerPhone("+7-911-111-22-33");
        app.setApplicationDate(LocalDate.now().minusDays(3));
        app.setStatus(ApplicationStatus.PAID);
        app.setProgram(p1);
        app = applicationRepository.save(app);

        Payment payment = new Payment();
        payment.setApplication(app);
        payment.setAmount(new BigDecimal("25000.00"));
        payment.setPaymentDate(LocalDate.now().minusDays(2));
        payment.setStatus(PaymentStatus.PAID);
        paymentRepository.save(payment);

        AssessmentResult result = new AssessmentResult();
        result.setApplication(app);
        result.setScore(88);
        result.setStatus(AssessmentStatus.PASSED);
        result.setResultDate(LocalDate.now().minusDays(1));
        result.setComment("Итоговая аттестация пройдена успешно");
        assessmentResultRepository.save(result);

        Certificate certificate = new Certificate();
        certificate.setApplication(app);
        certificate.setNumber("CPK-2025-0001");
        certificate.setIssueDate(LocalDate.now());
        certificate.setStatus(CertificateStatus.ISSUED);
        certificateRepository.save(certificate);
    }

    private User createUser(String username,
                            String password,
                            String fullName,
                            String email,
                            String phone,
                            RoleName roleName) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setFullName(fullName);
        user.setEmail(email);
        user.setPhone(phone);
        user.setRole(roleName);
        user.setEnabled(true);
        return user;
    }
}
