package ru.cpk.system.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.cpk.system.model.AccessStatus;
import ru.cpk.system.model.Application;
import ru.cpk.system.model.ApplicationStatus;
import ru.cpk.system.model.AssessmentStatus;
import ru.cpk.system.model.CertificateStatus;
import ru.cpk.system.model.DocumentStatus;
import ru.cpk.system.model.PaymentStatus;
import ru.cpk.system.model.ProgramType;
import ru.cpk.system.repository.ApplicationRepository;
import ru.cpk.system.repository.CertificateRepository;

@Service
@Transactional(readOnly = true)
public class AdminDashboardV2Service {

    public static final String QUEUE_ALL = "all";
    public static final String QUEUE_DOCS_PENDING = "docs_pending";
    public static final String QUEUE_ACCESS_BLOCKED = "access_blocked";
    public static final String QUEUE_TRIAL_ACCESS = "trial_access";
    public static final String QUEUE_IN_TRAINING = "in_training";
    public static final String QUEUE_CERTIFICATE_BLOCKED = "certificate_blocked";
    public static final String QUEUE_APPROVED_UNPAID = "approved_unpaid";
    public static final String QUEUE_COMPLETED = "completed";

    public static final String CERTIFICATE_STATE_ALL = "all";
    public static final String CERTIFICATE_STATE_ISSUED = "issued";
    public static final String CERTIFICATE_STATE_NOT_ISSUED = "not_issued";

    public static final String RISK_ALL = "all";
    public static final String RISK_CRITICAL = "critical";
    public static final String RISK_HIGH = "high";
    public static final String RISK_MEDIUM = "medium";
    public static final String RISK_LOW = "low";

    private static final Set<String> QUEUE_TYPES = Set.of(
        QUEUE_ALL,
        QUEUE_DOCS_PENDING,
        QUEUE_ACCESS_BLOCKED,
        QUEUE_TRIAL_ACCESS,
        QUEUE_IN_TRAINING,
        QUEUE_CERTIFICATE_BLOCKED,
        QUEUE_APPROVED_UNPAID,
        QUEUE_COMPLETED
    );
    private static final Set<String> RISK_LEVELS = Set.of(
        RISK_ALL,
        RISK_CRITICAL,
        RISK_HIGH,
        RISK_MEDIUM,
        RISK_LOW
    );
    private static final Set<String> CERTIFICATE_STATES = Set.of(
        CERTIFICATE_STATE_ALL,
        CERTIFICATE_STATE_ISSUED,
        CERTIFICATE_STATE_NOT_ISSUED
    );

    private final ApplicationRepository applicationRepository;
    private final CertificateRepository certificateRepository;
    private final StatsService statsService;

    public AdminDashboardV2Service(ApplicationRepository applicationRepository,
                                   CertificateRepository certificateRepository,
                                   StatsService statsService) {
        this.applicationRepository = applicationRepository;
        this.certificateRepository = certificateRepository;
        this.statsService = statsService;
    }

    public DashboardV2View buildDashboardView() {
        List<Application> applications = applicationRepository.findAll();
        LocalDate today = LocalDate.now();

        List<DrilldownRow> allRows = toRows(applications, today);
        List<DrilldownRow> docsRows = filterByQueue(allRows, QUEUE_DOCS_PENDING);
        List<DrilldownRow> accessBlockedRows = filterByQueue(allRows, QUEUE_ACCESS_BLOCKED);
        List<DrilldownRow> trialRows = filterByQueue(allRows, QUEUE_TRIAL_ACCESS);
        List<DrilldownRow> inTrainingRows = filterByQueue(allRows, QUEUE_IN_TRAINING);
        List<DrilldownRow> certRows = filterByQueue(allRows, QUEUE_CERTIFICATE_BLOCKED);
        List<DrilldownRow> approvedUnpaidRows = filterByQueue(allRows, QUEUE_APPROVED_UNPAID);

        List<SystemHealthKpi> systemHealth = List.of(
            new SystemHealthKpi("Неподтвержденные документы", docsRows.size(),
                "Требуют решения методиста", drilldownUrl(QUEUE_DOCS_PENDING)),
            new SystemHealthKpi("Блок доступа после оплаты", accessBlockedRows.size(),
                "Оплачено, но доступ не открыт", drilldownUrl(QUEUE_ACCESS_BLOCKED)),
            new SystemHealthKpi("Подтверждены, но не оплачены", approvedUnpaidRows.size(),
                "Кандидаты на дожим оплаты", drilldownUrl(QUEUE_APPROVED_UNPAID)),
            new SystemHealthKpi("В обучении", inTrainingRows.size(),
                "Активный учебный контур", drilldownUrl(QUEUE_IN_TRAINING)),
            new SystemHealthKpi("Готовы к удостоверению", certRows.size(),
                "Нужна выдача удостоверения", drilldownUrl(QUEUE_CERTIFICATE_BLOCKED)),
            new SystemHealthKpi("Пробный доступ активен", trialRows.size(),
                "Идет trial-период (3 дня)", drilldownUrl(QUEUE_TRIAL_ACCESS))
        );

        List<FunnelStage> funnel = buildFunnel(docsRows, accessBlockedRows, trialRows, inTrainingRows, certRows);
        FinanceSummary finance = buildFinanceSummary(applications, approvedUnpaidRows.size());
        List<BottleneckRow> bottlenecks = buildBottlenecks(docsRows, accessBlockedRows, trialRows, approvedUnpaidRows, certRows);
        List<TeacherRiskRow> teacherRisks = buildTeacherRisks(allRows);
        List<ActionItemRow> actionItems = buildActionItems(allRows);
        List<ProgramAnalyticsRow> programAnalytics = buildProgramAnalytics(applications);
        List<ContingentShareRow> contingent = buildContingentStructure(applications);
        CompletedSummary completedSummary = buildCompletedSummary(today.minusDays(30), today);

        return new DashboardV2View(
            systemHealth,
            finance,
            funnel,
            bottlenecks,
            teacherRisks,
            actionItems,
            programAnalytics,
            contingent,
            completedSummary
        );
    }

    public DrilldownView buildDrilldownView(String queueType,
                                            Long teacherId,
                                            boolean priorityOnly,
                                            Integer minAgeDays,
                                            String riskLevel,
                                            String sort) {
        String normalizedQueueType = normalizeQueueType(queueType);
        int normalizedMinAge = minAgeDays == null ? 0 : Math.max(minAgeDays, 0);
        String normalizedRiskLevel = normalizeRiskLevel(riskLevel);
        String normalizedSort = normalizeSort(sort);

        List<DrilldownRow> rows = toRows(applicationRepository.findAll(), LocalDate.now());
        rows = filterByQueue(rows, normalizedQueueType);
        if (teacherId != null) {
            rows = rows.stream()
                .filter(row -> Objects.equals(row.teacherId(), teacherId))
                .toList();
        }
        if (normalizedMinAge > 0) {
            rows = rows.stream()
                .filter(row -> row.ageDays() >= normalizedMinAge)
                .toList();
        }
        if (!RISK_ALL.equals(normalizedRiskLevel)) {
            rows = rows.stream()
                .filter(row -> normalizedRiskLevel.equals(row.priorityLevelKey()))
                .toList();
        }
        if (priorityOnly) {
            rows = rows.stream()
                .filter(row -> RISK_CRITICAL.equals(row.priorityLevelKey()) || RISK_HIGH.equals(row.priorityLevelKey()))
                .toList();
        }

        rows = sortRows(rows, normalizedSort);
        return new DrilldownView(
            normalizedQueueType,
            queueTypeLabel(normalizedQueueType),
            teacherId,
            priorityOnly,
            normalizedMinAge,
            normalizedRiskLevel,
            normalizedSort,
            rows,
            queueRegistryUrl(normalizedQueueType)
        );
    }

    public CaseView buildCaseView(Long applicationId) {
        Application application = applicationRepository.findById(applicationId)
            .orElseThrow(() -> new IllegalArgumentException("Кейс не найден"));
        LocalDate today = LocalDate.now();
        int ageDays = (int) Math.max(0, ChronoUnit.DAYS.between(application.getApplicationDate(), today));
        String resolvedQueueType = resolveQueueType(application);
        String queueType = resolvedQueueType != null
            ? resolvedQueueType
            : (application.getStatus() == ApplicationStatus.COMPLETED ? QUEUE_COMPLETED : QUEUE_ALL);
        PriorityDetails priority = calculatePriority(application, queueType, ageDays);
        boolean paid = application.getPayment() != null && application.getPayment().getStatus() == PaymentStatus.PAID;

        return new CaseView(
            application.getId(),
            application.getListenerFullName(),
            application.getProgram() != null ? application.getProgram().getTitle() : "—",
            queueType,
            queueTypeLabel(queueType),
            queueReason(application, queueType),
            ageDays,
            priority.score(),
            priority.levelKey(),
            priority.levelLabel(),
            priority.explanation(),
            application.getPayment() != null ? application.getPayment().getStatus().getLabel() : "Нет записи",
            application.getPayment() != null ? application.getPayment().getAmount() : BigDecimal.ZERO,
            application.getPayment() != null ? application.getPayment().getPaymentDate() : null,
            paid,
            application.getAssignedTeacher() != null ? application.getAssignedTeacher().getId() : null,
            application.getAssignedTeacher() != null ? application.getAssignedTeacher().getFullName() : "Не назначен",
            application.getProgressPercent(),
            application.getAssessmentResult() != null ? application.getAssessmentResult().getStatus().getLabel() : "Нет результата",
            application.getAssessmentResult() != null ? application.getAssessmentResult().getScore() : null,
            application.getAssessmentResult() != null ? application.getAssessmentResult().getStatus() == AssessmentStatus.PASSED : false,
            application.getCertificate() != null ? application.getCertificate().getStatus().getLabel() : "Не выдано",
            application.getCertificate() != null ? application.getCertificate().getNumber() : null,
            application.getCertificate() != null ? application.getCertificate().getIssueDate() : null,
            application.getCertificate() != null && application.getCertificate().getStatus() == CertificateStatus.ISSUED,
            "/applications/" + application.getId() + "/edit",
            application.getPayment() != null
                ? "/payments/" + application.getPayment().getId() + "/edit"
                : "/payments",
            application.getAssessmentResult() != null
                ? "/assessments/" + application.getAssessmentResult().getId() + "/edit"
                : "/assessments/new?applicationId=" + application.getId(),
            application.getCertificate() != null
                ? "/certificates/" + application.getCertificate().getId() + "/edit"
                : "/certificates/new?applicationId=" + application.getId()
        );
    }

    public CompletedDrilldownView buildCompletedDrilldownView(LocalDate dateFrom,
                                                              LocalDate dateTo,
                                                              String certificateState) {
        LocalDate normalizedFrom = dateFrom == null ? LocalDate.now().minusDays(30) : dateFrom;
        LocalDate normalizedTo = dateTo == null ? LocalDate.now() : dateTo;
        if (normalizedFrom.isAfter(normalizedTo)) {
            LocalDate temp = normalizedFrom;
            normalizedFrom = normalizedTo;
            normalizedTo = temp;
        }
        String normalizedCertificateState = normalizeCertificateState(certificateState);
        List<Application> issuedApplications = certificateRepository
            .findByStatusAndIssueDateBetweenOrderByIssueDateDesc(CertificateStatus.ISSUED, normalizedFrom, normalizedTo)
            .stream()
            .map(certificate -> certificate.getApplication())
            .toList();
        List<Application> pendingCertificateApplications = applicationRepository
            .findByStatusAndCompletedAtBetweenOrderByCompletedAtDesc(
                ApplicationStatus.COMPLETED,
                normalizedFrom,
                normalizedTo
            ).stream()
            .filter(application -> application.getCertificate() == null)
            .toList();

        List<CompletedRow> rows = switch (normalizedCertificateState) {
            case CERTIFICATE_STATE_NOT_ISSUED -> pendingCertificateApplications.stream()
                .map(application -> toCompletedRow(application, application.getCompletedAt(), false))
                .toList();
            case CERTIFICATE_STATE_ALL -> {
                List<CompletedRow> combined = new ArrayList<>();
                combined.addAll(issuedApplications.stream()
                    .map(application -> toCompletedRow(application, application.getCertificate().getIssueDate(), true))
                    .toList());
                combined.addAll(pendingCertificateApplications.stream()
                    .map(application -> toCompletedRow(application, application.getCompletedAt(), false))
                    .toList());
                yield combined.stream()
                    .sorted(Comparator.comparing(CompletedRow::closedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                    .toList();
            }
            default -> issuedApplications.stream()
                .map(application -> toCompletedRow(application, application.getCertificate().getIssueDate(), true))
                .toList();
        };

        long issuedCount = rows.stream().filter(CompletedRow::certificateIssued).count();
        long notIssuedCount = rows.size() - issuedCount;
        BigDecimal paidAmount = rows.stream()
            .filter(CompletedRow::paid)
            .map(CompletedRow::paymentAmount)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CompletedDrilldownView(
            normalizedFrom,
            normalizedTo,
            normalizedCertificateState,
            rows,
            rows.size(),
            issuedCount,
            notIssuedCount,
            paidAmount
        );
    }

    private CompletedSummary buildCompletedSummary(LocalDate dateFrom, LocalDate dateTo) {
        long completedCount = certificateRepository.countByStatusAndIssueDateBetween(
            CertificateStatus.ISSUED,
            dateFrom,
            dateTo
        );
        long issuedCount = completedCount;
        long notIssuedCount = applicationRepository.countByStatusAndCompletedAtBetweenAndCertificateIsNull(
            ApplicationStatus.COMPLETED,
            dateFrom,
            dateTo
        );
        BigDecimal paidAmount = certificateRepository.findByStatusAndIssueDateBetweenOrderByIssueDateDesc(
                CertificateStatus.ISSUED,
                dateFrom,
                dateTo
            ).stream()
            .map(certificate -> certificate.getApplication())
            .filter(application -> application != null
                && application.getPayment() != null
                && application.getPayment().getStatus() == PaymentStatus.PAID
                && application.getPayment().getAmount() != null)
            .map(application -> application.getPayment().getAmount())
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CompletedSummary(
            dateFrom,
            dateTo,
            completedCount,
            issuedCount,
            notIssuedCount,
            paidAmount,
            "/admin/dashboard-v2/completed?dateFrom=" + dateFrom + "&dateTo=" + dateTo + "&certificateState=issued"
        );
    }

    public List<QueueOption> queueOptions() {
        return List.of(
            new QueueOption(QUEUE_ALL, "Все очереди"),
            new QueueOption(QUEUE_DOCS_PENDING, "Документы"),
            new QueueOption(QUEUE_ACCESS_BLOCKED, "Передача/доступ"),
            new QueueOption(QUEUE_APPROVED_UNPAID, "Подтверждено, но не оплачено"),
            new QueueOption(QUEUE_TRIAL_ACCESS, "Пробный доступ"),
            new QueueOption(QUEUE_IN_TRAINING, "Обучение"),
            new QueueOption(QUEUE_CERTIFICATE_BLOCKED, "Удостоверения")
        );
    }

    public List<RiskOption> riskOptions() {
        return List.of(
            new RiskOption(RISK_ALL, "Все уровни"),
            new RiskOption(RISK_CRITICAL, "Критичный"),
            new RiskOption(RISK_HIGH, "Высокий"),
            new RiskOption(RISK_MEDIUM, "Средний"),
            new RiskOption(RISK_LOW, "Низкий")
        );
    }

    public List<SortOption> sortOptions() {
        return List.of(
            new SortOption("priority_desc", "Сначала критичные"),
            new SortOption("age_desc", "Сначала старые"),
            new SortOption("age_asc", "Сначала новые")
        );
    }

    private List<FunnelStage> buildFunnel(List<DrilldownRow> docsRows,
                                          List<DrilldownRow> accessBlockedRows,
                                          List<DrilldownRow> trialRows,
                                          List<DrilldownRow> inTrainingRows,
                                          List<DrilldownRow> certRows) {
        long total = docsRows.size() + accessBlockedRows.size() + trialRows.size() + inTrainingRows.size() + certRows.size();
        return List.of(
            toFunnelStage("Документы", docsRows.size(), QUEUE_DOCS_PENDING, total),
            toFunnelStage("Передача/доступ", accessBlockedRows.size(), QUEUE_ACCESS_BLOCKED, total),
            toFunnelStage("Пробный доступ", trialRows.size(), QUEUE_TRIAL_ACCESS, total),
            toFunnelStage("Обучение", inTrainingRows.size(), QUEUE_IN_TRAINING, total),
            toFunnelStage("Удостоверения", certRows.size(), QUEUE_CERTIFICATE_BLOCKED, total)
        );
    }

    private FunnelStage toFunnelStage(String title, long count, String queueType, long total) {
        long sharePercent = total == 0 ? 0 : Math.round((count * 100.0) / total);
        return new FunnelStage(title, count, sharePercent, queueType, drilldownUrl(queueType));
    }

    private FinanceSummary buildFinanceSummary(List<Application> applications, long squeezeCount) {
        BigDecimal paidAmount = BigDecimal.ZERO;
        BigDecimal pendingAmount = BigDecimal.ZERO;
        BigDecimal failedAmount = BigDecimal.ZERO;

        for (Application application : applications) {
            if (application.getPayment() == null || application.getPayment().getAmount() == null) {
                continue;
            }
            if (application.getPayment().getStatus() == PaymentStatus.PAID) {
                paidAmount = paidAmount.add(application.getPayment().getAmount());
            } else if (application.getPayment().getStatus() == PaymentStatus.PENDING) {
                pendingAmount = pendingAmount.add(application.getPayment().getAmount());
            } else if (application.getPayment().getStatus() == PaymentStatus.FAILED) {
                failedAmount = failedAmount.add(application.getPayment().getAmount());
            }
        }

        return new FinanceSummary(
            paidAmount,
            pendingAmount,
            failedAmount,
            squeezeCount,
            "/payments",
            "/payments",
            "/payments",
            "/applications?paymentState=UNPAID"
        );
    }

    private List<BottleneckRow> buildBottlenecks(List<DrilldownRow> docsRows,
                                                 List<DrilldownRow> accessBlockedRows,
                                                 List<DrilldownRow> trialRows,
                                                 List<DrilldownRow> approvedUnpaidRows,
                                                 List<DrilldownRow> certRows) {
        List<BottleneckRow> rows = List.of(
            toBottleneck(QUEUE_DOCS_PENDING, docsRows),
            toBottleneck(QUEUE_ACCESS_BLOCKED, accessBlockedRows),
            toBottleneck(QUEUE_APPROVED_UNPAID, approvedUnpaidRows),
            toBottleneck(QUEUE_TRIAL_ACCESS, trialRows),
            toBottleneck(QUEUE_CERTIFICATE_BLOCKED, certRows)
        );
        return rows.stream()
            .filter(row -> row.itemsCount() > 0)
            .sorted(Comparator.comparingLong(BottleneckRow::itemsCount).reversed()
                .thenComparingLong(BottleneckRow::maxAgeDays).reversed())
            .toList();
    }

    private BottleneckRow toBottleneck(String queueType, List<DrilldownRow> rows) {
        long count = rows.size();
        long avgAge = count == 0 ? 0 : Math.round(rows.stream().mapToLong(DrilldownRow::ageDays).average().orElse(0));
        long maxAge = rows.stream().mapToLong(DrilldownRow::ageDays).max().orElse(0);
        String riskKey = bottleneckRiskKey(count, maxAge);
        return new BottleneckRow(
            queueType,
            queueTypeLabel(queueType),
            count,
            avgAge,
            maxAge,
            riskKey,
            riskLabel(riskKey),
            nextAction(queueType),
            drilldownUrl(queueType)
        );
    }

    private String bottleneckRiskKey(long count, long maxAge) {
        if (maxAge >= 7 || count >= 10) {
            return RISK_CRITICAL;
        }
        if (maxAge >= 5 || count >= 6) {
            return RISK_HIGH;
        }
        if (maxAge >= 3 || count >= 3) {
            return RISK_MEDIUM;
        }
        return RISK_LOW;
    }

    private List<TeacherRiskRow> buildTeacherRisks(List<DrilldownRow> allRows) {
        Map<Long, TeacherRiskAccumulator> acc = new LinkedHashMap<>();
        for (StatsService.TeacherLoad load : statsService.teacherLoads()) {
            acc.put(load.teacherId(), new TeacherRiskAccumulator(
                load.teacherId(),
                load.teacherName(),
                load.currentStudents(),
                load.currentPrograms(),
                0,
                0
            ));
        }

        for (DrilldownRow row : allRows) {
            if (row.teacherId() == null) {
                continue;
            }
            TeacherRiskAccumulator current = acc.get(row.teacherId());
            if (current == null) {
                continue;
            }
            if (QUEUE_IN_TRAINING.equals(row.queueType())) {
                current.inTraining += 1;
            }
            if (QUEUE_ACCESS_BLOCKED.equals(row.queueType()) || QUEUE_APPROVED_UNPAID.equals(row.queueType())) {
                current.blocked += 1;
            }
        }

        return acc.values().stream()
            .map(item -> {
                long riskScore = item.currentStudents * 2L + item.currentPrograms * 3L + item.blocked * 5L;
                String riskKey = riskKeyByScore((int) riskScore);
                String explanation = "Слушатели (" + (item.currentStudents * 2L)
                    + ") + программы (" + (item.currentPrograms * 3L)
                    + ") + блокировки (" + (item.blocked * 5L) + ") = " + riskScore;
                return new TeacherRiskRow(
                    item.teacherId,
                    item.teacherName,
                    item.currentStudents,
                    item.currentPrograms,
                    item.inTraining,
                    item.blocked,
                    riskScore,
                    riskKey,
                    riskLabel(riskKey),
                    explanation,
                    drilldownUrl(QUEUE_IN_TRAINING) + "&teacherId=" + item.teacherId
                );
            })
            .sorted(Comparator.comparingLong(TeacherRiskRow::riskScore).reversed())
            .toList();
    }

    private List<ActionItemRow> buildActionItems(List<DrilldownRow> allRows) {
        return allRows.stream()
            .filter(row -> !QUEUE_IN_TRAINING.equals(row.queueType()))
            .sorted(Comparator.comparingInt(DrilldownRow::priorityScore).reversed()
                .thenComparingInt(DrilldownRow::ageDays).reversed())
            .limit(8)
            .map(row -> new ActionItemRow(
                row.applicationId(),
                row.listenerFullName(),
                row.programTitle(),
                row.queueTitle(),
                row.ageDays(),
                row.priorityScore(),
                row.priorityLevelKey(),
                row.priorityLevelLabel(),
                row.priorityExplanation(),
                row.nextAction(),
                row.nextActionUrl()
            ))
            .toList();
    }

    private List<ProgramAnalyticsRow> buildProgramAnalytics(List<Application> applications) {
        Map<Long, ProgramAnalyticsAccumulator> grouped = new LinkedHashMap<>();
        for (Application application : applications) {
            if (application.getProgram() == null) {
                continue;
            }
            Long programId = application.getProgram().getId();
            ProgramAnalyticsAccumulator accumulator = grouped.computeIfAbsent(programId, unused ->
                new ProgramAnalyticsAccumulator(
                    programId,
                    application.getProgram().getTitle(),
                    0,
                    0,
                    BigDecimal.ZERO
                ));

            accumulator.demandCount += 1;
            if (isActiveListener(application)) {
                accumulator.activeListeners += 1;
            }
            if (application.getPayment() != null
                && application.getPayment().getStatus() == PaymentStatus.PAID
                && application.getPayment().getAmount() != null) {
                accumulator.paidAmount = accumulator.paidAmount.add(application.getPayment().getAmount());
            }
        }

        return grouped.values().stream()
            .map(item -> new ProgramAnalyticsRow(
                item.programId,
                item.programTitle,
                item.demandCount,
                item.activeListeners,
                item.paidAmount,
                "/applications?programId=" + item.programId
            ))
            .sorted(Comparator.comparingLong(ProgramAnalyticsRow::demandCount).reversed()
                .thenComparing(ProgramAnalyticsRow::programTitle))
            .toList();
    }

    private List<ContingentShareRow> buildContingentStructure(List<Application> applications) {
        long advanced = 0;
        long retraining = 0;
        for (Application application : applications) {
            if (application.getProgram() == null || application.getProgram().getProgramType() == null) {
                continue;
            }
            if (application.getProgram().getProgramType() == ProgramType.ADVANCED_TRAINING) {
                advanced += 1;
            } else if (application.getProgram().getProgramType() == ProgramType.PROFESSIONAL_RETRAINING) {
                retraining += 1;
            }
        }
        long total = advanced + retraining;
        return List.of(
            new ContingentShareRow(
                ProgramType.ADVANCED_TRAINING.getLabel(),
                advanced,
                total == 0 ? 0 : Math.round(advanced * 100.0 / total)
            ),
            new ContingentShareRow(
                ProgramType.PROFESSIONAL_RETRAINING.getLabel(),
                retraining,
                total == 0 ? 0 : Math.round(retraining * 100.0 / total)
            )
        );
    }

    private boolean isActiveListener(Application application) {
        return application.getStatus() != ApplicationStatus.COMPLETED
            && application.getStatus() != ApplicationStatus.REJECTED;
    }

    private List<DrilldownRow> toRows(List<Application> applications, LocalDate today) {
        List<DrilldownRow> rows = new ArrayList<>();
        for (Application application : applications) {
            String queueType = resolveQueueType(application);
            if (queueType == null) {
                continue;
            }
            int ageDays = (int) Math.max(0, ChronoUnit.DAYS.between(application.getApplicationDate(), today));
            PriorityDetails priority = calculatePriority(application, queueType, ageDays);
            rows.add(new DrilldownRow(
                application.getId(),
                application.getListenerFullName(),
                application.getProgram() != null ? application.getProgram().getTitle() : "—",
                queueType,
                queueTypeLabel(queueType),
                queueReason(application, queueType),
                ageDays,
                priority.score(),
                priority.levelKey(),
                priority.levelLabel(),
                priority.explanation(),
                nextAction(application, queueType),
                nextActionUrl(application, queueType),
                application.getAssignedTeacher() != null ? application.getAssignedTeacher().getId() : null,
                application.getAssignedTeacher() != null ? application.getAssignedTeacher().getFullName() : "Не назначен"
            ));
        }
        return rows;
    }

    private PriorityDetails calculatePriority(Application application, String queueType, int ageDays) {
        int queueWeight = switch (queueType) {
            case QUEUE_DOCS_PENDING -> 100;
            case QUEUE_ACCESS_BLOCKED -> 95;
            case QUEUE_CERTIFICATE_BLOCKED -> 90;
            case QUEUE_APPROVED_UNPAID -> 85;
            case QUEUE_TRIAL_ACCESS -> 70;
            case QUEUE_IN_TRAINING -> 50;
            default -> 40;
        };

        int ageWeight = ageDays * 3;
        int score = queueWeight + ageWeight;
        List<String> extras = new ArrayList<>();

        boolean paid = application.getPayment() != null && application.getPayment().getStatus() == PaymentStatus.PAID;
        if (application.getDocStatus() == DocumentStatus.APPROVED && !paid) {
            score += 15;
            extras.add("нет оплаты (+15)");
        }
        if (QUEUE_ACCESS_BLOCKED.equals(queueType) && application.getAssignedTeacher() == null) {
            score += 20;
            extras.add("без преподавателя (+20)");
        }
        if (QUEUE_IN_TRAINING.equals(queueType) && application.getProgressPercent() < 25) {
            score += 10;
            extras.add("низкий прогресс (+10)");
        }

        String levelKey = riskKeyByScore(score);
        String levelLabel = riskLabel(levelKey);
        StringBuilder formula = new StringBuilder();
        formula.append(queueTypeLabel(queueType)).append(" (").append(queueWeight).append(")")
            .append(" + возраст ").append(ageDays).append("д (+").append(ageWeight).append(")");
        if (!extras.isEmpty()) {
            formula.append(" + ").append(String.join(" + ", extras));
        }
        formula.append(" = ").append(score);
        return new PriorityDetails(score, levelKey, levelLabel, formula.toString());
    }

    private String riskKeyByScore(int score) {
        if (score >= 120) {
            return RISK_CRITICAL;
        }
        if (score >= 100) {
            return RISK_HIGH;
        }
        if (score >= 85) {
            return RISK_MEDIUM;
        }
        return RISK_LOW;
    }

    private String riskLabel(String riskKey) {
        return switch (riskKey) {
            case RISK_CRITICAL -> "Критичный";
            case RISK_HIGH -> "Высокий";
            case RISK_MEDIUM -> "Средний";
            default -> "Низкий";
        };
    }

    private String resolveQueueType(Application application) {
        if (application.getDocStatus() == DocumentStatus.PENDING) {
            return QUEUE_DOCS_PENDING;
        }

        boolean paid = application.getPayment() != null && application.getPayment().getStatus() == PaymentStatus.PAID;
        boolean approved = application.getDocStatus() == DocumentStatus.APPROVED;
        boolean certificateBlocked = application.isTeacherCompleted()
            && application.getAssessmentResult() != null
            && application.getAssessmentResult().getStatus() == ru.cpk.system.model.AssessmentStatus.PASSED
            && application.getCertificate() == null;

        if (application.getAccessStatus() == AccessStatus.TRIAL_ACCESS) {
            return QUEUE_TRIAL_ACCESS;
        }
        if (approved && paid && application.getAccessStatus() != AccessStatus.FULL_ACCESS) {
            return QUEUE_ACCESS_BLOCKED;
        }
        if (approved && !paid) {
            return QUEUE_APPROVED_UNPAID;
        }
        if (certificateBlocked) {
            return QUEUE_CERTIFICATE_BLOCKED;
        }
        if (application.getAccessStatus() == AccessStatus.FULL_ACCESS
            && application.getStatus() != ApplicationStatus.COMPLETED
            && application.getStatus() != ApplicationStatus.REJECTED) {
            return QUEUE_IN_TRAINING;
        }
        return null;
    }

    private String queueReason(Application application, String queueType) {
        return switch (queueType) {
            case QUEUE_DOCS_PENDING -> "Ожидает проверку документов";
            case QUEUE_ACCESS_BLOCKED -> application.getAssignedTeacher() == null
                ? "Оплачено, но не назначен преподаватель"
                : "Оплачено, но полный доступ не открыт";
            case QUEUE_APPROVED_UNPAID -> "Документы подтверждены, оплата не зафиксирована";
            case QUEUE_TRIAL_ACCESS -> "Пробный доступ активен";
            case QUEUE_CERTIFICATE_BLOCKED -> "Аттестация пройдена, ожидает выдачи удостоверения";
            case QUEUE_IN_TRAINING -> "Активное обучение";
            case QUEUE_COMPLETED -> "Услуга оказана, кейс закрыт";
            default -> "Требуется операционное действие";
        };
    }

    private String nextAction(Application application, String queueType) {
        return "Открыть кейс";
    }

    private String nextAction(String queueType) {
        return switch (queueType) {
            case QUEUE_DOCS_PENDING -> "Проверить документы";
            case QUEUE_ACCESS_BLOCKED -> "Открыть полный доступ";
            case QUEUE_APPROVED_UNPAID -> "Проконтролировать оплату";
            case QUEUE_TRIAL_ACCESS -> "Контроль trial-периода";
            case QUEUE_CERTIFICATE_BLOCKED -> "Оформить удостоверение";
            case QUEUE_IN_TRAINING -> "Проверить прогресс";
            default -> "Проверить заявку";
        };
    }

    private String nextActionUrl(Application application, String queueType) {
        return caseUrl(application.getId());
    }

    private List<DrilldownRow> filterByQueue(List<DrilldownRow> rows, String queueType) {
        String normalizedQueueType = normalizeQueueType(queueType);
        if (QUEUE_ALL.equals(normalizedQueueType)) {
            return rows;
        }
        return rows.stream().filter(row -> normalizedQueueType.equals(row.queueType())).toList();
    }

    private List<DrilldownRow> sortRows(List<DrilldownRow> rows, String sort) {
        Comparator<DrilldownRow> comparator = switch (sort) {
            case "age_asc" -> Comparator.comparingInt(DrilldownRow::ageDays);
            case "age_desc" -> Comparator.comparingInt(DrilldownRow::ageDays).reversed();
            default -> Comparator.comparingInt(DrilldownRow::priorityScore).reversed()
                .thenComparingInt(DrilldownRow::ageDays).reversed();
        };
        return rows.stream().sorted(comparator).toList();
    }

    private String normalizeQueueType(String queueType) {
        if (queueType == null || queueType.isBlank()) {
            return QUEUE_ALL;
        }
        return QUEUE_TYPES.contains(queueType) ? queueType : QUEUE_ALL;
    }

    private String normalizeRiskLevel(String riskLevel) {
        if (riskLevel == null || riskLevel.isBlank()) {
            return RISK_ALL;
        }
        return RISK_LEVELS.contains(riskLevel) ? riskLevel : RISK_ALL;
    }

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "priority_desc";
        }
        if ("age_desc".equals(sort) || "age_asc".equals(sort)) {
            return sort;
        }
        return "priority_desc";
    }

    private String drilldownUrl(String queueType) {
        return "/admin/dashboard-v2/drilldown?queueType=" + queueType;
    }

    public String queueTypeLabel(String queueType) {
        return switch (queueType) {
            case QUEUE_DOCS_PENDING -> "Документы";
            case QUEUE_ACCESS_BLOCKED -> "Передача/доступ";
            case QUEUE_TRIAL_ACCESS -> "Пробный доступ";
            case QUEUE_IN_TRAINING -> "Обучение";
            case QUEUE_CERTIFICATE_BLOCKED -> "Удостоверения";
            case QUEUE_APPROVED_UNPAID -> "Подтверждено, но не оплачено";
            case QUEUE_COMPLETED -> "Завершено";
            default -> "Все очереди";
        };
    }

    private String caseUrl(Long applicationId) {
        return "/admin/dashboard-v2/case/" + applicationId;
    }

    private String queueRegistryUrl(String queueType) {
        return switch (queueType) {
            case QUEUE_DOCS_PENDING -> "/applications?docStatus=PENDING";
            case QUEUE_ACCESS_BLOCKED -> "/applications?docStatus=APPROVED&paymentState=PAID&accessStatus=NO_ACCESS";
            case QUEUE_APPROVED_UNPAID -> "/applications?docStatus=APPROVED&paymentState=UNPAID";
            case QUEUE_TRIAL_ACCESS -> "/applications?accessStatus=TRIAL_ACCESS";
            case QUEUE_IN_TRAINING -> "/applications?accessStatus=FULL_ACCESS";
            case QUEUE_CERTIFICATE_BLOCKED -> "/certificates";
            case QUEUE_COMPLETED -> "/applications?appStatus=COMPLETED";
            default -> "/applications";
        };
    }

    private String normalizeCertificateState(String certificateState) {
        if (certificateState == null || certificateState.isBlank()) {
            return CERTIFICATE_STATE_ISSUED;
        }
        return CERTIFICATE_STATES.contains(certificateState) ? certificateState : CERTIFICATE_STATE_ISSUED;
    }

    private CompletedRow toCompletedRow(Application application,
                                        LocalDate closedAt,
                                        boolean certificateIssued) {
        boolean paid = application.getPayment() != null && application.getPayment().getStatus() == PaymentStatus.PAID;
        return new CompletedRow(
            application.getId(),
            application.getListenerFullName(),
            application.getProgram() != null ? application.getProgram().getTitle() : "—",
            closedAt,
            application.getPayment() != null ? application.getPayment().getStatus().getLabel() : "Нет записи",
            application.getPayment() != null ? application.getPayment().getAmount() : BigDecimal.ZERO,
            paid,
            application.getAssessmentResult() != null ? application.getAssessmentResult().getStatus().getLabel() : "Нет результата",
            application.getAssessmentResult() != null ? application.getAssessmentResult().getScore() : null,
            application.getAssessmentResult() != null && application.getAssessmentResult().getStatus() == AssessmentStatus.PASSED,
            certificateIssued ? "Выдано" : "Не выдано",
            application.getCertificate() != null ? application.getCertificate().getNumber() : null,
            application.getCertificate() != null ? application.getCertificate().getIssueDate() : null,
            certificateIssued,
            caseUrl(application.getId())
        );
    }

    public record DashboardV2View(
        List<SystemHealthKpi> systemHealthKpis,
        FinanceSummary financeSummary,
        List<FunnelStage> funnelStages,
        List<BottleneckRow> bottlenecks,
        List<TeacherRiskRow> teacherRiskRows,
        List<ActionItemRow> actionItems,
        List<ProgramAnalyticsRow> programAnalytics,
        List<ContingentShareRow> contingentRows,
        CompletedSummary completedSummary
    ) {
    }

    public record DrilldownView(
        String queueType,
        String queueTypeLabel,
        Long teacherId,
        boolean priorityOnly,
        int minAgeDays,
        String riskLevel,
        String sort,
        List<DrilldownRow> rows,
        String queueRegistryUrl
    ) {
    }

    public record SystemHealthKpi(String title, long value, String subtitle, String drilldownUrl) {
    }

    public record FinanceSummary(
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        BigDecimal failedAmount,
        long squeezeCount,
        String paidUrl,
        String pendingUrl,
        String failedUrl,
        String squeezeUrl
    ) {
    }

    public record FunnelStage(String title, long count, long sharePercent, String queueType, String drilldownUrl) {
    }

    public record BottleneckRow(
        String queueType,
        String title,
        long itemsCount,
        long avgAgeDays,
        long maxAgeDays,
        String riskLevelKey,
        String riskLevelLabel,
        String nextAction,
        String drilldownUrl
    ) {
    }

    public record TeacherRiskRow(
        Long teacherId,
        String teacherName,
        long currentStudents,
        long currentPrograms,
        long inTrainingCount,
        long blockedCount,
        long riskScore,
        String riskLevelKey,
        String riskLevelLabel,
        String riskExplanation,
        String drilldownUrl
    ) {
    }

    public record ActionItemRow(
        Long applicationId,
        String listenerFullName,
        String programTitle,
        String queueTitle,
        int ageDays,
        int priorityScore,
        String priorityLevelKey,
        String priorityLevelLabel,
        String priorityExplanation,
        String nextAction,
        String nextActionUrl
    ) {
    }

    public record ProgramAnalyticsRow(
        Long programId,
        String programTitle,
        long demandCount,
        long activeListeners,
        BigDecimal paidAmount,
        String drilldownUrl
    ) {
    }

    public record ContingentShareRow(String title, long listenersCount, long percent) {
    }

    public record DrilldownRow(
        Long applicationId,
        String listenerFullName,
        String programTitle,
        String queueType,
        String queueTitle,
        String reason,
        int ageDays,
        int priorityScore,
        String priorityLevelKey,
        String priorityLevelLabel,
        String priorityExplanation,
        String nextAction,
        String nextActionUrl,
        Long teacherId,
        String teacherName
    ) {
    }

    public record QueueOption(String value, String label) {
    }

    public record RiskOption(String value, String label) {
    }

    public record SortOption(String value, String label) {
    }

    public record CaseView(
        Long applicationId,
        String listenerFullName,
        String programTitle,
        String queueType,
        String queueTitle,
        String reason,
        int ageDays,
        int priorityScore,
        String priorityLevelKey,
        String priorityLevelLabel,
        String priorityExplanation,
        String paymentStatusLabel,
        BigDecimal paymentAmount,
        LocalDate paymentDate,
        boolean paid,
        Long teacherId,
        String teacherName,
        int progressPercent,
        String assessmentStatusLabel,
        Integer assessmentScore,
        boolean assessmentPassed,
        String certificateStatusLabel,
        String certificateNumber,
        LocalDate certificateIssueDate,
        boolean certificateIssued,
        String applicationsUrl,
        String paymentsUrl,
        String assessmentsUrl,
        String certificatesUrl
    ) {
    }

    public record CompletedSummary(
        LocalDate dateFrom,
        LocalDate dateTo,
        long completedCount,
        long issuedCount,
        long notIssuedCount,
        BigDecimal paidAmount,
        String drilldownUrl
    ) {
    }

    public record CompletedRow(
        Long applicationId,
        String listenerFullName,
        String programTitle,
        LocalDate closedAt,
        String paymentStatusLabel,
        BigDecimal paymentAmount,
        boolean paid,
        String assessmentStatusLabel,
        Integer assessmentScore,
        boolean assessmentPassed,
        String certificateStatusLabel,
        String certificateNumber,
        LocalDate certificateIssueDate,
        boolean certificateIssued,
        String caseUrl
    ) {
    }

    public record CompletedDrilldownView(
        LocalDate dateFrom,
        LocalDate dateTo,
        String certificateState,
        List<CompletedRow> rows,
        long totalCount,
        long issuedCount,
        long notIssuedCount,
        BigDecimal paidAmount
    ) {
    }

    private record PriorityDetails(int score, String levelKey, String levelLabel, String explanation) {
    }

    private static final class ProgramAnalyticsAccumulator {
        private final Long programId;
        private final String programTitle;
        private long demandCount;
        private long activeListeners;
        private BigDecimal paidAmount;

        private ProgramAnalyticsAccumulator(Long programId,
                                           String programTitle,
                                           long demandCount,
                                           long activeListeners,
                                           BigDecimal paidAmount) {
            this.programId = programId;
            this.programTitle = programTitle;
            this.demandCount = demandCount;
            this.activeListeners = activeListeners;
            this.paidAmount = paidAmount;
        }
    }

    private static final class TeacherRiskAccumulator {
        private final Long teacherId;
        private final String teacherName;
        private final long currentStudents;
        private final long currentPrograms;
        private long inTraining;
        private long blocked;

        private TeacherRiskAccumulator(Long teacherId,
                                       String teacherName,
                                       long currentStudents,
                                       long currentPrograms,
                                       long inTraining,
                                       long blocked) {
            this.teacherId = teacherId;
            this.teacherName = teacherName;
            this.currentStudents = currentStudents;
            this.currentPrograms = currentPrograms;
            this.inTraining = inTraining;
            this.blocked = blocked;
        }
    }
}
