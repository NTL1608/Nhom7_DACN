package com.sieuvjp.greenbook.controller.customer;

import com.sieuvjp.greenbook.dto.BlogDTO;
import com.sieuvjp.greenbook.entity.Blog;
import com.sieuvjp.greenbook.enums.BlogStatus;
import com.sieuvjp.greenbook.service.BlogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j  // ← THÊM
@Controller
@RequestMapping("/blogs")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    // Danh sách blog (chỉ hiển thị PUBLISHED)
    @GetMapping
    public String listBlogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String keyword,
            Model model) {

        log.info("=== BLOG LIST === page: {}, keyword: {}", page, keyword);  // ← THÊM

        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedDate").descending());
        Page<Blog> blogPage;

        if (keyword != null && !keyword.isEmpty()) {
            blogPage = blogService.searchBlogs(keyword, pageable);
            model.addAttribute("keyword", keyword);
        } else {
            blogPage = blogService.findByStatus(BlogStatus.PUBLISHED, pageable);
        }

        List<BlogDTO> blogs = blogPage.getContent().stream()
                .filter(blog -> blog != null && blog.getStatus() == BlogStatus.PUBLISHED)
                .map(BlogDTO::fromEntity)
                .collect(Collectors.toList());

        log.info("Found {} blogs", blogs.size());  // ← THÊM

        model.addAttribute("blogs", blogs);
        model.addAttribute("currentPage", blogPage.getNumber());
        model.addAttribute("totalPages", blogPage.getTotalPages());
        model.addAttribute("totalItems", blogPage.getTotalElements());

        // ✅ THAY ĐỔI DUY NHẤT: từ "pages/blogs/list" → "customer/blogs/list"
        return "customer/blogs/list";
    }

    // Chi tiết blog
    @GetMapping("/{id}")
    public String blogDetail(@PathVariable Long id, Model model) {
        log.info("=== BLOG DETAIL === id: {}", id);  // ← THÊM

        Blog blog = blogService.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Blog not found with id: " + id));

        // Chỉ cho xem blog PUBLISHED
        if (blog.getStatus() != BlogStatus.PUBLISHED) {
            throw new IllegalArgumentException("Blog is not published");
        }

        model.addAttribute("blog", BlogDTO.fromEntity(blog));

        // Lấy blog liên quan (cùng tác giả hoặc gần đây)
        List<BlogDTO> recentBlogs = blogService.findRecentPublishedBlogs(4).stream()
                .filter(b -> !b.getId().equals(id))
                .limit(3)
                .map(BlogDTO::fromEntity)
                .collect(Collectors.toList());

        model.addAttribute("recentBlogs", recentBlogs);

        // ✅ THAY ĐỔI DUY NHẤT: từ "pages/blogs/detail" → "customer/blogs/detail"
        return "customer/blogs/detail";
    }
}