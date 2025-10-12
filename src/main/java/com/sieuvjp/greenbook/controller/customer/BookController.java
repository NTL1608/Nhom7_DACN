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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Slf4j
@Controller
@RequestMapping("/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    /**
     * Danh sách sách với filter và phân trang
     * URL: /books?categoryId=1&page=0&size=12&sort=title&order=asc&search=keyword
     */
    @GetMapping
    public String bookList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "desc") String order,
            @RequestParam(required = false) String search, // ✨ THÊM SEARCH PARAM
            Model model,
            Authentication authentication
    ) {
        log.info("=== BOOKS PAGE REQUEST ===");
        log.info("Params - categoryId: {}, page: {}, size: {}, sort: {}-{}, search: {}",
                categoryId, page, size, sort, order, search);

        // Add auth info - PHẢI LÀM TRƯỚC KHI LẤY DATA
        addAuthInfoToModel(model, authentication);

        // Tạo Sort object
        Sort.Direction direction = order.equalsIgnoreCase("asc")
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;
        Sort sortObj = Sort.by(direction, sort);

        // Lấy sách theo search, category hoặc tất cả
        try {
            var booksPage = (search != null && !search.trim().isEmpty())
                    ? bookService.searchBooks(search, PageRequest.of(page, size, sortObj))
                    : (categoryId != null
                    ? bookService.findByCategoryId(categoryId, PageRequest.of(page, size, sortObj))
                    : bookService.findAllPaginated(PageRequest.of(page, size, sortObj)));

            log.info("Found {} books", booksPage.getContent().size());

            model.addAttribute("books", booksPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", booksPage.getTotalPages());
            model.addAttribute("totalItems", booksPage.getTotalElements());
            model.addAttribute("hasNext", booksPage.hasNext());
            model.addAttribute("hasPrevious", booksPage.hasPrevious());

            // ✨ THÊM SEARCH KEYWORD VÀO MODEL
            if (search != null && !search.trim().isEmpty()) {
                model.addAttribute("searchKeyword", search);
            }

        } catch (Exception e) {
            log.error("ERROR loading books: ", e);
            model.addAttribute("books", java.util.Collections.emptyList());
            model.addAttribute("currentPage", 0);
            model.addAttribute("totalPages", 0);
            model.addAttribute("totalItems", 0);
        }

        // ✨ THÊM FEATURED BOOKS (4 sách mới nhất cho slider)
        try {
            var featuredBooks = bookService.findAllPaginated(
                    PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "id"))
            ).getContent();
            model.addAttribute("featuredBooks", featuredBooks);
            log.info("Loaded {} featured books", featuredBooks.size());
        } catch (Exception e) {
            log.error("ERROR loading featured books: ", e);
            model.addAttribute("featuredBooks", java.util.Collections.emptyList());
        }

        // Lấy danh mục cho filter
        try {
            var categories = categoryService.findAll();
            model.addAttribute("categories", categories);
            log.info("Loaded {} categories", categories.size()); // ✨ LOG ĐỂ DEBUG
        } catch (Exception e) {
            log.error("ERROR loading categories: ", e);
            model.addAttribute("categories", java.util.Collections.emptyList());
        }

        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("currentSort", sort);
        model.addAttribute("currentOrder", order);
        model.addAttribute("title", search != null && !search.trim().isEmpty()
                ? "Tìm kiếm: " + search
                : "Kho sách");

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

            // Lấy sách liên quan (cùng category, LOẠI TRỪ sách hiện tại)
            if (book.getCategory() != null) {
                try {
                    // Lấy nhiều hơn 4 sách để có đủ sau khi filter
                    var relatedBooksPage = bookService.findByCategoryId(
                            book.getCategory().getId(),
                            PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "id"))
                    );

                    // Lọc bỏ sách hiện tại và chỉ lấy 4 sách
                    var relatedBooks = relatedBooksPage.getContent()
                            .stream()
                            .filter(b -> !b.getId().equals(id)) // ⭐ LOẠI BỎ sách hiện tại
                            .limit(4) // Chỉ lấy 4 sách
                            .collect(java.util.stream.Collectors.toList());

                    model.addAttribute("relatedBooks", relatedBooks);
                    log.info("Found {} related books (excluding current book)", relatedBooks.size());
                } catch (Exception e) {
                    log.error("ERROR loading related books: ", e);
                    model.addAttribute("relatedBooks", java.util.Collections.emptyList());
                }
            } else {
                model.addAttribute("relatedBooks", java.util.Collections.emptyList());
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
            model.addAttribute("searchKeyword", keyword); // ✨ ĐỔI TỪ keyword → searchKeyword

            // ✨ THÊM FEATURED BOOKS
            var featuredBooks = bookService.findAllPaginated(
                    PageRequest.of(0, 4, Sort.by(Sort.Direction.DESC, "id"))
            ).getContent();
            model.addAttribute("featuredBooks", featuredBooks);

            // ✨ THÊM CATEGORIES
            model.addAttribute("categories", categoryService.findAll());

            log.info("Search found {} books", booksPage.getTotalElements());
        } catch (Exception e) {
            log.error("ERROR in search: ", e);
            model.addAttribute("books", java.util.Collections.emptyList());
            model.addAttribute("featuredBooks", java.util.Collections.emptyList());
            model.addAttribute("categories", java.util.Collections.emptyList());
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
    /**
     * Mua ngay - Bypass giỏ hàng, đi thẳng checkout
     * URL: /books/buy-now?bookId=1&quantity=1
     */
    @GetMapping("/buy-now")
    public String buyNow(
            @RequestParam Long bookId,
            @RequestParam(defaultValue = "1") int quantity,
            RedirectAttributes redirectAttributes,
            Model model,
            Authentication authentication
    ) {
        log.info("=== BUY NOW REQUEST - Book ID: {}, Quantity: {} ===", bookId, quantity);

        addAuthInfoToModel(model, authentication);

        try {
            // Lấy thông tin sách
            var book = bookService.findById(bookId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy sách"));

            // Kiểm tra tồn kho
            if (book.getStockQuantity() < quantity) {
                log.warn("Out of stock - Book ID: {}", bookId);
                return "redirect:/books?error=outofstock";
            }

            log.info("Redirecting to checkout with book: {}", book.getTitle());

            // Redirect đến checkout với bookId và quantity
            redirectAttributes.addAttribute("bookId", bookId);
            redirectAttributes.addAttribute("quantity", quantity);
            redirectAttributes.addAttribute("buyNow", true);

            return "redirect:/checkout";

        } catch (Exception e) {
            log.error("ERROR in buy now: ", e);
            return "redirect:/books?error=buynow";
        }
    }
}