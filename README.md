# Fruit Shop — base bàn giao của người 1

Ứng dụng Android **Java/XML + SQLite**, package `com.example.fruitshop`. Thư mục vật lý vẫn giữ nguyên theo yêu cầu; tên project trong Android Studio là **FruitShop**.

## Trạng thái và phạm vi

**Đã triển khai:** schema 5 bảng, model/DTO/interface chung, tính tiền/đơn vị/ngày, đăng nhập Admin/Customer, đăng ký Customer, SharedPreferences, sự kiện phiên và màn hình tài khoản theo vai trò.

**Chưa triển khai, dành cho người 2/3:** CRUD/tìm kiếm/danh mục trái cây, giỏ hàng, checkout, lịch sử/chi tiết đơn, cập nhật trạng thái đơn. Ba interface `FruitDataSource`, `OrderDataSource`, `CartStore` chưa có implementation. Màn hình chính chỉ có tài khoản/hồ sơ và đăng xuất, không giả lập các chức năng này.

## Mở và chạy

1. Android Studio → Open → chọn thư mục chứa `settings.gradle.kts` này.
2. Dùng Gradle JDK 21/JBR của Android Studio và SDK Platform 36.1.
3. Gradle Sync, chọn emulator/điện thoại Android 7.0 (API 24) trở lên, Run app.
4. Có thể đăng ký tài khoản Customer trực tiếp. Thông tin kiểm thử riêng tại [docs/DEMO.md](docs/DEMO.md).

```powershell
$env:JAVA_HOME = "$env:LOCALAPPDATA\Programs\Android Studio\jbr"
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
# Có emulator/thiết bị đang chạy:
.\gradlew.bat :app:connectedDebugAndroidTest
# Đóng gói working tree hiện tại, không yêu cầu git add/commit:
.\tools\package.ps1
```

Lần sync đầu cần mạng tải dependency; ứng dụng không dùng backend và không yêu cầu quyền Internet.

## Bàn giao

- [Hợp đồng dữ liệu, interface và phân quyền sở hữu cho người 2/3](docs/HANDOFF.md)
- [Kịch bản demo phần tài khoản](docs/DEMO.md)
- [Kết quả kiểm tra bản base](docs/VERIFICATION.md)

APK build: `app/build/outputs/apk/debug/app-debug.apk`. Bản đóng gói: `artifacts/FruitShop-debug.apk`, `artifacts/FruitShop-source.zip`, `artifacts/SHA256SUMS.txt`.

Database `fruit_shop.db` v1 và SharedPreferences `fruit_shop_session` tách riêng. Không chuyển dữ liệu/phiên của ứng dụng trước đó. Seed chỉ chạy khi tạo database lần đầu; bản cập nhật sau phải có migration bảo toàn dữ liệu.

## Làm việc nhóm

Base đang chuẩn bị trên nhánh `dong-1`; không tự commit/push. Sau khi nhóm ghi nhận và chia sẻ cùng một mốc base, người 2 và người 3 tạo nhánh từ mốc đó theo hướng dẫn HANDOFF. Giữ remote và lịch sử Git hiện có.
