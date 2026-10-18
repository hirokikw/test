package com.example.greeting.batch;

/**
 * バッチが失敗した。APIでは500にし、原因のスタックトレースはレスポンスに含めない。
 */
public class BatchFailedException extends RuntimeException {

    public BatchFailedException(final String message) {
        super(message);
    }

    public BatchFailedException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
