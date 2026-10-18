package com.example.greeting.api.dto;

/**
 * APIの共通エラー。画面にはこのmessageだけを出す。
 */
public record ApiErrorResponse(String message) {
}
