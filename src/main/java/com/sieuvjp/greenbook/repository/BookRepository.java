package com.sieuvjp.greenbook.repository;

import com.sieuvjp.greenbook.entity.Book;
import com.sieuvjp.greenbook.entity.Category;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BookRepository extends JpaRepository<Book, Long> {
    Page<Book> findByIsActive(boolean isActive, Pageable pageable);

    Page<Book> findByTitleContainingIgnoreCase(String title, Pageable pageable);

    Page<Book> findByCategory(Category category, Pageable pageable);

    List<Book> findByIsActiveAndStockQuantityGreaterThan(boolean isActive, int minStock);

    @Query("SELECT b FROM Book b WHERE b.soldQuantity > 0 ORDER BY b.soldQuantity DESC")
    List<Book> findBestSellingBooks(Pageable pageable);

    @Query("SELECT b FROM Book b WHERE b.createdAt >= :dateThreshold")
    List<Book> findNewArrivals(@Param("dateThreshold") LocalDateTime dateThreshold, Pageable pageable);

    @Query(value = "SELECT * FROM books b WHERE b.created_at >= DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY)", nativeQuery = true)
    List<Book> findNewArrivalsNative(Pageable pageable);

    List<Book> findByCreatedAtGreaterThanEqual(LocalDateTime date, Pageable pageable);

    // ⭐ THÊM MỚI - Tìm kiếm chính xác (phân biệt dấu tiếng Việt)
    @Query("SELECT b FROM Book b WHERE " +
            "b.isActive = true AND (" +
            "b.title LIKE %:keyword% OR " +
            "b.author LIKE %:keyword% OR " +
            "b.description LIKE %:keyword% OR " +
            "b.publisher LIKE %:keyword%)")
    Page<Book> searchBooks(@Param("keyword") String keyword, Pageable pageable);

    // ⭐ THÊM MỚI - Tìm kiếm theo category và keyword
    @Query("SELECT b FROM Book b WHERE " +
            "b.isActive = true AND " +
            "b.category.id = :categoryId AND (" +
            "b.title LIKE %:keyword% OR " +
            "b.author LIKE %:keyword% OR " +
            "b.description LIKE %:keyword% OR " +
            "b.publisher LIKE %:keyword%)")
    Page<Book> searchBooksByCategoryAndKeyword(
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable
    );

    // ⭐ THÊM MỚI - Tìm sách theo category, loại trừ sách hiện tại
    @Query("SELECT b FROM Book b WHERE b.category.id = :categoryId AND b.id != :excludeId AND b.isActive = true")
    Page<Book> findByCategoryIdAndIdNot(
            @Param("categoryId") Long categoryId,
            @Param("excludeId") Long excludeId,
            Pageable pageable
    );
}