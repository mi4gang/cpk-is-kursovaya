package ru.cpk.system.controller;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.ProgramRepository;
import ru.cpk.system.service.StatsService;

@Controller
public class HomeController {

    private final StatsService statsService;
    private final ProgramRepository programRepository;
    private final ApplicationRepository applicationRepository;

    public HomeController(StatsService statsService,
                          ProgramRepository programRepository,
                          ApplicationRepository applicationRepository) {
        this.statsService = statsService;
        this.programRepository = programRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("totalListeners", statsService.totalListeners());
        model.addAttribute("activePrograms", statsService.activePrograms());
        model.addAttribute("paidAmount", statsService.paidAmount());
        model.addAttribute("programs", programRepository.findAll(Sort.by(Sort.Direction.ASC, "title")));
        model.addAttribute("recentApplications", applicationRepository.findAll(Sort.by(Sort.Direction.DESC, "id")));
        return "home/index";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
