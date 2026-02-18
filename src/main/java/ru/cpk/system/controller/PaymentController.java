package ru.cpk.system.controller;

import jakarta.validation.Valid;
import java.time.LocalDate;
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
import ru.cpk.system.model.Payment;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.PaymentRepository;

@Controller
@RequestMapping("/payments")
public class PaymentController {

    private static final Set<String> ALLOWED_SORTS = Set.of("paymentDate", "amount", "status");

    private final PaymentRepository paymentRepository;
    private final ApplicationRepository applicationRepository;

    public PaymentController(PaymentRepository paymentRepository,
                             ApplicationRepository applicationRepository) {
        this.paymentRepository = paymentRepository;
        this.applicationRepository = applicationRepository;
    }

    @GetMapping
    public String list(@RequestParam(defaultValue = "paymentDate") String sort, Model model) {
        model.addAttribute("payments", paymentRepository.findAll(buildSort(sort)));
        model.addAttribute("sort", sort);
        return "payments/list";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/new")
    public String createForm(Model model) {
        Payment payment = new Payment();
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus(PaymentStatus.PENDING);
        model.addAttribute("payment", payment);
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
        return "payments/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Payment payment = paymentRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Платеж не найден"));
        model.addAttribute("payment", payment);
        model.addAttribute("statuses", PaymentStatus.values());
        model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
        return "payments/form";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/save")
    public String save(@Valid @ModelAttribute("payment") Payment payment,
                       BindingResult bindingResult,
                       Model model) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("statuses", PaymentStatus.values());
            model.addAttribute("applications", applicationRepository.findAll(Sort.by("id")));
            return "payments/form";
        }

        paymentRepository.save(payment);
        return "redirect:/payments";
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'METHODIST')")
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        paymentRepository.deleteById(id);
        return "redirect:/payments";
    }

    private Sort buildSort(String sort) {
        String selectedSort = ALLOWED_SORTS.contains(sort) ? sort : "paymentDate";
        return Sort.by(Sort.Direction.ASC, selectedSort);
    }
}
