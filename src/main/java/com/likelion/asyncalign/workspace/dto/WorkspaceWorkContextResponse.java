package com.likelion.asyncalign.workspace.dto;

import com.likelion.asyncalign.workspace.domain.WorkspaceMember;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Set;

public record WorkspaceWorkContextResponse(
        boolean overridden,
        String timeZoneId,
        LocalTime workStart,
        LocalTime workEnd,
        Set<DayOfWeek> workDays
) {
    public static WorkspaceWorkContextResponse from(WorkspaceMember member) {
        return new WorkspaceWorkContextResponse(
                member.isWorkContextOverridden(),
                member.getEffectiveTimeZoneId(),
                member.getEffectiveWorkStart(),
                member.getEffectiveWorkEnd(),
                member.getEffectiveWorkDays());
    }
}
