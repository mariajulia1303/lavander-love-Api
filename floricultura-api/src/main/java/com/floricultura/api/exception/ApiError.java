package com.floricultura.api.exception;

import java.time.OffsetDateTime;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

// corpo padrao de erro que a API devolve
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiError {

    private OffsetDateTime timestamp;
    private int status;
    private String error;
    private String message;
    // so aparece quando for erro de validacao (422), nos outros casos fica null e some do json
    private List<FieldErrorItem> fieldErrors;

    public ApiError() {
    }

    public ApiError(OffsetDateTime timestamp, int status, String error, String message) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
    }

    // cada campo invalido vira um item desses na lista
    public static class FieldErrorItem {
        private String field;
        private String message;

        public FieldErrorItem() {
        }

        public FieldErrorItem(String field, String message) {
            this.field = field;
            this.message = message;
        }

        public String getField() {
            return field;
        }

        public void setField(String field) {
            this.field = field;
        }

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    public OffsetDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(OffsetDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<FieldErrorItem> getFieldErrors() {
        return fieldErrors;
    }

    public void setFieldErrors(List<FieldErrorItem> fieldErrors) {
        this.fieldErrors = fieldErrors;
    }
}
