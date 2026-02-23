package ru.cpk.system.controller;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class ConsultationController {

    @GetMapping("/consultation")
    public String consultationForm() {
        return "redirect:/#consultation";
    }

    @PostMapping("/consultation")
    public String submitConsultation(@Valid @ModelAttribute("form") ConsultationForm form,
                                     BindingResult bindingResult,
                                     RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("consultationError",
                "Заявка не отправлена. Проверьте обязательные поля и попробуйте снова.");
            redirectAttributes.addFlashAttribute("form", form);
            return "redirect:/#consultation";
        }

        redirectAttributes.addFlashAttribute("consultationSuccess", true);
        redirectAttributes.addFlashAttribute("submittedName", form.getFullName());
        redirectAttributes.addFlashAttribute("form", new ConsultationForm());
        return "redirect:/#consultation";
    }

    public static class ConsultationForm {

        @NotBlank(message = "Укажите имя")
        private String fullName;

        @NotBlank(message = "Укажите телефон")
        private String phone;

        @NotBlank(message = "Укажите email")
        @Email(message = "Некорректный email")
        private String email;

        @NotBlank(message = "Укажите интересующее направление")
        private String interest;

        private String comment;

        public String getFullName() {
            return fullName;
        }

        public void setFullName(String fullName) {
            this.fullName = fullName;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getInterest() {
            return interest;
        }

        public void setInterest(String interest) {
            this.interest = interest;
        }

        public String getComment() {
            return comment;
        }

        public void setComment(String comment) {
            this.comment = comment;
        }
    }
}
