# Fruit Shop — tiến trình triển khai

Ứng dụng Android **Java/XML + SQLite**, package `com.example.fruitshop`. Thư mục vật lý vẫn giữ nguyên theo yêu cầu; tên project trong Android Studio là **FruitShop**.

## Trạng thái và phạm vi

**Người 1 — đã triển khai:** schema 5 bảng, model/DTO/interface chung, tính tiền/đơn vị/ngày, đăng nhập Admin/Customer, đăng ký Customer, SharedPreferences, sự kiện phiên và màn hình tài khoản theo vai trò.

**Người 2 — đã triển khai:** `FruitRepository` hiện thực `FruitDataSource` với danh sách, tìm theo tên/danh mục, xem chi tiết và thêm/sửa/xóa trái cây. Repository kiểm tra quyền Admin/Customer, giá, tồn, hạn dùng và đơn vị bán. `FruitCatalogActivity` có danh mục, tìm kiếm, chi tiết, form quản lý cho Admin; AdminHome đã mở được màn hình này. CustomerHome cũng mở được danh mục, nhận kết quả chọn số lượng và hiển thị trái cây vừa chọn cùng tiền tạm tính. Lựa chọn này chưa được thêm vào giỏ hoặc đặt thành đơn. `AdminOrdersActivity` đã có giao diện xem danh sách/chi tiết đơn và chuyển trạng thái qua `OrderDataSource`. Chi tiết phần việc xem [bàn giao người 2](docs/FRUIT_ADMIN_HANDOFF.md).

**Còn chờ tích hợp:** người 3 triển khai `OrderDataSource`, `CartStore`, giỏ hàng, checkout và lịch sử/chi tiết đơn cho Customer; chuyển kết quả chọn mua từ CustomerHome vào giỏ. Sau khi có `OrderDataSource` thật, cần nối provider trong `FruitShopApp` để hiện nút quản lý đơn trên AdminHome. Hiện chưa có đơn hàng giả lập hoặc thao tác chuyển trạng thái hoạt động độc lập.

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
- [Tiến trình và điểm nối module của người 2](docs/FRUIT_ADMIN_HANDOFF.md)
- [Kịch bản demo phần tài khoản](docs/DEMO.md)
- Kết quả kiểm tra đã lưu tại `docs/test-results/`.

APK build: `app/build/outputs/apk/debug/app-debug.apk`. Bản đóng gói: `artifacts/FruitShop-debug.apk`, `artifacts/FruitShop-source.zip`, `artifacts/SHA256SUMS.txt`.

Database `fruit_shop.db` v1 và SharedPreferences `fruit_shop_session` tách riêng. Không chuyển dữ liệu/phiên của ứng dụng trước đó. Seed chỉ chạy khi tạo database lần đầu; bản cập nhật sau phải có migration bảo toàn dữ liệu.

## Làm việc nhóm

Phần người 2 hiện nằm trên nhánh `huyln17`, dựa trên mốc base của người 1. Các điểm nối với module người 3 được ghi trong [tài liệu bàn giao](docs/FRUIT_ADMIN_HANDOFF.md). Giữ remote và lịch sử Git hiện có khi tích hợp.
