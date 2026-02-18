package ru.cpk.system.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
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
import org.springframework.web.bind.annotation.RequestParam;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.ProgramRepository;

@Controller
@RequestMapping("/applications")
public class ApplicationController {

    private static final Set<String> ALLOWED_SORTS = Set.of("listenerFullName", "applicationDate", "status");

    private final ApplicationRepository applicationRepository;
    private final ProgramRepository programRepository;

    public ApplicationController(ApplicationRepository applicationRepository,
                                 ProgramRepository programRepository) {
        this.applicationRepository = applicationRepository;
        this.programRepository = programRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "applicationDate") String sort,
                       Model model) {
        Sort sortOrder = buildSort(sort);
        List<Application> applications = q.isBlank()
            ? applicationRepository.findAll(sortOrder)
            : applicationRepository.findByListenerFullNameContainingIgnoreCase(q, sortOrder);

        model.addAttribute("applications", applications);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        return "applications/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST', 'STUDENT')")
    @GetMapping("/new")
    public String createForm(Model model) {
        Application application = new Application();
        application.setApplicationDate(LocalDate.now());
        application.setStatus(ApplicationStatus.SUBMITTED);
        model.addAttribute("application", application);
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("programs", programRepository.findAll(Sort.by("title")));
        return "applications/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Application application = applicationRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена"));
        model.addAttribute("application", application);
        model.addAttribute("statuses", ApplicationStatus.values());
        model.addAttribute("programs", programRepository.findAll(Sort.by("title")));
        return "applications/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST', 'STUDENT')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("application") Application application,
                       BindingResult bindingResult,
                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", ApplicationStatus.values());
            model.addAttribute("programs", programRepository.findAll(Sort.by("title")));
            return "applications/form";
        }

        applicationRepository.save(application);
        return "redirect:/applications";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        applicationRepository.deleteById(id);
        return "redirect:/applications";
    }

    private Sort buildSort(String sort) {
        String selectedSort = ALLOWED_SORTS.contains(sort) ? sort : "applicationDate";
        return Sort.by(Sort.Direction.ASC, selectedSort);
    }
}
