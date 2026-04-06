package com.finance.dashboard.dto.request;

import com.finance.dashboard.enums.RecordCategory;
import com.finance.dashboard.enums.RecordType;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateFinancialRecordRequest {

    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 13, fraction = 2, message = "Amount must have at most 13 integer digits and 2 decimal places")
    private BigDecimal amount;

    private RecordType type;

    private RecordCategory category;

    @PastOrPresent(message = "Date cannot be in the future")
    private LocalDate date;

    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    @Size(max = 500, message = "Notes must not exceed 500 characters")
    private String notes;
}
