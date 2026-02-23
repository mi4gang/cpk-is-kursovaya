package ru.cpk.system.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.cpk.system.service.AdminDashboardV2Service;
import ru.cpk.system.service.StatsService;
import java.time.LocalDate;

@Controller
@RequestMapping("/admin")
public class AdminDashboardV2Controller {

    private final AdminDashboardV2Service adminDashboardV2Service;
    private final StatsService statsService;

    public AdminDashboardV2Controller(AdminDashboardV2Service adminDashboardV2Service,
                                      StatsService statsService) {
        this.adminDashboardV2Service = adminDashboardV2Service;
        this.statsService = statsService;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard-v2")
    public String dashboardV2(Model model) {
        var dashboardView = adminDashboardV2Service.buildDashboardView();
        model.addAttribute("systemHealthKpis", dashboardView.systemHealthKpis());
        model.addAttribute("financeSummary", dashboardView.financeSummary());
        model.addAttribute("funnelStages", dashboardView.funnelStages());
        model.addAttribute("bottlenecks", dashboardView.bottlenecks());
        model.addAttribute("teacherRiskRows", dashboardView.teacherRiskRows());
        model.addAttribute("actionItems", dashboardView.actionItems());
        model.addAttribute("programAnalytics", dashboardView.programAnalytics());
        model.addAttribute("contingentRows", dashboardView.contingentRows());
        model.addAttribute("completedSummary", dashboardView.completedSummary());
        return "admin/dashboard-v2";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard-v2/drilldown")
    public String drilldownV2(@RequestParam(required = false) String queueType,
                              @RequestParam(required = false) Long teacherId,
                              @RequestParam(defaultValue = "false") boolean priorityOnly,
                              @RequestParam(required = false) Integer minAgeDays,
                              @RequestParam(required = false) String riskLevel,
                              @RequestParam(required = false) String sort,
                              Model model) {
        var view = adminDashboardV2Service.buildDrilldownView(queueType, teacherId, priorityOnly, minAgeDays, riskLevel, sort);
        model.addAttribute("drilldownRows", view.rows());
        model.addAttribute("queueType", view.queueType());
        model.addAttribute("queueTypeLabel", view.queueTypeLabel());
        model.addAttribute("selectedTeacherId", view.teacherId());
        model.addAttribute("priorityOnly", view.priorityOnly());
        model.addAttribute("minAgeDays", view.minAgeDays());
        model.addAttribute("riskLevel", view.riskLevel());
        model.addAttribute("sort", view.sort());
        model.addAttribute("queueOptions", adminDashboardV2Service.queueOptions());
        model.addAttribute("riskOptions", adminDashboardV2Service.riskOptions());
        model.addAttribute("sortOptions", adminDashboardV2Service.sortOptions());
        model.addAttribute("teacherLoads", statsService.teacherLoads());
        model.addAttribute("queueRegistryUrl", view.queueRegistryUrl());
        return "admin/drilldown-v2";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard-v2/case/{applicationId}")
    public String caseV2(@PathVariable Long applicationId, Model model) {
        var caseView = adminDashboardV2Service.buildCaseView(applicationId);
        model.addAttribute("caseView", caseView);
        return "admin/case-v2";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard-v2/completed")
    public String completedV2(@RequestParam(required = false) LocalDate dateFrom,
                              @RequestParam(required = false) LocalDate dateTo,
                              @RequestParam(required = false) String certificateState,
                              Model model) {
        var view = adminDashboardV2Service.buildCompletedDrilldownView(dateFrom, dateTo, certificateState);
        model.addAttribute("rows", view.rows());
        model.addAttribute("dateFrom", view.dateFrom());
        model.addAttribute("dateTo", view.dateTo());
        model.addAttribute("certificateState", view.certificateState());
        model.addAttribute("totalCount", view.totalCount());
        model.addAttribute("issuedCount", view.issuedCount());
        model.addAttribute("notIssuedCount", view.notIssuedCount());
        model.addAttribute("paidAmount", view.paidAmount());
        return "admin/completed-v2";
    }
}
