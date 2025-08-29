# Asynchronous Stock Analysis Architecture: Decoupling Long-Running Bedrock Workloads to Overcome CloudFront Timeouts

## CloudFront Limitation (Why Synchronous Broke)
- CloudFront + ALB + Spring origin has hard/soft timeouts (e.g. 30–60s to first byte, 1 minute idle). Your multi-agent Bedrock analysis can exceed that (model calls + news fetch + aggregation), causing 504/timeout or forced truncation.
- Large synchronous response also ties up origin threads (Tomcat), reducing throughput and increasing latency for others.
- Retries by the browser (user hitting again) would re-run expensive AI calls (no dedupe) and amplify cost.

## Asynchronous Pattern Implemented
1. Client `POST /analysis/analyze` with ticker & prompt.
2. Controller immediately:
   - Validates input.
   - Enqueues work (e.g. via ExecutorService / @Async) and returns `202 Accepted` + `{ jobId }` (`JobAcceptedResponse`).
   - `jobId` (UUID) becomes lookup key.
3. Worker thread performs:
   - Fan-out: fundamentals, technicals, news, trade synthesis (possibly sequential or parallel).
   - External API / Bedrock calls.
   - Normalizes agent responses.
   - Stores interim/final result in a shared store (in-memory map, cache, or DB) keyed by `jobId` with status = `RUNNING → COMPLETED / FAILED`.
4. Client polling:
   - `GET /analysis/analyze/{jobId}`.
   - While `RUNNING`: returns `202` (no body or minimal `{ status }`).
   - When `COMPLETE`: returns `200` + full `AnalysisResponse`.
   - On failure: `500/4xx` with error payload.
5. Frontend adaptation:
   - `startAnalysis` detects two modes:
     - New async (`202 + jobId`) → begin polling.
     - Legacy sync (`200 + full payload`) → display immediately.
   - Poll interval with mild backoff (2s then 4s) to limit load.
   - Stops polling on completion/timeout/error; avoids `/undefined 404` by validating `jobId` presence.

## How It Solves CloudFront Timeout
- Origin response for `POST` is fast (<100 ms), well under CloudFront limits.
- Long compute runs detached; no need for client to hold open a potentially >60s connection.
- Polling requests are lightweight and short-lived, each resetting CloudFront/origin timing expectations.
- Prevents 504s and preserves perceived responsiveness (user sees loading skeletons immediately).

## Additional Benefits
- **Resource isolation:** Long jobs don’t monopolize request threads.
- **Retry safety:** Repeated clicks reuse existing job (can add idempotency by hashing request).
- **Cost control:** Can add caching layer keyed by (ticker, date) to serve identical analyses without recompute.
- **Progressive enhancement:** Could upgrade to SSE/WebSocket later for push without changing job model.

## Potential Enhancements (Optional)
- Persist jobs (DB / Redis) for resilience across restarts.
- TTL cleanup (e.g. purge jobs > N hours).
- Cancellation endpoint (`DELETE /analysis/analyze/{jobId}`) to drop stale work.
- Partial progress metadata in polling (e.g. `{ stage: 'news', percent: 55 }`).
- Exponential backoff after longer intervals to further reduce load.

## Why Synchronous Previously Failed
- Frontend waited for full AI aggregation; if duration > CloudFront/ALB threshold, edge sent timeout before origin finished.
- Re-runs caused duplicate Bedrock calls (increased latency & cost).
- No mechanism to differentiate completed vs still-running; user-facing 404 due to missing `jobId` misinterpretation.

## Net Result
- Async layer decouples user latency from AI latency, conforms to CloudFront timing constraints, and adds robustness/flexibility without sacrificing UX (skeleton + “Analyzing…”).

Need architecture diagram text or guidance on implementing storage/queue? Ask and specify.

