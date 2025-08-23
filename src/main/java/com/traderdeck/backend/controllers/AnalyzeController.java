package com.traderdeck.backend.controllers;

import com.traderdeck.backend.dto.AnalysisRequest;
import com.traderdeck.backend.dto.AnalysisResponse;
import com.traderdeck.backend.dto.JobAcceptedResponse;
import com.traderdeck.backend.services.AnalysisJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/analysis")
@RequiredArgsConstructor
@Slf4j
public class AnalyzeController {

    private final AnalysisJobService jobService;

    @PostMapping("/analyze")
    public ResponseEntity<JobAcceptedResponse> analyzeAsync(@RequestBody AnalysisRequest request, @RequestHeader(value = "X-User-Id", required = false) String userIdHeader) {
        UUID userId = parseUser(userIdHeader);
        log.info("Async analyze request ticker={} userId={}", request.getTickerSymbol(), userId);
        String jobId = jobService.enqueue(request, userId);
        return ResponseEntity.accepted().body(new JobAcceptedResponse(jobId));
    }

    @GetMapping("/analyze/{jobId}")
    public ResponseEntity<?> getResult(@PathVariable String jobId) {
        Optional<AnalysisResponse> result = jobService.getResult(jobId);
        return result.<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(202).body(Map.of("jobId", jobId, "status", "IN_PROGRESS")));
    }

    private UUID parseUser(String raw) {
        try { return raw == null? UUID.randomUUID(): UUID.fromString(raw); } catch (Exception e) { return UUID.randomUUID(); }
    }
}