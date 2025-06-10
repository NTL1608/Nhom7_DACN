package com.sieuvjp.greenbook.controller.admin;

import com.sieuvjp.greenbook.dto.BookDTO;
import com.sieuvjp.greenbook.entity.Book;
import com.sieuvjp.greenbook.entity.Category;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.service.CategoryService;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.http.ResponseEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/books")
@RequiredArgsConstructor
public class AdminBookController {

    private final BookService bookService;
    private final CategoryService categoryService;

    @GetMapping("/suggest-description")
    public ResponseEntity<Map<String, String>> suggestBookDescription(@RequestParam String title) {
        try {
            Client client = new Client();
            String prompt = "Viết đoạn mô tả hấp dẫn, mang tính giới thiệu cho cuốn sách có tiêu đề: \"" + title + "\". Đoạn mô tả nên súc tích, truyền cảm hứng và thu hút độc giả.";

            GenerateContentResponse response = client.models.generateContent("gemini-1.5-flash", prompt, null);

            return ResponseEntity.ok(Map.of("description", response.text()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("description", "Lỗi AI: " + e.getMessage()));
        }
    }

    @GetMapping
    public String listBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Book> bookPage;

        if (keyword != null && !keyword.isEmpty()) {
            bookPage = bookService.searchBooks(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else if (categoryId != null) {
            bookPage = bookService.findByCategoryId(categoryId, pageable);
            model.addAttribute("categoryId", categoryId);
        } else {
            bookPage = bookService.findAllPaginated(pageable);
        }

        List<BookDTO> books = bookPage.getContent().stream()
                .map(BookDTO::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("books", books);
        model.addAttribute("currentPage", bookPage.getNumber());
        model.addAttribute("totalPages", bookPage.getTotalPages());
        model.addAttribute("totalItems", bookPage.getTotalElements());

        // For category filter
        List<Category> categories = categoryService.findAllActive();
        model.addAttribute("categories", categories);

        return "pages/book/index";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("book", new BookDTO());
        model.addAttribute("categories", categoryService.findAllActive());
        return "pages/book/form";
    }

    @PostMapping("/create")
    public String createBook(@Valid @ModelAttribute("book") BookDTO bookDTO,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        model.addAttribute("categories", categoryService.findAllActive());

        // Check for validation errors
        if (result.hasErrors()) {
            return "pages/book/form";
        }

        // Get category
        Category category = categoryService.findById(bookDTO.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + bookDTO.getCategoryId()));

        // Prepare book for save
        Book book = bookDTO.toEntity();
        book.setCategory(category);
        book.setActive(true);

        // Save book
        bookService.save(book);

        redirectAttributes.addFlashAttribute("successMessage", "Book created successfully");
        return "redirect:/admin/books";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Book book = bookService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book ID: " + id));

        model.addAttribute("book", BookDTO.fromEntity(book));
        model.addAttribute("categories", categoryService.findAllActive());
        return "pages/book/form";
    }

    @PostMapping("/edit/{id}")
    public String updateBook(@PathVariable Long id,
                             @Valid @ModelAttribute("book") BookDTO bookDTO,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        model.addAttribute("categories", categoryService.findAllActive());

        // Check for validation errors
        if (result.hasErrors()) {
            return "pages/book/form";
        }

        // Get existing book
        Book existingBook = bookService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book ID: " + id));

        // Get category
        Category category = categoryService.findById(bookDTO.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid category ID: " + bookDTO.getCategoryId()));

        // Prepare book for update
        Book book = bookDTO.toEntity();
        book.setId(id);
        book.setCategory(category);
        book.setActive(existingBook.isActive());

        // If no images are set in the DTO, keep the existing ones
        if (book.getImageUrls() == null || book.getImageUrls().isEmpty()) {
            book.setImageUrls(existingBook.getImageUrls());
        }

        // Save book
        bookService.save(book);

        redirectAttributes.addFlashAttribute("successMessage", "Book updated successfully");
        return "redirect:/admin/books";
    }

    @GetMapping("/delete/{id}")
    public String deleteBook(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Book deleted successfully");
        return "redirect:/admin/books";
    }

    @GetMapping("/toggle-status/{id}")
    public String toggleBookStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        bookService.toggleActiveStatus(id);
        redirectAttributes.addFlashAttribute("successMessage", "Book status updated successfully");
        return "redirect:/admin/books";
    }

    @GetMapping("/images/{id}")
    public String manageBookImages(@PathVariable Long id, Model model) {
        Book book = bookService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book ID: " + id));

        model.addAttribute("book", BookDTO.fromEntity(book));
        return "pages/book/images";
    }

    @PostMapping("/images/{id}/upload")
    public String uploadBookImage(@PathVariable Long id,
                                  @RequestParam("imageFile") MultipartFile imageFile,
                                  RedirectAttributes redirectAttributes) {

        if (imageFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload");
            return "redirect:/admin/books/images/" + id;
        }

        bookService.addBookImage(id, imageFile);
        redirectAttributes.addFlashAttribute("successMessage", "Image uploaded successfully");
        return "redirect:/admin/books/images/" + id;
    }

    @GetMapping("/images/{id}/delete/{imageUrl}")
    public String deleteBookImage(@PathVariable Long id,
                                  @PathVariable String imageUrl,
                                  RedirectAttributes redirectAttributes) {

        bookService.removeBookImage(id, "/uploads/books/" + id + "/" + imageUrl);
        redirectAttributes.addFlashAttribute("successMessage", "Image deleted successfully");
        return "redirect:/admin/books/images/" + id;
    }

    @GetMapping("/update-stock/{id}")
    public String showUpdateStockForm(@PathVariable Long id, Model model) {
        Book book = bookService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid book ID: " + id));

        model.addAttribute("book", BookDTO.fromEntity(book));
        return "pages/book/update-stock";
    }

    @PostMapping("/update-stock/{id}")
    public String updateBookStock(@PathVariable Long id,
                                  @RequestParam("stockQuantity") int stockQuantity,
                                  RedirectAttributes redirectAttributes) {

        bookService.updateStock(id, stockQuantity);
        redirectAttributes.addFlashAttribute("successMessage", "Stock updated successfully");
        return "redirect:/admin/books";
    }

    @ModelAttribute("pathUtils")
    public PathUtils pathUtils() {
        return new PathUtils();
    }

    public static class PathUtils {
        public String getFileName(String path) {
            if (path == null || path.isEmpty()) return "";
            return path.substring(path.lastIndexOf('/') + 1);
        }

        public String getFileNameWithoutExtension(String path) {
            String fileName = getFileName(path);
            int lastDot = fileName.lastIndexOf('.');
            return lastDot > 0 ? fileName.substring(0, lastDot) : fileName;
        }
    }
}