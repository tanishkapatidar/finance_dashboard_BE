package com.finance.dashboard.repository;

import com.finance.dashboard.entity.FinancialRecord;
import com.finance.dashboard.enums.RecordCategory;
import com.finance.dashboard.enums.RecordType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface FinancialRecordRepository extends JpaRepository<FinancialRecord, Long> {

    Optional<FinancialRecord> findByIdAndDeletedFalse(Long id);

    // --- Filtered Listing ---

    @Query("""
        SELECT r FROM FinancialRecord r
        WHERE r.deleted = false
          AND (:type IS NULL OR r.type = :type)
          AND (:category IS NULL OR r.category = :category)
          AND (:startDate IS NULL OR r.date >= :startDate)
          AND (:endDate IS NULL OR r.date <= :endDate)
          AND (:search IS NULL OR LOWER(r.title) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(r.notes) LIKE LOWER(CONCAT('%', :search, '%')))
        ORDER BY r.date DESC, r.createdAt DESC
        """)
    Page<FinancialRecord> findWithFilters(
        @Param("type") RecordType type,
        @Param("category") RecordCategory category,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate,
        @Param("search") String search,
        Pageable pageable
    );

    // --- Dashboard Analytics ---

    @Query("SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r WHERE r.deleted = false AND r.type = :type")
    BigDecimal sumByType(@Param("type") RecordType type);

    @Query("""
        SELECT COALESCE(SUM(r.amount), 0) FROM FinancialRecord r
        WHERE r.deleted = false AND r.type = :type
          AND r.date >= :startDate AND r.date <= :endDate
        """)
    BigDecimal sumByTypeAndDateRange(
        @Param("type") RecordType type,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );

    // Category-wise totals
    @Query("""
        SELECT r.category, r.type, COALESCE(SUM(r.amount), 0)
        FROM FinancialRecord r
        WHERE r.deleted = false
        GROUP BY r.category, r.type
        ORDER BY r.category
        """)
    List<Object[]> getCategoryWiseTotals();

    // Monthly trends (year/month grouping)
    @Query("""
        SELECT FUNCTION('YEAR', r.date) as yr,
               FUNCTION('MONTH', r.date) as mo,
               r.type,
               COALESCE(SUM(r.amount), 0)
        FROM FinancialRecord r
        WHERE r.deleted = false
          AND r.date >= :startDate
        GROUP BY FUNCTION('YEAR', r.date), FUNCTION('MONTH', r.date), r.type
        ORDER BY yr ASC, mo ASC
        """)
    List<Object[]> getMonthlyTrends(@Param("startDate") LocalDate startDate);

    // Recent activity
    @Query("""
        SELECT r FROM FinancialRecord r
        WHERE r.deleted = false
        ORDER BY r.createdAt DESC
        """)
    List<FinancialRecord> findRecentActivity(Pageable pageable);
}
