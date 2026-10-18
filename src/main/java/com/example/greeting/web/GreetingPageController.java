package com.example.greeting.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * 画面のHTMLだけを返す。一覧データの取得はブラウザのfetchがAPIを呼ぶ。
 */
@Controller
public class GreetingPageController {

    @GetMapping("/")
    public String index(final Model model) {
        model.addAttribute("title", "あいさつメッセージ生成");
        return "index";
    }
}
