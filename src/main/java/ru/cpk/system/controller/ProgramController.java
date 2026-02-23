package ru.cpk.system.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Set;
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
import ru.cpk.system.model.Program;
import ru.cpk.system.model.ProgramType;
import ru.cpk.system.repository.ProgramRepository;

@Controller
@RequestMapping("/programs")
public class ProgramController {

    private static final Set<String> ALLOWED_SORTS = Set.of("title", "startDate", "endDate", "durationHours");

    private final ProgramRepository programRepository;

    public ProgramController(ProgramRepository programRepository) {
        this.programRepository = programRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "") String q,
                       @RequestParam(defaultValue = "title") String sort,
                       Authentication authentication,
                       Model model) {
        Sort sortOrder = buildSort(sort);
        List<Program> programs = q.isBlank()
            ? programRepository.findAll(sortOrder)
            : programRepository.findByTitleContainingIgnoreCase(q, sortOrder);

        model.addAttribute("programs", programs);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        model.addAttribute("activeItem", resolveProgramsActiveItem(authentication));
        return "programs/list";
    }

    @GetMapping("/{id}")
    public String details(@PathVariable Long id,
                          Authentication authentication,
                          Model model) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Программа не найдена"));

        model.addAttribute("program", program);
        model.addAttribute("relatedPrograms",
            programRepository.findTop3ByCategoryAndActiveTrueAndIdNotOrderByStartDateAsc(
                program.getCategory(), program.getId()));
        model.addAttribute("activeItem", resolveProgramsActiveItem(authentication));
        return "programs/details";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/new")
    public String createForm(Authentication authentication, Model model) {
        Program program = new Program();
        program.setProgramType(ProgramType.ADVANCED_TRAINING);
        model.addAttribute("program", program);
        model.addAttribute("types", ProgramType.values());
        model.addAttribute("activeItem", resolveProgramsActiveItem(authentication));
        return "programs/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                           Authentication authentication,
                           Model model) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Программа не найдена"));
        model.addAttribute("program", program);
        model.addAttribute("types", ProgramType.values());
        model.addAttribute("activeItem", resolveProgramsActiveItem(authentication));
        return "programs/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("program") Program program,
                       BindingResult bindingResult,
                       Authentication authentication,
                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("types", ProgramType.values());
            model.addAttribute("activeItem", resolveProgramsActiveItem(authentication));
            return "programs/form";
        }

        if (program.getEndDate() != null && program.getStartDate() != null
            && program.getEndDate().isBefore(program.getStartDate())) {
            bindingResult.rejectValue("endDate", "invalid.endDate", "Дата окончания не может быть раньше даты начала");
            model.addAttribute("types", ProgramType.values());
            model.addAttribute("activeItem", resolveProgramsActiveItem(authentication));
            return "programs/form";
        }

        programRepository.save(program);
        return "redirect:/programs";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        programRepository.deleteById(id);
        return "redirect:/programs";
    }

    private Sort buildSort(String sort) {
        String selectedSort = ALLOWED_SORTS.contains(sort) ? sort : "title";
        return Sort.by(Sort.Direction.ASC, selectedSort);
    }

    private String resolveProgramsActiveItem(Authentication authentication) {
        if (authentication == null) {
            return "public-programs";
        }
        if (authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()))) {
            return "admin-programs";
        }
        if (authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_METHODIST".equals(authority.getAuthority()))) {
            return "methodist-programs";
        }
        if (authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_TEACHER".equals(authority.getAuthority()))) {
            return "teacher-programs";
        }
        if (authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_STUDENT".equals(authority.getAuthority()))) {
            return "student-programs";
        }
        return "public-programs";
    }
}
