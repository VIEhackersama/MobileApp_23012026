# Giao diện quản lý đơn hàng

Cập nhật ngày 23/10/2026 trên bản nền `e16ce8c`.

Danh sách Admin hiển thị từng đơn bằng một thẻ gồm mã đơn, ngày giờ theo định dạng Việt Nam, trạng thái tiếng Việt, tên và điện thoại khách hàng, số mặt hàng, tóm tắt số lượng và tổng tiền. Chạm vào thẻ hoặc vùng “Xem chi tiết” để mở màn hình chi tiết riêng.

Chi tiết hiển thị tên, điện thoại và email khách hàng; từng sản phẩm có số lượng, đơn giá và thành tiền. Tổng đơn và nút chuyển trạng thái nằm ở cuối màn hình; phần thông tin phía trên cuộn được khi đơn có nhiều món. Điện thoại/email có thể mở ứng dụng liên hệ tương ứng trên thiết bị.

Trạng thái chỉ chuyển **Chờ xác nhận → Đã xác nhận → Hoàn thành**. Nút cập nhật bị khóa trong khi xử lý; đơn hoàn thành không còn nút chuyển tiếp. Quay lại danh sách sẽ tải lại trạng thái mới. Có trạng thái tải, danh sách rỗng và nút thử lại khi đọc dữ liệu thất bại.

`OrderDetails` bổ sung `Customer`. Repository kiểm tra quyền đọc đơn trước khi trả thông tin liên hệ; Customer vẫn chỉ được đọc đơn của mình. Thông tin liên hệ lấy từ hồ sơ hiện tại, còn tên/giá/đơn vị/số lượng sản phẩm lấy từ snapshot `OrderItem`. Không thay schema hay tính lại tiền của đơn cũ.

## Kiểm tra

- Build APK, unit test và lint thành công; 10 unit test qua, lint 0 lỗi và 19 cảnh báo cấu hình/tài nguyên nền.
- 7 kiểm thử `OrderRepositoryInstrumentedTest` và 2 kiểm thử `AdminOrdersUiInstrumentedTest` qua trên emulator API 35.
- Test UI kiểm tra danh sách, thông tin khách, chi tiết món, tái tạo Activity, hai bước cập nhật trạng thái, tải lại danh sách và không trừ tồn lần nữa.
- Test UI dùng database và preferences riêng. Hash database/preferences gốc trước và sau test trùng khớp; đơn thực tế #1 giá 80.000đ còn nguyên.
- Kiểm tra trực tiếp danh sách và chi tiết trên database thật, không cập nhật trạng thái đơn thật.

Ảnh sau cập nhật:

- [Danh sách đơn](screenshots/05-admin-orders.png)
- [Chi tiết đơn](screenshots/06-admin-order-detail.png)

Các lệnh build:

```powershell
.\gradlew.bat --offline --no-daemon --console=plain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:assembleDebugAndroidTest
```

Kiểm thử thiết bị được chạy bằng `adb install -r` và `am instrument`, để bộ điều phối Gradle không gỡ ứng dụng/dữ liệu hiện có sau test. Log cục bộ nằm trong `verification/admin-orders-build.log`, `verification/admin-orders-device-tests.log` và `verification/admin-orders-data-preservation.json`.
