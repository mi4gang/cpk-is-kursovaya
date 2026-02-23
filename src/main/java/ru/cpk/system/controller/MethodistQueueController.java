package ru.cpk.system.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.DocumentStatus;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.UserRepository;
import ru.cpk.system.service.ApplicationWorkflowService;
import ru.cpk.system.service.StatsService;

@Controller
@RequestMapping("/methodist")
public class MethodistQueueController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationWorkflowService workflowService;
    private final StatsService statsService;

    public MethodistQueueController(ApplicationRepository applicationRepository,
                                    UserRepository userRepository,
                                    ApplicationWorkflowService workflowService,
                                    StatsService statsService) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.workflowService = workflowService;
        this.statsService = statsService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/queue")
    public String queue(Model model) {
        var docsQueue = refreshTrials(applicationRepository.findByDocStatusOrderByApplicationDateAsc(DocumentStatus.PENDING));
        var accessQueue = refreshTrials(applicationRepository.findByDocStatusAndAccessStatusNotOrderByApplicationDateAsc(
            DocumentStatus.APPROVED, AccessStatus.FULL_ACCESS));
        var inTrainingQueue = refreshTrials(applicationRepository.findByAccessStatusAndStatusNotOrderByApplicationDateAsc(
            AccessStatus.FULL_ACCESS, ApplicationStatus.COMPLETED));
        var certificateQueue = refreshTrials(applicationRepository.findReadyForCertificate());
        var trialQueue = accessQueue.stream()
            .filter(a -> a.getAccessStatus() == AccessStatus.TRIAL_ACCESS)
            .filter(a -> a.getPayment() == null || a.getPayment().getStatus() != PaymentStatus.PAID)
            .toList();

        model.addAttribute("docsQueue", docsQueue);
        model.addAttribute("accessQueue", accessQueue);
        model.addAttribute("inTrainingQueue", inTrainingQueue);
        model.addAttribute("certificateQueue", certificateQueue);
        model.addAttribute("trialQueue", trialQueue);
        model.addAttribute("teachers", userRepository.findByRoleOrderByFullNameAsc(RoleName.TEACHER));

        model.addAttribute("docQueueCount", statsService.pendingDocumentQueue());
        model.addAttribute("accessQueueCount", statsService.fullAccessQueue());
        model.addAttribute("certificateQueueCount", statsService.readyForCertificateQueue());
        return "methodist/queue";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/queue/{id}/docs")
    public String updateDocuments(@PathVariable Long id,
                                  @RequestParam DocumentStatus status,
                                  Authentication authentication) {
        ensureMethodistOrAdmin(authentication);
        Application current = findApplication(id);
        Application incoming = new Application();
        incoming.setDocStatus(status);
        incoming.setAccessStatus(current.getAccessStatus());
        incoming.setAssignedTeacher(current.getAssignedTeacher());
        workflowService.updateByMethodist(current, incoming);
        return "redirect:/methodist/queue";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/queue/{id}/open-full-access")
    public String openFullAccess(@PathVariable Long id, Authentication authentication) {
        ensureMethodistOrAdmin(authentication);
        Application current = findApplication(id);
        workflowService.openFullAccess(current);
        return "redirect:/methodist/queue";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/queue/{id}/trial")
    public String openTrial(@PathVariable Long id, Authentication authentication) {
        ensureMethodistOrAdmin(authentication);
        Application current = findApplication(id);
        workflowService.openTrialAccess(current);
        return "redirect:/methodist/queue";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/queue/{id}/assign-teacher")
    public String assignTeacher(@PathVariable Long id,
                                @RequestParam Long teacherId,
                                Authentication authentication) {
        ensureMethodistOrAdmin(authentication);
        User teacher = userRepository.findById(teacherId)
            .orElseThrow(() -> new IllegalArgumentException("Преподаватель не найден"));

        Application current = findApplication(id);
        workflowService.assignTeacherOnly(current, teacher);
        return "redirect:/methodist/queue";
    }

    private java.util.List<Application> refreshTrials(java.util.List<Application> applications) {
        return applications.stream()
            .map(workflowService::applyTrialExpiration)
            .toList();
    }

    private void ensureMethodistOrAdmin(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
        if (user.getRole() != RoleName.METHODIST && user.getRole() != RoleName.ADMIN) {
            throw new IllegalArgumentException("Доступно только методисту или администратору");
        }
    }

    private Application findApplication(Long id) {
        return applicationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
    }
}
