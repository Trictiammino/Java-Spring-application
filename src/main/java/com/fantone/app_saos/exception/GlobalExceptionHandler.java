package com.fantone.app_saos.exception;

import com.fantone.app_saos.dto.response.MessageResponse;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.security.SignatureException;
import java.util.List;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceConflictException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public MessageResponse handleConflict(RuntimeException ex) {
        return new MessageResponse(ex.getMessage());
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public MessageResponse handleNotFound(RuntimeException ex) {
        return new MessageResponse(ex.getMessage());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponse handleBadJson(HttpMessageNotReadableException ex) {
        return new MessageResponse("Malformed JSON request");
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponse handleValidationErrors(MethodArgumentNotValidException ex) {

        String errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .reduce((a, b) -> a + " | " + b)
                .orElse("Validation error");

        return new MessageResponse(errors);
    }

    @ExceptionHandler({
            ExpiredJwtException.class, MalformedJwtException.class,
            UnsupportedJwtException.class, SecurityException.class,
            SignatureException.class, JwtException.class
    })
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public MessageResponse handleInvalidJwt() {
        return new MessageResponse("Invalid or Expired JWT.");
    }

    @ExceptionHandler({UsernameNotFoundException.class})
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public MessageResponse handleAuthErrors(RuntimeException ex) {
        return new MessageResponse(ex.getMessage());
    }

    @ExceptionHandler(RefreshTokenException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public MessageResponse handleRefreshTokenError(RefreshTokenException ex) {
        return new MessageResponse(ex.getMessage());
    }

}
