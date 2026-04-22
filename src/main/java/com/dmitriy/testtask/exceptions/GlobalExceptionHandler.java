package com.dmitriy.testtask.exceptions;

import com.dmitriy.testtask.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(WalletNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleWalletNotFound(WalletNotFoundException exception) {
        log.warn(exception.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.NOT_FOUND.value(),
                exception.getMessage(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientFunds(InsufficientFundsException exception) {
        log.warn(exception.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                exception.getMessage(),
                Instant.now()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleInvalidJson(HttpMessageNotReadableException ex) {
        log.warn("Invalid JSON: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request format",
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid request data",
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception exception) {
        log.warn(exception.getMessage());
        ErrorResponse error = new ErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal server error",
                Instant.now()
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }
}
