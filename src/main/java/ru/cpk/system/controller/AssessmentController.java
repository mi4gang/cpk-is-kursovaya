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
import ru.cpk.system.model.AssessmentResult;
import ru.cpk.system.model.AssessmentStatus;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.AssessmentResultRepository;

@Controller
@RequestMapping("/assessments")
public class AssessmentController {

    private final AssessmentResultRepository assessmentResultRepository;
    private final ApplicationRepository applicationRepository;

    public AssessmentController(AssessmentResultRepository assessmentResultRepository,
                                ApplicationRepository applicationRepository) {
        this.assessmentResultRepository = assessmentResultRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("assessments", assessmentResultRepository.findAll(Sort.by(Sort.Direction.DESC, "resultDate")));
        return "assessments/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/new")
    public String createForm(Model model) {
        AssessmentResult assessment = new AssessmentResult();
        assessment.setResultDate(LocalDate.now());
        assessment.setStatus(AssessmentStatus.NOT_PASSED);
        model.addAttribute("assessment", assessment);
        model.addAttribute("statuses", AssessmentStatus.values());
        model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
        return "assessments/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        AssessmentResult assessment = assessmentResultRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Результат аттестации не найден"));
        model.addAttribute("assessment", assessment);
        model.addAttribute("statuses", AssessmentStatus.values());
        model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
        return "assessments/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("assessment") AssessmentResult assessment,
                       BindingResult bindingResult,
                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", AssessmentStatus.values());
            model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
            return "assessments/form";
        }

        assessmentResultRepository.save(assessment);
        return "redirect:/assessments";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'TEACHER')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        assessmentResultRepository.deleteById(id);
        return "redirect:/assessments";
    }
}
