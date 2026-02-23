package ru.cpk.system.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import ru.cpk.system.service.AdminDashboardV2Service;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard")
    public String dashboard() {
        return "redirect:/admin/dashboard-v2";
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/dashboard/drilldown")
    public String drilldown(@RequestParam(required = false) String type,
                            @RequestParam(required = false) Long teacherId) {
        String queueType = mapLegacyTypeToQueue(type);
        String redirect = "redirect:/admin/dashboard-v2/drilldown?queueType=" + queueType;
        if (teacherId != null) {
            redirect += "&teacherId=" + teacherId;
        }
        return redirect;
    }

    private String mapLegacyTypeToQueue(String type) {
        if ("pending-docs".equals(type)) {
            return AdminDashboardV2Service.QUEUE_DOCS_PENDING;
        }
        if ("handoff".equals(type)) {
            return AdminDashboardV2Service.QUEUE_ACCESS_BLOCKED;
        }
        if ("trial".equals(type)) {
            return AdminDashboardV2Service.QUEUE_TRIAL_ACCESS;
        }
        if ("in-training".equals(type)) {
            return AdminDashboardV2Service.QUEUE_IN_TRAINING;
        }
        if ("ready-certificate".equals(type)) {
            return AdminDashboardV2Service.QUEUE_CERTIFICATE_BLOCKED;
        }
        if ("approved-unpaid".equals(type)) {
            return AdminDashboardV2Service.QUEUE_APPROVED_UNPAID;
        }
        return AdminDashboardV2Service.QUEUE_ALL;
    }
}
