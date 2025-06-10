package com.sieuvjp.greenbook.controller.admin;

import com.sieuvjp.greenbook.entity.Book;
import com.sieuvjp.greenbook.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Controller
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {

    private final BookRepository bookRepository;

    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportCSV() throws IOException {
        List<Book> books = bookRepository.findAll();

        StringBuilder csv = new StringBuilder("ID,Title,Author,Price,Stock,Sold,Publisher,Published Date\n");
        for (Book book : books) {
            csv.append(book.getId()).append(",")
                    .append(safe(book.getTitle())).append(",")
                    .append(safe(book.getAuthor())).append(",")
                    .append(book.getOriginalPrice()).append(",")
                    .append(book.getStockQuantity()).append(",")
                    .append(book.getSoldQuantity()).append(",")
                    .append(safe(book.getPublisher())).append(",")
                    .append(book.getPublishedDate()).append("\n");
        }

        ByteArrayInputStream inputStream = new ByteArrayInputStream(csv.toString().getBytes(StandardCharsets.UTF_8));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=books_report.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(new InputStreamResource(inputStream));
    }

    private String safe(String s) {
        return s == null ? "" : s.replace(",", " "); // tránh lỗi CSV do dấu phẩy
    }
}
