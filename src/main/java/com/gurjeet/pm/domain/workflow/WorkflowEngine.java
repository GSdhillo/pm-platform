package com.gurjeet.pm.domain.workflow;

import com.gurjeet.pm.common.error.UnprocessableException;
import com.gurjeet.pm.domain.model.HookKind;
import com.gurjeet.pm.domain.model.Issue;
import com.gurjeet.pm.domain.model.TransitionHook;
import com.gurjeet.pm.domain.model.WorkflowStatus;
import com.gurjeet.pm.domain.model.WorkflowTransition;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

public final class WorkflowEngine {
    private WorkflowEngine() {}

    public static final String V_REQUIRE_ASSIGNEE = "REQUIRE_ASSIGNEE";
    public static final String V_REQUIRE_ESTIMATE = "REQUIRE_ESTIMATE";

    public static final String A_ASSIGN_REVIEWER = "ASSIGN_REVIEWER";

    public static WorkflowTransition requireTransition(WorkflowStatus current,
                                                       WorkflowStatus target,
                                                       List<WorkflowTransition> projectTransitions,
                                                       Map<UUID, WorkflowStatus> statusById) {
        return projectTransitions.stream()
                .filter(t -> t.getFromStatusId().equals(current.getId()) && t.getToStatusId().equals(target.getId()))
                .findFirst()
                .orElseThrow(() -> {
                    List<String> allowed = projectTransitions.stream()
                            .filter(t -> t.getFromStatusId().equals(current.getId()))
                            .map(t -> statusById.get(t.getToStatusId()).getName())
                            .sorted()
                            .collect(Collectors.toList());
                    return new UnprocessableException("WORKFLOW_VIOLATION",
                            "Transition from \"" + current.getName() + "\" to \"" + target.getName() + "\" is not allowed.",
                            Map.of("currentStatus", current.getName(), "allowedTransitions", allowed));
                });
    }

    public static void runValidators(Issue issue, List<TransitionHook> hooks) {
        for (TransitionHook hook : hooks) {
            if (hook.getKind() != HookKind.VALIDATOR) continue;
            switch (hook.getHookType()) {
                case V_REQUIRE_ASSIGNEE -> {
                    if (issue.getAssigneeId() == null) {
                        throw new UnprocessableException("VALIDATOR_FAILED",
                                "Transition blocked: issue must have an assignee.",
                                Map.of("validator", V_REQUIRE_ASSIGNEE));
                    }
                }
                case V_REQUIRE_ESTIMATE -> {
                    if (issue.getStoryPoints() == null) {
                        throw new UnprocessableException("VALIDATOR_FAILED",
                                "Transition blocked: issue must have story points.",
                                Map.of("validator", V_REQUIRE_ESTIMATE));
                    }
                }
                default -> {  }
            }
        }
    }

    public static void applyActions(Issue issue, List<TransitionHook> hooks) {
        for (TransitionHook hook : hooks) {
            if (hook.getKind() != HookKind.ACTION) continue;
            if (WorkflowEngine.A_ASSIGN_REVIEWER.equals(hook.getHookType())) {
                String reviewerId = hook.getConfig().path("userId").asText(null);
                if (reviewerId != null) issue.setAssigneeId(UUID.fromString(reviewerId));
            }
        }
    }
}
