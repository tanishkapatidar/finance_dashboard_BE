package com.finance.dashboard.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardSummaryResponse {
    private BigDecimal totalIncome;
    private BigDecimal totalExpenses;
    private BigDecimal netBalance;
    private long totalRecords;
    private CategoryBreakdown categoryBreakdown;
    private List<MonthlyTrend> monthlyTrends;
    private List<FinancialRecordResponse> recentActivity;

    @Data
    @Builder
    public static class CategoryBreakdown {
        private Map<String, BigDecimal> incomeByCategory;
        private Map<String, BigDecimal> expenseByCategory;
    }

    @Data
    @Builder
    public static class MonthlyTrend {
        private int year;
        private int month;
        private String monthLabel;
        private BigDecimal income;
        private BigDecimal expenses;
        private BigDecimal net;
    }
}
