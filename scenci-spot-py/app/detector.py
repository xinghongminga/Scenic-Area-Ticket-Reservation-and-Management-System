from __future__ import annotations

from dataclasses import dataclass
from datetime import datetime, timedelta
import os
from pathlib import Path
from typing import List

import cv2
from ultralytics import YOLO


@dataclass
class DetectResultPoint:
    stat_time: str
    people_count: int


class VideoPeopleDetector:
    def __init__(self, model_path: str = "yolov8n.pt", conf_threshold: float = 0.25):
        self.model_path = os.getenv("YOLO_MODEL_PATH", model_path)
        self.conf_threshold = conf_threshold
        self.imgsz = int(os.getenv("YOLO_IMGSZ", "320"))
        self.max_process_seconds = int(os.getenv("YOLO_MAX_PROCESS_SECONDS", "600"))
        self.model = None

    def _ensure_model(self):
        if self.model is None:
            self.model = YOLO(self.model_path)

    def detect_video(self, video_path: str, sample_ms: int, frame_step: int = 10) -> List[DetectResultPoint]:
        self._ensure_model()
        p = Path(video_path)
        if not p.exists() or not p.is_file():
            raise FileNotFoundError(f"video file not found: {video_path}")

        cap = cv2.VideoCapture(str(p))
        if not cap.isOpened():
            raise RuntimeError(f"failed to open video: {video_path}")

        fps = cap.get(cv2.CAP_PROP_FPS)
        if fps is None or fps <= 0:
            fps = 25.0
        total_frames = int(cap.get(cv2.CAP_PROP_FRAME_COUNT) or 0)
        duration_seconds = (total_frames / fps) if total_frames > 0 else 0

        interval_frames = max(1, int(frame_step or 10))
        frame_index = 0
        points: List[DetectResultPoint] = []
        seen_track_ids = set()

        # 将视频时间轴映射到“当前时间往前推”，与后端统计口径兼容
        start_time = datetime.now() - timedelta(seconds=max(1, int(duration_seconds)))

        try:
            start_ts = datetime.now()
            while True:
                if (datetime.now() - start_ts).total_seconds() > self.max_process_seconds:
                    raise RuntimeError(f"video process timeout: exceeded {self.max_process_seconds}s")

                ok, frame = cap.read()
                if not ok:
                    break

                if frame_index % interval_frames != 0:
                    frame_index += 1
                    continue

                result = self.model.track(
                    frame,
                    verbose=False,
                    conf=self.conf_threshold,
                    classes=[0],
                    imgsz=self.imgsz,
                    persist=True,
                    tracker="bytetrack.yaml",
                )[0]
                boxes = result.boxes
                people_count = 0
                if boxes is not None and boxes.cls is not None:
                    track_ids = []
                    if boxes.id is not None:
                        track_ids = [int(tid) for tid in boxes.id.tolist() if tid is not None]
                    if track_ids:
                        new_track_ids = [tid for tid in track_ids if tid not in seen_track_ids]
                        people_count = len(new_track_ids)
                        seen_track_ids.update(track_ids)
                    else:
                        # YOLO 类别 0 对应 person
                        classes = boxes.cls.tolist()
                        people_count = sum(1 for c in classes if int(c) == 0)

                stat_time = (start_time + timedelta(seconds=frame_index / fps)).isoformat(timespec="seconds")
                points.append(DetectResultPoint(stat_time=stat_time, people_count=int(people_count)))

                frame_index += 1
        finally:
            cap.release()

        if not points:
            raise RuntimeError("no sample points generated from video")

        return points
