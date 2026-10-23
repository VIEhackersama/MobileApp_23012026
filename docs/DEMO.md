# Demo phần tài khoản — người 1

Tài khoản local seed khi tạo database lần đầu:

| Vai trò | Username | Mật khẩu |
|---|---|---|
| Admin | `admin01` | `admin123` |
| Customer | `customer01` | `customer123` |
| Customer | `customer02` | `customer123` |

Đây là dữ liệu kiểm thử bài tập. Không hiển thị mật khẩu mẫu trên màn hình đăng nhập.

1. Đăng nhập sai để thấy lỗi và thử lại được.
2. Đăng nhập Admin: thấy tài khoản Admin, không cần hồ sơ Customer, có đăng xuất.
3. Đăng xuất → đăng ký tài khoản mới. Thử xác nhận mật khẩu sai rồi sửa đúng.
4. Đăng ký thành công tự vào trang Customer, hiện tên/điện thoại/email. Không ghi nhớ mặc định.
5. Đăng xuất, đăng nhập lại với ghi nhớ. Force-stop và mở lại: khôi phục đúng vai trò.
6. Đăng xuất, nhấn Back và mở lại: không vào lại trang đã đăng nhập.

Các bước trên demo phần tài khoản. Ở phiên bản hiện tại, Admin có thể mở **Trái cây** để tìm, thêm, sửa, xóa; Customer có thể mở **Xem và chọn trái cây**, chọn số lượng rồi xem tiền tạm tính trên trang chủ. Giỏ hàng, checkout và lịch sử đơn chưa được tích hợp.
