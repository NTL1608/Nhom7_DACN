package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.dto.BookDTO;
import com.sieuvjp.greenbook.entity.Book;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/customer/books")
@RequiredArgsConstructor
public class BookRestController {

    private final BookService bookService;
    private final CategoryService categoryService;

    /**
     * API: Lấy danh sách sách với filter và phân trang
     * GET /api/customer/books?categoryId=1&page=0&size=12&sort=title&order=asc
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getBooks(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String order
    ) {
        log.info("API Request - categoryId: {}, page: {}, sort: {}-{}", categoryId, page, sort, order);

        try {
            // Tạo Sort object
            Sort.Direction direction = order.equalsIgnoreCase("asc")
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC;
            Sort sortObj = Sort.by(direction, sort);

            // Lấy dữ liệu
            Page<Book> booksPage = categoryId != null
                    ? bookService.findByCategoryId(categoryId, PageRequest.of(page, size, sortObj))
                    : bookService.findAllPaginated(PageRequest.of(page, size, sortObj));

            // Convert sang DTO
            List<BookDTO> bookDTOs = booksPage.getContent().stream()
                    .map(BookDTO::fromEntity)
                    .collect(Collectors.toList());

            // Chuẩn bị response
            Map<String, Object> response = new HashMap<>();
            response.put("books", bookDTOs);
            response.put("currentPage", page);
            response.put("totalPages", booksPage.getTotalPages());
            response.put("totalItems", booksPage.getTotalElements());
            response.put("hasNext", booksPage.hasNext());
            response.put("hasPrevious", booksPage.hasPrevious());

            log.info("API Response - {} books found", bookDTOs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in getBooks API: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể tải danh sách sách");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * API: Lấy chi tiết sách theo ID
     * GET /api/customer/books/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBookById(@PathVariable Long id) {
        log.info("API Request - Get book detail: {}", id);

        try {
            Book book = bookService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sách với ID: " + id));

            BookDTO bookDTO = BookDTO.fromEntity(book);

            // Lấy sách liên quan
            List<BookDTO> relatedBooks = null;
            if (book.getCategory() != null) {
                Page<Book> relatedPage = bookService.findByCategoryId(
                        book.getCategory().getId(),
                        PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "id"))
                );
                relatedBooks = relatedPage.getContent().stream()
                        .filter(b -> !b.getId().equals(id))
                        .map(BookDTO::fromEntity)
                        .collect(Collectors.toList());
            }

            Map<String, Object> response = new HashMap<>();
            response.put("book", bookDTO);
            response.put("relatedBooks", relatedBooks);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in getBookById API: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể tải thông tin sách");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(404).body(errorResponse);
        }
    }

    /**
     * API: Tìm kiếm sách
     * GET /api/customer/books/search?keyword=java&page=0&size=12
     */
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size
    ) {
        log.info("API Request - Search: {}", keyword);

        try {
            Page<Book> booksPage = bookService.searchBooks(
                    keyword,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
            );

            List<BookDTO> bookDTOs = booksPage.getContent().stream()
                    .map(BookDTO::fromEntity)
                    .collect(Collectors.toList());

            Map<String, Object> response = new HashMap<>();
            response.put("books", bookDTOs);
            response.put("keyword", keyword);
            response.put("currentPage", page);
            response.put("totalPages", booksPage.getTotalPages());
            response.put("totalItems", booksPage.getTotalElements());

            log.info("Search found {} books", bookDTOs.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error in searchBooks API: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Lỗi khi tìm kiếm");
            errorResponse.put("message", e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    /**
     * API: Lấy tất cả danh mục
     * GET /api/customer/books/categories
     */
    @GetMapping("/categories")
    public ResponseEntity<?> getAllCategories() {
        log.info("API Request - Get all categories");

        try {
            var categories = categoryService.findAll();
            log.info("Found {} categories", categories.size());
            return ResponseEntity.ok(categories);
        } catch (Exception e) {
            log.error("Error in getAllCategories API: ", e);
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Không thể tải danh mục");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
}