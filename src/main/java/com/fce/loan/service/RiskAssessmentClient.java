package com.fce.loan.service;

public interface RiskAssessmentClient {
    RiskScore getRiskScore(LoanApplication application);
}
