package ru.cpk.system.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.DocumentStatus;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.PaymentRepository;
import ru.cpk.system.repository.ProgramRepository;
import ru.cpk.system.repository.UserRepository;

@Service
public class StatsService {

    private final ApplicationRepository applicationRepository;
    private final ProgramRepository programRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;

    public StatsService(ApplicationRepository applicationRepository,
                        ProgramRepository programRepository,
                        PaymentRepository paymentRepository,
                        UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.programRepository = programRepository;
        this.paymentRepository = paymentRepository;
        this.userRepository = userRepository;
    }

    public long totalListeners() {
        return applicationRepository.count();
    }

    public long activePrograms() {
        return programRepository.countByActiveTrue();
    }

    public BigDecimal paidAmount() {
        return paymentRepository.sumPaidAmounts();
    }

    public long pendingDocumentQueue() {
        return applicationRepository.countByDocStatus(DocumentStatus.PENDING);
    }

    public long fullAccessQueue() {
        return applicationRepository.countByDocStatusAndAccessStatusNot(DocumentStatus.APPROVED, AccessStatus.FULL_ACCESS);
    }

    public long readyForCertificateQueue() {
        return applicationRepository.countReadyForCertificate();
    }

    public long trialAccessCount() {
        return applicationRepository.countByAccessStatus(AccessStatus.TRIAL_ACCESS);
    }

    public long approvedButUnpaidCount() {
        return applicationRepository.countApprovedDocsWithoutPaidPayment();
    }

    public long inTrainingCount() {
        return applicationRepository.countByAccessStatusAndStatusNot(AccessStatus.FULL_ACCESS, ApplicationStatus.COMPLETED);
    }

    public long totalTeachers() {
        return userRepository.countByRole(RoleName.TEACHER);
    }

    public List<TeacherLoad> teacherLoads() {
        List<User> teachers = userRepository.findByRoleOrderByFullNameAsc(RoleName.TEACHER);
        List<TeacherLoad> result = new ArrayList<>();
        for (User teacher : teachers) {
            long currentStudents = applicationRepository.countCurrentStudentsForTeacher(teacher.getId());
            long currentPrograms = applicationRepository.countCurrentProgramsForTeacher(teacher.getId());
            result.add(new TeacherLoad(teacher.getId(), teacher.getFullName(), currentStudents, currentPrograms));
        }
        return result;
    }

    public record TeacherLoad(Long teacherId, String teacherName, long currentStudents, long currentPrograms) {
    }
}
