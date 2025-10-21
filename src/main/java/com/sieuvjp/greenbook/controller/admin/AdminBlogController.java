package com.sieuvjp.greenbook.controller.admin;

import com.sieuvjp.greenbook.dto.BlogDTO;
import com.sieuvjp.greenbook.entity.Blog;
import com.sieuvjp.greenbook.entity.User;
import com.sieuvjp.greenbook.enums.BlogStatus;
import com.sieuvjp.greenbook.service.BlogService;
import com.google.genai.Client;
import com.google.genai.types.GenerateContentResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.sieuvjp.greenbook.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/blogs")
@RequiredArgsConstructor
public class AdminBlogController {

    private final BlogService blogService;
    private final UserService userService;

    @GetMapping("/suggest-content")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public ResponseEntity<Map<String, String>> suggestContent(@RequestParam String title) {
        Client client = new Client();
        try {
            String prompt = "Viết một bài blog chuyên nghiệp với tiêu đề: \"" + title + "\". Bài viết gồm mở đầu, nội dung chính và kết luận.";

            GenerateContentResponse response = client.models.generateContent("gemini-1.5-flash", prompt, null);

            return ResponseEntity.ok(Map.of("content", response.text()));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("content", "Lỗi AI: " + e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String listBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) BlogStatus status,
            Model model) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        Page<Blog> blogPage;

        if (keyword != null && !keyword.isEmpty()) {
            blogPage = blogService.searchBlogs(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else if (status != null) {
            blogPage = blogService.findByStatus(status, pageable);
            model.addAttribute("status", status);
        } else {
            blogPage = blogService.findAllPaginated(pageable);
        }

        List<BlogDTO> blogs = blogPage.getContent().stream()
                .map(BlogDTO::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("blogs", blogs);
        model.addAttribute("currentPage", blogPage.getNumber());
        model.addAttribute("totalPages", blogPage.getTotalPages());
        model.addAttribute("totalItems", blogPage.getTotalElements());
        model.addAttribute("blogStatuses", BlogStatus.values());

        return "pages/blog/index";
    }

    @GetMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String showCreateForm(Model model) {
        BlogDTO blog = new BlogDTO();
        blog.setStatus(BlogStatus.DRAFT);

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String currentUsername = authentication.getName();
        User currentUser = userService.findByUsername(currentUsername)
                .orElseThrow(() -> new IllegalStateException("Current user not found"));

        blog.setUserId(currentUser.getId());

        model.addAttribute("blog", blog);
        model.addAttribute("blogStatuses", BlogStatus.values());
        return "pages/blog/form";
    }

    @PostMapping("/create")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String createBlog(@Valid @ModelAttribute("blog") BlogDTO blogDTO,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes) {

        model.addAttribute("blogStatuses", BlogStatus.values());

        if (result.hasErrors()) {
            return "pages/blog/form";
        }

        User user = userService.findById(blogDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + blogDTO.getUserId()));

        Blog blog = blogDTO.toEntity();
        blog.setUser(user);

        blogService.save(blog);

        redirectAttributes.addFlashAttribute("successMessage", "Blog created successfully");
        return "redirect:/admin/blogs";
    }

    @GetMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String showEditForm(@PathVariable Long id, Model model) {
        Blog blog = blogService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid blog ID: " + id));

        model.addAttribute("blog", BlogDTO.fromEntity(blog));
        model.addAttribute("blogStatuses", BlogStatus.values());
        return "pages/blog/form";
    }

    @PostMapping("/edit/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String updateBlog(@PathVariable Long id,
                             @Valid @ModelAttribute("blog") BlogDTO blogDTO,
                             BindingResult result,
                             Model model,
                             RedirectAttributes redirectAttributes,
                             Authentication authentication) { // ⭐ THÊM Authentication

        model.addAttribute("blogStatuses", BlogStatus.values());

        if (result.hasErrors()) {
            return "pages/blog/form";
        }

        Blog existingBlog = blogService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid blog ID: " + id));

        User user = userService.findById(blogDTO.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid user ID: " + blogDTO.getUserId()));

        Blog blog = blogDTO.toEntity();
        blog.setId(id);
        blog.setUser(user);

        // ⭐ LIBRARIAN KHÔNG ĐƯỢC PUBLISH/UNPUBLISH
        boolean isLibrarian = authentication.getAuthorities().stream()
                .anyMatch(auth -> auth.getAuthority().equals("ROLE_LIBRARIAN"));

        if (isLibrarian) {
            // Giữ nguyên trạng thái cũ
            blog.setStatus(existingBlog.getStatus());
            System.out.println("🔒 LIBRARIAN không được đổi trạng thái publish - Giữ nguyên");
        }

        if (blog.getImageUrl() == null || blog.getImageUrl().isEmpty()) {
            blog.setImageUrl(existingBlog.getImageUrl());
        }

        blogService.save(blog);

        redirectAttributes.addFlashAttribute("successMessage", "Blog updated successfully");
        return "redirect:/admin/blogs";
    }

    @GetMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')") // ⭐ CHỈ ADMIN
    public String deleteBlog(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        blogService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Blog deleted successfully");
        return "redirect:/admin/blogs";
    }

    @GetMapping("/change-status/{id}/{status}")
    @PreAuthorize("hasRole('ADMIN')") // ⭐ CHỈ ADMIN
    public String changeBlogStatus(@PathVariable Long id,
                                   @PathVariable BlogStatus status,
                                   RedirectAttributes redirectAttributes) {

        blogService.updateBlogStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Blog status updated successfully");
        return "redirect:/admin/blogs";
    }

    @GetMapping("/upload-image/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String showUploadImageForm(@PathVariable Long id, Model model) {
        Blog blog = blogService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid blog ID: " + id));

        model.addAttribute("blog", BlogDTO.fromEntity(blog));
        return "pages/blog/upload-image";
    }

    @PostMapping("/upload-image/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'LIBRARIAN')") // ⭐ Cả 2 role
    public String uploadBlogImage(@PathVariable Long id,
                                  @RequestParam("imageFile") MultipartFile imageFile,
                                  RedirectAttributes redirectAttributes) {

        if (imageFile.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Please select a file to upload");
            return "redirect:/admin/blogs/upload-image/" + id;
        }

        blogService.uploadBlogImage(id, imageFile);
        redirectAttributes.addFlashAttribute("successMessage", "Image uploaded successfully");
        return "redirect:/admin/blogs/edit/" + id;
    }
}