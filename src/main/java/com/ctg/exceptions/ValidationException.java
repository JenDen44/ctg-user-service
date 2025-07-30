package com.ctg.exceptions;

import com.ctg.model.ErrorField;
import lombok.Getter;

import java.util.List;

@Getter
public class ValidationException extends RuntimeException {

    private final List<ErrorField> errorFields;

    public ValidationException(List<ErrorField> errors) {
        super("Validation failed");
        this.errorFields = errors;
    }
}
