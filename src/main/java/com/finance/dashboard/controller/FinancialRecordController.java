package com.finance.dashboard.controller;

import com.finance.dashboard.dto.request.CreateFinancialRecordRequest;
import com.finance.dashboard.dto.request.UpdateFinancialRecordRequest;
import com.finance.dashboard.dto.response.ApiResponse;
import com.finance.dashboard.dto.response.FinancialRecordResponse;
import com.finance.dashboard.dto.response.PagedResponse;
import com.finance.dashboard.enums.RecordCategory;
import com.finance.dashboard.enums.RecordType;
import com.finance.dashboard.service.FinancialRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Financial Records", description = "Manage income and expense records")
public class FinancialRecordController {

    private final FinancialRecordService recordService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Create a financial record",
        description = "Create a new income or expense record. **Admin only.**"
    )
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> createRecord(
            @Valid @RequestBody CreateFinancialRecordRequest request) {
        FinancialRecordResponse record = recordService.createRecord(request);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Financial record created successfully", record));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(
        summary = "List financial records",
        description = """
            Get a paginated list of financial records.
            Supports filtering by type, category, date range, and keyword search.
            **Analyst and Admin only.**
            """
    )
    public ResponseEntity<ApiResponse<PagedResponse<FinancialRecordResponse>>> getRecords(
            @Parameter(description = "Page number (0-indexed)") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size (max 100)") @RequestParam(defaultValue = "10") int size,
            @Parameter(description = "Filter by type: INCOME or EXPENSE") @RequestParam(required = false) RecordType type,
            @Parameter(description = "Filter by category") @RequestParam(required = false) RecordCategory category,
            @Parameter(description = "Filter from date (yyyy-MM-dd)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Filter to date (yyyy-MM-dd)") @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @Parameter(description = "Search in title or notes") @RequestParam(required = false) String search) {

        // Clamp page size to prevent abuse
        int clampedSize = Math.min(size, 100);

        PagedResponse<FinancialRecordResponse> records =
            recordService.getRecords(page, clampedSize, type, category, startDate, endDate, search);

        return ResponseEntity.ok(ApiResponse.success("Records retrieved successfully", records));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'ANALYST')")
    @Operation(summary = "Get a record by ID", description = "Retrieve a specific financial record. **Analyst and Admin only.**")
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> getRecordById(@PathVariable Long id) {
        FinancialRecordResponse record = recordService.getRecordById(id);
        return ResponseEntity.ok(ApiResponse.success("Record retrieved successfully", record));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Update a financial record",
        description = "Partially update any fields of a financial record. **Admin only.**"
    )
    public ResponseEntity<ApiResponse<FinancialRecordResponse>> updateRecord(
            @PathVariable Long id,
            @Valid @RequestBody UpdateFinancialRecordRequest request) {
        FinancialRecordResponse record = recordService.updateRecord(id, request);
        return ResponseEntity.ok(ApiResponse.success("Record updated successfully", record));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(
        summary = "Delete a financial record",
        description = "Soft-delete a financial record (it remains in the database but is hidden). **Admin only.**"
    )
    public ResponseEntity<ApiResponse<Void>> deleteRecord(@PathVariable Long id) {
        recordService.deleteRecord(id);
        return ResponseEntity.ok(ApiResponse.success("Record deleted successfully"));
    }
}
