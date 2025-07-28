package com.ctg.handler;

import com.ctg.exceptions.ResourceNotFoundException;
import com.ctg.exceptions.ValidationException;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        Map<String, String> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Validation error"
                ));

        ex.getBindingResult().getGlobalErrors().forEach(error -> {
            errors.put(error.getObjectName(), error.getDefaultMessage());
        });

        return new ErrorResponse("Validation failed", HttpStatus.BAD_REQUEST, errors);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse(ex.getMessage(), HttpStatus.NOT_FOUND, null);
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCustomValidation(ValidationException ex) {
        return new ErrorResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, ex.getErrors());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleAllExceptions(Exception ex) {
        return new ErrorResponse("Internal server error", HttpStatus.INTERNAL_SERVER_ERROR, null);
    }

    @Getter
    @AllArgsConstructor
    static class ErrorResponse {

        private String message;

        private int statusCode;

        private String error;

        private Map<String, String> errors;

        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSZ")
        private final Instant timestamp = Instant.now();

        public ErrorResponse(String message, HttpStatus status, Map<String, String> errors) {
            this.message = message;
            this.statusCode = status.value();
            this.error = status.getReasonPhrase();
            this.errors = errors;
        }
    }
}
