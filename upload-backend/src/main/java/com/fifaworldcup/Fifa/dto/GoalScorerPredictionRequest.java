package com.fifaworldcup.Fifa.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class GoalScorerPredictionRequest {
    @NotNull
    private Long matchId;

    @NotNull
    private List<Long> playerIds;  // players predicted to score

    private Long firstGoalScorerPlayerId;  // optional: predict first goal scorer for extra bonus

    private Map<Long, Integer> playerGoalCounts;  // optional: playerId -> number of goals predicted (default 1)
}
