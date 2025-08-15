package com.ctg.handler;

import com.ctg.exceptions.ResourceNotFoundException;
import com.ctg.exceptions.ValidationException;
import com.ctg.model.ErrorField;
import com.ctg.model.ErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleValidationExceptions(MethodArgumentNotValidException ex) {
        List<ErrorField> errorFields = new ArrayList<>();

        // Field errors
        ex.getBindingResult().getFieldErrors().forEach(fieldError ->
                errorFields.add(new ErrorField(fieldError.getField(), fieldError.getDefaultMessage()))
        );

        // Global errors
        ex.getBindingResult().getGlobalErrors().forEach(globalError ->
                errorFields.add(new ErrorField(globalError.getObjectName(), globalError.getDefaultMessage()))
        );

        return new ErrorResponse("Validation failed", null, HttpStatus.BAD_REQUEST, errorFields);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleResourceNotFound(ResourceNotFoundException ex) {
        return new ErrorResponse("Not found", ex.getMessage(), HttpStatus.NOT_FOUND, null);
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCustomValidation(ValidationException ex) {
        return new ErrorResponse(ex.getMessage(), null, HttpStatus.BAD_REQUEST, ex.getErrorFields());
    }

    @ExceptionHandler(MismatchedInputException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleJsonParseError(MismatchedInputException ex) {
        return new ErrorResponse(ex.getMessage(), null, HttpStatus.BAD_REQUEST, null);
    }

    @ExceptionHandler(InvalidFormatException.class)
    public ErrorResponse handleInvalidFormat(InvalidFormatException ex) {
        return new ErrorResponse(ex.getMessage(), null, HttpStatus.BAD_REQUEST, null);
    }
}
