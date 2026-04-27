package com.berdachuk.meteoris.insight.api;

import com.berdachuk.meteoris.insight.agent.UnknownAskUserTicketException;
import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

@RestControllerAdvice(assignableTypes = {ChatApiController.class, EvaluationApiController.class})
@SuppressWarnings("deprecation")
public class MeteorisApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ProblemDetail> onIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Invalid request body", ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ProblemDetail> onValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return problem(HttpStatus.BAD_REQUEST, "Validation failed", detail, request);
    }

    @ExceptionHandler(UnknownAskUserTicketException.class)
    ResponseEntity<ProblemDetail> onUnknownTicket(UnknownAskUserTicketException ex, WebRequest request) {
        return problem(HttpStatus.NOT_FOUND, "AskUser ticket not found", ex.getMessage(), request);
    }

    @ExceptionHandler(IllegalStateException.class)
    ResponseEntity<ProblemDetail> onIllegalState(IllegalStateException ex, WebRequest request) {
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "Internal error", ex.getMessage(), request);
    }

    private static ResponseEntity<ProblemDetail> problem(
            HttpStatus status, String title, String detail, WebRequest request) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, detail);
        pd.setTitle(title);
        if (request instanceof ServletWebRequest swr) {
            HttpServletRequest req = swr.getRequest();
            if (req != null) {
                pd.setInstance(URI.create(req.getRequestURI()));
            }
        }
        pd.setType(URI.create("https://meteoris.local/problems/" + status.value()));
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("application/problem+json"))
                .body(pd);
    }
}
