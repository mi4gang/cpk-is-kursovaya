package ru.cpk.system.controller;

import jakarta.validation.ConstraintViolationException;
import org.springframework.ui.Model;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BindException.class, MethodArgumentNotValidException.class, ConstraintViolationException.class, IllegalArgumentException.class})
    public String handleValidation(Exception ex, Model model) {
        model.addAttribute("message", "Произошла ошибка. Проверьте данные и вернитесь назад.");
        model.addAttribute("details", ex.getMessage());
        return "error/general";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneral(Exception ex, Model model) {
        model.addAttribute("message", "Произошла ошибка. Проверьте данные и вернитесь назад.");
        model.addAttribute("details", ex.getMessage());
        return "error/general";
    }
}
