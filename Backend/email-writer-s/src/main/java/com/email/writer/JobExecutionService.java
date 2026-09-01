package com.email.writer;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Unified service to wrap background jobs in robust transactional boundaries.
 * In a fully distributed environment, this would integrate with ShedLock.
 */
@Service
@RequiredArgsConstructor
public class JobExecutionService {
    
    // In a real scenario, this would have ShedLock annotations like @SchedulerLock
    @Transactional
    public void executeTransactionalJob(Runnable job) {
        job.run();
    }
}
