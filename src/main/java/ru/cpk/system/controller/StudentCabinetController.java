package ru.cpk.system.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.Certificate;
import ru.cpk.system.model.DocumentStatus;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.model.Program;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.CertificateRepository;
import ru.cpk.system.repository.ProgramRepository;
import ru.cpk.system.repository.UserRepository;
import ru.cpk.system.service.ApplicationWorkflowService;

@Controller
@RequestMapping("/student")
public class StudentCabinetController {

    private static final DateTimeFormatter UPLOAD_TS_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final ProgramRepository programRepository;
    private final CertificateRepository certificateRepository;
    private final ApplicationWorkflowService workflowService;
    private final Map<String, Map<String, MockUploadedDocument>> mockUploadedDocuments = new ConcurrentHashMap<>();

    public StudentCabinetController(UserRepository userRepository,
                                    ApplicationRepository applicationRepository,
                                    ProgramRepository programRepository,
                                    CertificateRepository certificateRepository,
                                    ApplicationWorkflowService workflowService) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.programRepository = programRepository;
        this.certificateRepository = certificateRepository;
        this.workflowService = workflowService;
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/cabinet")
    public String cabinet(@RequestParam(defaultValue = "active") String view,
                          Authentication authentication,
                          Model model) {
        User student = currentStudent(authentication);

        String selectedView = normalizeView(view);
        List<Application> applications = applicationRepository.findByStudentIdOrderByApplicationDateDesc(student.getId());
        List<Application> updatedApplications = new ArrayList<>();
        for (Application application : applications) {
            updatedApplications.add(workflowService.applyTrialExpiration(application));
        }

        long completedCount = updatedApplications.stream().filter(StudentCabinetController::isCompleted).count();
        long activeCount = updatedApplications.size() - completedCount;
        List<Application> filteredApplications = filterApplications(updatedApplications, selectedView);

        List<Program> recommendations = List.of();
        if (!filteredApplications.isEmpty()) {
            Program currentProgram = filteredApplications.get(0).getProgram();
            if (currentProgram != null) {
                recommendations = programRepository.findTop3ByCategoryAndActiveTrueAndIdNotOrderByStartDateAsc(
                    currentProgram.getCategory(), currentProgram.getId());
            }
        }

        model.addAttribute("applications", filteredApplications);
        model.addAttribute("cabinetView", selectedView);
        model.addAttribute("activeCount", activeCount);
        model.addAttribute("completedCount", completedCount);
        model.addAttribute("recommendations", recommendations);
        model.addAttribute("activeItem", "student-cabinet");
        return "student/cabinet";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @GetMapping("/profile")
    public String profile(@RequestParam(required = false) String saved,
                          Authentication authentication,
                          Model model) {
        User student = currentStudent(authentication);
        StudentProfileForm profileForm = StudentProfileForm.fromUser(student);
        model.addAttribute("profileForm", profileForm);
        model.addAttribute("saved", saved != null);
        model.addAttribute("activeItem", "student-profile");
        populateProfileModel(student, model);
        return "student/profile";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/profile")
    public String updateProfile(@Valid @ModelAttribute("profileForm") StudentProfileForm profileForm,
                                BindingResult bindingResult,
                                Authentication authentication,
                                Model model) {
        User student = currentStudent(authentication);
        String normalizedEmail = profileForm.getEmail().trim().toLowerCase();
        userRepository.findByEmail(normalizedEmail)
            .filter(user -> !user.getId().equals(student.getId()))
            .ifPresent(user -> bindingResult.rejectValue("email", "email.exists", "Email уже зарегистрирован"));

        if (bindingResult.hasErrors()) {
            model.addAttribute("saved", false);
            model.addAttribute("activeItem", "student-profile");
            populateProfileModel(student, model);
            return "student/profile";
        }

        student.setFullName(profileForm.getFullName().trim());
        student.setEmail(normalizedEmail);
        student.setPhone(profileForm.getPhone().trim());
        userRepository.save(student);
        return "redirect:/student/profile?saved=1";
    }

    @PreAuthorize("hasRole('STUDENT')")
    @PostMapping("/profile/documents/mock-upload")
    public String uploadMockDocument(@RequestParam String documentType,
                                     @RequestParam("documentFile") MultipartFile documentFile,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User student = currentStudent(authentication);
        if (documentFile == null || documentFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("uploadError", "Выберите файл для прикрепления.");
            return "redirect:/student/profile";
        }
        String normalizedType = normalizeDocumentType(documentType);
        if (normalizedType == null) {
            redirectAttributes.addFlashAttribute("uploadError", "Неизвестный тип документа.");
            return "redirect:/student/profile";
        }

        String originalFileName = documentFile.getOriginalFilename();
        String safeFileName = StringUtils.hasText(originalFileName)
            ? originalFileName.replaceAll("\\s+", "_")
            : "document.pdf";
        mockUploadedDocuments
            .computeIfAbsent(student.getUsername(), unused -> new ConcurrentHashMap<>())
            .put(normalizedType, new MockUploadedDocument(safeFileName, LocalDateTime.now()));
        redirectAttributes.addFlashAttribute(
            "uploadSuccess",
            "Документ \"" + documentTypeLabel(documentType) + "\" принят как заглушка: "
                + safeFileName + ". В демо-версии файл не сохраняется."
        );
        return "redirect:/student/profile";
    }

    private static String normalizeView(String view) {
        if ("completed".equalsIgnoreCase(view)) {
            return "completed";
        }
        if ("all".equalsIgnoreCase(view)) {
            return "all";
        }
        return "active";
    }

    private static List<Application> filterApplications(List<Application> applications, String selectedView) {
        if ("all".equals(selectedView)) {
            return applications;
        }
        List<Application> filtered = new ArrayList<>();
        for (Application application : applications) {
            boolean completed = isCompleted(application);
            if ("completed".equals(selectedView) && completed) {
                filtered.add(application);
            }
            if ("active".equals(selectedView) && !completed) {
                filtered.add(application);
            }
        }
        return filtered;
    }

    private static boolean isCompleted(Application application) {
        return application.getStatus() == ApplicationStatus.COMPLETED;
    }

    private User currentStudent(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Слушатель не найден"));
    }

    private void populateProfileModel(User student, Model model) {
        List<Certificate> certificates = certificateRepository.findByApplicationStudentIdOrderByIssueDateDesc(student.getId());

        List<MockDocumentRow> documents = List.of(
            buildMockDocument(student.getUsername(), "passport", "Паспорт"),
            buildMockDocument(student.getUsername(), "diploma", "Диплом"),
            buildMockDocument(student.getUsername(), "application", "Заявление")
        );

        model.addAttribute("studentUser", student);
        model.addAttribute("mockDocuments", documents);
        model.addAttribute("certificates", certificates);
    }

    private String mockDocLink(String username, String fileName) {
        return "https://docs.example/" + username + "/" + fileName;
    }

    private MockDocumentRow buildMockDocument(String username, String documentType, String title) {
        Map<String, MockUploadedDocument> userDocs = mockUploadedDocuments.getOrDefault(username, Map.of());
        MockUploadedDocument uploadedDocument = userDocs.get(documentType);
        if (uploadedDocument == null) {
            return new MockDocumentRow(title, "Не прикреплен (mock)", null);
        }
        String status = "Прикреплен (mock), " + uploadedDocument.uploadedAt().format(UPLOAD_TS_FORMAT);
        String link = mockDocLink(username, uploadedDocument.fileName());
        return new MockDocumentRow(title, status, link);
    }

    private String normalizeDocumentType(String documentType) {
        return switch (documentType) {
            case "passport", "diploma", "application" -> documentType;
            default -> null;
        };
    }

    private String documentTypeLabel(String documentType) {
        String normalizedType = normalizeDocumentType(documentType);
        if (normalizedType == null) {
            return "Документ";
        }
        return switch (normalizedType) {
            case "passport" -> "Паспорт";
            case "diploma" -> "Диплом";
            case "application" -> "Заявление";
            default -> "Документ";
        };
    }

    public static String stageLabel(Application application) {
        if (application.getDocStatus() == DocumentStatus.PENDING) {
            return "Проверка документов";
        }
        if (application.getDocStatus() == DocumentStatus.REJECTED) {
            return "Документы отклонены";
        }
        boolean paid = application.getPayment() != null && application.getPayment().getStatus() == PaymentStatus.PAID;
        if (application.getAccessStatus() == AccessStatus.TRIAL_ACCESS) {
            if (paid && application.getAssignedTeacher() == null) {
                return "Ожидает назначения преподавателя";
            }
            if (paid) {
                return "Ожидает открытия полного доступа";
            }
            return "Пробный доступ";
        }
        if (application.getAccessStatus() == AccessStatus.NO_ACCESS) {
            if (paid) {
                return "Ожидает открытия доступа";
            }
            return "Ожидание оплаты";
        }
        if (application.isTeacherCompleted()) {
            return "Обучение завершено";
        }
        if (application.getProgressPercent() > 0) {
            return "Обучение в процессе";
        }
        return "Доступ открыт";
    }

    public static String nextStep(Application application) {
        if (application.getDocStatus() == DocumentStatus.PENDING) {
            return "Дождитесь проверки документов методистом.";
        }
        if (application.getDocStatus() == DocumentStatus.REJECTED) {
            return "Обновите документы и подайте заявку повторно.";
        }
        boolean paid = application.getPayment() != null && application.getPayment().getStatus() == PaymentStatus.PAID;
        if (application.getAccessStatus() == AccessStatus.TRIAL_ACCESS) {
            if (paid && application.getAssignedTeacher() == null) {
                return "Оплата зафиксирована. Ожидайте назначения преподавателя методистом.";
            }
            if (paid) {
                return "Оплата зафиксирована. Ожидайте открытия полного доступа.";
            }
            return "Пробный период активен. Для полного доступа требуется оплата 100%.";
        }
        if (application.getAccessStatus() == AccessStatus.NO_ACCESS) {
            if (paid) {
                return "Ожидайте назначения преподавателя и открытия полного доступа методистом.";
            }
            return "Оплатите программу для открытия полного доступа.";
        }
        if (!application.isTeacherCompleted()) {
            return "Продолжайте обучение и ожидайте итоговой аттестации.";
        }
        if (application.getCertificate() == null) {
            return "Ожидайте выдачу удостоверения методистом.";
        }
        return "Удостоверение доступно в личном кабинете.";
    }

    public static final class StudentProfileForm {

        @NotBlank(message = "Укажите ФИО")
        private String fullName;

        @Email(message = "Некорректный email")
        @NotBlank(message = "Укажите email")
        private String email;

        @NotBlank(message = "Укажите телефон")
        private String phone;

        public static StudentProfileForm fromUser(User user) {
            StudentProfileForm form = new StudentProfileForm();
            form.setFullName(user.getFullName());
            form.setEmail(user.getEmail());
            form.setPhone(user.getPhone());
            return form;
        }

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }
    }

    public record MockDocumentRow(String title, String status, String link) {
    }

    private record MockUploadedDocument(String fileName, LocalDateTime uploadedAt) {
    }
}
