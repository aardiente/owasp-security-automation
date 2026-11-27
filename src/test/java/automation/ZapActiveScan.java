
package automation;

import org.openqa.selenium.json.Json;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ClientApi;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Manages the standalone ZAP assessment of a lab application and saves its completion status for reports.
 */
public final class ZapActiveScan {

    /**
     * Prevents direct creation of this utility class.
     */
    private ZapActiveScan() {}

    /**
     * Spiders and actively scans a configured lab origin within a deadline, writing a JSON execution summary. Allocates part of the deadline to a ZAP spider crawl and spends the remainder on active scanning; writes completion, progress, timing, and error fields even on failure.
     *
     * @param targetUrl the HTTP(S) URL of the authorized lab application to spider and scan
     * @param limit the maximum allowed duration for the spider and active scan
     * @param statusOutput the path where scan execution status is written as JSON
     * @return {@code true} only if the active scan reached 100% before the deadline; otherwise {@code false}
     */
    public static boolean run(String targetUrl, Duration limit, Path statusOutput) {
        Instant started = Instant.now();
        String scanId = "";
        String spiderId = "";
        String status = "failed";
        String error = "";
        int progress = 0;
        int spiderProgress = 0;
        ClientApi client = null;

        try {
            URI target = URI.create(targetUrl);
            if (!"http".equalsIgnoreCase(target.getScheme()) && !"https".equalsIgnoreCase(target.getScheme()))
                throw new IllegalArgumentException("Active scanning requires HTTP(S).");
            if (target.getHost() == null || target.getRawAuthority() == null)
                throw new IllegalArgumentException("Invalid active scan target.");

            String origin = target.getScheme() + "://" + target.getRawAuthority();
            String contextName = "Automation_" + target.getHost().replaceAll("[^A-Za-z0-9_-]", "_");

            client = ZapManager.getZapClient();
            client.context.newContext(contextName);
            client.context.includeInContext(contextName, Pattern.quote(origin) + "(?:/.*|\\?.*)?");
            client.context.setContextInScope(contextName, "true");
            client.core.accessUrl(origin + "/", "false");

            Instant deadline = Instant.now().plus(limit);
            Instant spiderDeadline = Instant.now().plus(limit.dividedBy(5));

            ApiResponseElement spiderStart = (ApiResponseElement) client.spider.scan(origin + "/", "20", "true", contextName, "false");
            spiderId = spiderStart.getValue();
            status = "spider_running";
            System.out.println("[ZAP SPIDER] Started " + spiderId + " for " + origin);

            while (Instant.now().isBefore(spiderDeadline) && Instant.now().isBefore(deadline)) {
                spiderProgress = Integer.parseInt(((ApiResponseElement) client.spider.status(spiderId)).getValue());
                if (spiderProgress >= 100) break;

                Thread.sleep(1000);
            }

            if (spiderProgress < 100) {
                client.spider.stop(spiderId);
                System.out.println("[ZAP SPIDER] Stopped at crawl time limit.");
            }

            if (!Instant.now().isBefore(deadline)) {
                status = "timed_out";
            } else {
                ApiResponseElement scanStart = (ApiResponseElement) client.ascan.scan(
                        origin + "/", "true", "true", null, null, null);
                scanId = scanStart.getValue();
                status = "running";
                System.out.println("[ZAP ACTIVE] Started " + scanId + " for " + origin);

                while (Instant.now().isBefore(deadline)) {
                    progress = Integer.parseInt(((ApiResponseElement) client.ascan.status(scanId)).getValue());

                    if (progress >= 100) {
                        status = "completed";
                        break;
                    }

                    Thread.sleep(1000);
                }

                if (!"completed".equals(status)) {
                    client.ascan.stop(scanId);
                    status = "timed_out";
                }
            }

            System.out.println("[ZAP ACTIVE] Status: " + status +
                    " | Progress: " + progress + "%");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            status = "interrupted";
            error = "Scan thread interrupted.";

            if (client != null)
                stop(client, scanId, spiderId);
        } catch (Exception e) {
            status = "failed";
            error = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
            System.err.println("[ZAP ACTIVE] " + error);

            if (client != null)
                stop(client, scanId, spiderId);

        } finally {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("status", status);
            result.put("target", targetUrl);
            result.put("scanId", scanId);
            result.put("spiderId", spiderId);
            result.put("progress", progress);
            result.put("spiderProgress", spiderProgress);
            result.put("startedAt", started.toString());
            result.put("finishedAt", Instant.now().toString());
            result.put("durationMs", Duration.between(started, Instant.now()).toMillis());
            result.put("error", error);

            try {
                Path destination = statusOutput.toAbsolutePath().normalize();
                if (destination.getParent() != null)
                    Files.createDirectories(destination.getParent());

                Files.writeString(destination, new Json().toJson(result), StandardCharsets.UTF_8);
                System.out.println("[ZAP ACTIVE] Status saved: " + destination);
            } catch (Exception e) {
                System.err.println("[ZAP ACTIVE] Could not save status: " + e.getMessage());
            }
        }

        return "completed".equals(status);
    }

    /**
     * Attempts to stop any active ZAP scanner and spider jobs that were started.
     *
     * @param client the ZAP API client controlling scanner jobs
     * @param scanId the identifier of the active-scan job to stop
     * @param spiderId the identifier of the spider job to stop
     */
    private static void stop(ClientApi client, String scanId, String spiderId) {
        if (!scanId.isBlank()) {
            try { client.ascan.stop(scanId); }
            catch (Exception e) { System.err.println("[ZAP] Stop active scan: " + e.getMessage()); }
        }
        if (!spiderId.isBlank()) {
            try { client.spider.stop(spiderId); }
            catch (Exception e) { System.err.println("[ZAP] Stop spider: " + e.getMessage()); }
        }
    }
}
