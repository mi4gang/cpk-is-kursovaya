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
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

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
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate applicationDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DocumentStatus docStatus = DocumentStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AccessStatus accessStatus = AccessStatus.NO_ACCESS;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate trialEndsAt;

    @Column
    private Boolean trialWasUsed = false;

    @Min(0)
    @Max(100)
    @Column(nullable = false)
    private int progressPercent = 0;

    @Column(nullable = false)
    private boolean teacherCompleted = false;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "program_id", nullable = false)
    private Program program;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "student_id", nullable = false)
    private User student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_teacher_id")
    private User assignedTeacher;

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

    public DocumentStatus getDocStatus() {
        return docStatus;
    }

    public void setDocStatus(DocumentStatus docStatus) {
        this.docStatus = docStatus;
    }

    public AccessStatus getAccessStatus() {
        return accessStatus;
    }

    public void setAccessStatus(AccessStatus accessStatus) {
        this.accessStatus = accessStatus;
    }

    public LocalDate getTrialEndsAt() {
        return trialEndsAt;
    }

    public void setTrialEndsAt(LocalDate trialEndsAt) {
        this.trialEndsAt = trialEndsAt;
    }

    public Boolean getTrialWasUsed() {
        return trialWasUsed;
    }

    public void setTrialWasUsed(Boolean trialWasUsed) {
        this.trialWasUsed = trialWasUsed;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public boolean isTeacherCompleted() {
        return teacherCompleted;
    }

    public void setTeacherCompleted(boolean teacherCompleted) {
        this.teacherCompleted = teacherCompleted;
    }

    public LocalDate getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDate completedAt) {
        this.completedAt = completedAt;
    }

    public Program getProgram() {
        return program;
    }

    public void setProgram(Program program) {
        this.program = program;
    }

    public User getStudent() {
        return student;
    }

    public void setStudent(User student) {
        this.student = student;
    }

    public User getAssignedTeacher() {
        return assignedTeacher;
    }

    public void setAssignedTeacher(User assignedTeacher) {
        this.assignedTeacher = assignedTeacher;
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
