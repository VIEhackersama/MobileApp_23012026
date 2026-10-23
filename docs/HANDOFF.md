# Hợp đồng bàn giao base — người 1 → người 2 và người 3

## Ranh giới công việc

Base này chỉ triển khai tài khoản và nền tảng. Không có implementation `FruitDataSource`, `OrderDataSource`, `CartStore`; không có UI nghiệp vụ trái cây/giỏ/đơn, không gọi stub trả dữ liệu giả.

| Phần | Chủ sở hữu |
|---|---|
| Schema, migration, model/DTO/enums, tiền/số lượng/ngày, lỗi chung | Người 1 |
| AuthRepository, SessionManager, FruitShopApp, BaseActivity, AccountHomeActivity, đăng nhập/đăng ký, build, manifest | Người 1 |
| FruitDataSource implementation; CRUD/tìm/chi tiết/danh mục trái cây; giao diện Admin quản lý trái cây/đơn | Người 2 |
| OrderDataSource và CartStore implementation; giỏ/checkout/lịch sử/chi tiết đơn; nghiệp vụ chuyển trạng thái | Người 3 |
| AdminHomeActivity | Base tài khoản đã tạo; chuyển cho người 2 mở rộng |
| CustomerHomeActivity | Base tài khoản đã tạo; chuyển cho người 3 mở rộng |

Người 2/3 tạo layout, resource và test riêng cho module của mình. Người 2 có thể dùng template `activity_list`, `item_record`, `ic_add` hiện có. Người 3 tạo layout đơn riêng, không sửa template của người 2. Dùng `strings_fruit.xml` và `strings_orders.xml` để tránh cùng sửa chuỗi chung.

Nếu cần thêm Activity, dependency, schema hoặc thay hợp đồng, phối hợp người 1 tích hợp. Không tự tạo DBHelper/database thứ hai. Hai trang chính có thể chuyển sang layout riêng/extends BaseActivity nhưng phải giữ kiểm tra vai trò và sự kiện đăng xuất. Không gắn hai module vào Application trước khi có implementation thật.

## Môi trường và cách nối

- Package `com.example.fruitshop`; Java 11, JDK chạy Gradle 21; API 24+; compile SDK 36.1, target 36.
- `FruitShopApp.database`: helper chung; `auth`: xác thực/quyền; `session`: phiên; `clock`: đồng hồ ngày; `databaseExecutor`: executor DB; `main`: handler giao diện.
- Repository/interface là API **Java nội bộ đồng bộ**, không phải API HTTP. Mọi truy vấn/ghi SQLite và hash mật khẩu chạy trên `databaseExecutor`/`BaseActivity.submit`.
- Lấy `actorAccountId` từ phiên đã xác minh; gọi `auth.accountById`, `requireAdmin` hoặc `requireCustomer` trong repository. Không nhận role từ Intent/giỏ/SharedPreferences để cấp quyền.
- `BaseActivity` xác minh lại tài khoản khi resume, chuyển màn hình về đúng vai trò và chống callback khôi phục phiên cũ sau đăng xuất. `requiredRole()` phải trả ADMIN hoặc CUSTOMER cho màn hình có quyền riêng.
- Listener phiên chạy đồng bộ trên thread gọi `signIn`/`signOut` (các Activity nền tảng gọi trên main thread). Người 3 đăng ký listener một lần cho CartStore cấp Application, xóa giỏ của tài khoản cũ khi đổi/đăng xuất; gỡ listener khi hủy store. `generation()` tăng mỗi lần đăng nhập/đăng xuất để nhận biết callback cũ.
- Không ghi mật khẩu vào log, preferences hoặc model Account. AuthRepository chỉ trả id/username/role.

## Schema phiên bản 1

```text
Account(id, username, password, role)
Fruit(id, name, category, price, quantity, expiryDate, description, unit)
Customer(id, fullname, phone, email, accountId)
Order(id, customerId, orderDate, totalAmount, status)
OrderItem(id, orderId, fruitId, fruitName, unit, unitPrice, quantity, lineTotal)
```

- Tên file: `fruit_shop.db`. Tất cả id là INTEGER PRIMARY KEY AUTOINCREMENT/Java long.
- `Account.username`: unique NOCASE; `password`: chuỗi hash, không phải mật khẩu thô; `role`: ADMIN/CUSTOMER, không đổi sau tạo.
- `Customer.accountId`: unique, FK Account ON DELETE RESTRICT. Trigger chặn gắn hồ sơ Customer cho Admin. Admin không có hồ sơ Customer.
- `Fruit.price`: INTEGER > 0, tiền VND theo đơn vị bán. `quantity`: INTEGER >= 0. `unit`: KG/PIECE/PACK, trigger không cho đổi sau tạo. `expiryDate`: ISO `yyyy-MM-dd`; schema kiểm tra khuôn dạng, module trái cây phải gọi `BusinessDates.isValid` để kiểm tra ngày thực.
- Luôn quote tên bảng `"Order"` trong SQL/SQLiteDatabase (`db.insertOrThrow("\"Order\"", ...)`). `customerId` FK Customer RESTRICT, `orderDate` epoch milliseconds >= 0, `totalAmount` nguyên VND >= 0.
- `OrderItem.orderId`: FK Order RESTRICT. `fruitId`: nullable Long, FK Fruit ON DELETE SET NULL. Dòng snapshot giữ fruitName, unit, unitPrice > 0, quantity > 0, lineTotal >= 0. Unique(orderId, fruitId): giỏ cần gộp cùng mặt hàng trước đặt đơn.
- Khóa ngoại luôn bật. Có index Order.customerId, OrderItem.orderId/fruitId. Không drop dữ liệu để nâng cấp; thay schema cần migration của người 1.
- Chưa có đơn seed. Test schema có insert fixture trực tiếp để kiểm tra khóa ngoại/snapshot; đó không phải code đặt hàng được bàn giao.

## Đơn vị, ngày và tiền

| unit | Giá lưu | quantity lưu | Nhập trên giao diện |
|---|---|---|---|
| KG | VND/kg | gram nguyên | kg, tối đa 3 số lẻ; nhận dấu chấm hoặc phẩy |
| PIECE | VND/quả | số quả nguyên | số nguyên |
| PACK | VND/phần | số phần nguyên | số nguyên |

`Quantities.parse(text, unit)` nhận số không âm, bao gồm 0 cho tồn kho. Module giỏ/checkout phải kiểm tra thêm quantity > 0. `Quantities.format` hiển thị đơn vị; không dùng double cho tồn.

`Money.lineTotal(price, baseQuantity, unit)` tính thành tiền bằng BigDecimal, KG chia 1000 và làm tròn HALF_UP tới đồng. `Money.add` cộng có kiểm tra overflow; `Money.format` hiển thị VND. Ví dụ 500g × 40.000 + 2 quả × 15.000 + 1 phần × 25.000 = 75.000đ. 500g × 40.001 = 20.001đ. Một lượng rất nhỏ có thể có lineTotal = 0 do làm tròn; schema cho phép tổng không âm.

`BusinessDates.today/isExpired/plusDays` dùng BusinessClock có thể inject, timezone thiết bị. Hết hạn khi expiryDate < hôm nay; hôm nay vẫn được bán. Kiểm tra lại thời điểm đặt, không chỉ khi thêm giỏ.

## Chữ ký interface và hành vi cần thực hiện tiếp

### Người 2 — FruitDataSource

```java
List<Fruit> list(long actorAccountId, String query);
Fruit detail(long actorAccountId, long fruitId);
long create(long actorAccountId, FruitInput input);
void update(long actorAccountId, long fruitId, FruitInput input);
void delete(long actorAccountId, long fruitId);
```

`list/detail` yêu cầu tài khoản hợp lệ; Admin xem toàn bộ, Customer xem hàng còn hạn và còn tồn. Tìm tên/category không phân biệt hoa/thường theo khả năng SQLite; query rỗng trả danh sách. Không tìm thấy → NOT_FOUND. Ghi dữ liệu chỉ Admin; validate ngày thực, các trường bắt buộc, giá > 0, tồn >= 0, unit bất biến. Customer có giỏ cũ vẫn có thể gặp NOT_FOUND/EXPIRED/OUT_OF_STOCK khi refresh. Xóa mặt hàng không xóa snapshot đơn.

### Người 3 — OrderDataSource

```java
long place(long actorAccountId, List<CartLine> lines);
List<Order> list(long actorAccountId);
OrderDetails detail(long actorAccountId, long orderId);
void advanceStatus(long actorAccountId, long orderId, OrderStatus nextStatus);
```

CartLine chứa `fruitId`, `quantity` theo đơn vị cơ sở, `quotedUnitPrice` khách đã xem. `place` chỉ Customer, giỏ không rỗng, gộp duplicate fruitId và kiểm tra overflow. Đọc lại unit/giá/hạn/tồn từ DB; giá lệch → PRICE_CHANGED và chưa tạo đơn; hết hạn/hết tồn → EXPIRED/OUT_OF_STOCK. Cả đơn và chi tiết và trừ tồn phải nằm trong một transaction, không nhận totalAmount từ UI. Tạo PENDING; trả Order.id.

`list/detail`: Admin xem tất cả, Customer chỉ đơn của Customer.id gắn account hiện tại; không tiết lộ chi tiết đơn người khác. Không tồn tại/không thuộc quyền đọc → NOT_FOUND. `advanceStatus` chỉ Admin, PENDING → CONFIRMED → COMPLETED; chuyển khác → INVALID_STATUS. Không trừ tồn thêm, chưa có hủy đơn.

### Người 3 — CartStore

```java
List<CartLine> lines(long actorAccountId);
void add(long actorAccountId, CartLine line);
void setQuantity(long actorAccountId, long fruitId, long baseQuantity);
void remove(long actorAccountId, long fruitId);
void clear(long actorAccountId);
```

Trong bộ nhớ theo phiên, không persist qua process death; giữ khi xoay màn hình bằng store cấp Application. `lines` trả snapshot không sửa được. `add` gộp cùng fruitId, dùng quote mới nhất; `setQuantity` > 0, muốn bỏ món gọi remove. clear/remove gọi nhiều lần an toàn. Chỉ nhận actor Customer đang đăng nhập; riêng clear phải dùng được cho ID cũ trong listener sau logout. Không dùng dữ liệu giỏ làm quyền hoặc tồn kho thật.

## AuthRepository đã có implementation

```java
Account login(String username, String password);
Account registerCustomer(RegistrationInput input);
Account accountById(long accountId);
Account requireAdmin(long actorAccountId);
Customer requireCustomer(long actorAccountId);
```

RegistrationInput không có role. Đăng ký transaction Account + Customer; trả Account, UI signIn(false) và điều hướng. Username 3–40 ký tự `[A-Za-z0-9._-]`; mật khẩu 6–128, giữ nguyên whitespace; xác nhận phải khớp. Điện thoại bỏ khoảng trắng/dấu gạch ngang rồi nhận `+` tùy chọn và 8–15 chữ số; email dùng Android Patterns. Không có API đổi quyền, quản lý khách hoặc đăng ký Admin.

## Mã lỗi chung

`DomainException.code`: INVALID_INPUT, AUTH_FAILED, SESSION_EXPIRED, USERNAME_EXISTS, FORBIDDEN, NOT_FOUND, OUT_OF_STOCK, EXPIRED, PRICE_CHANGED, INVALID_STATUS, STORAGE_ERROR. Không phân nhánh theo chuỗi thông báo; không trả lỗi SQL thô ra UI.

## Trình tự tích hợp

1. Nhóm ghi nhận/chia sẻ mốc base `dong-1` sau khi review; không đưa thay đổi chưa commit của người khác vào commit một cách tự động.
2. Người 2 tạo `fruit-admin`, người 3 tạo `fruit-customer-orders` từ cùng mốc. Không reset hoặc force-push nhánh đang có.
3. Người 2 cung cấp implementation thật và danh mục; người 3 nối thêm vào giỏ, đồng thời cung cấp OrderDataSource để người 2 dùng cho UI đơn Admin.
4. Người 1 nối Application/manifest/navigation sau khi có module thật. Các kiểm thử nền tảng phải còn qua, bổ sung kiểm thử module riêng.

Base không phải toàn bộ Fruit Shop hoàn chỉnh; người 1 không làm thay phần người 2/3.
