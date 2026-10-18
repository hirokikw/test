package com.example.greeting.api.validation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 前後の空白を除いた名前が1文字以上50文字以内であることを検証する。
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = GreetingNameValidator.class)
public @interface GreetingName {

    String message() default "名前を入力してください。";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
