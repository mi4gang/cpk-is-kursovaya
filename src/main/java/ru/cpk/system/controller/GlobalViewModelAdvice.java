package ru.cpk.system.controller;

import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.UserRepository;

@Component
@ControllerAdvice
public class GlobalViewModelAdvice {

    private final UserRepository userRepository;

    public GlobalViewModelAdvice(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @ModelAttribute("teacherDisplayName")
    public String teacherDisplayName(Authentication authentication) {
        if (authentication == null) {
            return null;
        }
        Optional<User> userOptional = userRepository.findByUsername(authentication.getName());
        if (userOptional.isEmpty()) {
            return null;
        }
        User user = userOptional.get();
        if (user.getRole() != RoleName.TEACHER) {
            return null;
        }
        if (user.getFullName() != null && !user.getFullName().isBlank()) {
            return user.getFullName();
        }
        return user.getUsername();
    }
}
