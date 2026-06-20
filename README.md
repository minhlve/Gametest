# Phá Gạch (Breakout) — Game Android 2D bằng Kotlin

Game đơn giản: dùng ngón tay kéo thanh trượt (paddle) để hứng bóng,
phá hết gạch để thắng. Để bóng rơi xuống đáy là thua.

## 1. Cách mở và chạy game

1. Cài **Android Studio** (bản mới nhất): https://developer.android.com/studio
2. Mở Android Studio → **File → Open** → chọn thư mục `BreakoutGame` (thư mục gốc, chứa file `settings.gradle.kts`).
3. Lần đầu mở, Android Studio sẽ tự tải các thành phần cần thiết (Gradle, SDK...) — chờ thanh "Sync" chạy xong (vài phút, cần mạng internet).
4. Nếu được hỏi tạo "Gradle Wrapper", chọn **OK/Yes** để Android Studio tự tạo.
5. Cắm điện thoại Android qua USB (bật **chế độ nhà phát triển** + **gỡ lỗi USB**), hoặc tạo máy giả lập (Emulator) trong Android Studio.
6. Bấm nút **Run ▶** (màu xanh) ở thanh công cụ.

Nếu không có điện thoại/máy thật, Android Studio có sẵn công cụ
**Device Manager** để tạo máy ảo Android — chạy thử ngay trên máy tính.

## 2. Cấu trúc project — file nào làm gì?

```
BreakoutGame/
├── app/
│   ├── build.gradle.kts          ← khai báo "linh kiện" app cần (thư viện, phiên bản Android...)
│   └── src/main/
│       ├── AndroidManifest.xml   ← "giấy khai sinh" của app (tên, màn hình nào mở đầu tiên...)
│       ├── java/com/example/breakout/
│       │   ├── MainActivity.kt  ← màn hình mở ra đầu tiên khi chạy app
│       │   └── GameView.kt      ← TOÀN BỘ logic của game (quan trọng nhất, đọc file này kỹ nhất)
│       └── res/values/
│           ├── strings.xml      ← chữ hiển thị (tên app...)
│           └── themes.xml       ← giao diện/màu chủ đề của app
├── build.gradle.kts              ← cấu hình chung cho cả project
└── settings.gradle.kts           ← khai báo project gồm những module nào
```

Bạn chỉ cần đọc kỹ **2 file Kotlin** (`MainActivity.kt` và `GameView.kt`),
phần còn lại là cấu hình mà Android Studio tạo & quản lý gần như tự động.

## 3. Vài khái niệm Kotlin cơ bản dùng trong code (vì bạn mới học)

- `class Tên(...) : LớpCha()` → tạo ra một "khuôn mẫu" đối tượng, kế thừa từ lớp có sẵn.
- `val` → biến **không đổi** giá trị sau khi gán. `var` → biến **có thể đổi** giá trị.
- `fun tenHam(...) { ... }` → khai báo một hàm (đoạn code có thể gọi lại nhiều lần).
- `override fun ...` → "ghi đè" một hàm có sẵn của lớp cha để viết hành vi riêng.
- `for (x in 0 until n)` → lặp lại từ 0 đến n-1, giống vòng for trong các ngôn ngữ khác.

## 4. Cách game hoạt động (đọc trong `GameView.kt` để thấy rõ)

Mọi game 2D đơn giản đều lặp đi lặp lại 3 bước, khoảng 60 lần mỗi giây:

1. **update()** — cập nhật vị trí bóng, kiểm tra va chạm với tường/paddle/gạch.
2. **onDraw()** — vẽ lại toàn bộ hình (gạch, paddle, bóng, điểm số) theo trạng thái mới.
3. **onTouchEvent()** — lắng nghe ngón tay người chơi để di chuyển paddle.

Vòng lặp này được tạo bằng một `Handler` tự gọi lại chính nó mỗi 16 mili giây
(biến `gameLoop` trong file `GameView.kt`) — đây chính là "động cơ" chạy suốt
cả game.

## 5. Gợi ý tự thử chỉnh sửa để học nhanh hơn

- Đổi `ballSpeedX`/`ballSpeedY` để bóng nhanh/chậm hơn.
- Đổi `rows`, `cols` để có nhiều/ít gạch hơn.
- Đổi màu trong các biến `...Paint` (ví dụ `paddlePaint`, `ballPaint`).
- Thêm tính năng mới, ví dụ: mỗi lần thua thì trừ 1 "mạng" (life) thay vì thua luôn.

Chỉnh code, bấm Run lại, xem ngay kết quả trên máy/điện thoại — đó là cách
nhanh nhất để hiểu code Android hoạt động ra sao.
