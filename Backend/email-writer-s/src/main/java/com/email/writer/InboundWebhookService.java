package com.email.writer;

import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import java.nio.charset.StandardCharsets;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class InboundWebhookService {

    private final WebhookEventRepository webhookEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${webhook.secret:my_super_secret_webhook_key}")
    private String webhookSecret;

    public boolean validateSignature(String payload, String signature) {
        if (signature == null || signature.isEmpty()) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expectedSignature = HexFormat.of().formatHex(hmacBytes);
            
            // Typical format is "sha256=xxxx" or just "xxxx"
            String providedSignature = signature.startsWith("sha256=") ? signature.substring(7) : signature;
            
            return java.security.MessageDigest.isEqual(
                expectedSignature.getBytes(StandardCharsets.UTF_8), 
                providedSignature.getBytes(StandardCharsets.UTF_8)
            );
        } catch (Exception e) {
            return false;
        }
    }

    public void queueWebhookEvent(String source, String payload) {
        WebhookEvent event = WebhookEvent.builder()
                .triggerSource(source)
                .payloadJson(payload)
                .status("PENDING")
                .retryCount(0)
                .build();
        
        webhookEventRepository.save(event);
    }
}
