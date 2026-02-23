package ru.cpk.system.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import ru.cpk.system.service.AdminDashboardV2Service;
import ru.cpk.system.service.StatsService;

@SpringBootTest
@AutoConfigureMockMvc
class AdminDashboardV2ControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminDashboardV2Service adminDashboardV2Service;

    @MockBean
    private StatsService statsService;

    @BeforeEach
    void setUp() {
        when(adminDashboardV2Service.buildDashboardView()).thenReturn(
            new AdminDashboardV2Service.DashboardV2View(
                List.of(new AdminDashboardV2Service.SystemHealthKpi(
                    "Неподтвержденные документы",
                    1L,
                    "Требуют решения методиста",
                    "/admin/dashboard-v2/drilldown?queueType=docs_pending"
                )),
                new AdminDashboardV2Service.FinanceSummary(
                    java.math.BigDecimal.ONE,
                    java.math.BigDecimal.ZERO,
                    java.math.BigDecimal.ZERO,
                    1L,
                    "/payments",
                    "/payments",
                    "/payments",
                    "/applications?paymentState=UNPAID"
                ),
                List.of(new AdminDashboardV2Service.FunnelStage(
                    "Документы",
                    1L,
                    100L,
                    AdminDashboardV2Service.QUEUE_DOCS_PENDING,
                    "/admin/dashboard-v2/drilldown?queueType=docs_pending"
                )),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                new AdminDashboardV2Service.CompletedSummary(
                    java.time.LocalDate.now().minusDays(30),
                    java.time.LocalDate.now(),
                    0L,
                    0L,
                    0L,
                    java.math.BigDecimal.ZERO,
                    "/admin/dashboard-v2/completed"
                )
            )
        );

        when(adminDashboardV2Service.buildDrilldownView(anyString(), any(), anyBoolean(), any(), anyString(), anyString())).thenReturn(
            new AdminDashboardV2Service.DrilldownView(
                AdminDashboardV2Service.QUEUE_DOCS_PENDING,
                "Документы",
                null,
                false,
                0,
                "all",
                "priority_desc",
                List.of(new AdminDashboardV2Service.DrilldownRow(
                    1L,
                    "Смирнов Алексей Павлович",
                    "Управление проектами в ИТ",
                    AdminDashboardV2Service.QUEUE_DOCS_PENDING,
                    "Документы",
                    "Ожидает проверку документов",
                    2,
                    106,
                    "high",
                    "Высокий",
                    "Документы (100) + возраст 2д (+6) = 106",
                    "Открыть кейс",
                    "/admin/dashboard-v2/case/1",
                    null,
                    "Не назначен"
                )),
                "/applications?docStatus=PENDING"
            )
        );

        when(adminDashboardV2Service.queueOptions()).thenReturn(
            List.of(new AdminDashboardV2Service.QueueOption("all", "Все очереди"))
        );
        when(adminDashboardV2Service.riskOptions()).thenReturn(
            List.of(new AdminDashboardV2Service.RiskOption("all", "Все уровни"))
        );
        when(adminDashboardV2Service.sortOptions()).thenReturn(
            List.of(new AdminDashboardV2Service.SortOption("priority_desc", "Приоритет: сначала важные"))
        );
        when(statsService.teacherLoads()).thenReturn(
            List.of(new StatsService.TeacherLoad(1L, "Ильин Павел Сергеевич", 2, 1))
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenDashboardV2() throws Exception {
        mockMvc.perform(get("/admin/dashboard-v2"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/dashboard-v2"))
            .andExpect(model().attributeExists("systemHealthKpis", "financeSummary", "funnelStages", "bottlenecks", "teacherRiskRows", "actionItems", "programAnalytics", "contingentRows"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void nonAdminCannotOpenDashboardV2() throws Exception {
        mockMvc.perform(get("/admin/dashboard-v2"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void drilldownV2AcceptsFilters() throws Exception {
        mockMvc.perform(get("/admin/dashboard-v2/drilldown")
                .param("queueType", "docs_pending")
                .param("teacherId", "5")
                .param("priorityOnly", "true")
                .param("minAgeDays", "2")
                .param("riskLevel", "high")
                .param("sort", "age_desc"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/drilldown-v2"))
            .andExpect(model().attributeExists("drilldownRows", "queueOptions", "riskOptions", "sortOptions", "teacherLoads"));

        verify(adminDashboardV2Service).buildDrilldownView("docs_pending", 5L, true, 2, "high", "age_desc");
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenCaseAndCompletedDrilldown() throws Exception {
        when(adminDashboardV2Service.buildCaseView(1L)).thenReturn(
            new AdminDashboardV2Service.CaseView(
                1L,
                "Смирнов Алексей Павлович",
                "Управление проектами в ИТ",
                AdminDashboardV2Service.QUEUE_DOCS_PENDING,
                "Документы",
                "Ожидает проверку документов",
                2,
                106,
                "high",
                "Высокий",
                "Документы (100) + возраст 2д (+6) = 106",
                "Нет записи",
                java.math.BigDecimal.ZERO,
                null,
                false,
                null,
                "Не назначен",
                0,
                "Нет результата",
                null,
                false,
                "Не выдано",
                null,
                null,
                false,
                "/applications?id=1",
                "/payments",
                "/assessments",
                "/certificates"
            )
        );
        when(adminDashboardV2Service.buildCompletedDrilldownView(any(), any(), any())).thenReturn(
            new AdminDashboardV2Service.CompletedDrilldownView(
                java.time.LocalDate.now().minusDays(30),
                java.time.LocalDate.now(),
                "all",
                List.of(),
                0L,
                0L,
                0L,
                java.math.BigDecimal.ZERO
            )
        );

        mockMvc.perform(get("/admin/dashboard-v2/case/1"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/case-v2"))
            .andExpect(model().attributeExists("caseView"));

        mockMvc.perform(get("/admin/dashboard-v2/completed"))
            .andExpect(status().isOk())
            .andExpect(view().name("admin/completed-v2"))
            .andExpect(model().attributeExists("rows", "dateFrom", "dateTo", "certificateState"));
    }

    @Test
    @WithMockUser(roles = "STUDENT")
    void nonAdminCannotOpenCaseAndCompletedDrilldown() throws Exception {
        mockMvc.perform(get("/admin/dashboard-v2/case/1"))
            .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/dashboard-v2/completed"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void legacyDashboardRouteRedirectsToV2() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/dashboard-v2"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void legacyDrilldownRouteRedirectsToV2() throws Exception {
        mockMvc.perform(get("/admin/dashboard/drilldown")
                .param("type", "handoff")
                .param("teacherId", "3"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/admin/dashboard-v2/drilldown?queueType=access_blocked&teacherId=3"));
    }
}
