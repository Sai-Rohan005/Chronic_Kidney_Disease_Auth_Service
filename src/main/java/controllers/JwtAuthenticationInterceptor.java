package controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import services.JwtTokenService;

import java.io.IOException;
import java.util.Optional;

@Component
public class JwtAuthenticationInterceptor implements HandlerInterceptor {

    public static final String AUTHENTICATED_EMAIL_ATTRIBUTE = "authenticatedEmail";
    public static final String AUTHENTICATED_ROLE_ATTRIBUTE = "authenticatedRole";

    private final JwtTokenService jwtTokenService;

    public JwtAuthenticationInterceptor(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) throws IOException {
        String token = JwtTokenService.bearerToken(
                request.getHeader("Authorization"));
        if (token == null) {
            return unauthorized(response);
        }

        Optional<JwtTokenService.AuthenticatedUser> authenticatedUser =
                jwtTokenService.authenticate(token);
        if (authenticatedUser.isEmpty()) {
            return unauthorized(response);
        }

        JwtTokenService.AuthenticatedUser user = authenticatedUser.get();
        request.setAttribute(AUTHENTICATED_EMAIL_ATTRIBUTE, user.email());
        request.setAttribute(AUTHENTICATED_ROLE_ATTRIBUTE, user.role());
        return true;
    }

    private static boolean unauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(
                "{\"message\":\"Authentication required or token expired\"}");
        return false;
    }
}
