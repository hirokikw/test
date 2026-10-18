package com.example.greeting.service;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.greeting.api.InvalidGreetingNameException;
import com.example.greeting.api.dto.GreetingResponse;
import com.example.greeting.api.validation.GreetingNameValidator;
import com.example.greeting.domain.Greeting;
import com.example.greeting.domain.GreetingStatus;
import com.example.greeting.repository.GreetingRepository;

/**
 * 登録、一覧取得、未処理レコードの更新を行う。
 */
@Service
public class GreetingService {

    private final GreetingRepository greetingRepository;
    private final Clock clock;

    public GreetingService(final GreetingRepository greetingRepository, final Clock clock) {
        this.greetingRepository = greetingRepository;
        this.clock = clock;
    }

    /**
     * 名前の前後空白を除いて保存する。状態は未処理。
     */
    @Transactional
    public GreetingResponse register(final String name) {
        String trimmed = name == null ? "" : name.strip();
        if (trimmed.isEmpty()) {
            throw new InvalidGreetingNameException("名前を入力してください。");
        }
        if (trimmed.length() > GreetingNameValidator.MAX_LENGTH) {
            throw new InvalidGreetingNameException("名前は50文字以内で入力してください。");
        }
        Greeting greeting = new Greeting(trimmed, OffsetDateTime.now(clock));
        return GreetingResponse.from(greetingRepository.save(greeting));
    }

    @Transactional(readOnly = true)
    public List<GreetingResponse> findAll() {
        return greetingRepository.findAllByOrderByIdAsc().stream()
                .map(GreetingResponse::from)
                .toList();
    }

    /**
     * PENDINGだけを更新する。{@code @Transactional} のメソッドが終わると変更がコミットされる。
     *
     * @return 今回更新した件数
     */
    @Transactional
    public int completePending() {
        List<Greeting> pending = greetingRepository.findByStatusOrderByIdAsc(GreetingStatus.PENDING);
        OffsetDateTime processedAt = OffsetDateTime.now(clock);
        for (Greeting greeting : pending) {
            greeting.markCompleted("こんにちは、" + greeting.getName() + "さん！", processedAt);
        }
        greetingRepository.saveAll(pending);
        return pending.size();
    }
}
