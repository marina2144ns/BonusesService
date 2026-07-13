package ru.stockmann.BonusesService.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import ru.stockmann.BonusesService.services.TransactionsBonusesDeltaService;

@RestController
@RequestMapping("/v1.0/TransactionsBonuses")
public class TransactionsBonusesController {

    @Autowired
    private TransactionsBonusesDeltaService deltaService;

    @GetMapping(
            value = "/delta",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    public ResponseEntity<StreamingResponseBody> getDelta(
            @RequestParam(
                    name = "cursor",
                    required = false,
                    defaultValue = "0"
            ) String cursor,
            @RequestParam(
                    name = "limit",
                    required = false,
                    defaultValue = "500"
            ) Integer limit
    ) {
        validateInputParameters(cursor, limit);

        StreamingResponseBody responseBody = outputStream ->
                deltaService.writeDelta(
                        cursor,
                        limit,
                        outputStream
                );

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(responseBody);
    }

    private void validateInputParameters(String cursor, Integer limit) {
        if (cursor == null || cursor.trim().isEmpty()) {
            throw new RuntimeException("Параметр cursor не заполнен");
        }

        try {
            Long.parseLong(cursor);
        } catch (NumberFormatException ex) {
            throw new RuntimeException("Параметр cursor должен быть числом");
        }

        if (limit == null) {
            throw new RuntimeException("Параметр limit не заполнен");
        }

        if (limit <= 0) {
            throw new RuntimeException("Параметр limit должен быть больше нуля");
        }

        if (limit > 5000) {
            throw new RuntimeException("Параметр limit не может быть больше 5000");
        }
    }

    private static class SuccessResponse {
        private final boolean status;
        private final Object data;

        public SuccessResponse(boolean status, Object data) {
            this.status = status;
            this.data = data;
        }

        public boolean isStatus() {
            return status;
        }

        public Object getData() {
            return data;
        }
    }

    private static class ErrorResponse {
        private final boolean status;
        private final ErrorDetails error;

        public ErrorResponse(boolean status, int code, String message) {
            this.status = status;
            this.error = new ErrorDetails(code, message);
        }

        public boolean isStatus() {
            return status;
        }

        public ErrorDetails getError() {
            return error;
        }
    }

    private static class ErrorDetails {
        private final int code;
        private final String message;

        public ErrorDetails(int code, String message) {
            this.code = code;
            this.message = message;
        }

        public int getCode() {
            return code;
        }

        public String getMessage() {
            return message;
        }
    }
}