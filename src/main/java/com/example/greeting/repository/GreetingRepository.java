package com.example.greeting.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.greeting.domain.Greeting;
import com.example.greeting.domain.GreetingStatus;

/**
 * Greetingテーブルへのアクセス。
 * メソッド名からSQLを組み立てるのがSpring Data JPAの派生クエリ。
 */
public interface GreetingRepository extends JpaRepository<Greeting, Long> {

    List<Greeting> findAllByOrderByIdAsc();

    List<Greeting> findByStatusOrderByIdAsc(GreetingStatus status);
}
