package com.chat.service;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AdminAuthService {

    public static final String ADMIN_SESSION_KEY = "ADMIN_AUTH";
    private static final String FIXED_ADMIN_USERNAME = "admin";
    private static final String FIXED_ADMIN_PASSWORD = "kun666777@";

    public boolean login(String username, String password, HttpSession session) {
        if (username == null || password == null) {
            return false;
        }
        if (!FIXED_ADMIN_USERNAME.equals(username)) {
            return false;
        }
        if (!FIXED_ADMIN_PASSWORD.equals(password)) {
            return false;
        }
        session.setAttribute(ADMIN_SESSION_KEY, Boolean.TRUE);
        session.setAttribute("adminUsername", FIXED_ADMIN_USERNAME);
        return true;
    }

    public void logout(HttpSession session) {
        session.removeAttribute(ADMIN_SESSION_KEY);
        session.removeAttribute("adminUsername");
    }

    public boolean isLoggedIn(HttpSession session) {
        return Boolean.TRUE.equals(session.getAttribute(ADMIN_SESSION_KEY));
    }

    public Map<String, Object> getSessionInfo(HttpSession session) {
        return Map.of(
                "loggedIn", isLoggedIn(session),
                "username", String.valueOf(session.getAttribute("adminUsername") == null ? "" : session.getAttribute("adminUsername"))
        );
    }
}
