package com.example.greeting.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.greeting.api.dto.BatchRunResponse;
import com.example.greeting.api.dto.CreateGreetingRequest;
import com.example.greeting.api.dto.GreetingResponse;
import com.example.greeting.batch.GreetingBatchLauncher;
import com.example.greeting.service.GreetingService;

import jakarta.validation.Valid;

/**
 * あいさつとバッチ起動のREST API。業務処理はServiceとLauncherに委譲する。
 */
@RestController
public class GreetingApiController {

    private final GreetingService greetingService;
    private final GreetingBatchLauncher greetingBatchLauncher;

    public GreetingApiController(final GreetingService greetingService,
                                 final GreetingBatchLauncher greetingBatchLauncher) {
        this.greetingService = greetingService;
        this.greetingBatchLauncher = greetingBatchLauncher;
    }

    @PostMapping("/api/greetings")
    public ResponseEntity<GreetingResponse> create(@Valid @RequestBody final CreateGreetingRequest request) {
        GreetingResponse response = greetingService.register(request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/greetings")
    public List<GreetingResponse> list() {
        return greetingService.findAll();
    }

    @DeleteMapping("/api/greetings/{id}")
    public ResponseEntity<Void> delete(@PathVariable final Long id) {
        greetingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/batch/greetings")
    public BatchRunResponse runBatch() {
        return greetingBatchLauncher.launch();
    }
}
