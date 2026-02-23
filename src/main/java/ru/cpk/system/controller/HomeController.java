package ru.cpk.system.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ProgramRepository;
import ru.cpk.system.repository.UserRepository;

@Controller
public class HomeController {

    private final ProgramRepository programRepository;
    private final UserRepository userRepository;

    public HomeController(ProgramRepository programRepository,
                          UserRepository userRepository) {
        this.programRepository = programRepository;
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public String publicHome(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()) {
            User user = userRepository.findByUsername(authentication.getName()).orElse(null);
            if (user != null) {
                return "redirect:/dashboard";
            }
        }
        model.addAttribute("programs", programRepository.findTop6ByActiveTrueOrderByStartDateAsc());
        if (!model.containsAttribute("form")) {
            model.addAttribute("form", new ConsultationController.ConsultationForm());
        }
        return "home/public";
    }

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        User user = userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Пользователь не найден"));

        if (user.getRole() == RoleName.ADMIN) {
            return "redirect:/admin/dashboard-v2";
        }
        if (user.getRole() == RoleName.METHODIST) {
            return "redirect:/methodist/queue";
        }
        if (user.getRole() == RoleName.TEACHER) {
            return "redirect:/teacher/groups";
        }
        if (user.getRole() == RoleName.STUDENT) {
            return "redirect:/student/cabinet";
        }

        return "redirect:/";
    }

    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
