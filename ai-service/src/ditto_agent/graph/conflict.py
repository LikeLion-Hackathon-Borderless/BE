from collections.abc import Callable
from datetime import datetime, time
from zoneinfo import ZoneInfo

from ditto_agent.schema import ConflictResult, DraftContext

ConflictChecker = Callable[[str, DraftContext], ConflictResult]

# Placeholder only — real working-hours/holiday logic ("C-5/C-6, 코드/DB 로직") belongs to
# the FastAPI teammate's module. This exists so the graph is runnable standalone for
# tests/cli_demo.py. Swap it via build_graph(conflict_checker=...); see agent/README.md.


def default_conflict_checker(
    time_confirmed: str, context: DraftContext
) -> ConflictResult:
    try:
        sender_dt = datetime.fromisoformat(time_confirmed)
    except ValueError:
        return ConflictResult(
            receiver_local_time="미확정",
            within_working_hours=False,
            note="ISO8601 형식이 아니라 변환 불가 — 실제 시간 파싱은 팀원 모듈에서 처리",
        )

    if sender_dt.tzinfo is None:
        sender_dt = sender_dt.replace(tzinfo=ZoneInfo(context.sender_tz))
    receiver_dt = sender_dt.astimezone(ZoneInfo(context.receiver_tz))
    try:
        work_start = time.fromisoformat(context.receiver_work_start)
        work_end = time.fromisoformat(context.receiver_work_end)
    except ValueError:
        work_start = time(9, 0)
        work_end = time(18, 0)
    work_days = {day.upper() for day in context.receiver_work_days}
    within_hours = (
        receiver_dt.strftime("%A").upper() in work_days
        and work_start <= receiver_dt.timetz().replace(tzinfo=None) <= work_end
    )

    return ConflictResult(
        receiver_local_time=receiver_dt.isoformat(),
        within_working_hours=within_hours,
        note=None
        if within_hours
        else (
            f"수신자 근무시간({context.receiver_work_start}-{context.receiver_work_end}, "
            f"{','.join(context.receiver_work_days)}) 밖"
        ),
    )
