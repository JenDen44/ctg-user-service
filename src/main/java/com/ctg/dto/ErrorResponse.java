package com.ctg.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.util.List;

@Getter
@AllArgsConstructor
public class ErrorResponse {

    private String title;

    private String message;

    private int code;

    private List<ErrorField> fields;

    public ErrorResponse(String title, String message, HttpStatus code, List<ErrorField> fields) {
        this.title = title;
        this.message = message;
        this.code = code.value();
        this.fields = fields;
    }
}
