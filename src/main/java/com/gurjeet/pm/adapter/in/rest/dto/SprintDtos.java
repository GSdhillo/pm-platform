package com.gurjeet.pm.adapter.in.rest.dto;

import com.gurjeet.pm.domain.model.Sprint;
import com.gurjeet.pm.domain.model.SprintStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SprintDtos {
    private SprintDtos() {}

    public record CreateSprintRequest(@NotBlank @Size(max = 200) String name,
                                      @Size(max = 1000) String goal,
                                      LocalDate startDate, LocalDate endDate) {}

    public record StartSprintRequest(LocalDate startDate, LocalDate endDate) {}

    public record CompleteSprintRequest(List<UUID> carryOverIssueIds, UUID targetSprintId) {}

    public record SprintResponse(UUID id, String name, String goal, SprintStatus status,
                                 LocalDate startDate, LocalDate endDate,
                                 Instant completedAt, Integer completedPoints) {
        public static SprintResponse from(Sprint s) {
            return new SprintResponse(s.getId(), s.getName(), s.getGoal(), s.getStatus(),
                    s.getStartDate(), s.getEndDate(), s.getCompletedAt(), s.getCompletedPoints());
        }
    }
}
