package com.techplant.affiliate.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class SessionAuth {
    public CurrentUser requireUser(HttpServletRequest request) {
        CurrentUser current = current(request);
        if (!"USER".equals(current.role())) {
            throw ApiException.forbidden();
        }
        return current;
    }

    public CurrentUser requireAdmin(HttpServletRequest request) {
        CurrentUser current = current(request);
        if (!"ADMIN".equals(current.role())) {
            throw ApiException.forbidden();
        }
        return current;
    }

    public CurrentUser current(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            throw ApiException.unauthorized("UNAUTHORIZED", "请先登录");
        }
        Object role = session.getAttribute("role");
        return new CurrentUser(
                (Long) session.getAttribute("userId"),
                role == null ? "USER" : role.toString(),
                (String) session.getAttribute("phone"));
    }

    public void establish(HttpServletRequest request, CurrentUser user) {
        HttpSession session = request.getSession(true);
        session.setAttribute("userId", user.id());
        session.setAttribute("role", user.role());
        session.setAttribute("phone", user.phone());
    }

    public record CurrentUser(long id, String role, String phone) {
    }
}
