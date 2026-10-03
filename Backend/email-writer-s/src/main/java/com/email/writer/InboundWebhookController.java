package com.email.writer;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/webhook/trigger")
@RequiredArgsConstructor
public class InboundWebhookController {

    private final InboundWebhookService webhookService;

    @PostMapping("/{source}")
    public ResponseEntity<String> receiveWebhook(
            @PathVariable String source,
            @RequestHeader(value = "X-Webhook-Signature", required = false) String signature,
            @RequestBody String rawPayload) {
        
        // Validate cryptographic signature
        if (!webhookService.validateSignature(rawPayload, signature)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing webhook signature.");
        }

        // Add to persistent queue
        webhookService.queueWebhookEvent(source, rawPayload);

        return ResponseEntity.accepted().body("Webhook accepted and queued for processing.");
    }
}
