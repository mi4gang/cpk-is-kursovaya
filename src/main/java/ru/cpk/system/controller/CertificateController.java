package ru.cpk.system.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.cpk.system.model.Certificate;
import ru.cpk.system.model.CertificateStatus;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.CertificateRepository;

@Controller
@RequestMapping("/certificates")
public class CertificateController {

    private final CertificateRepository certificateRepository;
    private final ApplicationRepository applicationRepository;

    public CertificateController(CertificateRepository certificateRepository,
                                 ApplicationRepository applicationRepository) {
        this.certificateRepository = certificateRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("certificates", certificateRepository.findAll(Sort.by(Sort.Direction.DESC, "issueDate")));
        return "certificates/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/new")
    public String createForm(Model model) {
        Certificate certificate = new Certificate();
        certificate.setIssueDate(LocalDate.now());
        certificate.setStatus(CertificateStatus.NOT_ISSUED);
        model.addAttribute("certificate", certificate);
        model.addAttribute("statuses", CertificateStatus.values());
        model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
        return "certificates/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Certificate certificate = certificateRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Удостоверение не найдено"));
        model.addAttribute("certificate", certificate);
        model.addAttribute("statuses", CertificateStatus.values());
        model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
        return "certificates/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("certificate") Certificate certificate,
                       BindingResult bindingResult,
                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", CertificateStatus.values());
            model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
            return "certificates/form";
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
}
