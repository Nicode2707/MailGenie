package com.email.writer;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import java.time.LocalDateTime;

/**
 * Entity tracking blocklist (DNSBL) entries for sender domains.
 * Monitors whether a domain appears on known spam blocklists.
 */
@Entity
@Table(name = "domain_blocklist_entries", indexes = {
        @Index(name = "idx_dbe_domain", columnList = "domain"),
        @Index(name = "idx_dbe_listed", columnList = "isListed")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DomainBlocklistEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The domain being checked.
     */
    @Column(nullable = false, length = 255)
    private String domain;

    /**
     * Name of the blocklist (e.g., Spamhaus ZEN, SORBS, Barracuda).
     */
    @Column(nullable = false, length = 100)
    private String blocklistName;

    /**
     * DNSBL lookup result (e.g., 127.0.0.2 listed, NXDOMAIN clean).
     */
    @Column(length = 100)
    private String lookupResult;

    /**
     * Whether the domain is currently listed on this blocklist.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isListed = false;

    /**
     * When the listing was first detected.
     */
    private LocalDateTime listedAt;

    /**
     * When the listing was last confirmed.
     */
    private LocalDateTime lastCheckedAt;

    /**
     * When the listing was removed (if delisted).
     */
    private LocalDateTime delistedAt;

    /**
     * Whether an alert has been sent for this listing.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean alertSent = false;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
