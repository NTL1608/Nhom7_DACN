package com.sieuvjp.greenbook.controller.admin;

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
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/report")
@RequiredArgsConstructor
public class ReportController {

    private final OrderService orderService;
    private final BookService bookService;
    private final CategoryService categoryService;
    private final OrderDetailRepository orderDetailRepository;
    private final CategoryRepository categoryRepository;

    // Định nghĩa màu sắc
    private static final BaseColor PRIMARY_COLOR = new BaseColor(41, 128, 185);
    private static final BaseColor SECONDARY_COLOR = new BaseColor(52, 152, 219);
    private static final BaseColor SUCCESS_COLOR = new BaseColor(39, 174, 96);
    private static final BaseColor DANGER_COLOR = new BaseColor(231, 76, 60);
    private static final BaseColor HEADER_BG = new BaseColor(236, 240, 241);
    private static final BaseColor TABLE_BORDER = new BaseColor(189, 195, 199);

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPDF(
            @RequestParam(required = false, defaultValue = "") String reporterName,
            @RequestParam(required = false, defaultValue = "") String reporterPosition,
            @RequestParam(required = false, defaultValue = "") String reportNote
    ) {
        try {
            // Lấy dữ liệu
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            LocalDateTime endOfMonth = LocalDateTime.now()
                    .withDayOfMonth(LocalDateTime.now().toLocalDate().lengthOfMonth())
                    .withHour(23).withMinute(59).withSecond(59);

            double totalRevenueCurrentMonth = orderService.getRevenueBetween(startOfMonth, endOfMonth);

            LocalDateTime startOfPreviousMonth = startOfMonth.minusMonths(1);
            LocalDateTime endOfPreviousMonth = startOfMonth.minusSeconds(1);
            double totalRevenuePreviousMonth = orderService.getRevenueBetween(startOfPreviousMonth, endOfPreviousMonth);

            List<Map<String, Object>> topSellingBooksData = orderService.getTopSellingBooks(5);
            List<Object[]> topSellingCategories = categoryRepository.findTopSellingCategories(5);

            double growthRate = calculateGrowthRate(totalRevenuePreviousMonth, totalRevenueCurrentMonth);

            // Tạo document PDF
            Document document = new Document(PageSize.A4, 40, 40, 50, 50);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            PdfWriter writer = PdfWriter.getInstance(document, outputStream);

            // Thêm header và footer
            writer.setPageEvent(new PdfPageEventHelper() {
                @Override
                public void onEndPage(PdfWriter writer, Document document) {
                    try {
                        Font footerFont = createVietnameseFont(9, Font.ITALIC);

                        // Footer
                        PdfPTable footer = new PdfPTable(2);
                        footer.setTotalWidth(document.getPageSize().getWidth() - 80);
                        footer.setWidths(new float[]{1, 1});

                        PdfPCell left = new PdfPCell(new Phrase("Green Book - Hệ thống quản lý nhà sách", footerFont));
                        left.setBorder(Rectangle.TOP);
                        left.setBorderColor(TABLE_BORDER);
                        left.setHorizontalAlignment(Element.ALIGN_LEFT);
                        left.setPadding(5);

                        PdfPCell right = new PdfPCell(new Phrase("Trang " + writer.getPageNumber(), footerFont));
                        right.setBorder(Rectangle.TOP);
                        right.setBorderColor(TABLE_BORDER);
                        right.setHorizontalAlignment(Element.ALIGN_RIGHT);
                        right.setPadding(5);

                        footer.addCell(left);
                        footer.addCell(right);
                        footer.writeSelectedRows(0, -1, 40, 40, writer.getDirectContent());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            document.open();

            // Fonts
            Font titleFont = createVietnameseFont(24, Font.BOLD);
            Font subtitleFont = createVietnameseFont(14, Font.NORMAL);
            Font headerFont = createVietnameseFont(16, Font.BOLD);
            Font normalFont = createVietnameseFont(11, Font.NORMAL);
            Font boldFont = createVietnameseFont(11, Font.BOLD);
            Font smallFont = createVietnameseFont(9, Font.NORMAL);

            // ============ HEADER SECTION ============
            addHeaderSection(document, titleFont, subtitleFont, smallFont, startOfMonth, endOfMonth,
                    reporterName, reporterPosition);

            // ============ TỔNG QUAN ============
            addOverviewSection(document, headerFont, normalFont, boldFont,
                    totalRevenueCurrentMonth, totalRevenuePreviousMonth, growthRate);

            // ============ BIỂU ĐỒ TĂNG TRƯỞNG ============
            addGrowthIndicator(document, normalFont, growthRate);

            // ============ TOP SÁCH BÁN CHẠY ============
            addTopBooksSection(document, headerFont, normalFont, boldFont, topSellingBooksData);

            // ============ TOP DANH MỤC ============
            addTopCategoriesSection(document, headerFont, normalFont, boldFont, topSellingCategories);

            // ============ SO SÁNH CHI TIẾT ============
            addComparisonSection(document, headerFont, normalFont, boldFont,
                    totalRevenuePreviousMonth, totalRevenueCurrentMonth, growthRate);

            // ============ GHI CHÚ VÀ CHỮ KÝ ============
            addNotesAndSignatureSection(document, headerFont, normalFont, smallFont,
                    reporterName, reporterPosition, reportNote);

            document.close();
            writer.close();

            // Trả về response
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            String filename = "BaoCaoDoanhThu_" +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf";
            headers.setContentDispositionFormData("attachment", filename);
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok().headers(headers).body(outputStream.toByteArray());

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError()
                    .body(("Lỗi tạo PDF: " + e.getMessage()).getBytes());
        }
    }

    // ============ CÁC PHƯƠNG THỨC HELPER ============

    private void addHeaderSection(Document doc, Font titleFont, Font subtitleFont,
                                  Font smallFont, LocalDateTime start, LocalDateTime end,
                                  String reporterName, String reporterPosition) throws DocumentException {

        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        try {
            headerTable.setWidths(new float[]{2, 3});
        } catch (DocumentException e) {
            e.printStackTrace();
        }

        // Cột trái: Logo + Info
        PdfPCell leftCell = new PdfPCell();
        leftCell.setBorder(Rectangle.NO_BORDER);
        Paragraph companyInfo = new Paragraph();
        companyInfo.add(new Chunk("GREEN BOOK\n", createVietnameseFont(16, Font.BOLD)));
        companyInfo.add(new Chunk("Hệ thống nhà sách trực tuyến\n", smallFont));
        companyInfo.add(new Chunk("Website: greenbook.vn | Hotline: 1900-8080", smallFont));
        leftCell.addElement(companyInfo);

        // Cột phải: Tiêu đề + Người lập
        PdfPCell rightCell = new PdfPCell();
        rightCell.setBorder(Rectangle.NO_BORDER);
        rightCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        Paragraph reportTitle = new Paragraph();
        reportTitle.add(new Chunk("BÁO CÁO DOANH THU\n", titleFont));
        reportTitle.add(new Chunk("THÁNG " + start.getMonthValue() + "/" + start.getYear() + "\n\n", subtitleFont));
        reportTitle.add(new Chunk("Ngày tạo: " +
                new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()) + "\n", smallFont));

        // Thêm thông tin người lập báo cáo
        if (reporterName != null && !reporterName.trim().isEmpty()) {
            reportTitle.add(new Chunk("Người lập: " + reporterName, smallFont));
            if (reporterPosition != null && !reporterPosition.trim().isEmpty()) {
                reportTitle.add(new Chunk(" (" + reporterPosition + ")", smallFont));
            }
            reportTitle.add(new Chunk("\n", smallFont));
        }

        rightCell.addElement(reportTitle);

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        doc.add(headerTable);

        // Đường kẻ phân cách
        addSeparatorLine(doc, PRIMARY_COLOR);
        doc.add(new Paragraph("\n"));
    }

    private void addOverviewSection(Document doc, Font headerFont, Font normalFont,
                                    Font boldFont, double current, double previous, double growth) throws DocumentException {

        Paragraph header = new Paragraph("TỔNG QUAN DOANH THU", headerFont);
        header.setSpacingBefore(10f);
        header.setSpacingAfter(15f);
        doc.add(header);

        PdfPTable table = new PdfPTable(3);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{1, 1, 1});
        table.setSpacingAfter(20f);

        addMetricCard(table, "Doanh thu tháng này",
                String.format("%,.0f VNĐ", current),
                PRIMARY_COLOR, normalFont, boldFont);

        addMetricCard(table, "Doanh thu tháng trước",
                String.format("%,.0f VNĐ", previous),
                SECONDARY_COLOR, normalFont, boldFont);

        BaseColor growthColor = growth >= 0 ? SUCCESS_COLOR : DANGER_COLOR;
        String growthText = String.format("%s%.2f%%", growth >= 0 ? "+" : "", growth);
        addMetricCard(table, "Tăng trưởng", growthText, growthColor, normalFont, boldFont);

        doc.add(table);
    }

    private void addMetricCard(PdfPTable table, String label, String value,
                               BaseColor color, Font normalFont, Font boldFont) {

        PdfPCell cell = new PdfPCell();
        cell.setPadding(15);
        cell.setBorderColor(TABLE_BORDER);
        cell.setBorderWidth(1);

        Paragraph content = new Paragraph();
        Chunk labelChunk = new Chunk(label + "\n\n", normalFont);
        labelChunk.setTextRise(2);
        content.add(labelChunk);

        Font valueFont = new Font(boldFont.getBaseFont(), 18, Font.BOLD, color);
        content.add(new Chunk(value, valueFont));
        content.setAlignment(Element.ALIGN_CENTER);

        cell.addElement(content);
        table.addCell(cell);
    }

    private void addGrowthIndicator(Document doc, Font normalFont, double growth)
            throws DocumentException {

        Paragraph indicator = new Paragraph();
        indicator.setAlignment(Element.ALIGN_CENTER);

        String arrow = growth >= 0 ? "▲" : "▼";
        BaseColor color = growth >= 0 ? SUCCESS_COLOR : DANGER_COLOR;
        String text = String.format("%s Tăng trưởng %s%.2f%% so với tháng trước",
                arrow, growth >= 0 ? "+" : "", growth);

        Font indicatorFont = new Font(normalFont.getBaseFont(), 12, Font.BOLD, color);
        indicator.add(new Chunk(text, indicatorFont));
        indicator.setSpacingAfter(20f);

        doc.add(indicator);
    }

    private void addTopBooksSection(Document doc, Font headerFont, Font normalFont,
                                    Font boldFont, List<Map<String, Object>> books) throws DocumentException {

        Paragraph header = new Paragraph("TOP 5 SÁCH BÁN CHẠY NHẤT", headerFont);
        header.setSpacingBefore(10f);
        header.setSpacingAfter(15f);
        doc.add(header);

        PdfPTable table = new PdfPTable(new float[]{1, 5, 2, 3});
        table.setWidthPercentage(100);
        table.setSpacingAfter(20f);

        addStyledHeader(table, "#", boldFont);
        addStyledHeader(table, "Tên sách", boldFont);
        addStyledHeader(table, "Số lượng", boldFont);
        addStyledHeader(table, "Doanh thu", boldFont);

        if (books != null && !books.isEmpty()) {
            int rank = 1;
            for (Map<String, Object> book : books) {
                String title = book.get("title") != null ? book.get("title").toString() : "N/A";
                String quantity = book.get("soldQuantity") != null ?
                        book.get("soldQuantity").toString() : "0";
                double revenue = book.get("revenue") != null ?
                        ((Number)book.get("revenue")).doubleValue() : 0.0;

                addStyledCell(table, String.valueOf(rank++), normalFont, Element.ALIGN_CENTER);
                addStyledCell(table, title, normalFont, Element.ALIGN_LEFT);
                addStyledCell(table, quantity, normalFont, Element.ALIGN_CENTER);
                addStyledCell(table, String.format("%,.0f VNĐ", revenue), normalFont, Element.ALIGN_RIGHT);
            }
        } else {
            addNoDataCell(table, "Không có dữ liệu", normalFont, 4);
        }

        doc.add(table);
    }

    private void addTopCategoriesSection(Document doc, Font headerFont, Font normalFont,
                                         Font boldFont, List<Object[]> categories) throws DocumentException {

        Paragraph header = new Paragraph("TOP 5 DANH MỤC BÁN CHẠY", headerFont);
        header.setSpacingBefore(10f);
        header.setSpacingAfter(15f);
        doc.add(header);

        PdfPTable table = new PdfPTable(new float[]{1, 4, 3, 2});
        table.setWidthPercentage(100);
        table.setSpacingAfter(20f);

        addStyledHeader(table, "#", boldFont);
        addStyledHeader(table, "Danh mục", boldFont);
        addStyledHeader(table, "Doanh thu", boldFont);
        addStyledHeader(table, "Tỷ trọng", boldFont);

        double totalRevenue = 0;
        if (categories != null) {
            for (Object[] cat : categories) {
                if (cat[1] != null) {
                    totalRevenue += ((Number)cat[1]).doubleValue();
                }
            }
        }

        if (categories != null && !categories.isEmpty()) {
            int rank = 1;
            for (Object[] cat : categories) {
                String name = cat[0] != null ? cat[0].toString() : "N/A";
                double revenue = cat[1] != null ? ((Number)cat[1]).doubleValue() : 0.0;
                double percentage = totalRevenue > 0 ? (revenue / totalRevenue) * 100 : 0;

                addStyledCell(table, String.valueOf(rank++), normalFont, Element.ALIGN_CENTER);
                addStyledCell(table, name, normalFont, Element.ALIGN_LEFT);
                addStyledCell(table, String.format("%,.0f VNĐ", revenue), normalFont, Element.ALIGN_RIGHT);
                addStyledCell(table, String.format("%.1f%%", percentage), normalFont, Element.ALIGN_CENTER);
            }
        } else {
            addNoDataCell(table, "Không có dữ liệu", normalFont, 4);
        }

        doc.add(table);
    }

    private void addComparisonSection(Document doc, Font headerFont, Font normalFont,
                                      Font boldFont, double previous, double current, double growth) throws DocumentException {

        Paragraph header = new Paragraph("PHÂN TÍCH CHI TIẾT", headerFont);
        header.setSpacingBefore(10f);
        header.setSpacingAfter(15f);
        doc.add(header);

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{3, 2});
        table.setSpacingAfter(20f);

        addStyledHeader(table, "Chỉ số", boldFont);
        addStyledHeader(table, "Giá trị", boldFont);

        String[][] data = {
                {"Doanh thu tháng trước", String.format("%,.0f VNĐ", previous)},
                {"Doanh thu tháng này", String.format("%,.0f VNĐ", current)},
                {"Chênh lệch", String.format("%s%,.0f VNĐ",
                        current >= previous ? "+" : "", current - previous)},
                {"Tỷ lệ tăng/giảm", String.format("%s%.2f%%", growth >= 0 ? "+" : "", growth)}
        };

        for (String[] row : data) {
            addStyledCell(table, row[0], normalFont, Element.ALIGN_LEFT);
            addStyledCell(table, row[1], boldFont, Element.ALIGN_RIGHT);
        }

        doc.add(table);
    }

    private void addNotesAndSignatureSection(Document doc, Font headerFont, Font normalFont,
                                             Font smallFont, String reporterName, String reporterPosition, String reportNote)
            throws DocumentException {

        // Ghi chú
        Paragraph header = new Paragraph("GHI CHÚ", headerFont);
        header.setSpacingBefore(10f);
        header.setSpacingAfter(10f);
        doc.add(header);

        Paragraph notes = new Paragraph();
        notes.add(new Chunk("• Báo cáo được tự động tạo bởi hệ thống Green Book\n", smallFont));
        notes.add(new Chunk("• Dữ liệu được tính toán dựa trên đơn hàng đã hoàn thành\n", smallFont));

        if (reportNote != null && !reportNote.trim().isEmpty()) {
            notes.add(new Chunk("• Ghi chú: " + reportNote + "\n", smallFont));
        }

        notes.add(new Chunk("• Mọi thắc mắc xin liên hệ bộ phận IT: it@greenbook.vn\n", smallFont));
        notes.setSpacingAfter(30f);
        doc.add(notes);

        // Phần chữ ký
        if (reporterName != null && !reporterName.trim().isEmpty()) {
            PdfPTable signatureTable = new PdfPTable(2);
            signatureTable.setWidthPercentage(100);
            signatureTable.setWidths(new float[]{1, 1});

            // Cột trái - trống
            PdfPCell leftCell = new PdfPCell();
            leftCell.setBorder(Rectangle.NO_BORDER);
            leftCell.addElement(new Paragraph(" "));

            // Cột phải - chữ ký
            PdfPCell rightCell = new PdfPCell();
            rightCell.setBorder(Rectangle.NO_BORDER);
            rightCell.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph signature = new Paragraph();
            signature.setAlignment(Element.ALIGN_CENTER);
            signature.add(new Chunk("Ngày " + new SimpleDateFormat("dd").format(new Date()) +
                    " tháng " + new SimpleDateFormat("MM").format(new Date()) +
                    " năm " + new SimpleDateFormat("yyyy").format(new Date()) + "\n\n",
                    smallFont));
            signature.add(new Chunk("Người lập báo cáo\n", normalFont));

            if (reporterPosition != null && !reporterPosition.trim().isEmpty()) {
                signature.add(new Chunk("(" + reporterPosition + ")\n", smallFont));
            }

            signature.add(new Chunk("\n\n\n\n", normalFont)); // Khoảng trống cho chữ ký
            signature.add(new Chunk(reporterName, createVietnameseFont(12, Font.BOLD)));

            rightCell.addElement(signature);

            signatureTable.addCell(leftCell);
            signatureTable.addCell(rightCell);

            doc.add(signatureTable);
        }
    }

    // Helper methods
    private void addStyledHeader(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(PRIMARY_COLOR);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(10f);
        cell.setBorderColor(TABLE_BORDER);

        Font whiteFont = new Font(font.getBaseFont(), font.getSize(), font.getStyle(), BaseColor.WHITE);
        cell.setPhrase(new Phrase(text, whiteFont));
        table.addCell(cell);
    }

    private void addStyledCell(PdfPTable table, String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(alignment);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        cell.setPadding(8f);
        cell.setBorderColor(TABLE_BORDER);
        table.addCell(cell);
    }

    private void addNoDataCell(PdfPTable table, String text, Font font, int colspan) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setColspan(colspan);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setPadding(15f);
        cell.setBorderColor(TABLE_BORDER);
        cell.setBackgroundColor(HEADER_BG);
        table.addCell(cell);
    }

    private void addSeparatorLine(Document doc, BaseColor color) throws DocumentException {
        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setBorderWidthBottom(2);
        cell.setBorderColorBottom(color);
        cell.setFixedHeight(2);
        line.addCell(cell);
        doc.add(line);
    }

    private Font createVietnameseFont(int size, int style) {
        try {
            InputStream fontStream = getClass().getClassLoader()
                    .getResourceAsStream("fonts/DejaVuSans.ttf");
            if (fontStream == null) {
                throw new IOException("Không tìm thấy font trong resources");
            }
            byte[] fontData = fontStream.readAllBytes();
            BaseFont baseFont = BaseFont.createFont("DejaVuSans.ttf",
                    BaseFont.IDENTITY_H, BaseFont.EMBEDDED, true, fontData, null);
            fontStream.close();
            return new Font(baseFont, size, style);
        } catch (Exception e) {
            System.err.println("Lỗi load font: " + e.getMessage());
            return FontFactory.getFont("Arial", BaseFont.WINANSI, BaseFont.EMBEDDED, size, style);
        }
    }

    private double calculateGrowthRate(double previous, double current) {
        return previous == 0 ? 0 : ((current - previous) / previous) * 100;
    }
}