package com.example.greeting;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 登録日時と処理日時を同じタイムゾーンで作るための設定。
 */
@Configuration
public class TimeConfig {

    /** 画面表示とDBに保存する日時の基準。日本時間で統一する。 */
    public static final ZoneId ZONE = ZoneId.of("Asia/Tokyo");

    @Bean
    public Clock clock() {
        return Clock.system(ZONE);
    }
}
