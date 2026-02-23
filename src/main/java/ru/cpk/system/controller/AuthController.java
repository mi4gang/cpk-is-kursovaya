package ru.cpk.system.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import ru.cpk.system.model.RoleName;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.UserRepository;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String loginPage(Authentication authentication) {
        if (authentication != null
            && authentication.isAuthenticated()
            && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/dashboard";
        }
        return "login";
    }

    @GetMapping("/register")
    public String registerForm(Model model) {
        model.addAttribute("form", new StudentRegistrationForm());
        return "register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("form") StudentRegistrationForm form,
                           BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return "register";
        }

        if (!form.getPassword().equals(form.getConfirmPassword())) {
            bindingResult.rejectValue("confirmPassword", "password.mismatch", "Пароли не совпадают");
            return "register";
        }

        if (userRepository.findByUsername(form.getUsername()).isPresent()) {
            bindingResult.rejectValue("username", "username.exists", "Логин уже занят");
            return "register";
        }

        if (userRepository.findByEmail(form.getEmail()).isPresent()) {
            bindingResult.rejectValue("email", "email.exists", "Email уже зарегистрирован");
            return "register";
        }

        User student = new User();
        student.setUsername(form.getUsername().trim());
        student.setPassword(passwordEncoder.encode(form.getPassword()));
        student.setFullName(form.getFullName().trim());
        student.setEmail(form.getEmail().trim().toLowerCase());
        student.setPhone(form.getPhone().trim());
        student.setRole(RoleName.STUDENT);
        student.setEnabled(true);
        userRepository.save(student);

        return "redirect:/login?registered";
    }

    public static class StudentRegistrationForm {

        @NotBlank(message = "Укажите логин")
        private String username;

        @NotBlank(message = "Укажите пароль")
        private String password;

        @NotBlank(message = "Подтвердите пароль")
        private String confirmPassword;

        @NotBlank(message = "Укажите ФИО")
        private String fullName;

        @Email(message = "Некорректный email")
        @NotBlank(message = "Укажите email")
        private String email;

        @NotBlank(message = "Укажите телефон")
        private String phone;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
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
}
