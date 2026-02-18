package ru.cpk.system.repository;

import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import ru.cpk.system.model.Payment;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("select coalesce(sum(p.amount), 0) from Payment p where p.status = ru.cpk.system.model.PaymentStatus.PAID")
    BigDecimal sumPaidAmounts();
}
