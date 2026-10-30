package com.geli.warehouse.web;

import com.geli.warehouse.exception.BadRequestException;
import com.geli.warehouse.exception.BusinessRuleException;
import com.geli.warehouse.exception.DuplicateReferenceException;
import com.geli.warehouse.exception.DuplicateSkuException;
import com.geli.warehouse.exception.InsufficientStockException;
import com.geli.warehouse.exception.NotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    public ProblemDetail notFound(NotFoundException ex, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ProblemDetail badRequest(BadRequestException ex, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ProblemDetail businessRule(BusinessRuleException ex, HttpServletRequest request) {
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, "Business Rule Violation", ex.getMessage(), request);
    }

    @ExceptionHandler({DuplicateSkuException.class, DuplicateReferenceException.class})
    public ProblemDetail conflict(RuntimeException ex, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Conflict", ex.getMessage(), request);
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ProblemDetail insufficientStock(InsufficientStockException ex, HttpServletRequest request) {
        ProblemDetail detail = problem(HttpStatus.CONFLICT, "Insufficient Stock", ex.getMessage(), request);
        detail.setProperty("sku", ex.getSku());
        detail.setProperty("available", ex.getAvailable());
        detail.setProperty("requested", ex.getRequested());
        return detail;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        ProblemDetail detail = problem(
                HttpStatus.BAD_REQUEST, "Validation Failed", "Request validation failed", request);
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        ex.getBindingResult().getGlobalErrors()
                .forEach(error -> fieldErrors.putIfAbsent(error.getObjectName(), error.getDefaultMessage()));
        detail.setProperty("fieldErrors", fieldErrors);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail unreadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return problem(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                "Request body could not be read. Check the JSON syntax and field names.",
                request
        );
    }

    @ExceptionHandler(ConcurrencyFailureException.class)
    public ProblemDetail concurrency(ConcurrencyFailureException ex, HttpServletRequest request) {
        log.warn("Concurrent update conflict on {}", request.getRequestURI());
        return problem(
                HttpStatus.CONFLICT,
                "Conflict",
                "The record was updated at the same time. Retry the request.",
                request
        );
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail dataConflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Data integrity conflict on {}", request.getRequestURI());
        return problem(
                HttpStatus.CONFLICT,
                "Conflict",
                "The request conflicts with existing data",
                request
        );
    }

    private ProblemDetail problem(HttpStatus status, String title, String detail, HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setTitle(title);
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
