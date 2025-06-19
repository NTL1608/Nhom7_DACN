package com.sieuvjp.greenbook.repository;

import com.sieuvjp.greenbook.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    boolean existsByName(String name);
    List<Category> findByIsActive(boolean isActive);

    @Query(value = "SELECT c.name, SUM(od.quantity * od.original_price) as total_revenue " +
            "FROM categories c " +
            "JOIN books b ON c.id = b.category_id " +
            "JOIN order_details od ON b.id = od.book_id " +
            "JOIN orders o ON od.order_id = o.id " +
            "WHERE o.status = 'COMPLETED' " +
            "GROUP BY c.id, c.name " +
            "ORDER BY total_revenue DESC " +
            "LIMIT :limit", nativeQuery = true)
    List<Object[]> findTopSellingCategories(@Param("limit") int limit);
}