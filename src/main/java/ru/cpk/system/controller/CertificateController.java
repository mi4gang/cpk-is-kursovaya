package ru.cpk.system.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
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
import ru.cpk.system.model.Certificate;
import ru.cpk.system.model.CertificateStatus;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.CertificateRepository;
import ru.cpk.system.service.ApplicationWorkflowService;

@Controller
@RequestMapping("/certificates")
public class CertificateController {

    private final CertificateRepository certificateRepository;
    private final ApplicationRepository applicationRepository;
    private final ApplicationWorkflowService workflowService;

    public CertificateController(CertificateRepository certificateRepository,
                                 ApplicationRepository applicationRepository,
                                 ApplicationWorkflowService workflowService) {
        this.certificateRepository = certificateRepository;
        this.applicationRepository = applicationRepository;
        this.workflowService = workflowService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping
    public String list(Authentication authentication, Model model) {
        model.addAttribute("certificates", certificateRepository.findAll(Sort.by(Sort.Direction.DESC, "issueDate")));
        model.addAttribute("activeItem", resolveBackofficeActiveItem(authentication, "admin-certificates", "methodist-certificates"));
        return "certificates/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/new")
    public String createForm(@RequestParam(required = false) Long applicationId,
                             Authentication authentication,
                             Model model) {
        Certificate certificate = new Certificate();
        certificate.setIssueDate(LocalDate.now());
        certificate.setStatus(CertificateStatus.ISSUED);
        var applications = applicationRepository.findReadyForCertificate();
        if (applicationId != null) {
            applications.stream()
                .filter(application -> application.getId().equals(applicationId))
                .findFirst()
                .ifPresent(certificate::setApplication);
        }
        model.addAttribute("certificate", certificate);
        model.addAttribute("statuses", CertificateStatus.values());
        model.addAttribute("applications", applications);
        model.addAttribute("activeItem", resolveBackofficeActiveItem(authentication, "admin-certificates", "methodist-certificates"));
        return "certificates/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Authentication authentication,
                           Model model) {
        Certificate certificate = certificateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Удостоверение не найдено"));
        model.addAttribute("certificate", certificate);
        model.addAttribute("statuses", CertificateStatus.values());
        model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
        model.addAttribute("activeItem", resolveBackofficeActiveItem(authentication, "admin-certificates", "methodist-certificates"));
        return "certificates/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("certificate") Certificate certificate,
                       BindingResult bindingResult,
                       Authentication authentication,
                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", CertificateStatus.values());
            model.addAttribute("applications", certificate.getId() == null
                ? applicationRepository.findReadyForCertificate()
                : applicationRepository.findAll(Sort.by("id")));
            model.addAttribute("activeItem", resolveBackofficeActiveItem(authentication, "admin-certificates", "methodist-certificates"));
            return "certificates/form";
        }

        if (!workflowService.canIssueCertificate(certificate.getApplication())) {
            throw new IllegalArgumentException("Удостоверение можно выдать только после успешной аттестации и завершения преподавателем");
        }

        if (certificate.getId() == null) {
            certificate.setStatus(CertificateStatus.ISSUED);
        }

        certificateRepository.save(certificate);
        return "redirect:/certificates";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        certificateRepository.deleteById(id);
        return "redirect:/certificates";
    }

    private String resolveBackofficeActiveItem(Authentication authentication,
                                               String adminActiveItem,
                                               String methodistActiveItem) {
        if (authentication != null && authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return adminActiveItem;
        }
        return methodistActiveItem;
    }
}
