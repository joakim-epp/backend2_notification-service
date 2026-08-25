package com.backend2.backend2_notificationservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.net.URI;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail validationFailed(MethodArgumentNotValidException e, HttpServletRequest request) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(f -> Map.of("field", f.getField(), "message",
                        f.getDefaultMessage() == null ? "Invalid value" : f.getDefaultMessage()))
                .toList();

        ProblemDetail problem = problem(HttpStatus.BAD_REQUEST, "Invalid input",
                "One or more fields are invalid", "VALIDATION_FAILED",
                "/problems/validation-failed", request);
        problem.setProperty("errors", errors);
        return problem;
    }

    /**
     * Everything the request could not even be read as: an unparsable date, a query parameter that
     * is missing or not a positive number. One wording covers them, the client sent something we
     * could not act on.
     */
    @ExceptionHandler({MethodArgumentTypeMismatchException.class, ConstraintViolationException.class,
            MissingServletRequestParameterException.class, HttpMessageNotReadableException.class})
    ProblemDetail invalidRequest(HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Invalid request", "Invalid parameter",
                "INVALID_REQUEST", "/problems/invalid-request", request);
    }

    @ExceptionHandler(CustomerNotFoundException.class)
    ProblemDetail customerNotFound(CustomerNotFoundException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Customer was not found", e.getMessage(),
                "CUSTOMER_NOT_FOUND", "/problems/customer-not-found", request);
    }

    @ExceptionHandler(CustomerHasNoEmailException.class)
    ProblemDetail customerHasNoEmail(CustomerHasNoEmailException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Customer has no email address", e.getMessage(),
                "CUSTOMER_HAS_NO_EMAIL", "/problems/customer-has-no-email", request);
    }

    @ExceptionHandler(CustomerServiceUnavailableException.class)
    ResponseEntity<ProblemDetail> customerServiceUnavailable(HttpServletRequest request) {
        ProblemDetail problem = problem(HttpStatus.SERVICE_UNAVAILABLE,
                "Service is unavailable right now",
                "We could not look up the customer right now, so no confirmation was sent. Try again later",
                "CUSTOMER_SERVICE_UNAVAILABLE", "/problems/customer-service-unavailable", request);

        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header(HttpHeaders.RETRY_AFTER, "5")
                .body(problem);
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail,
                                  String errorCode, String type, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setType(URI.create(type));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("errorCode", errorCode);
        return problem;
    }
}
