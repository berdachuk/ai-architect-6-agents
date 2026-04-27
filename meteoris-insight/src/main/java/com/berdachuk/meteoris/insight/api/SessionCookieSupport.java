package com.berdachuk.meteoris.insight.api;

import com.berdachuk.meteoris.insight.core.IdGenerator;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Optional;

public final class SessionCookieSupport {

    public static final String COOKIE_NAME = "METEORIS_SESSION";

    private SessionCookieSupport() {}

    public static Optional<String> readExisting(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> COOKIE_NAME.equals(c.getName()))
                .map(Cookie::getValue)
                .filter(v -> !v.isBlank())
                .findFirst();
    }

    public static void writeSessionCookie(HttpServletResponse response, String sessionId) {
        Cookie c = new Cookie(COOKIE_NAME, sessionId);
        c.setPath("/");
        c.setHttpOnly(true);
        c.setMaxAge(60 * 60 * 24 * 30);
        response.addCookie(c);
    }

    public static String readOrCreate(
            HttpServletRequest request, HttpServletResponse response, IdGenerator idGenerator) {
        Optional<String> existing = readExisting(request);
        if (existing.isPresent()) {
            return existing.get();
        }
        String sid = idGenerator.generateId();
        writeSessionCookie(response, sid);
        return sid;
    }
}
