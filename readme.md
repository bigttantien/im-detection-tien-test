Cách build:
- Thêm file model vô thư mục `app/src/main/assets`, file model phải có extension `.tflite`
- Vào file `app/src/main/java/io/benkon/sample/image_detection/Constant.kt` sửa:
  - `MODEL_FILE`: tên model file, không bao gồm extension `.tflite`
  - Sửa các tham số khác nếu cần.

Cách app chạy:
- Option `Reset before detect`: Reset variables của model trước mỗi lần detect.
- Tất cả file hình có extension `.png`, `.jpg`, `.jpeg` trong thư mục được chọn sau khi bắm `Select` sẽ được detect.
- Output của việc detect lưu chính thư mục được chọn, các file sau được tạo:
  - `<tên file hình gốc>_labled.jpeg`: file hình được vẽ bounding box.
  - `<tên file hình gốc>.txt`: thông tin các bounding box.
  - `<tên file hình gốc>.log`: chứa output data `<Constant.OUTPUT_SIZE[2]>` dòng, [`<Constant.OUTPUT_SIZE[1]>`] phần tử mỗi dòng.
  - `<tên file hình gốc>_input_bitmap.txt`: chứa input bitmap content theo dạng:
    - x, y, alpha, red, green, blue
    - x = row
    - y = column
    - top left: x=0, y =0
  - `<tên file hình gốc>_processed_bitmap.txt`: chứa processed bitmap content.