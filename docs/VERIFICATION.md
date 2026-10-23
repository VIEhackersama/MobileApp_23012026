# Nghiệm thu base người 1 — Fruit Shop

Ngày kiểm tra: **23/10/2026**, Asia/Bangkok. Đây là bản nền tảng/tài khoản, **không phải app Fruit Shop hoàn chỉnh**.

## Phạm vi đã xác nhận

| Nội dung | Trạng thái |
|---|---|
| Project/package/nhận diện Fruit Shop, Java/XML, executor nền | Đã có |
| SQLite schema 5 bảng, model, DTO, enum, helper tiền/số lượng/ngày | Đã có |
| AuthRepository đăng nhập, đăng ký Customer và kiểm tra quyền | Đã có implementation và kiểm thử |
| SharedPreferences, khôi phục phiên, sự kiện thay đổi phiên | Đã có implementation và kiểm thử |
| Trang tài khoản Admin/Customer, hồ sơ, đăng xuất | Đã có |
| FruitDataSource | Chỉ interface; implementation thuộc người 2 |
| OrderDataSource, CartStore | Chỉ interface; implementation thuộc người 3 |
| UI CRUD trái cây, danh mục, giỏ, checkout, lịch sử và quản lý đơn | Chưa triển khai, dành cho người 2/3 |

Không có class runtime implements ba interface nghiệp vụ trên; không có repository trả kết quả giả. Các fixture SQL trong test chỉ kiểm tra schema/khóa ngoại, không được dùng làm code nghiệp vụ.

## Môi trường và lệnh

- JBR 21, Gradle 9.4.1, AGP 9.2.1; compile SDK 36.1, target 36, min 24.
- Android Emulator API 35/AOSP x86_64, WHPX.
- APK `com.example.fruitshop`, versionName 1.0, versionCode 1, chữ ký debug.

```powershell
.\gradlew.bat --offline --no-daemon --console=plain :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
```

Kết quả lượt nghiệm thu cuối: **BUILD SUCCESSFUL**.

| Bộ kiểm thử | Tổng | Qua | Lỗi/thất bại | Bỏ qua |
|---|---:|---:|---:|---:|
| FoundationTest: mật khẩu, gram/kg, số nguyên, tiền, ngày/timezone | 10 | 10 | 0 | 0 |
| AuthSchemaInstrumentedTest: seed, auth, transaction, rollback, schema và FK | 10 | 10 | 0 | 0 |
| SessionInstrumentedTest: ghi nhớ, sự kiện, cách ly preference, giả mạo role | 5 | 5 | 0 | 0 |
| AccountUiInstrumentedTest: lỗi đăng nhập, Admin, đăng ký Customer, logout/Back | 4 | 4 | 0 | 0 |
| **Tổng** | **29** | **29** | **0** | **0** |

Bằng chứng: [unit-tests.xml](test-results/unit-tests.xml), [android-tests.xml](test-results/android-tests.xml), [lint-results.txt](test-results/lint-results.txt).

- Lint: **0 lỗi, 13 cảnh báo**: 4 gợi ý nâng phiên bản SDK/thư viện và 9 resource chưa được dùng ở bản tài khoản (bao gồm template danh sách và nhãn để mở rộng). Không tắt các cảnh báo để che kết quả.
- `apksigner verify`: qua. `aapt2 dump badging`: đúng package/nhãn/min SDK/target SDK.
- Đã kiểm tra các DEX trong APK: không còn package ứng dụng cũ.
- Đã đối chiếu source runtime, resource, test và cấu hình: không còn nghiệp vụ cũ.
- Không thay phiên bản SDK/thư viện hoặc thêm backend.

## Kiểm tra trực tiếp

- Cài APK cuối lên emulator thành công.
- Admin đăng nhập dù không có hồ sơ Customer. Ghi nhớ, force-stop và mở launcher: vào đúng trang Admin.
- Customer hiển thị đúng tên, điện thoại và email; không chọn ghi nhớ thì force-stop và mở lại quay về đăng nhập.
- Giao diện đăng ký có đủ thông tin, mật khẩu xác nhận, không có lựa chọn vai trò Admin. Luồng đăng ký tự vào Customer và không ghi nhớ đã được UI test kiểm tra.
- Trang chính chỉ có phần tài khoản và đăng xuất. Không xuất hiện chức năng nghiệp vụ chưa thực hiện.
- Crash buffer emulator không ghi nhận crash trong phiên kiểm tra trực tiếp.

Ảnh chụp: [đăng nhập](screenshots/01-login.png), [Admin](screenshots/02-admin-account.png), [đăng ký](screenshots/03-register.png), [Customer](screenshots/04-customer-account.png).

## Bảo toàn và bàn giao

- Thư mục vật lý giữ nguyên, nhánh làm việc `dong-1`.
- Agent không chạy git reset, commit hoặc push; giữ remote và lịch sử của nhóm.
- README trước thay đổi, patch local, mốc Git trước triển khai và các gói cũ được lưu tại `C:\Users\Admin\Documents\VPProjects\.fruitshop-backups\20261023-164810`.
- Script đóng gói lấy working tree, gồm cả file mới và loại file đã xóa, không thay Git index.
- Gói mới: `artifacts/FruitShop-debug.apk`, `artifacts/FruitShop-source.zip`, `artifacts/SHA256SUMS.txt`. ZIP không chứa cache, database người dùng, emulator, local.properties hoặc khóa ký.
- Ranh giới người 2/3 và điểm nối được ghi tại [HANDOFF.md](HANDOFF.md).

Đã kiểm tra trên emulator API 35, chưa kiểm tra điện thoại thật hoặc toàn bộ API 24–36. Các kiểm thử đặt hàng, chống bán vượt tồn và chuyển trạng thái sẽ do người 2/3 bổ sung khi có implementation.
