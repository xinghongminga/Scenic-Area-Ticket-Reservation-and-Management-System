import os
import json
from typing import Optional

from fastapi import FastAPI, HTTPException, Request

from app.detector import VideoPeopleDetector
from app.models import DetectPeopleReq, DetectPeopleResp, DetectPeopleData, DetectPoint, now_iso

app = FastAPI(title="Scenic Video Detection Service", version="1.0.0")
detector = VideoPeopleDetector(conf_threshold=float(os.getenv("YOLO_CONF_THRESHOLD", "0.25")))


@app.get("/health")
def health() -> dict:
    return {"status": "ok", "serverTime": now_iso()}


@app.post("/api/detect/people", response_model=DetectPeopleResp)
async def detect_people(request: Request) -> DetectPeopleResp:
    try:
        raw_body = await request.body()
        if not raw_body or not raw_body.strip():
            raise HTTPException(status_code=400, detail="empty request body")

        try:
            payload = json.loads(raw_body.decode("utf-8"))
        except UnicodeDecodeError as ex:
            preview = raw_body[:120].hex()
            raise HTTPException(status_code=400, detail=f"request body is not valid utf-8, hex preview: {preview}") from ex
        except json.JSONDecodeError as ex:
            preview = raw_body[:200].decode("utf-8", errors="replace")
            raise HTTPException(
                status_code=400,
                detail=f"invalid json body at line {ex.lineno} column {ex.colno}: {ex.msg}; body preview: {preview}",
            ) from ex

        req = DetectPeopleReq(
            jobId=_read_int(payload, "jobId", "job_id"),
            scenicId=_read_int(payload, "scenicId", "scenic_id"),
            videoPath=_read_str(payload, "videoPath", "video_path"),
            areaCode=_read_str(payload, "areaCode", "area_code", default="MAIN"),
            direction=_read_str(payload, "direction", default="ENTER"),
            sampleMs=_read_int(payload, "sampleMs", "sample_ms", default=1000),
            frameStep=_read_int(payload, "frameStep", "frame_step", default=10),
            fileSize=_read_int(payload, "fileSize", "file_size", default=None),
        )
        points = detector.detect_video(req.videoPath, req.sampleMs, req.frameStep)
        return DetectPeopleResp(
            data=DetectPeopleData(
                points=[
                    DetectPoint(statTime=p.stat_time, peopleCount=p.people_count)
                    for p in points
                ]
            ),
            serverTime=now_iso(),
        )
    except HTTPException:
        raise
    except FileNotFoundError as ex:
        raise HTTPException(status_code=400, detail=str(ex)) from ex
    except Exception as ex:
        raise HTTPException(status_code=500, detail=f"detect failed: {ex}") from ex


def _read_str(payload: dict, *keys: str, default: Optional[str] = None) -> str:
    for key in keys:
        value = payload.get(key)
        if value is not None and str(value).strip():
            return str(value).strip()
    if default is not None:
        return default
    joined = ", ".join(keys)
    raise HTTPException(status_code=400, detail=f"missing required field: {joined}")


def _read_int(payload: dict, *keys: str, default=None):
    for key in keys:
        value = payload.get(key)
        if value is None or value == "":
            continue
        try:
            return int(value)
        except (TypeError, ValueError) as ex:
            joined = ", ".join(keys)
            raise HTTPException(status_code=400, detail=f"invalid integer field {joined}: {value}") from ex
    return default
