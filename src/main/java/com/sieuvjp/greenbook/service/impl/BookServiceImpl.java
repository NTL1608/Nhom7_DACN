package com.sieuvjp.greenbook.service.impl;

import com.sieuvjp.greenbook.entity.Book;
import com.sieuvjp.greenbook.entity.Category;
import com.sieuvjp.greenbook.exception.ResourceNotFoundException;
import com.sieuvjp.greenbook.repository.BookRepository;
import com.sieuvjp.greenbook.repository.CategoryRepository;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.util.FileUploadUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;
    private final FileUploadUtil fileUploadUtil;

    @Override
    public List<Book> findAll() {
        return bookRepository.findAll();
    }

    @Override
    public Page<Book> findAllPaginated(Pageable pageable) {
        return bookRepository.findAll(pageable);
    }

    @Override
    public Page<Book> findByIsActive(boolean isActive, Pageable pageable) {
        return bookRepository.findByIsActive(isActive, pageable);
    }

    @Override
    public Optional<Book> findById(Long id) {
        return bookRepository.findById(id);
    }

    @Override
    @Transactional
    public Book save(Book book) {
        return bookRepository.save(book);
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        bookRepository.deleteById(id);
    }

    @Override
    public Page<Book> findByCategoryId(Long categoryId, Pageable pageable) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));

        return bookRepository.findByCategory(category, pageable);
    }

    @Override
    public Page<Book> searchBooks(String keyword, Pageable pageable) {
        return bookRepository.findByTitleContainingIgnoreCase(keyword, pageable);
    }

    @Override
    public List<Book> findBestSellers(int limit) {
        return bookRepository.findBestSellingBooks(PageRequest.of(0, limit));
    }

    @Override
    public List<Book> findNewArrivals(int limit) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        return bookRepository.findNewArrivals(thirtyDaysAgo, PageRequest.of(0, limit));

    }

    @Override
    public List<Book> findByLowStock(int threshold) {
        return bookRepository.findByIsActiveAndStockQuantityGreaterThan(true, 0)
                .stream()
                .filter(book -> book.getStockQuantity() <= threshold)
                .toList();
    }

    @Override
    @Transactional
    public void toggleActiveStatus(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        book.setActive(!book.isActive());
        bookRepository.save(book);
    }

    @Override
    @Transactional
    public void updateStock(Long bookId, int quantity) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        book.setStockQuantity(quantity);
        bookRepository.save(book);
    }

    @Override
    @Transactional
    public void addBookImage(Long bookId, MultipartFile imageFile) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        try {
            String fileName = fileUploadUtil.saveFile(imageFile, "books/" + bookId);
            String imageUrl = "/uploads/books/" + bookId + "/" + fileName;

            List<String> imageUrls = book.getImageUrls();
            if (imageUrls == null) {
                imageUrls = new ArrayList<>();
            }

            imageUrls.add(imageUrl);
            book.setImageUrls(imageUrls);
            book.saveImageUrls();

            bookRepository.save(book);
            log.debug(imageUrl);
            log.debug(book.getTitle());
            log.debug(book.getImageUrls().toString());
        } catch (IOException e) {
            throw new RuntimeException("Could not store the image file", e);
        }
    }

    @Override
    @Transactional
    public void removeBookImage(Long bookId, String imageUrl) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        List<String> imageUrls = book.getImageUrls();
        if (imageUrls != null) {
            imageUrls.remove(imageUrl);
            book.setImageUrls(imageUrls);

            bookRepository.save(book);
            book.saveImageUrls();

            // Remove the file from storage
            try {
                String fileName = imageUrl.substring(imageUrl.lastIndexOf("/") + 1);
                fileUploadUtil.deleteFile("books/" + bookId + "/" + fileName);
            } catch (IOException e) {
                throw new RuntimeException("Could not delete the image file", e);
            }
        }
    }
}