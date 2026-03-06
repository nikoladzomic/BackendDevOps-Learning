package com.example.learning.exception;

import com.example.learning.security.jwt.JWTAuthenticationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // 401 - pogrešan login / auth fail
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<String> handleBadCredentials(BadCredentialsException ex) {

        log.warn("Authentication failed: {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                401,
                "UNAUTHORIZED",
                "Invalid email or password",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(response.getMessage());
    }

    // 403 - nema prava (ADMIN endpoint npr)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<String> handleAccessDenied(AccessDeniedException ex) {
        
        log.warn("Access denied: {}", ex.getMessage());
        
        ErrorResponse response = new ErrorResponse(
                403,
                "FORBIDDEN",
                "You dont have permission to access this resource",
                LocalDateTime.now()
        );
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(response.getMessage());
    }

    // fallback - sve ostalo
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleGeneric(Exception ex) {

        log.error("Unhandled exception", ex);

        ErrorResponse response = new ErrorResponse(
                500,
                "INTERNAL SERVER ERROR",
                "Something went wrong",
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response.getMessage());
    }

    @ExceptionHandler(JwtAuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleJwtException(JWTAuthenticationFilter ex) {

        ErrorResponse response = new ErrorResponse(
                401,
                "UNAUTHORIZED",
                "Jwt token not valid",
                LocalDateTime.now()
        );

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {

        log.warn("Method not supported: {}", ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                405,
                "METHOD NOT ALLOWED",
                ex.getMessage(),
                LocalDateTime.now()
        );

        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(response);
    }
}
