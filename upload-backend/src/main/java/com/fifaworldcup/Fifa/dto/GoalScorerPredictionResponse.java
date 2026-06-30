package com.fifaworldcup.Fifa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GoalScorerPredictionResponse {
    private Long id;
    private Long matchId;
    private String team1Name;
    private String team2Name;
    private Long playerId;
    private String playerName;
    private String playerTeam;
    private boolean firstGoalScorer;
    private int predictedGoals;
    private int pointsEarned;
    private String matchStatus;
    private String username;
}
