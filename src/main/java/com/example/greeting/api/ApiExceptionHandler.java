package com.example.greeting.api;

import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.example.greeting.api.dto.ApiErrorResponse;
import com.example.greeting.batch.BatchAlreadyRunningException;
import com.example.greeting.batch.BatchFailedException;

/**
 * APIの例外を共通のJSON（message）に変換する。スタックトレースは返さない。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(final MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .filter(Objects::nonNull)
                .collect(Collectors.joining(" "));
        if (message.isBlank()) {
            message = "入力内容が正しくありません。";
        }
        return ResponseEntity.badRequest().body(new ApiErrorResponse(message));
    }

    @ExceptionHandler(InvalidGreetingNameException.class)
    public ResponseEntity<ApiErrorResponse> handleInvalidName(final InvalidGreetingNameException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(GreetingNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> handleNotFound(final GreetingNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ApiErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(final MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse("IDの形式が正しくありません。"));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> handleUnreadable(final HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(new ApiErrorResponse("JSONの形式が正しくありません。"));
    }

    @ExceptionHandler(BatchAlreadyRunningException.class)
    public ResponseEntity<ApiErrorResponse> handleConflict(final BatchAlreadyRunningException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ApiErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(BatchFailedException.class)
    public ResponseEntity<ApiErrorResponse> handleBatchFailed(final BatchFailedException ex) {
        log.error("バッチ実行に失敗しました", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(final Exception ex) throws Exception {
        // 存在しないURLはSpring MVCの404のままにする
        if (ex instanceof NoResourceFoundException) {
            throw ex;
        }
        log.error("予期しないエラー", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ApiErrorResponse("サーバーでエラーが発生しました。"));
    }
}
