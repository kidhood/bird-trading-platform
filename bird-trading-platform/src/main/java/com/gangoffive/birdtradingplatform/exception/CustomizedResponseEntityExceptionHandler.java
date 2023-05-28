package com.gangoffive.birdtradingplatform.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.time.LocalDateTime;

@ControllerAdvice
public class CustomizedResponseEntityExceptionHandler extends ResponseEntityExceptionHandler {
    @ExceptionHandler(Exception.class)
    public final ResponseEntity<ApiError> handleAllExceptions(Exception ex, HttpServletRequest request) throws Exception {
        ApiError errorDetails = new ApiError(request.getRequestURI(),
                ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR.value() , LocalDateTime.now());
        return new ResponseEntity<ApiError>(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR);

    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {

        ApiError errorDetails = new ApiError(request.getDescription(false),
                                    "Total Errors:" + ex.getErrorCount() + " First Error:" + ex.getFieldError().getDefaultMessage()
                                            ,HttpStatus.BAD_REQUEST.value()
                                            ,LocalDateTime.now() );
        return new ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST);
    }



}
