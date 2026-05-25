# Auto Clicker Android

Ứng dụng tự động click và vuốt màn hình Android.

## Tính năng
- ✅ Hiển thị nổi trên tất cả ứng dụng khác (overlay)
- ✅ Chế độ 1: Click với delay cố định (người dùng tự cài)
- ✅ Chế độ 2: Click ngẫu nhiên, delay không trùng lặp
- ✅ Chế độ Vuốt: vuốt nhiều vị trí khác nhau, delay riêng cho từng điểm
- ✅ Nút nổi kéo được, hiện số lần đã click
- ✅ Giao diện tối (dark UI)

---

## Cách Build APK

### Yêu cầu
- Android Studio Hedgehog (2023.1.1) trở lên
- JDK 8 trở lên (thường kèm Android Studio)

### Bước 1 — Mở dự án
1. Giải nén file ZIP này
2. Mở Android Studio → **File → Open**
3. Chọn thư mục `AutoClicker` vừa giải nén
4. Chờ Gradle sync (lần đầu ~3-5 phút tải dependencies)

### Bước 2 — Build APK
- Menu **Build → Build Bundle(s) / APK(s) → Build APK(s)**
- Hoặc chạy lệnh trong Terminal:
  ```
  cd AutoClicker
  ./gradlew assembleDebug
  ```

### Bước 3 — Tìm file APK
- APK xuất ra tại: `app/build/outputs/apk/debug/app-debug.apk`
- Chép file này sang điện thoại và cài đặt

---

## Cách dùng trên điện thoại

### Cấp quyền lần đầu (bắt buộc)

**1. Quyền hiển thị trên ứng dụng khác:**
- Cài APK → mở app → app sẽ hỏi
- Hoặc vào: Cài đặt → Ứng dụng → Auto Clicker → Hiển thị trên các ứng dụng khác → Bật

**2. Accessibility Service (bắt buộc để click/vuốt):**
- Cài đặt → Trợ năng (Accessibility) → Dịch vụ đã cài → Auto Clicker → Bật
- ⚠️ Đây là quyền cốt lõi, app không hoạt động nếu thiếu

### Sử dụng

1. **Cài đặt vị trí click**: vào Cài đặt → nhập tọa độ X, Y
   - Để tìm tọa độ: Developer Options → Pointer Location → chạm màn hình
2. **Chọn chế độ**: Fixed (delay đều) hoặc Random (ngẫu nhiên)
3. **Nhấn Bắt đầu** → nút nổi xuất hiện
4. **Chuyển sang app khác** → nhấn ▶ trên nút nổi để bắt đầu click
5. **Nhấn ■** để dừng

### Cấu hình Vuốt
- Bật "Chế độ Vuốt" trên màn hình chính
- Nhấn "Cấu hình điểm vuốt"
- Thêm các điểm: tọa độ bắt đầu → kết thúc, thời gian vuốt, delay
- Các điểm sẽ được thực hiện lần lượt và lặp lại

---

## Cấu trúc code

```
app/src/main/java/com/autoclicker/app/
├── model/
│   ├── AppSettings.kt       — Cài đặt ứng dụng
│   ├── ClickMode.kt         — Enum FIXED / RANDOM
│   └── SwipePoint.kt        — Dữ liệu 1 điểm vuốt
├── utils/
│   ├── PreferenceManager.kt — Lưu/đọc cài đặt
│   └── RandomDelayGenerator.kt — Tạo delay ngẫu nhiên không trùng
├── service/
│   ├── AutoClickAccessibilityService.kt — Engine click/vuốt
│   └── OverlayService.kt    — Nút nổi foreground service
└── ui/
    ├── MainActivity.kt
    ├── SettingsActivity.kt
    └── SwipeConfigActivity.kt
```

---

## Lưu ý
- Một số điện thoại (MIUI, ColorOS) cần thêm bước cấp quyền chạy nền
- Android 13+ có thể hỏi thêm quyền notification
- Tọa độ click/vuốt tính theo pixel thực của màn hình
