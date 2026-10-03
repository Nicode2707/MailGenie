package com.email.writer;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for DomainBlocklistEntry entity.
 */
@Repository
public interface DomainBlocklistEntryRepository extends JpaRepository<DomainBlocklistEntry, Long> {

    List<DomainBlocklistEntry> findByDomain(String domain);

    Optional<DomainBlocklistEntry> findByDomainAndBlocklistName(String domain, String blocklistName);

    List<DomainBlocklistEntry> findByIsListedTrue();

    List<DomainBlocklistEntry> findByDomainAndIsListedTrue(String domain);

    @Query("SELECT b.domain, COUNT(b) FROM DomainBlocklistEntry b WHERE b.isListed = true GROUP BY b.domain")
    List<Object[]> countActiveListingsByDomain();

    long countByDomainAndIsListedTrue(String domain);

    List<DomainBlocklistEntry> findByAlertSentFalseAndIsListedTrue();
}
