package ru.cpk.system.config;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        Set<String> roles = AuthorityUtils.authorityListToSet(authentication.getAuthorities());
        String targetUrl = "/dashboard";

        if (roles.contains("ROLE_ADMIN")) {
            targetUrl = "/admin/dashboard-v2";
        } else if (roles.contains("ROLE_METHODIST")) {
            targetUrl = "/methodist/queue";
        } else if (roles.contains("ROLE_TEACHER")) {
            targetUrl = "/teacher/groups";
        } else if (roles.contains("ROLE_STUDENT")) {
            targetUrl = "/student/cabinet";
        }

        response.sendRedirect(request.getContextPath() + targetUrl);
    }
}
