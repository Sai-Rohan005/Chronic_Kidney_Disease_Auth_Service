package services.web;

import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.*;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Shared wire contract, kept local while the services remain independent repositories. */
@RestControllerAdvice
public class ApiErrorAdvice extends ResponseEntityExceptionHandler implements ResponseBodyAdvice<Object> {
    private static final Logger log = LoggerFactory.getLogger(ApiErrorAdvice.class);

    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception exception, Object body,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        String message = exception instanceof ResponseStatusException explicit && explicit.getReason() != null
                ? explicit.getReason() : reason(status.value());
        String path = ((ServletWebRequest) request).getRequest().getRequestURI();
        return new ResponseEntity<>(error(status.value(), message, path), headers, status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> unexpected(Exception exception, jakarta.servlet.http.HttpServletRequest request) {
        log.error("Unhandled request failure", exception);
        return ResponseEntity.status(500).body(error(500, "An unexpected error occurred", request.getRequestURI()));
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
            Class<? extends HttpMessageConverter<?>> converterType, ServerHttpRequest request, ServerHttpResponse response) {
        if (response instanceof ServletServerHttpResponse servlet && servlet.getServletResponse().getStatus() >= 400) {
            int status = servlet.getServletResponse().getStatus();
            String message = body instanceof Map<?, ?> map && map.get("message") instanceof String text
                    ? text : reason(status);
            return error(status, message, ((ServletServerHttpRequest) request).getServletRequest().getRequestURI());
        }
        return body;
    }

    private static Map<String, Object> error(int status, String message, String path) {
        return Map.of("timestamp", Instant.now().toString(), "status", status,
                "error", reason(status), "message", message, "path", path);
    }

    private static String reason(int status) {
        HttpStatus known = HttpStatus.resolve(status);
        return known == null ? "HTTP Error" : known.getReasonPhrase();
    }
}
