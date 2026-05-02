from datetime import datetime
from typing import List, Optional

from pydantic import BaseModel, Field


class DetectPeopleReq(BaseModel):
    jobId: int
    scenicId: int
    videoPath: str
    areaCode: str = "MAIN"
    direction: str = "ENTER"
    sampleMs: int = Field(default=1000, ge=200)
    frameStep: int = Field(default=10, ge=1)
    fileSize: Optional[int] = None


class DetectPoint(BaseModel):
    statTime: str
    peopleCount: int


class DetectPeopleData(BaseModel):
    points: List[DetectPoint]


class DetectPeopleResp(BaseModel):
    code: int = 0
    message: str = "ok"
    data: DetectPeopleData
    serverTime: str

def now_iso() -> str:
    return datetime.now().isoformat(timespec="seconds")
