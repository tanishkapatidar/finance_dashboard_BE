package com.finance.dashboard.service;

import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.dto.response.DashboardSummaryResponse.CategoryBreakdown;
import com.finance.dashboard.dto.response.DashboardSummaryResponse.MonthlyTrend;
import com.finance.dashboard.dto.response.FinancialRecordResponse;
import com.finance.dashboard.entity.FinancialRecord;
import com.finance.dashboard.enums.RecordType;
import com.finance.dashboard.repository.FinancialRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final FinancialRecordRepository recordRepository;
    private final FinancialRecordService financialRecordService;

    private static final int RECENT_ACTIVITY_LIMIT = 10;
    private static final int MONTHS_OF_TRENDS = 6;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary() {
        // --- Totals ---
        BigDecimal totalIncome   = recordRepository.sumByType(RecordType.INCOME);
        BigDecimal totalExpenses = recordRepository.sumByType(RecordType.EXPENSE);
        BigDecimal netBalance    = totalIncome.subtract(totalExpenses);
        long totalRecords        = recordRepository.findWithFilters(null, null, null, null, null,
                                        PageRequest.of(0, 1)).getTotalElements();

        // --- Category Breakdown ---
        CategoryBreakdown breakdown = buildCategoryBreakdown();

        // --- Monthly Trends (last N months) ---
        List<MonthlyTrend> trends = buildMonthlyTrends();

        // --- Recent Activity ---
        List<FinancialRecord> recent = recordRepository.findRecentActivity(
            PageRequest.of(0, RECENT_ACTIVITY_LIMIT));
        List<FinancialRecordResponse> recentActivity = recent.stream()
            .map(financialRecordService::toResponse)
            .collect(Collectors.toList());

        return DashboardSummaryResponse.builder()
            .totalIncome(totalIncome)
            .totalExpenses(totalExpenses)
            .netBalance(netBalance)
            .totalRecords(totalRecords)
            .categoryBreakdown(breakdown)
            .monthlyTrends(trends)
            .recentActivity(recentActivity)
            .build();
    }

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getPeriodSummary(LocalDate startDate, LocalDate endDate) {
        BigDecimal income   = recordRepository.sumByTypeAndDateRange(RecordType.INCOME, startDate, endDate);
        BigDecimal expenses = recordRepository.sumByTypeAndDateRange(RecordType.EXPENSE, startDate, endDate);
        BigDecimal net      = income.subtract(expenses);

        return DashboardSummaryResponse.builder()
            .totalIncome(income)
            .totalExpenses(expenses)
            .netBalance(net)
            .build();
    }

    // --- Private Helpers ---

    private CategoryBreakdown buildCategoryBreakdown() {
        List<Object[]> rows = recordRepository.getCategoryWiseTotals();

        Map<String, BigDecimal> incomeByCategory  = new LinkedHashMap<>();
        Map<String, BigDecimal> expenseByCategory = new LinkedHashMap<>();

        for (Object[] row : rows) {
            String categoryName = row[0].toString();
            RecordType type     = (RecordType) row[1];
            BigDecimal total    = (BigDecimal) row[2];

            if (type == RecordType.INCOME) {
                incomeByCategory.put(categoryName, total);
            } else {
                expenseByCategory.put(categoryName, total);
            }
        }

        return CategoryBreakdown.builder()
            .incomeByCategory(incomeByCategory)
            .expenseByCategory(expenseByCategory)
            .build();
    }

    private List<MonthlyTrend> buildMonthlyTrends() {
        LocalDate startDate = LocalDate.now().minusMonths(MONTHS_OF_TRENDS).withDayOfMonth(1);
        List<Object[]> rows = recordRepository.getMonthlyTrends(startDate);

        // Build a map: "YYYY-MM" -> [income, expenses]
        Map<String, BigDecimal[]> trendMap = new LinkedHashMap<>();

        for (Object[] row : rows) {
            int year       = ((Number) row[0]).intValue();
            int month      = ((Number) row[1]).intValue();
            RecordType type = (RecordType) row[2];
            BigDecimal amt  = (BigDecimal) row[3];

            String key = year + "-" + String.format("%02d", month);
            trendMap.computeIfAbsent(key, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});

            if (type == RecordType.INCOME) {
                trendMap.get(key)[0] = amt;
            } else {
                trendMap.get(key)[1] = amt;
            }
        }

        return trendMap.entrySet().stream().map(entry -> {
            String[] parts = entry.getKey().split("-");
            int year  = Integer.parseInt(parts[0]);
            int month = Integer.parseInt(parts[1]);
            BigDecimal income   = entry.getValue()[0];
            BigDecimal expenses = entry.getValue()[1];
            String monthLabel   = Month.of(month).getDisplayName(TextStyle.SHORT, Locale.ENGLISH) + " " + year;

            return MonthlyTrend.builder()
                .year(year)
                .month(month)
                .monthLabel(monthLabel)
                .income(income)
                .expenses(expenses)
                .net(income.subtract(expenses))
                .build();
        }).collect(Collectors.toList());
    }
}
