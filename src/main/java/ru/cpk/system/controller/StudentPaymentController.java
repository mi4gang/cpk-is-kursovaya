package ru.cpk.system.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.DocumentStatus;
import ru.cpk.system.model.Payment;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.model.User;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.PaymentRepository;
import ru.cpk.system.repository.UserRepository;
import ru.cpk.system.service.ApplicationWorkflowService;

@Controller
@RequestMapping("/student/payments")
@PreAuthorize("hasRole('STUDENT')")
public class StudentPaymentController {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final PaymentRepository paymentRepository;
    private final ApplicationWorkflowService workflowService;

    public StudentPaymentController(UserRepository userRepository,
                                    ApplicationRepository applicationRepository,
                                    PaymentRepository paymentRepository,
                                    ApplicationWorkflowService workflowService) {
        this.userRepository = userRepository;
        this.applicationRepository = applicationRepository;
        this.paymentRepository = paymentRepository;
        this.workflowService = workflowService;
    }

    @GetMapping("/{applicationId}/checkout")
    public String checkout(@PathVariable Long applicationId,
                           Authentication authentication,
                           Model model) {
        User student = currentStudent(authentication);
        Application application = resolveStudentApplication(applicationId, student);
        ensureDocsApproved(application);

        Payment payment = paymentRepository.findByApplicationId(applicationId).orElse(null);
        BigDecimal amount = payment != null ? payment.getAmount() : defaultAmount(application);
        boolean alreadyPaid = payment != null && payment.getStatus() == PaymentStatus.PAID;

        model.addAttribute("studentApplication", application);
        model.addAttribute("amount", amount);
        model.addAttribute("alreadyPaid", alreadyPaid);
        return "student/payment-checkout";
    }

    @PostMapping("/{applicationId}/pay")
    public String pay(@PathVariable Long applicationId,
                      Authentication authentication) {
        User student = currentStudent(authentication);
        Application application = resolveStudentApplication(applicationId, student);
        ensureDocsApproved(application);

        Payment payment = paymentRepository.findByApplicationId(applicationId).orElseGet(() -> {
            Payment created = new Payment();
            created.setApplication(application);
            created.setAmount(defaultAmount(application));
            return created;
        });

        if (payment.getAmount() == null || payment.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            payment.setAmount(defaultAmount(application));
        }
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus(PaymentStatus.PAID);

        Payment savedPayment = paymentRepository.save(payment);
        workflowService.syncAfterPayment(savedPayment);
        return "redirect:/applications?paid=1";
    }

    private User currentStudent(Authentication authentication) {
        return userRepository.findByUsername(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Слушатель не найден"));
    }

    private Application resolveStudentApplication(Long applicationId, User student) {
        return applicationRepository.findByIdAndStudentId(applicationId, student.getId())
            .orElseThrow(() -> new IllegalArgumentException("Заявка не найдена или не принадлежит слушателю"));
    }

    private void ensureDocsApproved(Application application) {
        if (application.getDocStatus() != DocumentStatus.APPROVED) {
            throw new IllegalArgumentException("Оплата доступна только после подтверждения документов");
        }
    }

    private BigDecimal defaultAmount(Application application) {
        int hours = application.getProgram().getDurationHours();
        if (hours <= 48) {
            return new BigDecimal("18000.00");
        }
        if (hours <= 96) {
            return new BigDecimal("25000.00");
        }
        return new BigDecimal("42000.00");
    }
}
