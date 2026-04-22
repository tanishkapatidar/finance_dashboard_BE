package com.finance.dashboard.controller;

import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.DashboardSummaryResponse;
import com.finance.dashboard.exception.BadRequestException;
import com.finance.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.YearMonth;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Dashboard", description = "Aggregated analytics and summary data — all authenticated roles")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/summary")
    @Operation(
        summary = "Full dashboard summary",
        description = """
            Returns a complete dashboard snapshot including:
            - Total income, total expenses, net balance
            - Category-wise breakdown (income and expense)
            - Monthly trends for the last 6 months
            - 10 most recent transactions
            
            **Accessible by: VIEWER, ANALYST, ADMIN**
            """
    )
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        DashboardSummaryResponse summary = dashboardService.getSummary();
        return ResponseEntity.ok(ApiResponse.success("Dashboard summary retrieved successfully", summary));
    }

    @GetMapping("/summary/period")
    @Operation(
        summary = "Period-specific summary",
        description = """
            Returns income, expense, and net balance for a custom date range.
            
            **Accessible by: VIEWER, ANALYST, ADMIN**
            """
    )
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getPeriodSummary(
            @Parameter(description = "Start date (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date (yyyy-MM-dd)", required = true)
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        if (endDate.isBefore(startDate)) {
            throw new BadRequestException("End date must be on or after start date");
        }
        if (startDate.plusYears(5).isBefore(endDate)) {
            throw new BadRequestException("Date range cannot exceed 5 years");
        }

        DashboardSummaryResponse summary = dashboardService.getPeriodSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success("Period summary retrieved successfully", summary));
    }

    @GetMapping("/summary/monthly")
    @Operation(
        summary = "Monthly summary",
        description = """
            Returns income, expense, and net balance for a specific calendar month.
            
            **Accessible by: VIEWER, ANALYST, ADMIN**
            """
    )
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getMonthlySummary(
            @Parameter(description = "Year (e.g. 2025)", required = true) @RequestParam int year,
            @Parameter(description = "Month number 1–12", required = true) @RequestParam int month) {

        if (month < 1 || month > 12) {
            throw new BadRequestException("Month must be between 1 and 12");
        }
        if (year < 2000 || year > LocalDate.now().getYear()) {
            throw new BadRequestException("Year must be between 2000 and the current year");
        }

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startDate = ym.atDay(1);
        LocalDate endDate   = ym.atEndOfMonth();

        DashboardSummaryResponse summary = dashboardService.getPeriodSummary(startDate, endDate);
        return ResponseEntity.ok(ApiResponse.success(
            "Monthly summary for " + ym + " retrieved successfully", summary));
    }
}
