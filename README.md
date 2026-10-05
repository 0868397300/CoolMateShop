# HƯỚNG DẪN CHẠY VÀ CẤU HÌNH DỰ ÁN COOLMATESHOP

Dự án Website Thời trang Nam Coolmate xây dựng trên nền tảng Spring Boot 3.2.5, Java 21, Spring Data JPA, Hibernate, Thymeleaf, Bootstrap 5.3 và Microsoft SQL Server.

---

## 1. HƯỚNG DẪN KHẮC PHỤC LỖI KẾT NỐI SQL SERVER ("Login failed for user 'sa'")

Nếu khi khởi động bạn gặp lỗi:
```
com.microsoft.sqlserver.jdbc.SQLServerException: Login failed for user 'sa'
```

### Nguyên nhân:
Mật khẩu của tài khoản `sa` trên máy tính của bạn khác với mật khẩu mặc định `123456` đang khai báo trong file cấu hình.

### Cách xử lý cực kỳ đơn giản:
1. Mở file: `src/main/resources/application.properties`
2. Tìm đến dòng số 12:
   ```properties
   spring.datasource.username=sa
   spring.datasource.password=123456
   ```
3. Đổi giá trị `spring.datasource.password` thành đúng mật khẩu tài khoản `sa` mà bạn đã cài đặt trên máy tính của mình (ví dụ: `123`, `12345678`, `admin`, `root`...).
4. Lưu file và chạy lại ứng dụng trong STS (Spring Tool Suite) hoặc Eclipse/IntelliJ.

*(Lưu ý: Dự án đã được cấu hình sẵn `spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.SQLServerDialect` nên khi nhập đúng mật khẩu, Hibernate sẽ kết nối và khởi động mượt mà ngay).*

---

## 2. HỆ THỐNG HÌNH ẢNH SẢN PHẨM & BANNER (ĐÃ TÍCH HỢP SẴN)

Dự án đã được tích hợp đầy đủ bộ ảnh chuẩn tỉ lệ thời trang 3:4 và các banner, logo thương hiệu Coolmate vào thư mục tài nguyên tĩnh:

### Cấu trúc thư mục ảnh:
```
src/main/resources/static/
├── favicon.ico
└── images/
    ├── logo.png, logo-white.png
    ├── default-product.jpg
    ├── products/
    │   ├── ao-thun-compact-den-1.jpg, ao-thun-compact-den-2.jpg
    │   ├── ao-thun-compact-trang-1.jpg, ao-thun-compact-navy-1.jpg
    │   ├── polo-excool-navy-1.jpg, polo-excool-den-1.jpg
    │   ├── singlet-promax-xanh-1.jpg, ao-khoac-daily-den-1.jpg
    │   ├── short-running-5inch-den-1.jpg, quan-ut-pants-den-1.jpg
    │   ├── boxer-bamboo-den-1.jpg, tat-the-thao-trang-1.jpg
    ├── categories/
    │   ├── ao-nam-thumb.jpg, quan-nam-thumb.jpg
    │   ├── do-lot-thumb.jpg, phu-kien-thumb.jpg
    ├── banners/
    │   ├── banner-everyday.jpg, banner-running.jpg
    │   ├── banner-excool.jpg, banner-careshare.jpg
    └── reviews/
        ├── rv-compact-1.jpg, return-short-1.jpg
```

### Cơ chế gọi URL hình ảnh:
- Gọi theo danh mục: `http://localhost:8080/images/products/ao-thun-compact-den-1.jpg`
- Gọi trực tiếp: `http://localhost:8080/images/ao-thun-compact-den-1.jpg`
- Gọi banner: `http://localhost:8080/images/banners/banner-everyday.jpg`
- Gọi danh mục: `http://localhost:8080/images/categories/ao-nam-thumb.jpg`

Dự án hỗ trợ cả hai đường dẫn URL (có thư mục con và không có thư mục con), đồng thời các Entity (`Product`, `ProductImage`, `Category`, `Collection`) đều có cơ chế tự động chuyển đổi URL và fallback dự phòng (`onerror`) trong Thymeleaf để không bao giờ bị lỗi vỡ ảnh.

---

## 3. CÁC TÍNH NĂNG ĐÃ HOÀN THIỆN THEO CHECKLIST

- **CL-01: Trang chủ & Bán chạy**: Top Ticker, Hero gradient, Value propositions, Top 8 Best Sellers.
- **CL-02: Bộ lọc sản phẩm đa tiêu chí**: Route `/danh-muc/{slug}` lọc theo danh mục, màu sắc, size, khoảng giá, sắp xếp.
- **CL-03: Chi tiết SP & Đổi ảnh theo Màu**: Route `/san-pham/{slug}`, đổi swatch màu, size SKU, ảnh lớn.
- **CL-04: Công cụ Tư vấn chọn Size**: Modal nhập Chiều cao/Cân nặng tự tính toán size phù hợp (S - 3XL).
- **CL-05: Giỏ hàng & Thanh tiến trình Freeship**: `/gio-hang`, freeship từ 200k, tính tổng tiền tự động.
- **CL-06: Checkout & Đặt hàng**: Trang `/thanh-toan/checkout`, hỗ trợ COD và cổng VNPAY Sandbox.
