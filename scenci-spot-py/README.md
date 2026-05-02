# scenci-spot-py

视频人头检测服务（FastAPI + YOLO），供 Java 后端 `VideoJobService` 调用。

## 1. 安装依赖

```bash
pip install -r requirements.txt
```

## 2. 启动服务

```bash
uvicorn app.main:app --host 0.0.0.0 --port 5001
```

如果无法访问 GitHub（模型下载失败），请先手动准备模型文件并设置环境变量：

```bash
set YOLO_MODEL_PATH=C:\\models\\yolov8n.pt
set YOLO_CONF_THRESHOLD=0.25
uvicorn app.main:app --host 0.0.0.0 --port 5001
```

说明：

- `YOLO_MODEL_PATH` 默认值是当前目录下 `yolov8n.pt`
- 服务已改为懒加载模型，启动时不会强制下载；首次调用检测接口时才加载模型

## 3. 健康检查

- GET `/health`

## 4. 检测接口

- POST `/api/detect/people`

请求示例：

```json
{
  "jobId": 1,
  "scenicId": 1,
  "videoPath": "C:/Users/xxx/video/test.mp4",
  "areaCode": "GATE_IN",
  "direction": "ENTER",
  "sampleMs": 1000,
  "fileSize": 1234567
}
```

响应示例（后端可直接解析）：

```json
{
  "code": 0,
  "message": "ok",
  "data": {
    "points": [
      { "statTime": "2026-04-22T10:01:00", "peopleCount": 2 },
      { "statTime": "2026-04-22T10:01:01", "peopleCount": 1 }
    ]
  },
  "serverTime": "2026-04-22T10:02:30"
}
```

## 5. 与 Java 后端联调配置

在后端 `application.yml` 配置：

```yaml
app:
  video-detection:
    enabled: true
    base-url: http://127.0.0.1:5001
    timeout-ms: 120000
```

也可以用环境变量：

- `VIDEO_DETECTION_ENABLED=true`
- `VIDEO_DETECTION_BASE_URL=http://127.0.0.1:5001`
- `VIDEO_DETECTION_TIMEOUT_MS=120000`

## 6. 视频文件存储目录

Java 后端上传视频后，会把文件保存到后端配置的本地目录。默认是：

- `C:\Users\86152\Desktop\毕业设计\video`

你也可以在后端里通过环境变量覆盖：

- `VIDEO_UPLOAD_DIR=C:\Users\86152\Desktop\毕业设计\video`

## 7. 说明

- 当前按 YOLO 的 `person` 类别统计人数。
- 首次启动会自动下载模型文件（`yolov8n.pt`），需要联网。
- 如果有 NVIDIA GPU，可自行安装 GPU 版 PyTorch 以提升速度。
