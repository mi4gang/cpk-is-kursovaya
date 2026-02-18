package ru.cpk.system.controller;

import jakarta.validation.Valid;
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
import ru.cpk.system.model.Program;
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
                       Model model) {
        Sort sortOrder = buildSort(sort);
        List<Program> programs = q.isBlank()
            ? programRepository.findAll(sortOrder)
            : programRepository.findByTitleContainingIgnoreCase(q, sortOrder);

        model.addAttribute("programs", programs);
        model.addAttribute("q", q);
        model.addAttribute("sort", sort);
        return "programs/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("program", new Program());
        return "programs/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Program program = programRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Программа не найдена"));
        model.addAttribute("program", program);
        return "programs/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("program") Program program,
                       BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "programs/form";
        }

        if (program.getEndDate() != null && program.getStartDate() != null
            && program.getEndDate().isBefore(program.getStartDate())) {
            bindingResult.rejectValue("endDate", "invalid.endDate", "Дата окончания не может быть раньше даты начала");
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
}
