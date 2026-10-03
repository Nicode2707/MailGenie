package com.email.writer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookConfigService and related entities.
 * Covers CRUD validation, event dispatching, delivery log queries,
 * and entity state transitions.
 */
@ExtendWith(MockitoExtension.class)
class WebhookConfigServiceTest {

    @Mock
    private WebhookConfigRepository configRepository;

    @Mock
    private WebhookDeliveryLogRepository deliveryLogRepository;

    @Mock
    private WebhookDeliveryService deliveryService;

    @InjectMocks
    private WebhookConfigService webhookConfigService;

    private WebhookConfig sampleConfig;

    @BeforeEach
    void setUp() {
        sampleConfig = WebhookConfig.builder()
                .id(1L)
                .name("Test Webhook")
                .url("https://example.com/webhook")
                .secret("my-secret-key")
                .eventType("EMAIL_GENERATED")
                .active(true)
                .maxRetries(3)
                .timeoutSeconds(10)
                .successCount(0)
                .failureCount(0)
                .build();
    }

    // ---- CRUD Tests ----

    @Test
    void createWebhook_validConfig_savesAndReturns() {
        when(configRepository.save(any(WebhookConfig.class))).thenReturn(sampleConfig);
        WebhookConfig result = webhookConfigService.createWebhook(sampleConfig);
        assertNotNull(result);
        assertEquals("Test Webhook", result.getName());
        verify(configRepository).save(any(WebhookConfig.class));
    }

    @Test
    void createWebhook_missingUrl_throwsException() {
        sampleConfig.setUrl(null);
        assertThrows(IllegalArgumentException.class,
                () -> webhookConfigService.createWebhook(sampleConfig));
    }

    @Test
    void createWebhook_missingName_throwsException() {
        sampleConfig.setName("");
        assertThrows(IllegalArgumentException.class,
                () -> webhookConfigService.createWebhook(sampleConfig));
    }

    @Test
    void createWebhook_invalidUrlProtocol_throwsException() {
        sampleConfig.setUrl("ftp://example.com/webhook");
        assertThrows(IllegalArgumentException.class,
                () -> webhookConfigService.createWebhook(sampleConfig));
    }

    @Test
    void getById_existingId_returnsConfig() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(sampleConfig));
        WebhookConfig result = webhookConfigService.getById(1L);
        assertEquals(1L, result.getId());
    }

    @Test
    void getById_missingId_throwsException() {
        when(configRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NoSuchElementException.class,
                () -> webhookConfigService.getById(99L));
    }

    @Test
    void deleteWebhook_existingId_deletes() {
        when(configRepository.existsById(1L)).thenReturn(true);
        assertTrue(webhookConfigService.deleteWebhook(1L));
        verify(configRepository).deleteById(1L);
    }

    @Test
    void deleteWebhook_missingId_returnsFalse() {
        when(configRepository.existsById(99L)).thenReturn(false);
        assertFalse(webhookConfigService.deleteWebhook(99L));
    }

    @Test
    void toggleActive_activeConfig_deactivates() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(sampleConfig));
        when(configRepository.save(any())).thenReturn(sampleConfig);

        WebhookConfig result = webhookConfigService.toggleActive(1L);
        assertFalse(result.isActive());
    }

    @Test
    void updateWebhook_updatesNameAndUrl() {
        when(configRepository.findById(1L)).thenReturn(Optional.of(sampleConfig));
        when(configRepository.save(any())).thenReturn(sampleConfig);

        WebhookConfig updates = new WebhookConfig();
        updates.setName("Updated Name");
        updates.setUrl("https://updated.com/webhook");

        WebhookConfig result = webhookConfigService.updateWebhook(1L, updates);
        assertEquals("Updated Name", result.getName());
    }

    // ---- Event Dispatch Tests ----

    @Test
    void dispatchEvent_matchingWebhooks_deliversToAll() {
        WebhookConfig w2 = WebhookConfig.builder()
                .id(2L).name("W2").url("https://example.com/hook2")
                .active(true).eventType("EMAIL_GENERATED")
                .maxRetries(1).timeoutSeconds(5)
                .build();

        when(configRepository.findActiveByEventType("EMAIL_GENERATED"))
                .thenReturn(List.of(sampleConfig, w2));
        when(deliveryService.deliver(any(), anyString(), any())).thenReturn(
                WebhookDeliveryLog.builder().id(1L).status("DELIVERED").build());

        Map<String, Object> data = Map.of("provider", "groq", "tone", "professional");
        webhookConfigService.dispatchEvent("EMAIL_GENERATED", data);

        verify(deliveryService, times(2)).deliver(any(), anyString(), any());
    }

    @Test
    void dispatchEvent_noMatchingWebhooks_noDelivery() {
        when(configRepository.findActiveByEventType("EMAIL_GENERATED"))
                .thenReturn(Collections.emptyList());

        webhookConfigService.dispatchEvent("EMAIL_GENERATED", Map.of());
        verify(deliveryService, never()).deliver(any(), anyString(), any());
    }

    @Test
    void dispatchEvent_deliveryFailure_doesNotThrow() {
        when(configRepository.findActiveByEventType("EMAIL_GENERATED"))
                .thenReturn(List.of(sampleConfig));
        when(deliveryService.deliver(any(), anyString(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        assertDoesNotThrow(() ->
                webhookConfigService.dispatchEvent("EMAIL_GENERATED", Map.of()));
    }

    @Test
    void dispatchEmailGenerated_buildsCorrectPayload() {
        when(configRepository.findActiveByEventType("EMAIL_GENERATED"))
                .thenReturn(List.of(sampleConfig));
        when(deliveryService.deliver(any(), anyString(), any())).thenReturn(
                WebhookDeliveryLog.builder().id(1L).status("DELIVERED").build());

        webhookConfigService.dispatchEmailGenerated("groq", "professional", 250);
        verify(deliveryService).deliver(eq(sampleConfig), eq("EMAIL_GENERATED"), any());
    }

    @Test
    void dispatchEmailScheduled_buildsCorrectPayload() {
        when(configRepository.findActiveByEventType("EMAIL_SCHEDULED"))
                .thenReturn(List.of(sampleConfig));
        when(deliveryService.deliver(any(), anyString(), any())).thenReturn(
                WebhookDeliveryLog.builder().id(1L).status("DELIVERED").build());

        webhookConfigService.dispatchEmailScheduled(42L, "jane@test.com", "2026-09-01T10:00:00");
        verify(deliveryService).deliver(eq(sampleConfig), eq("EMAIL_SCHEDULED"), any());
    }

    @Test
    void dispatchEmailFailed_buildsCorrectPayload() {
        when(configRepository.findActiveByEventType("EMAIL_FAILED"))
                .thenReturn(List.of(sampleConfig));
        when(deliveryService.deliver(any(), anyString(), any())).thenReturn(
                WebhookDeliveryLog.builder().id(1L).status("DELIVERED").build());

        webhookConfigService.dispatchEmailFailed("groq", "API key invalid");
        verify(deliveryService).deliver(eq(sampleConfig), eq("EMAIL_FAILED"), any());
    }

    // ---- Delivery Logs Tests ----

    @Test
    void getDeliveryLogs_delegatesToRepository() {
        List<WebhookDeliveryLog> logs = List.of(
                WebhookDeliveryLog.builder().id(1L).webhookConfigId(1L).status("DELIVERED").build()
        );
        when(deliveryLogRepository.findByWebhookConfigIdOrderByCreatedAtDesc(1L))
                .thenReturn(logs);

        List<WebhookDeliveryLog> result = webhookConfigService.getDeliveryLogs(1L);
        assertEquals(1, result.size());
    }

    @Test
    void getDeliveryStats_returnsCountsByStatus() {
        when(deliveryLogRepository.statsByWebhookId(1L)).thenReturn(List.of(
                new Object[]{"DELIVERED", 10L},
                new Object[]{"FAILED", 2L}
        ));

        Map<String, Long> stats = webhookConfigService.getDeliveryStats(1L);
        assertEquals(10L, stats.get("DELIVERED"));
        assertEquals(2L, stats.get("FAILED"));
        assertEquals(0L, stats.get("EXHAUSTED"));
    }

    @Test
    void getGlobalDeliverySummary_delegatesToRepository() {
        when(deliveryLogRepository.countByStatus()).thenReturn(List.of(
                new Object[]{"DELIVERED", 50L},
                new Object[]{"FAILED", 5L}
        ));

        Map<String, Long> summary = webhookConfigService.getGlobalDeliverySummary();
        assertEquals(50L, summary.get("DELIVERED"));
        assertEquals(5L, summary.get("FAILED"));
    }

    @Test
    void getRecentFailures_delegatesToRepository() {
        when(deliveryLogRepository.findRecentFailures(5)).thenReturn(Collections.emptyList());
        List<WebhookDeliveryLog> result = webhookConfigService.getRecentFailures(5);
        assertTrue(result.isEmpty());
    }

    @Test
    void purgeOldLogs_delegatesToRepository() {
        when(deliveryLogRepository.purgeOlderThan(any())).thenReturn(7);
        int purged = webhookConfigService.purgeOldLogs(30);
        assertEquals(7, purged);
    }

    // ---- WebhookConfig Entity Tests ----

    @Test
    void webhookConfig_subscribesTo_matchingType_returnsTrue() {
        assertTrue(sampleConfig.subscribesTo("EMAIL_GENERATED"));
    }

    @Test
    void webhookConfig_subscribesTo_wildcard_returnsTrue() {
        sampleConfig.setEventType("*");
        assertTrue(sampleConfig.subscribesTo("ANY_EVENT"));
    }

    @Test
    void webhookConfig_subscribesTo_nonMatching_returnsFalse() {
        assertFalse(sampleConfig.subscribesTo("EMAIL_FAILED"));
    }

    @Test
    void webhookConfig_subscribesTo_commaSeparatedMatchesFirst() {
        sampleConfig.setEventType("EMAIL_GENERATED,EMAIL_SCHEDULED");
        assertTrue(sampleConfig.subscribesTo("EMAIL_SCHEDULED"));
    }

    @Test
    void webhookConfig_recordSuccess_incrementsCount() {
        sampleConfig.recordSuccess();
        assertEquals(1, sampleConfig.getSuccessCount());
        assertNotNull(sampleConfig.getLastDeliveredAt());
    }

    @Test
    void webhookConfig_recordFailure_incrementsCount() {
        sampleConfig.recordFailure();
        assertEquals(1, sampleConfig.getFailureCount());
        assertNotNull(sampleConfig.getLastFailedAt());
    }

    @Test
    void webhookConfig_getSuccessRate_zeroAttempts_returnsZero() {
        assertEquals(0.0, sampleConfig.getSuccessRate());
    }

    @Test
    void webhookConfig_getSuccessRate_calculatesCorrectly() {
        sampleConfig.setSuccessCount(7);
        sampleConfig.setFailureCount(3);
        assertEquals(70.0, sampleConfig.getSuccessRate());
    }

    @Test
    void webhookConfig_getTotalAttempts_sumsCorrectly() {
        sampleConfig.setSuccessCount(10);
        sampleConfig.setFailureCount(5);
        assertEquals(15, sampleConfig.getTotalAttempts());
    }

    // ---- WebhookDeliveryLog Entity Tests ----

    @Test
    void deliveryLog_markDelivered_setsAllFields() {
        WebhookDeliveryLog log = WebhookDeliveryLog.builder().build();
        log.markDelivered(200, "OK", 150L);
        assertEquals("DELIVERED", log.getStatus());
        assertEquals(200, log.getHttpStatusCode());
        assertEquals(150L, log.getDurationMs());
    }

    @Test
    void deliveryLog_markFailed_setsError() {
        WebhookDeliveryLog log = WebhookDeliveryLog.builder().build();
        log.markFailed("Connection refused", 5000L);
        assertEquals("FAILED", log.getStatus());
        assertEquals("Connection refused", log.getErrorMessage());
    }

    @Test
    void deliveryLog_markExhausted_setsTerminalState() {
        WebhookDeliveryLog log = WebhookDeliveryLog.builder().build();
        log.markExhausted("All retries failed");
        assertEquals("EXHAUSTED", log.getStatus());
        assertTrue(log.isTerminal());
    }

    @Test
    void deliveryLog_isTerminal_delivered_returnsTrue() {
        WebhookDeliveryLog log = WebhookDeliveryLog.builder().status("DELIVERED").build();
        assertTrue(log.isTerminal());
    }

    @Test
    void deliveryLog_getRetryDelayMs_exponentialBackoff() {
        WebhookDeliveryLog log1 = WebhookDeliveryLog.builder().attemptNumber(1).build();
        assertEquals(1000, log1.getRetryDelayMs());
        WebhookDeliveryLog log3 = WebhookDeliveryLog.builder().attemptNumber(3).build();
        assertEquals(4000, log3.getRetryDelayMs());
    }

    // ---- HMAC Signature Tests ----

    @Test
    void generateSignature_validSecret_returnsHex() {
        String sig = WebhookDeliveryService.generateSignature("secret", "payload");
        assertNotNull(sig);
        assertFalse(sig.isEmpty());
    }

    @Test
    void generateSignature_nullSecret_returnsEmpty() {
        assertEquals("", WebhookDeliveryService.generateSignature(null, "payload"));
    }

    @Test
    void verifySignature_matchingSignature_returnsTrue() {
        String payload = "test-payload";
        String secret = "my-secret";
        String sig = WebhookDeliveryService.generateSignature(secret, payload);
        assertTrue(WebhookDeliveryService.verifySignature(secret, payload, sig));
    }

    @Test
    void verifySignature_mismatchedSignature_returnsFalse() {
        assertFalse(WebhookDeliveryService.verifySignature("secret", "payload", "wrong-sig"));
    }
}
