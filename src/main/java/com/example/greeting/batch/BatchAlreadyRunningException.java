package com.example.greeting.batch;

/**
 * 同じバッチがすでに動いている。APIでは409にする。
 */
public class BatchAlreadyRunningException extends RuntimeException {

    public BatchAlreadyRunningException(final String message) {
        super(message);
    }
}
