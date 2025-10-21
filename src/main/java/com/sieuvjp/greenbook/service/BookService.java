package com.sieuvjp.greenbook.service;

import com.sieuvjp.greenbook.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

public interface BookService {
    List<Book> findAll();
    Page<Book> findAllPaginated(Pageable pageable);
    Page<Book> findByIsActive(boolean isActive, Pageable pageable);
    Optional<Book> findById(Long id);
    Book save(Book book);
    void deleteById(Long id);
    Page<Book> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Book> searchBooks(String keyword, Pageable pageable);
    List<Book> findBestSellers(int limit);
    List<Book> findNewArrivals(int limit);
    List<Book> findByLowStock(int threshold);
    void toggleActiveStatus(Long bookId);
    void updateStock(Long bookId, int quantity);
    void addBookImage(Long bookId, MultipartFile imageFile);
    void removeBookImage(Long bookId, String imageUrl);

    // ⭐ THÊM MỚI - Admin methods (bao gồm inactive books)
    Page<Book> searchAllBooksForAdmin(String keyword, Pageable pageable);
    Page<Book> findAllByCategoryIdForAdmin(Long categoryId, Pageable pageable);
}