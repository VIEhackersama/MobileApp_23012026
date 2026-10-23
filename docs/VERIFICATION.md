# Kết quả kiểm tra bản bàn giao

Ngày kiểm tra: **23/10/2026** (Asia/Bangkok).

## Môi trường

- Windows, Android Studio JBR 21, Gradle Wrapper 9.4.1, Android Gradle Plugin 9.2.1.
- Compile SDK 36.1, target SDK 36, min SDK 24.
- Android Emulator **StudentManager_API35**, Android 15/API 35, AOSP x86_64, WHPX.
- Package `com.example.studentmanager`, versionName `1.0`, versionCode `1`.

## Kết quả tự động

Lệnh hoàn tất thành công trên bản mã nguồn bàn giao:

```powershell
.\gradlew.bat --offline --no-daemon :app:assembleDebug :app:testDebugUnitTest :app:lintDebug :app:connectedDebugAndroidTest
```

| Nhóm | Tổng | Qua | Lỗi/thất bại | Bỏ qua |
|---|---:|---:|---:|---:|
| Unit test PasswordHasher | 3 | 3 | 0 | 0 |
| UI đăng nhập và phân quyền hiển thị | 3 | 3 | 0 | 0 |
| Repository, SQLite, transaction và phân quyền | 9 | 9 | 0 | 0 |
| SharedPreferences và phiên đăng nhập | 3 | 3 | 0 | 0 |
| **Tổng** | **18** | **18** | **0** | **0** |

- Android Lint: **0 lỗi, 4 cảnh báo** về target SDK/compile SDK và phiên bản AppCompat/RecyclerView mới hơn. Project giữ phiên bản công cụ trong kế hoạch và SDK đang có trên máy; không tắt các cảnh báo này.
- `apksigner verify`: thành công.
- `aapt2 dump badging`: đúng package, version, min SDK và target SDK.
- `git diff --cached --check`: không có lỗi khoảng trắng sau chuẩn hóa line ending của Gradle Wrapper.

Bằng chứng: [unit-tests.xml](test-results/unit-tests.xml), [android-tests.xml](test-results/android-tests.xml), [lint-results.txt](test-results/lint-results.txt).

## Kiểm tra trực tiếp trên emulator

- Cài APK debug thành công, mở được ứng dụng.
- Đăng nhập `teacher01` với ghi nhớ; force-stop tiến trình, mở lại launcher: khôi phục đúng màn hình giáo viên.
- Mở danh sách sinh viên và bảng điểm giáo viên; dữ liệu hiển thị đúng.
- Sửa điểm mẫu từ `8.5` thành `8.5555`, lưu rồi mở lại form: vẫn đúng `8.5555`, không mất phần thập phân. Sau kiểm tra đã khôi phục `8.5`.
- Đăng xuất giáo viên, đăng nhập `student01`: chỉ thấy 2 bản ghi của `SV001`, không có nút thêm/sửa/xóa.
- Sinh viên không chọn ghi nhớ; force-stop rồi mở lại ứng dụng: quay về màn hình đăng nhập.
- Crash buffer của emulator không ghi nhận crash trong phiên kiểm tra.

Ảnh chụp trực tiếp, đã kiểm tra bố cục và nội dung:

1. [Đăng nhập](screenshots/01-login.png)
2. [Trang chính giáo viên](screenshots/02-teacher-home.png)
3. [Danh sách sinh viên](screenshots/03-students.png)
4. [Bảng điểm giáo viên](screenshots/04-teacher-scores.png)
5. [Form sửa điểm và kiểm tra độ chính xác](screenshots/05-score-editor.png)
6. [Trang chính sinh viên](screenshots/06-student-home.png)
7. [Bảng điểm sinh viên](screenshots/07-student-scores.png)

## Giới hạn kiểm tra và bàn giao

- Đã chạy trên emulator API 35; chưa kiểm tra trên điện thoại thật hoặc toàn bộ phiên bản API 24–36.
- APK dùng chữ ký debug để phục vụ bài tập; chưa tạo bản phát hành lên cửa hàng.
- Git local được khởi tạo, mã nguồn đã stage để dễ kiểm tra. Chưa tạo commit hoặc đẩy GitHub/GitLab vì chưa có repository đích của nhóm.
- Bộ bảng và quyền nghiệp vụ sử dụng giả định trong kế hoạch đã được đồng ý triển khai; ảnh gốc vẫn gọi các bảng này là ví dụ.

## File bàn giao

- `artifacts/StudentManager-debug.apk`: APK đã build.
- `artifacts/StudentManager-source.zip`: mã nguồn, Gradle Wrapper, kiểm thử, README và bằng chứng kiểm tra; không gồm cache, SDK, emulator, khóa ký hoặc cấu hình SDK riêng của máy.
- `artifacts/SHA256SUMS.txt`: SHA-256 của APK và ZIP để đối chiếu.
