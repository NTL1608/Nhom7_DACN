package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.service.CategoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.stream.Collectors;


@Slf4j
@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    /**
     * Danh sách sách với filter và phân trang
     * URL: /books?categoryId=1&page=0&size=12&sort=title&order=asc
     */
    @GetMapping
    public String bookList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String order,
            Model model,
            Authentication authentication
    ) {
        log.info("=== BOOKS PAGE REQUEST ===");
        log.info("Params - categoryId: {}, page: {}, size: {}, sort: {}-{}",
                categoryId, page, size, sort, order);

        // Add auth info - PHẢI LÀM TRƯỚC KHI LẤY DATA
        addAuthInfoToModel(model, authentication);

        // Tạo Sort object
        Sort.Direction direction = order.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sort);

        // Lấy sách theo category hoặc tất cả
        try {
            var booksPage = categoryId != null
                    ? bookService.findByCategoryId(categoryId, PageRequest.of(page, size, sortObj))
                    : bookService.findAllPaginated(PageRequest.of(page, size, sortObj));

            log.info("Found {} books", booksPage.getContent().size());

            model.addAttribute("books", booksPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", booksPage.getTotalPages());
            model.addAttribute("totalItems", booksPage.getTotalElements());
            model.addAttribute("hasNext", booksPage.hasNext());
            model.addAttribute("hasPrevious", booksPage.hasPrevious());
        } catch (Exception e) {
            log.error("ERROR loading books: ", e);
            model.addAttribute("books", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalItems", 0);
        }

        // Lấy danh mục cho filter
        try {
            model.addAttribute("categories", categoryService.findAll());
        } catch (Exception e) {
            log.error("ERROR loading categories: ", e);
            model.addAttribute("categories", java.util.Collections.emptyList());
        }

        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentOrder", order);
        model.addAttribute("title", "Kho sách");

        log.info("=== RENDERING books.html ===");
        return "customer/books";
    }

    /**
     * Chi tiết sách
     * URL: /books/1
     */
    @GetMapping("/{id}")
    public String bookDetail(
            @PathVariable Long id,
            Model model,
            Authentication authentication
    ) {
        log.info("=== BOOK DETAIL REQUEST - ID: {} ===", id);

        addAuthInfoToModel(model, authentication);

        try {
            var book = bookService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));

            model.addAttribute("book", book);
            model.addAttribute("title", book.getTitle());

            // Lấy sách liên quan (cùng category)
            if (book.getCategory() != null) {
                try {
                    var relatedBooks = bookService.findByCategoryId(
                            book.getCategory().getId(),
                            PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "id"))
                    );
                    model.addAttribute("relatedBooks", relatedBooks.getContent());
                } catch (Exception e) {
                    log.error("ERROR loading related books: ", e);
                    model.addAttribute("relatedBooks", java.util.Collections.emptyList());
                }
            }
        } catch (Exception e) {
            log.error("ERROR loading book detail: ", e);
            return "redirect:/books?error=notfound";
        }

        return "customer/book-detail";
    }

    /**
     * Tìm kiếm sách
     * URL: /books/search?keyword=java
     */
    @GetMapping("/search")
    public String searchBooks(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            Model model,
            Authentication authentication
    ) {
        log.info("=== SEARCH REQUEST - Keyword: {} ===", keyword);

        addAuthInfoToModel(model, authentication);

        try {
            var booksPage = bookService.searchBooks(
                    keyword,
                    PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
            );

            model.addAttribute("books", booksPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", booksPage.getTotalPages());
            model.addAttribute("totalItems", booksPage.getTotalElements());
            model.addAttribute("keyword", keyword);

            log.info("Search found {} books", booksPage.getTotalElements());
        } catch (Exception e) {
            log.error("ERROR in search: ", e);
            model.addAttribute("books", java.util.Collections.emptyList());
        }

        model.addAttribute("title", "Kết quả tìm kiếm: " + keyword);
        return "customer/books";
    }

    // ========================================
    // HELPER METHOD - FIXED CHO ANONYMOUS USER
    // ========================================
    private void addAuthInfoToModel(Model model, Authentication authentication) {
        boolean isLoggedIn = false;
        String userName = null;

        try {
            // Kiểm tra null và authenticated
            if (authentication != null && authentication.isAuthenticated()) {
                Object principal = authentication.getPrincipal();

                // Kiểm tra không phải anonymous user
                if (principal != null && !"anonymousUser".equals(principal.toString())) {
                    isLoggedIn = true;

                    // Cast sang User entity
                    if (principal instanceof User) {
                        User user = (User) principal;
                        userName = user.getFullName();
                    } else if (principal instanceof org.springframework.security.core.userdetails.User) {
                        // Trường hợp UserDetails
                        userName = ((org.springframework.security.core.userdetails.User) principal).getUsername();
                    } else {
                        // Fallback
                        userName = authentication.getName();
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error getting authentication info: {}", e.getMessage());
            // Không throw exception, chỉ log warning
            isLoggedIn = false;
            userName = null;
        }

        log.debug("Auth info - isLoggedIn: {}, userName: {}", isLoggedIn, userName);
        model.addAttribute("isLoggedIn", isLoggedIn);
        model.addAttribute("userName", userName);
    }
}