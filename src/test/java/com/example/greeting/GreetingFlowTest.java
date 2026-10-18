package com.example.greeting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.example.greeting.domain.Greeting;
import com.example.greeting.domain.GreetingStatus;
import com.example.greeting.repository.GreetingRepository;

/**
 * 登録、検証、バッチ更新の一連の流れを確認する。
 */
@SpringBootTest
@AutoConfigureMockMvc
class GreetingFlowTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private GreetingRepository greetingRepository;

    @BeforeEach
    void clean() {
        greetingRepository.deleteAll();
    }

    @Test
    @DisplayName("正常な名前を登録するとPENDINGになり、前後の空白は除かれる")
    void registerValidName() throws Exception {
        mockMvc.perform(post("/api/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonName("  田中  ")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("田中"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.message").value(nullValue()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.processedAt").value(nullValue()));

        Greeting saved = greetingRepository.findAll().getFirst();
        assertThat(saved.getStatus()).isEqualTo(GreetingStatus.PENDING);
        assertThat(saved.getName()).isEqualTo("田中");
        assertThat(saved.getMessage()).isNull();
        assertThat(saved.getProcessedAt()).isNull();
    }

    @Test
    @DisplayName("空白のみの名前は400になる")
    void rejectBlankName() throws Exception {
        mockMvc.perform(post("/api/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonName("   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("名前を入力してください。"))
                .andExpect(jsonPath("$.trace").doesNotExist());

        assertThat(greetingRepository.count()).isZero();
    }

    @Test
    @DisplayName("50文字を超える名前は400になる")
    void rejectTooLongName() throws Exception {
        mockMvc.perform(post("/api/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonName("あ".repeat(51))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("名前は50文字以内で入力してください。"));

        mockMvc.perform(post("/api/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonName("あ".repeat(50))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("あ".repeat(50)));
    }

    @Test
    @DisplayName("不正なJSONは400になる")
    void rejectInvalidJson() throws Exception {
        mockMvc.perform(post("/api/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("JSONの形式が正しくありません。"))
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    @DisplayName("データがないときは空配列を返す")
    void listEmpty() throws Exception {
        mockMvc.perform(get("/api/greetings"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("バッチ実行でメッセージと処理日時が付きCOMPLETEDになる")
    void batchCompletesPending() throws Exception {
        mockMvc.perform(post("/api/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonName("田中")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/batch/greetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobExecutionId").isNumber())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.processedCount").value(1));

        mockMvc.perform(get("/api/greetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("COMPLETED"))
                .andExpect(jsonPath("$[0].message").value("こんにちは、田中さん！"))
                .andExpect(jsonPath("$[0].processedAt").isNotEmpty());
    }

    @Test
    @DisplayName("再実行では処理済みデータは変わらず処理件数0になる")
    void secondRunDoesNotChangeCompleted() throws Exception {
        mockMvc.perform(post("/api/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonName("田中")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/batch/greetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1));

        Greeting completed = greetingRepository.findAll().getFirst();

        mockMvc.perform(post("/api/batch/greetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.processedCount").value(0));

        Greeting again = greetingRepository.findById(completed.getId()).orElseThrow();
        assertThat(again.getStatus()).isEqualTo(GreetingStatus.COMPLETED);
        assertThat(again.getMessage()).isEqualTo(completed.getMessage());
        assertThat(again.getProcessedAt()).isEqualTo(completed.getProcessedAt());
    }

    @Test
    @DisplayName("処理済みと未処理が混在するときは未処理だけ更新する")
    void batchUpdatesOnlyPending() throws Exception {
        mockMvc.perform(post("/api/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonName("佐藤")))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/batch/greetings"))
                .andExpect(status().isOk());
        Greeting completed = greetingRepository.findAllByOrderByIdAsc().getFirst();

        mockMvc.perform(post("/api/greetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonName("鈴木")))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/batch/greetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(1));

        var all = greetingRepository.findAllByOrderByIdAsc();
        assertThat(all).hasSize(2);
        assertThat(all.get(0).getMessage()).isEqualTo(completed.getMessage());
        assertThat(all.get(0).getProcessedAt()).isEqualTo(completed.getProcessedAt());
        assertThat(all.get(0).getStatus()).isEqualTo(GreetingStatus.COMPLETED);
        assertThat(all.get(1).getName()).isEqualTo("鈴木");
        assertThat(all.get(1).getStatus()).isEqualTo(GreetingStatus.COMPLETED);
        assertThat(all.get(1).getMessage()).isEqualTo("こんにちは、鈴木さん！");
        assertThat(all.get(1).getProcessedAt()).isNotNull();
    }

    @Test
    @DisplayName("データが0件でもバッチは正常終了し処理件数0になる")
    void batchWithNoData() throws Exception {
        mockMvc.perform(post("/api/batch/greetings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.processedCount").value(0));

        mockMvc.perform(get("/api/greetings"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("トップ画面に登録とバッチ実行が表示される")
    void pageShowsControls() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("登録")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("バッチ実行")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("一覧更新")));
    }

    private String jsonName(final String name) {
        return "{\"name\":\"" + name + "\"}";
    }
}
