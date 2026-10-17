package com.example.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Business rules loaded from application.properties:
 * <pre>
 * library.loan.period-days   - allowed loan period in days (default 14)
 * library.fine.daily-amount  - fixed fine per overdue day in Toman (default 5000)
 * </pre>
 */
@ConfigurationProperties(prefix = "library")
public record LibraryProperties(
        @DefaultValue("14") int loanPeriodDays,
        Fine fine) {

    public record Fine(@DefaultValue("5000") long dailyAmount) {
    }
}
