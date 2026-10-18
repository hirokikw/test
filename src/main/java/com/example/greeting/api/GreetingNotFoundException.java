package com.example.greeting.api;

/**
 * 指定したあいさつが存在しない。APIでは404にする。
 */
public class GreetingNotFoundException extends RuntimeException {

    public GreetingNotFoundException(final String message) {
        super(message);
    }
}
