// File này khai báo các "plugin" (công cụ build) sẽ dùng trong toàn bộ project.
// apply false = chỉ khai báo phiên bản ở đây, module app/ sẽ tự "bật" nó lên.
plugins {
    id("com.android.application") version "8.2.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
}
