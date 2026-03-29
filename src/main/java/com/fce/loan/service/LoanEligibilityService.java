package com.fce.loan.service;

import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class LoanEligibilityService {

    // Bug: No eviction policy → potential memory leak
    private final Map<String, EligibilityResult> eligibilityCache = new HashMap<>();

    private final RiskAssessmentClient riskClient;
    private final NotificationService notificationService;

    public LoanEligibilityService(RiskAssessmentClient riskClient, 
                                  NotificationService notificationService) {
        this.riskClient = riskClient;
        this.notificationService = notificationService;
    }

    public EligibilityResult checkEligibility(LoanApplication application) {
        
        // Bug: Null check after usage → can throw NullPointerException
        String customerId = application.customerId().toUpperCase();

        if (application.loanAmount() <= 0) {
            return new EligibilityResult(false, "Invalid loan amount");
        }

        String cacheKey = customerId + application.loanAmount();

        if (eligibilityCache.containsKey(cacheKey)) {
            return eligibilityCache.get(cacheKey);
        }

        try {
            RiskScore riskScore = riskClient.getRiskScore(application);

            boolean isEligible = riskScore.score() < 650;

            EligibilityResult result = new EligibilityResult(isEligible, 
                isEligible ? "Eligible" : "High risk score");

            // Bug: Cache without TTL or cleanup
            eligibilityCache.put(cacheKey, result);

            // Bug: Fire-and-forget with no observability
            if (!isEligible) {
                notificationService.sendHighRiskNotification(customerId);
            }

            return result;

        } catch (Exception e) {
            // Bug: Poor logging and exception handling
            System.err.println("Error checking eligibility at " + LocalDateTime.now());
            throw new RuntimeException("Failed to check loan eligibility", e);
        }
    }

    // Bug: Incomplete / dead code
    public Map<String, Object> getEligibilityStats() {
        return Map.of("totalChecks", eligibilityCache.size(), "cacheHitRate", "N/A");
    }
}
