package com.holisun.backend.security;

import tools.jackson.databind.ObjectMapper;
import com.holisun.backend.dto.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;

/**
 * Raspunde cu acelasi format ErrorResponse folosit de GlobalExceptionHandler
 * atunci cand o cerere neautentificata loveste un endpoint protejat — asta se
 * intampla in filter chain-ul Spring Security, inainte de DispatcherServlet,
 * deci nu trece prin @RestControllerAdvice.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = new ErrorResponse(
                Instant.now(),
                HttpServletResponse.SC_UNAUTHORIZED,
                "Unauthorized",
                "Autentificare necesara",
                request.getRequestURI()
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
