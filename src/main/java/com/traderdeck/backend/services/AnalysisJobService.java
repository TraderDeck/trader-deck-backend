package com.traderdeck.backend.services;

import com.traderdeck.backend.dto.AnalysisRequest;
import com.traderdeck.backend.dto.AnalysisResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisJobService {

    private final BedrockAnalysisService bedrockAnalysisService;

    private ThreadPoolTaskExecutor executor;
    private final Map<String, AnalysisResponse> results = new ConcurrentHashMap<>();
    private final Map<String, Instant> submittedAt = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("analysis-job-");
        executor.initialize();
    }

    public String enqueue(AnalysisRequest request, UUID userId) {
        String jobId = UUID.randomUUID().toString();
        submittedAt.put(jobId, Instant.now());
        log.info("Enqueued analysis job {} ticker={} userId={}", jobId, request.getTickerSymbol(), userId);
        executor.submit(() -> {
            try {
                AnalysisResponse resp = bedrockAnalysisService.analyzeStock(request.getTickerSymbol(), request.getUserPrompt(), userId);
                results.put(jobId, resp);
                log.info("Completed analysis job {} ticker={} agents={} ", jobId, request.getTickerSymbol(), resp.getAgents()==null?0:resp.getAgents().size());
            } catch (Exception ex) {
                log.error("Job {} failed: {}", jobId, ex.getMessage(), ex);
                results.put(jobId, AnalysisResponse.builder()
                        .tickerSymbol(request.getTickerSymbol())
                        .agents(java.util.List.of(
                                AnalysisResponse.AgentResponse.builder()
                                        .agent("error")
                                        .summary("Job failed: " + ex.getMessage())
                                        .redFlags(java.util.List.of("error"))
                                        .greenFlags(java.util.List.of())
                                        .buyScore(null)
                                        .extra(java.util.Map.of("detail", ex.getClass().getSimpleName()))
                                        .build()
                        ))
                        .build());
            }
        });
        return jobId;
    }

    public Optional<AnalysisResponse> getResult(String jobId) {
        return Optional.ofNullable(results.get(jobId));
    }

    public Optional<Instant> getSubmittedAt(String jobId) { return Optional.ofNullable(submittedAt.get(jobId)); }
}
