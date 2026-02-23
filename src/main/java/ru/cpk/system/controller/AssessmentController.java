package ru.cpk.system.controller;

import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.AssessmentResult;
import ru.cpk.system.model.AssessmentStatus;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.AssessmentResultRepository;
import ru.cpk.system.repository.UserRepository;
import ru.cpk.system.service.ApplicationWorkflowService;

@Controller
@RequestMapping("/assessments")
public class AssessmentController {

    private final AssessmentResultRepository assessmentResultRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final ApplicationWorkflowService workflowService;

    public AssessmentController(AssessmentResultRepository assessmentResultRepository,
                                ApplicationRepository applicationRepository,
                                UserRepository userRepository,
                                ApplicationWorkflowService workflowService) {
        this.assessmentResultRepository = assessmentResultRepository;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.workflowService = workflowService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping
    public String list(Authentication authentication, Model model) {
        User currentUser = currentUser(authentication);
        List<AssessmentResult> assessments = currentUser.getRole() == RoleName.ADMIN
            ? assessmentResultRepository.findAll(Sort.by(Sort.Direction.DESC, "resultDate"))
            : assessmentResultRepository.findByApplicationAssignedTeacherIdOrderByResultDateDesc(currentUser.getId());
        model.addAttribute("assessments", assessments);
        model.addAttribute("passThreshold", ApplicationWorkflowService.PASS_THRESHOLD);
        model.addAttribute("teacherDisplayName", currentUserDisplayName(currentUser));
        model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
        return "assessments/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long applicationId,
                             @RequestParam(required = false) Long fromProgramId,
                             Authentication authentication,
                             Model model) {
        User currentUser = currentUser(authentication);
        AssessmentResult assessment = new AssessmentResult();
        assessment.setResultDate(LocalDate.now());
        assessment.setStatus(AssessmentStatus.NOT_PASSED);

        List<Application> applications = availableApplications(currentUser);
        if (applicationId != null) {
            Application selected = applications.stream()
                .filter(application -> application.getId().equals(applicationId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Заявка недоступна для аттестации"));
            assessment.setApplication(selected);
        }

        model.addAttribute("assessment", assessment);
        model.addAttribute("applications", applications);
        model.addAttribute("teacherDisplayName", currentUserDisplayName(currentUser));
        model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
        model.addAttribute("fromProgramId", fromProgramId);
        return "assessments/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           @RequestParam(required = false) Long fromProgramId,
                           Authentication authentication,
                           Model model) {
        User currentUser = currentUser(authentication);
        AssessmentResult assessment = assessmentResultRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Результат аттестации не найден"));
        if (currentUser.getRole() == RoleName.TEACHER
            && (assessment.getApplication().getAssignedTeacher() == null
            || !assessment.getApplication().getAssignedTeacher().getId().equals(currentUser.getId()))) {
            throw new IllegalArgumentException("Преподаватель может редактировать только свои результаты");
        }

        model.addAttribute("assessment", assessment);
        model.addAttribute("applications", editableApplications(currentUser, assessment.getApplication()));
        model.addAttribute("teacherDisplayName", currentUserDisplayName(currentUser));
        model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
        model.addAttribute("fromProgramId", fromProgramId);
        return "assessments/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("assessment") AssessmentResult assessment,
                       BindingResult bindingResult,
                       @RequestParam(required = false) Long fromProgramId,
                       Authentication authentication,
                       Model model) {
        User currentUser = currentUser(authentication);
        if (bindingResult.hasErrors()) {
            model.addAttribute("applications", availableApplications(currentUser));
            model.addAttribute("teacherDisplayName", currentUserDisplayName(currentUser));
            model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
            model.addAttribute("fromProgramId", fromProgramId);
            return "assessments/form";
        }

        if (currentUser.getRole() == RoleName.TEACHER) {
            Application application = applicationRepository.findByIdAndAssignedTeacherId(
                    assessment.getApplication().getId(), currentUser.getId())
                .orElseThrow(() -> new IllegalArgumentException("Заявка не назначена текущему преподавателю"));
            assessment.setApplication(application);
        }

        AssessmentStatus calculatedStatus = assessment.getScore() >= ApplicationWorkflowService.PASS_THRESHOLD
            ? AssessmentStatus.PASSED
            : AssessmentStatus.NOT_PASSED;
        assessment.setStatus(calculatedStatus);
        AssessmentResult savedAssessment = assessmentResultRepository.save(assessment);
        workflowService.applyAssessmentStatus(savedAssessment.getApplication(), savedAssessment.getScore());
        if (fromProgramId != null) {
            String message = URLEncoder.encode("Результат аттестации сохранен", StandardCharsets.UTF_8);
            return "redirect:/teacher/groups/" + fromProgramId + "?message=" + message;
        }
        return "redirect:/assessments";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, Authentication authentication) {
        User currentUser = currentUser(authentication);
        AssessmentResult assessment = assessmentResultRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Результат аттестации не найден"));
        if (currentUser.getRole() == RoleName.TEACHER
            && (assessment.getApplication().getAssignedTeacher() == null
            || !assessment.getApplication().getAssignedTeacher().getId().equals(currentUser.getId()))) {
            throw new IllegalArgumentException("Преподаватель может удалять только свои результаты");
        }
        assessmentResultRepository.deleteById(id);
        return "redirect:/assessments";
    }

    private List<Application> availableApplications(User currentUser) {
        List<Application> applications = currentUser.getRole() == RoleName.ADMIN
            ? applicationRepository.findAll(Sort.by("id"))
            : applicationRepository.findByAssignedTeacherIdOrderByApplicationDateDesc(currentUser.getId());
        return applications.stream()
            .filter(app -> assessmentResultRepository.findByApplicationId(app.getId()).isEmpty())
            .toList();
    }

    private List<Application> editableApplications(User currentUser, Application selectedApplication) {
        List<Application> candidates = new java.util.ArrayList<>(availableApplications(currentUser));
        boolean alreadyIncluded = candidates.stream().anyMatch(item -> item.getId().equals(selectedApplication.getId()));
        if (!alreadyIncluded) {
            candidates.add(selectedApplication);
        }
        return candidates;
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
            return "admin-assessments";
        }
        return "teacher-assessments";
    }
}
