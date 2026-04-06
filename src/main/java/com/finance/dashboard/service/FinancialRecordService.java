package com.finance.dashboard.service;

import com.finance.dashboard.dto.request.CreateFinancialRecordRequest;
import com.finance.dashboard.dto.request.UpdateFinancialRecordRequest;
import com.finance.dashboard.dto.response.FinancialRecordResponse;
import com.finance.dashboard.dto.response.PagedResponse;
import com.finance.dashboard.entity.FinancialRecord;
import com.finance.dashboard.entity.User;
import com.finance.dashboard.enums.RecordCategory;
import com.finance.dashboard.enums.RecordType;
import com.finance.dashboard.exception.ResourceNotFoundException;
import com.finance.dashboard.repository.FinancialRecordRepository;
import com.finance.dashboard.repository.UserRepository;
import com.finance.dashboard.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class FinancialRecordService {

    private final FinancialRecordRepository recordRepository;
    private final UserRepository userRepository;

    @Transactional
    public FinancialRecordResponse createRecord(CreateFinancialRecordRequest request) {
        User currentUser = getCurrentUser();

        FinancialRecord record = FinancialRecord.builder()
            .amount(request.getAmount())
            .type(request.getType())
            .category(request.getCategory())
            .date(request.getDate())
            .title(request.getTitle())
            .notes(request.getNotes())
            .createdBy(currentUser)
            .build();

        FinancialRecord saved = recordRepository.save(record);
        log.info("Created financial record id={} by user '{}'", saved.getId(), currentUser.getUsername());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PagedResponse<FinancialRecordResponse> getRecords(
            int page, int size,
            RecordType type,
            RecordCategory category,
            LocalDate startDate,
            LocalDate endDate,
            String search) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        // Normalize empty search to null so JPQL filter is skipped
        String normalizedSearch = StringUtils.hasText(search) ? search.trim() : null;

        Page<FinancialRecord> records = recordRepository.findWithFilters(
            type, category, startDate, endDate, normalizedSearch, pageable);

        return PagedResponse.of(records.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public FinancialRecordResponse getRecordById(Long id) {
        FinancialRecord record = findActiveRecord(id);
        return toResponse(record);
    }

    @Transactional
    public FinancialRecordResponse updateRecord(Long id, UpdateFinancialRecordRequest request) {
        FinancialRecord record = findActiveRecord(id);
        User currentUser = getCurrentUser();

        if (request.getAmount() != null)   record.setAmount(request.getAmount());
        if (request.getType() != null)     record.setType(request.getType());
        if (request.getCategory() != null) record.setCategory(request.getCategory());
        if (request.getDate() != null)     record.setDate(request.getDate());
        if (request.getTitle() != null)    record.setTitle(request.getTitle());
        if (request.getNotes() != null)    record.setNotes(request.getNotes());

        record.setUpdatedBy(currentUser);

        FinancialRecord updated = recordRepository.save(record);
        log.info("Updated financial record id={} by user '{}'", id, currentUser.getUsername());
        return toResponse(updated);
    }

    @Transactional
    public void deleteRecord(Long id) {
        FinancialRecord record = findActiveRecord(id);
        record.setDeleted(true);
        recordRepository.save(record);
        log.info("Soft-deleted financial record id={}", id);
    }

    // --- Helpers ---

    private FinancialRecord findActiveRecord(Long id) {
        return recordRepository.findByIdAndDeletedFalse(id)
            .orElseThrow(() -> new ResourceNotFoundException("Financial record", id));
    }

    private User getCurrentUser() {
        UserPrincipal principal = (UserPrincipal) SecurityContextHolder
            .getContext().getAuthentication().getPrincipal();
        return userRepository.findByIdAndDeletedFalse(principal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }

    public FinancialRecordResponse toResponse(FinancialRecord record) {
        return FinancialRecordResponse.builder()
            .id(record.getId())
            .amount(record.getAmount())
            .type(record.getType())
            .category(record.getCategory())
            .date(record.getDate())
            .title(record.getTitle())
            .notes(record.getNotes())
            .createdBy(record.getCreatedBy() != null ? record.getCreatedBy().getUsername() : null)
            .createdAt(record.getCreatedAt())
            .updatedAt(record.getUpdatedAt())
            .build();
    }
}
