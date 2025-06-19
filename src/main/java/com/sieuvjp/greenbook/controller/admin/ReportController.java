package com.sieuvjp.greenbook.controller.admin;

// Các thư viện và annotation cần thiết
import lombok.RequiredArgsConstructor;
import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.sieuvjp.greenbook.service.OrderService;
import com.sieuvjp.greenbook.service.BookService;
import com.sieuvjp.greenbook.service.CategoryService;
import com.sieuvjp.greenbook.repository.OrderDetailRepository;
import com.sieuvjp.greenbook.repository.CategoryRepository;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

// Controller xử lý yêu cầu xuất báo cáo PDF
@Controller
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {

    // Inject các service và repository
    private final OrderService orderService;
    private final BookService bookService;
    private final CategoryService categoryService;
    private final OrderDetailRepository orderDetailRepository;
    private final CategoryRepository categoryRepository;

    // API để xuất PDF
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPDF() {
        try {
            // Tính thời gian đầu và cuối của tháng hiện tại
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfMonth = LocalDateTime.now().withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth()).withHour(23).withMinute(59).withSecond(59);
            double totalRevenueCurrentMonth = orderService.getRevenueBetween(startOfMonth, endOfMonth);

            // Tháng trước
            LocalDateTime startOfPreviousMonth = startOfMonth.minusMonths(1);
            LocalDateTime endOfPreviousMonth = startOfMonth.minusSeconds(1);
            double totalRevenuePreviousMonth = orderService.getRevenueBetween(startOfPreviousMonth, endOfPreviousMonth);

            // Lấy top sách và danh mục bán chạy
            List<Map<String, Object>> topSellingBooksData = orderService.getTopSellingBooks(5);
            List<Object[]> topSellingCategories = categoryRepository.findTopSellingCategories(5);

            // Tính tăng trưởng
            double growthRate = calculateGrowthRate(totalRevenuePreviousMonth, totalRevenueCurrentMonth);

            // Tạo tài liệu PDF
            Document document = new Document(PageSize.A4);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);
            document.open();

            // Font tiếng Việt
            Font titleFont = createVietnameseFont(18, Font.BOLD);
            Font headerFont = createVietnameseFont(14, Font.BOLD);
            Font normalFont = createVietnameseFont(12, Font.NORMAL);

            // Tiêu đề
            Paragraph title = new Paragraph("BÁO CÁO DOANH THU", titleFont);
            title.setAlignment(Element.ALIGN_RIGHT);
            title.setSpacingAfter(20f);
            document.add(title);

            // Thông tin công ty
            document.add(new Paragraph("Công ty: Nhà sách Green Book", normalFont));
            document.add(new Paragraph("Thời gian tạo báo cáo: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date()), normalFont));
            document.add(new Paragraph(" "));

            // Tổng quan
            Paragraph overview = new Paragraph("TỔNG QUAN", headerFont);
            overview.setSpacingBefore(10f);
            overview.setSpacingAfter(10f);
            document.add(overview);
            document.add(new Paragraph("Tổng doanh thu tháng này: " + String.format("%,.0f", totalRevenueCurrentMonth) + " VNĐ", normalFont));
            document.add(new Paragraph("Tỷ lệ tăng trưởng: " + String.format("%.2f%%", growthRate), normalFont));
            document.add(new Paragraph(" "));

            // Bảng sách bán chạy
            Paragraph bookHeader = new Paragraph("TOP CUỐN SÁCH BÁN CHẠY NHẤT", headerFont);
            bookHeader.setSpacingBefore(15f);
            bookHeader.setSpacingAfter(10f);
            document.add(bookHeader);

            PdfPTable bookTable = new PdfPTable(3);
            bookTable.setWidthPercentage(100);
            bookTable.setWidths(new float[]{4, 2, 3});
            addTableHeader(bookTable, "Tên sách", headerFont);
            addTableHeader(bookTable, "Số lượng bán", headerFont);
            addTableHeader(bookTable, "Doanh thu", headerFont);

            if (topSellingBooksData != null && !topSellingBooksData.isEmpty()) {
                for (Map<String, Object> book : topSellingBooksData) {
                    String bookTitle = book.get("title") != null ? book.get("title").toString() : "Không có";
                    String soldQuantity = book.get("soldQuantity") != null ? book.get("soldQuantity").toString() : "0";
                    double revenue = book.get("revenue") != null ? ((Number)book.get("revenue")).doubleValue() : 0.0;
                    addTableCell(bookTable, bookTitle, normalFont, Element.ALIGN_LEFT);
                    addTableCell(bookTable, soldQuantity, normalFont, Element.ALIGN_CENTER);
                    addTableCell(bookTable, String.format("%,.0f", revenue) + " VNĐ", normalFont, Element.ALIGN_RIGHT);
                }
            } else {
                addNoDataRow(bookTable, "Không có dữ liệu", normalFont, 3);
            }
            document.add(bookTable);
            document.add(new Paragraph(" "));

            // Bảng danh mục bán chạy
            Paragraph categoryHeader = new Paragraph("TOP 5 DANH MỤC BÁN CHẠY NHẤT", headerFont);
            categoryHeader.setSpacingBefore(15f);
            categoryHeader.setSpacingAfter(10f);
            document.add(categoryHeader);

            PdfPTable categoryTable = new PdfPTable(2);
            categoryTable.setWidthPercentage(100);
            categoryTable.setWidths(new float[]{3, 2});
            addTableHeader(categoryTable, "Danh mục", headerFont);
            addTableHeader(categoryTable, "Doanh thu", headerFont);

            if (topSellingCategories != null && !topSellingCategories.isEmpty()) {
                for (Object[] category : topSellingCategories) {
                    String categoryName = category[0] != null ? category[0].toString() : "Không có";
                    double categoryRevenue = category[1] != null ? ((Number)category[1]).doubleValue() : 0.0;
                    addTableCell(categoryTable, categoryName, normalFont, Element.ALIGN_LEFT);
                    addTableCell(categoryTable, String.format("%,.0f", categoryRevenue) + " VNĐ", normalFont, Element.ALIGN_RIGHT);
                }
            } else {
                addNoDataRow(categoryTable, "Không có dữ liệu", normalFont, 2);
            }
            document.add(categoryTable);
            document.add(new Paragraph(" "));

            // So sánh doanh thu
            Paragraph compareHeader = new Paragraph("SO SÁNH DOANH THU", headerFont);
            compareHeader.setSpacingBefore(15f);
            compareHeader.setSpacingAfter(10f);
            document.add(compareHeader);

            PdfPTable compareTable = new PdfPTable(2);
            compareTable.setWidthPercentage(100);
            compareTable.setWidths(new float[]{3, 2});
            addTableHeader(compareTable, "Mô tả", headerFont);
            addTableHeader(compareTable, "Giá trị", headerFont);

            String[][] compareData = {
                    {"Doanh thu tháng trước", String.format("%,.0f", totalRevenuePreviousMonth) + " VNĐ"},
                    {"Doanh thu tháng này", String.format("%,.0f", totalRevenueCurrentMonth) + " VNĐ"},
                    {"Chênh lệch", String.format("%,.0f", (totalRevenueCurrentMonth - totalRevenuePreviousMonth)) + " VNĐ"},
                    {"Tỷ lệ tăng/giảm", String.format("%.2f%%", growthRate)}
            };

            for (String[] row : compareData) {
                addTableCell(compareTable, row[0], normalFont, Element.ALIGN_LEFT);
                addTableCell(compareTable, row[1], normalFont, Element.ALIGN_RIGHT);
            }

            document.add(compareTable);
            document.close();
            writer.close();

            // Trả về PDF cho client
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDispositionFormData("attachment", "bao_cao_doanh_thu.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok().headers(headers).body(outputStream.toByteArray());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(("Lỗi tạo PDF: " + e.getMessage()).getBytes());
        }
    }

    // Tạo font tiếng Việt từ file DejaVuSans.ttf
    private Font createVietnameseFont(int size, int style) {
        try {
            InputStream fontStream = getClass().getClassLoader().getResourceAsStream("fonts/DejaVuSans.ttf");
            if (fontStream == null) throw new IOException("Không tìm thấy font trong resources");
            byte[] fontData = fontStream.readAllBytes();
            BaseFont baseFont = BaseFont.createFont("DejaVuSans.ttf", BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontData, null);
            fontStream.close();
            return new Font(baseFont, size, style);
        } catch (Exception e) {
            System.err.println("Lỗi load font: " + e.getMessage());
            e.printStackTrace();
            return FontFactory.getFont("Arial", BaseFont.WINANSI, BaseFont.EMBEDDED, size, style);
        }
    }

    // Thêm header cho bảng
    private void addTableHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setBackgroundColor(BaseColor.LIGHT_GRAY);
        cell.setPadding(8f);
        table.addCell(cell);
    }

    // Thêm ô dữ liệu vào bảng
    private void addTableCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setPadding(5f);
        table.addCell(cell);
    }

    // Thêm dòng "Không có dữ liệu"
    private void addNoDataRow(PdfPTable table, String text, Font font, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(10f);
        table.addCell(cell);
    }

    // Tính tỷ lệ tăng trưởng doanh thu
    private double calculateGrowthRate(double previousRevenue, double currentRevenue) {
        return previousRevenue == 0 ? 0 : ((currentRevenue - previousRevenue) / previousRevenue) * 100;
    }
}
