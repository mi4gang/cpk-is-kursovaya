package ru.cpk.system.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

@Entity
@Table(name = "applications")
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String listenerFullName;

    @Email
    @NotBlank
    @Column(nullable = false)
    private String listenerEmail;

    @NotBlank
    @Column(nullable = false)
    private String listenerPhone;

    @Column(nullable = false)
    private LocalDate applicationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @OneToOne(mappedBy = "application", fetch = FetchType.LAZY)
    private Payment payment;

    @OneToOne(mappedBy = "application", fetch = FetchType.LAZY)
    private AssessmentResult assessmentResult;

    @OneToOne(mappedBy = "application", fetch = FetchType.LAZY)
    private Certificate certificate;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getListenerFullName() {
        return listenerFullName;
    }

    public void setListenerFullName(String listenerFullName) {
        this.listenerFullName = listenerFullName;
    }

    public String getListenerEmail() {
        return listenerEmail;
    }

    public void setListenerEmail(String listenerEmail) {
        this.listenerEmail = listenerEmail;
    }

    public String getListenerPhone() {
        return listenerPhone;
    }

    public void setListenerPhone(String listenerPhone) {
        this.listenerPhone = listenerPhone;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public AssessmentResult getAssessmentResult() {
        return assessmentResult;
    }

    public void setAssessmentResult(AssessmentResult assessmentResult) {
        this.assessmentResult = assessmentResult;
    }

    public Certificate getCertificate() {
        return certificate;
    }

    public void setCertificate(Certificate certificate) {
        this.certificate = certificate;
    }
}
