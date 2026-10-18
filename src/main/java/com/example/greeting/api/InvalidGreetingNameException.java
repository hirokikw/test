package com.example.greeting.api;

/**
 * 検証を通過しなかった名前。APIでは400にする。
 */
public class InvalidGreetingNameException extends RuntimeException {

    public InvalidGreetingNameException(final String message) {
        super(message);
    }
}
