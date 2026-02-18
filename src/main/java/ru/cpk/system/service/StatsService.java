package ru.cpk.system.service;

import java.math.BigDecimal;
import org.springframework.stereotype.Service;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.PaymentRepository;
import ru.cpk.system.repository.ProgramRepository;

@Service
public class StatsService {

    private final ApplicationRepository applicationRepository;
    private final ProgramRepository programRepository;
    private final PaymentRepository paymentRepository;

    public StatsService(ApplicationRepository applicationRepository,
                        ProgramRepository programRepository,
                        PaymentRepository paymentRepository) {
        this.applicationRepository = applicationRepository;
        this.programRepository = programRepository;
        this.paymentRepository = paymentRepository;
    }

    public long totalListeners() {
        return applicationRepository.count();
    }

    public long activePrograms() {
        return programRepository.countByActiveTrue();
    }

    public BigDecimal paidAmount() {
        return paymentRepository.sumPaidAmounts();
    }
}
