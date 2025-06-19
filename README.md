# GreenBook - Hệ thống quản lý bán sách trực tuyến

<p align="center">
  <img src="src/main/resources/static/images/logo.png" alt="GreenBook Logo" width="200">
</p>

## Giới thiệu

GreenBook là một ứng dụng web quản lý bán sách trực tuyến được phát triển bằng Spring Boot, cho phép admin quản lý toàn bộ quy trình bán hàng từ quản lý sách, danh mục, đơn hàng, khuyến mãi đến quản lý người dùng và blog.

## Các tính năng chính

- **Quản lý sách**: Thêm, sửa, xóa, tìm kiếm sách, quản lý hình ảnh sách
- **Quản lý danh mục**: Phân loại sách theo danh mục
- **Quản lý đơn hàng**: Xem và cập nhật trạng thái đơn hàng
- **Quản lý khuyến mãi**: Tạo và quản lý các chương trình khuyến mãi
- **Quản lý người dùng**: Phân quyền người dùng (admin, librarian, customer)
- **Quản lý blog**: Viết và xuất bản bài viết

## Công nghệ sử dụng

- **Backend**: Java 24, Spring Boot 3.2.3, Spring Security, Spring Data JPA
- **Frontend**: Thymeleaf, Bootstrap 5, jQuery, Font Awesome
- **Database**: MySQL 8
- **Build Tool**: Maven

## Yêu cầu hệ thống

- JDK 24
- Maven 3.6.3+
- MySQL 8.0+

## Cài đặt và chạy ứng dụng

### Chuẩn bị cơ sở dữ liệu

1. Cài đặt MySQL và tạo database:

```sql
CREATE DATABASE greenbook CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'greenbook'@'localhost' IDENTIFIED BY 'greenbook123';
GRANT ALL PRIVILEGES ON greenbook.* TO 'greenbook'@'localhost';
FLUSH PRIVILEGES;
```

### Cấu hình ứng dụng

1. Clone repository:

```bash
git clone https://github.com/NTDzVEKNY/BTL_SpringBoot_Greenbook.git
cd BTL_SpringBoot_Greenbook
```

2. Cấu hình kết nối database trong file `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/greenbook?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC
spring.datasource.username=greenbook
spring.datasource.password=greenbook123
```

username và password tùy vào mysql server bạn thiết lập

### Biên dịch và chạy ứng dụng

#### Sử dụng Maven:

```bash
mvn clean package
java -jar target/greenbook-0.0.1-SNAPSHOT.jar
```

Hoặc chạy trực tiếp:

```bash
 GOOGLE_API_KEY=<your-ai-api-key> mvn spring-boot:run
```

Sau khi ứng dụng khởi chạy, truy cập: http://localhost:8080

### Tài khoản mặc định

Khi ứng dụng chạy lần đầu, hệ thống sẽ tự động tạo tài khoản admin với thông tin:
- Username: admin
- Password: admin123

*Lưu ý: Hãy đổi mật khẩu này sau khi đăng nhập lần đầu.*

## Cấu trúc dự án

```
com.sieuvjp.greenbook
├── config          # Các file cấu hình
├── controller      # Các controller xử lý request
├── dto             # Data Transfer Objects
├── entity          # JPA Entities
├── enums           # Các enum
├── exception       # Xử lý ngoại lệ
├── repository      # JPA Repositories
├── service         # Business logic
└── util            # Các tiện ích
```

## Triển khai với Docker
Cấu hình kết nối Ai API ở đây là Gemini của Google trong file `docker-compose.yml`:

```properties
 environment:
  - SPRING_DATASOURCE_URL=jdbc:mysql://db:3306/greenbook?createDatabaseIfNotExist=true&useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true
  - SPRING_DATASOURCE_USERNAME=greenbook
  - SPRING_DATASOURCE_PASSWORD=greenbook123
  - SPRING_JPA_HIBERNATE_DDL_AUTO=update
  - SPRING_JPA_SHOW_SQL=false
  - GOOGLE_API_KEY=
```

GOOGLE_API_KEY cần điền API key Gemini của bản - đăng ký được trên trang của google

Sau đó để triển khai ứng dụng bằng Docker, sử dụng lệnh sau: (Ở đây có một số Unit tét nên trước khi chạy cái này bạn phải kết nối được db mysql trước để nó thực hiện test trước sau đó nó mới đóng gói lại)

```bash
# Đóng gói ứng dụng
mvn clean package -P prod

# Chạy với Docker Compose
docker-compose up -d
```

Ứng dụng sẽ chạy tại: http://localhost:8080
PHPMyAdmin sẽ chạy tại: http://localhost:8081

---

&copy; 2025 GreenBook. All rights reserved.
