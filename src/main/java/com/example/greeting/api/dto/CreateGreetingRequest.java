package com.example.greeting.api.dto;

import com.example.greeting.api.validation.GreetingName;

/**
 * 登録APIのリクエスト。Entityは外に出さない。
 */
public class CreateGreetingRequest {

    @GreetingName
    private String name;

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }
}
