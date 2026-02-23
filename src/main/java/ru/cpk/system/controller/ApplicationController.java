package ru.cpk.system.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
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
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.DocumentStatus;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.ProgramRepository;
import ru.cpk.system.repository.UserRepository;
import ru.cpk.system.service.ApplicationWorkflowService;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private static final Set<String> ALLOWED_SORTS = Set.of("listenerFullName", "applicationDate", "status");

    private final ApplicationRepository applicationRepository;
    private final ProgramRepository programRepository;
    private final UserRepository userRepository;
    private final ApplicationWorkflowService workflowService;

    public ApplicationController(ApplicationRepository applicationRepository,
                                 ProgramRepository programRepository,
                                 UserRepository userRepository,
                                 ApplicationWorkflowService workflowService) {
        this.applicationRepository = applicationRepository;
        this.programRepository = programRepository;
        this.userRepository = userRepository;
        this.workflowService = workflowService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST', 'STUDENT')")
    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "applicationDate") String sort,
                       @RequestParam(required = false) DocumentStatus docStatus,
                       @RequestParam(required = false) AccessStatus accessStatus,
                       @RequestParam(required = false) ApplicationStatus appStatus,
                       @RequestParam(required = false) Long programId,
                       @RequestParam(required = false) Long teacherId,
                       @RequestParam(required = false) String paymentState,
                       @RequestParam(required = false) String paid,
                       Authentication authentication,
                       Model model) {
        User currentUser = currentUser(authentication);
        Sort sortOrder = buildSort(sort);

        List<Application> applications;
        if (currentUser.getRole() == RoleName.STUDENT) {
            applications = applicationRepository.findByStudentIdOrderByApplicationDateDesc(currentUser.getId());
        } else if (q.isBlank()) {
            applications = applicationRepository.findAll(sortOrder);
        } else {
            applications = applicationRepository.findByListenerFullNameContainingIgnoreCase(q, sortOrder);
        }
        if (currentUser.getRole() != RoleName.STUDENT) {
            applications = applyBackofficeFilters(applications, docStatus, accessStatus, appStatus, programId, teacherId, paymentState);
        }

        List<Application> updatedApplications = new ArrayList<>();
        for (Application application : applications) {
            updatedApplications.add(workflowService.applyTrialExpiration(application));
        }

        model.addAttribute("applications", updatedApplications);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("isStudent", currentUser.getRole() == RoleName.STUDENT);
        model.addAttribute("paymentSuccess", paid != null);
        model.addAttribute("docStatuses", DocumentStatus.values());
        model.addAttribute("accessStatuses", AccessStatus.values());
        model.addAttribute("applicationStatuses", ApplicationStatus.values());
        model.addAttribute("programs", programRepository.findAll(Sort.by("title")));
        model.addAttribute("teachers", userRepository.findByRoleOrderByFullNameAsc(RoleName.TEACHER));
        model.addAttribute("selectedDocStatus", docStatus);
        model.addAttribute("selectedAccessStatus", accessStatus);
        model.addAttribute("selectedAppStatus", appStatus);
        model.addAttribute("selectedProgramId", programId);
        model.addAttribute("selectedTeacherId", teacherId);
        model.addAttribute("selectedPaymentState", paymentState);
        model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
        if (currentUser.getRole() != RoleName.STUDENT) {
            model.addAttribute("programGroups", buildProgramGroups(updatedApplications));
        }
        return "applications/list";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long programId,
                             Authentication authentication,
                             Model model) {
        User currentUser = currentUser(authentication);

        Application application = new Application();
        application.setStatus(ApplicationStatus.SUBMITTED);
        application.setDocStatus(DocumentStatus.PENDING);
        application.setAccessStatus(AccessStatus.NO_ACCESS);
        applyStudentIdentity(application, currentUser);
        if (programId != null) {
            programRepository.findById(programId).ifPresent(application::setProgram);
        }

        model.addAttribute("application", application);
        populateFormOptions(model, currentUser);
        model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
        return "applications/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        User currentUser = currentUser(authentication);
        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));

        model.addAttribute("application", workflowService.applyTrialExpiration(application));
        populateFormOptions(model, currentUser);
        model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
        return "applications/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST', 'STUDENT')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("application") Application application,
                       BindingResult bindingResult,
                       Authentication authentication,
                       Model model) {
        User currentUser = currentUser(authentication);
        if (bindingResult.hasErrors()) {
            if (currentUser.getRole() == RoleName.STUDENT) {
                applyStudentIdentity(application, currentUser);
                if (application.getApplicationDate() == null) {
                    application.setApplicationDate(LocalDate.now());
                }
            }
            populateFormOptions(model, currentUser);
            model.addAttribute("activeItem", resolveActiveItem(currentUser.getRole()));
            return "applications/form";
        }

        if (currentUser.getRole() == RoleName.STUDENT) {
            if (application.getId() != null) {
                throw new IllegalArgumentException("Слушатель может только создавать новую заявку");
            }
            applyStudentIdentity(application, currentUser);
            workflowService.initializeStudentApplication(application, currentUser);
            applicationRepository.save(application);
            return "redirect:/student/cabinet";
        }

        if (application.getId() == null) {
            throw new IllegalArgumentException("Новая заявка создается слушателем");
        }

        Application current = applicationRepository.findById(application.getId())
            .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
        current.setListenerFullName(application.getListenerFullName());
        current.setListenerEmail(application.getListenerEmail());
        current.setListenerPhone(application.getListenerPhone());
        current.setApplicationDate(application.getApplicationDate());
        current.setProgram(application.getProgram());

        workflowService.updateByMethodist(current, application);
        return "redirect:/applications";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        applicationRepository.deleteById(id);
        return "redirect:/applications";
    }

    private void populateFormOptions(Model model, User currentUser) {
        model.addAttribute("programs", programRepository.findAll(Sort.by("title")));
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("docStatuses", DocumentStatus.values());
        model.addAttribute("accessStatuses", AccessStatus.values());
        model.addAttribute("teachers", userRepository.findByRoleOrderByFullNameAsc(RoleName.TEACHER));
        model.addAttribute("isStudent", currentUser.getRole() == RoleName.STUDENT);
        model.addAttribute("isMethodistEditor",
            currentUser.getRole() == RoleName.METHODIST || currentUser.getRole() == RoleName.ADMIN);
        if (currentUser.getRole() == RoleName.STUDENT) {
            model.addAttribute("studentProfileFullName", normalizedStudentName(currentUser));
            model.addAttribute("studentProfileEmail", normalizedStudentEmail(currentUser));
            model.addAttribute("studentProfilePhone", normalizedStudentPhone(currentUser));
        }
    }

    private void applyStudentIdentity(Application application, User student) {
        application.setListenerFullName(normalizedStudentName(student));
        application.setListenerEmail(normalizedStudentEmail(student));
        application.setListenerPhone(normalizedStudentPhone(student));
    }

    private String normalizedStudentName(User student) {
        return student.getFullName() != null && !student.getFullName().isBlank()
            ? student.getFullName().trim()
            : student.getUsername();
    }

    private String normalizedStudentEmail(User student) {
        return student.getEmail() != null && !student.getEmail().isBlank()
            ? student.getEmail().trim().toLowerCase()
            : student.getUsername() + "@cpk.local";
    }

    private String normalizedStudentPhone(User student) {
        return student.getPhone() != null && !student.getPhone().isBlank()
            ? student.getPhone().trim()
            : "+7-900-000-00-00";
    }

    private User currentUser(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));
    }

    private Sort buildSort(String sort) {
        String selectedSort = ALLOWED_SORTS.contains(sort) ? sort : "applicationDate";
        return Sort.by(Sort.Direction.ASC, selectedSort);
    }

    private String resolveActiveItem(RoleName role) {
        if (role == RoleName.ADMIN) {
            return "admin-applications";
        }
        if (role == RoleName.METHODIST) {
            return "methodist-applications";
        }
        return "student-cabinet";
    }

    private List<Application> applyBackofficeFilters(List<Application> applications,
                                                     DocumentStatus docStatus,
                                                     AccessStatus accessStatus,
                                                     ApplicationStatus appStatus,
                                                     Long programId,
                                                     Long teacherId,
                                                     String paymentState) {
        Stream<Application> stream = applications.stream();
        if (docStatus != null) {
            stream = stream.filter(application -> application.getDocStatus() == docStatus);
        }
        if (accessStatus != null) {
            stream = stream.filter(application -> application.getAccessStatus() == accessStatus);
        }
        if (appStatus != null) {
            stream = stream.filter(application -> application.getStatus() == appStatus);
        }
        if (programId != null) {
            stream = stream.filter(application -> application.getProgram() != null
                && programId.equals(application.getProgram().getId()));
        }
        if (teacherId != null) {
            stream = stream.filter(application -> application.getAssignedTeacher() != null
                && teacherId.equals(application.getAssignedTeacher().getId()));
        }
        if (paymentState != null && !paymentState.isBlank()) {
            stream = stream.filter(application -> matchesPaymentState(application, paymentState));
        }
        return stream.toList();
    }

    private boolean matchesPaymentState(Application application, String paymentState) {
        if ("PAID".equalsIgnoreCase(paymentState)) {
            return application.getPayment() != null
                && application.getPayment().getStatus() == PaymentStatus.PAID;
        }
        if ("PENDING".equalsIgnoreCase(paymentState)) {
            return application.getPayment() != null
                && application.getPayment().getStatus() == PaymentStatus.PENDING;
        }
        if ("FAILED".equalsIgnoreCase(paymentState)) {
            return application.getPayment() != null
                && application.getPayment().getStatus() == PaymentStatus.FAILED;
        }
        if ("UNPAID".equalsIgnoreCase(paymentState)) {
            return application.getPayment() == null
                || application.getPayment().getStatus() != PaymentStatus.PAID;
        }
        return true;
    }

    private List<ProgramGroupRow> buildProgramGroups(List<Application> applications) {
        Map<Long, ProgramGroupAccumulator> grouped = new LinkedHashMap<>();
        for (Application application : applications) {
            if (application.getProgram() == null) {
                continue;
            }
            Long key = application.getProgram().getId();
            ProgramGroupAccumulator accumulator = grouped.computeIfAbsent(key, unused ->
                new ProgramGroupAccumulator(
                    key,
                    application.getProgram().getTitle(),
                    0,
                    0
                ));
            accumulator.applicationsCount += 1;
            if (isActiveListener(application)) {
                accumulator.activeListeners += 1;
            }
        }
        return grouped.values().stream()
            .map(row -> new ProgramGroupRow(
                row.programId,
                row.programTitle,
                row.applicationsCount,
                row.activeListeners
            ))
            .sorted(Comparator.comparingLong(ProgramGroupRow::applicationsCount).reversed()
                .thenComparing(ProgramGroupRow::programTitle))
            .toList();
    }

    private boolean isActiveListener(Application application) {
        return application.getStatus() != ApplicationStatus.COMPLETED
            && application.getStatus() != ApplicationStatus.REJECTED;
    }

    private static final class ProgramGroupAccumulator {
        private final Long programId;
        private final String programTitle;
        private long applicationsCount;
        private long activeListeners;

        private ProgramGroupAccumulator(Long programId,
                                        String programTitle,
                                        long applicationsCount,
                                        long activeListeners) {
            this.programId = programId;
            this.programTitle = programTitle;
            this.applicationsCount = applicationsCount;
            this.activeListeners = activeListeners;
        }
    }

    public record ProgramGroupRow(Long programId,
                                  String programTitle,
                                  long applicationsCount,
                                  long activeListeners) {
    }
}
