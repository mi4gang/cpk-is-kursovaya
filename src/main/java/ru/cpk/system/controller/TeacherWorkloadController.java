package ru.cpk.system.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.UserRepository;
import ru.cpk.system.service.ApplicationWorkflowService;

@Controller
@RequestMapping("/teacher")
public class TeacherWorkloadController {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationWorkflowService workflowService;

    public TeacherWorkloadController(UserRepository userRepository,
                                     ApplicationRepository applicationRepository,
                                     ApplicationWorkflowService workflowService) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.workflowService = workflowService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/workload")
    public String workloadRedirect() {
        return "redirect:/teacher/groups";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/groups")
    public String groups(Authentication authentication, Model model) {
        User currentUser = currentUser(authentication);
        List<Application> applications = visibleApplications(currentUser);
        Map<Long, GroupAccumulator> grouped = new LinkedHashMap<>();
        for (Application application : applications) {
            Long programId = application.getProgram().getId();
            GroupAccumulator accumulator = grouped.computeIfAbsent(programId, unused ->
                new GroupAccumulator(programId, application.getProgram().getTitle(), 0, 0, 0));
            accumulator.studentCount += 1;
            accumulator.progressTotal += application.getProgressPercent();
            if (application.getAssessmentResult() != null) {
                accumulator.assessedCount += 1;
            }
        }

        List<ProgramGroupRow> groups = new ArrayList<>();
        for (GroupAccumulator accumulator : grouped.values()) {
            long average = accumulator.studentCount == 0 ? 0 : Math.round((double) accumulator.progressTotal / accumulator.studentCount);
            long assessedPercent = accumulator.studentCount == 0
                ? 0
                : Math.round((double) accumulator.assessedCount * 100 / accumulator.studentCount);
            groups.add(new ProgramGroupRow(
                accumulator.programId,
                accumulator.programTitle,
                accumulator.studentCount,
                average,
                assessedPercent
            ));
        }

        model.addAttribute("groups", groups);
        model.addAttribute("isAdminView", currentUser.getRole() == RoleName.ADMIN);
        model.addAttribute("teacherDisplayName", currentUserDisplayName(currentUser));
        model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
        return "teacher/groups";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/groups/{programId}")
    public String groupDetails(@PathVariable Long programId,
                               @RequestParam(required = false) String message,
                               @RequestParam(required = false) String error,
                               Authentication authentication,
                               Model model) {
        User currentUser = currentUser(authentication);
        List<Application> applications = visibleApplications(currentUser).stream()
            .filter(application -> application.getProgram().getId().equals(programId))
            .toList();
        if (applications.isEmpty()) {
            throw new IllegalArgumentException("Группа программы не найдена для текущего преподавателя");
        }

        model.addAttribute("program", applications.get(0).getProgram());
        model.addAttribute("applications", applications);
        model.addAttribute("isAdminView", currentUser.getRole() == RoleName.ADMIN);
        model.addAttribute("message", message);
        model.addAttribute("error", error);
        model.addAttribute("teacherDisplayName", currentUserDisplayName(currentUser));
        model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
        return "teacher/group-details";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/groups/{programId}/applications/{id}/progress")
    public String updateProgress(@PathVariable Long programId,
                                 @PathVariable Long id,
                                 @RequestParam int progressPercent,
                                 Authentication authentication,
                                 RedirectAttributes redirectAttributes) {
        User currentUser = currentUser(authentication);
        Application application = resolveTeacherApplication(id, programId, currentUser);
        workflowService.updateTeacherProgress(application, progressPercent);
        redirectAttributes.addAttribute("message", "Прогресс сохранен");
        return "redirect:/teacher/groups/{programId}";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/groups/{programId}/applications/{id}/complete")
    public String markCompleted(@PathVariable Long programId,
                                @PathVariable Long id,
                                Authentication authentication,
                                RedirectAttributes redirectAttributes) {
        User currentUser = currentUser(authentication);
        Application application = resolveTeacherApplication(id, programId, currentUser);
        try {
            workflowService.markTeacherCompleted(application);
            redirectAttributes.addAttribute("message", "Обучение завершено");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
        }
        return "redirect:/teacher/groups/{programId}";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/workload/{id}/progress")
    public String updateProgressLegacy(@PathVariable Long id,
                                       @RequestParam int progressPercent,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        User currentUser = currentUser(authentication);
        Application application = resolveTeacherApplication(id, null, currentUser);
        workflowService.updateTeacherProgress(application, progressPercent);
        redirectAttributes.addAttribute("message", "Прогресс сохранен");
        redirectAttributes.addAttribute("programId", application.getProgram().getId());
        return "redirect:/teacher/groups/{programId}";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/workload/{id}/complete")
    public String markCompletedLegacy(@PathVariable Long id,
                                      Authentication authentication,
                                      RedirectAttributes redirectAttributes) {
        User currentUser = currentUser(authentication);
        Application application = resolveTeacherApplication(id, null, currentUser);
        try {
            workflowService.markTeacherCompleted(application);
            redirectAttributes.addAttribute("message", "Обучение завершено");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addAttribute("error", ex.getMessage());
        }
        redirectAttributes.addAttribute("programId", application.getProgram().getId());
        return "redirect:/teacher/groups/{programId}";
    }

    private Application resolveTeacherApplication(Long applicationId, Long programId, User currentUser) {
        Application application;
        if (currentUser.getRole() == RoleName.ADMIN) {
            application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
        } else {
            application = applicationRepository.findByIdAndAssignedTeacherId(applicationId, currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Заявка не назначена текущему преподавателю"));
        }
        if (programId != null && !application.getProgram().getId().equals(programId)) {
            throw new IllegalArgumentException("Заявка не относится к выбранной группе");
        }
        return application;
    }

    private List<Application> visibleApplications(User currentUser) {
        List<Application> base = currentUser.getRole() == RoleName.ADMIN
            ? applicationRepository.findAssignedApplications()
            : applicationRepository.findByAssignedTeacherIdOrderByApplicationDateDesc(currentUser.getId());
        return base.stream()
            .filter(application -> application.getAccessStatus() == AccessStatus.FULL_ACCESS)
            .filter(application -> application.getStatus() != ApplicationStatus.REJECTED)
            .toList();
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    private String currentUserDisplayName(User currentUser) {
        if (currentUser.getFullName() != null && !currentUser.getFullName().isBlank()) {
            return currentUser.getFullName();
        }
        return currentUser.getUsername();
    }

    private String resolveActiveItem(RoleName role) {
        if (role == RoleName.ADMIN) {
            return "admin-dashboard";
        }
        return "teacher-groups";
    }

    private static final class GroupAccumulator {
        private final Long programId;
        private final String programTitle;
        private long studentCount;
        private long progressTotal;
        private long assessedCount;

        private GroupAccumulator(Long programId,
                                 String programTitle,
                                 long studentCount,
                                 long progressTotal,
                                 long assessedCount) {
            this.programId = programId;
            this.programTitle = programTitle;
            this.studentCount = studentCount;
            this.progressTotal = progressTotal;
            this.assessedCount = assessedCount;
        }
    }

    public record ProgramGroupRow(Long programId,
                                  String programTitle,
                                  long studentCount,
                                  long avgProgress,
                                  long assessedPercent) {
    }
}
