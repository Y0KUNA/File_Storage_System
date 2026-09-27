# k6 Load Test Suite — File Storage Service

Bộ script test hiệu năng cho hệ thống microservices, chạy qua api-gateway. Viết dựa trên API contract trong `03-api-contracts-ver2_0.md`.

## Cài đặt

Chạy trong Ubuntu qua WSL2, không phải PowerShell hay CMD.

```
sudo gpg -k
sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
sudo apt-get update
sudo apt-get install k6
```

## Trước khi chạy

Đảm bảo `docker compose up -d` đã chạy toàn bộ hệ thống trong Ubuntu, và bạn biết đúng port của `api-gateway`. Sửa `BASE_URL` trong `config.js` nếu không phải `http://localhost:8080/api/v1`.

## Thứ tự chạy

### 1. Smoke test trước tiên, luôn luôn

```
k6 run smoke-test.js
```

1 VU, 1 lần chạy toàn bộ luồng: đăng ký, đăng nhập, tạo folder root, upload, virus scan, download. Nếu bước nào fail, sửa trước khi chạy load test. Chạy load test khi hệ thống còn lỗi cơ bản chỉ cho ra số liệu vô nghĩa.

### 2. Test riêng từng phần

```
k6 run auth-load-test.js
k6 run upload-load-test.js
```

Dùng để cô lập vấn đề. Nếu mixed-scenario-test chậm, chạy hai lệnh này riêng để biết identity-service hay upload-service là điểm nghẽn.

### 3. Test kịch bản hỗn hợp, gần với thực tế nhất

```
k6 run mixed-scenario-test.js
```

70% VU chỉ đọc (list folder, xem quota), 30% VU ghi (upload). Đây là số liệu nên đưa vào CV hoặc báo cáo, vì phản ánh tải hỗn hợp thay vì tải đơn lẻ không thực tế.

## Đọc kết quả

k6 in ra các chỉ số sau khi test xong:

- `http_req_duration`: thời gian phản hồi. Xem `p(95)` và `p(99)`, không chỉ giá trị trung bình. Trung bình dễ bị số liệu tốt che khuất các request chậm hiếm gặp.
- `http_req_failed`: tỉ lệ request lỗi. Trên 1% là dấu hiệu hệ thống đã quá tải ở mức VU đó.
- `iterations`: tổng số lần chạy hết 1 vòng kịch bản, tương ứng số lượng "hành động người dùng" đã mô phỏng.

Mỗi test có `thresholds` riêng, khai báo trong file. Nếu k6 báo `THRESHOLD FAILED` khi kết thúc, nghĩa là hệ thống không đạt mục tiêu hiệu năng đã đặt ra cho endpoint đó, không phải lỗi script.

## Lưu ý khi ghi vào CV hoặc báo cáo

Ghi rõ môi trường test là WSL2 trên máy cá nhân, không phải server production. Số liệu tuyệt đối (req/s, ms) sẽ khác trên hạ tầng thật, nhưng cách thiết kế kịch bản test (ramping load, kịch bản hỗn hợp, threshold rõ ràng) là phần thể hiện kỹ năng, quan trọng hơn con số cụ thể.

## Tùy chỉnh

- Đổi kích thước file test: `k6 run -e FILE_SIZE_KB=500 upload-load-test.js`
- Đổi BASE_URL: `k6 run -e BASE_URL=http://localhost:9000/api/v1 smoke-test.js`
- Đổi số VU/thời gian: sửa trực tiếp mảng `stages` trong từng file, không cần flag dòng lệnh riêng.
