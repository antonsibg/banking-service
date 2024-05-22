package ru.antonsibgatulin.bankingservice.advise;

import io.micrometer.common.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.support.WebExchangeBindException;
import ru.antonsibgatulin.bankingservice.dto.exception.ApiError;
import ru.antonsibgatulin.bankingservice.except.*;


import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


import org.springframework.http.HttpStatus;

import org.springframework.http.ResponseEntity;

import org.springframework.web.bind.annotation.ExceptionHandler;


import javax.validation.ConstraintViolationException;

@ControllerAdvice
@ResponseBody
@Order(Ordered.HIGHEST_PRECEDENCE) // Set the highest precedence
public class ExceptionHandlerController {

    @Autowired
    private ApplicationContext applicationContext;

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler({BadRequestException.class})
    public ResponseEntity<ApiError> handleBadRequestException(BadRequestException badRequestException) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(badRequestException.getMessage(), badRequestException.getCode()));
    }

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler({AuthenticationException.class})
    public ResponseEntity<ApiError> handleAuthenticationFail(AuthenticationException authenticationException) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ApiError(authenticationException.getMessage(), authenticationException.getCode()));
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler({NotFoundException.class})
    public ResponseEntity<ApiError> handleNotFoundException(NotFoundException notFoundException) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiError(notFoundException.getMessage(), notFoundException.getCode()));
    }



    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(RegistrationException.class)
    @ResponseBody
    public ResponseEntity<ApiError> handleRegistrationException(RegistrationException ex) {
        return new ResponseEntity<>(new ApiError(ex.getMessage(), 409), HttpStatus.BAD_REQUEST);
    }


    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(AlreadyExistException.class)
    @ResponseBody
    public ResponseEntity<ApiError> handleAlreadyException(AlreadyExistException ex) {

        return new ResponseEntity<>(new ApiError(ex.getMessage(), 409), HttpStatus.BAD_REQUEST);
    }


    @ExceptionHandler(WebExchangeBindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public HttpStatus handleWebExchangeBindException(WebExchangeBindException e) {

        FieldError fieldError = e.getBindingResult().getFieldError();
        if (fieldError != null) {
            throw new WebExchangeBindException(e.getMethodParameter(), e.getBindingResult());
        }
        throw e;
    }


    @ExceptionHandler({ConstraintViolationException.class})
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResponseEntity<ApiError> handleConstraintViolationException(ConstraintViolationException e) {
        String fieldName = e.getMessage();
        String message = getResourceMessage(fieldName + " already exist", "Already Exists");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(message, 400));
    }


    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ResponseBody
    public ResponseEntity<ApiError> onMethodArgumentNotValidException(
            MethodArgumentNotValidException e
    ) {
        final List<String> violations = e.getBindingResult().getFieldErrors().stream()

                .map(error -> error.getDefaultMessage())

                .collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ApiError(violations.stream().collect(Collectors.joining(", ")), 400));


    }


    @ExceptionHandler(AccessDeniedForUnAutorizationException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ResponseBody
    public ResponseEntity<ApiError> handleCustomAccessDeniedException(AccessDeniedForUnAutorizationException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ApiError(ex.getMessage(), 403));
    }

    private String getResourceMessage(String key, String defaultMessage) {
        String message = applicationContext.getMessage(key, null, Locale.getDefault());
        if (StringUtils.isNotEmpty(message)) {
            return message;
        }
        return defaultMessage;
    }
}
