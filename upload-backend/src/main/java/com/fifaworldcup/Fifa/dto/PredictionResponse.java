package com.fifaworldcup.Fifa.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PredictionResponse {
    private Long id;
    private Long matchId;
    private String team1Name;
    private String team2Name;
    private String team1Flag;
    private String team2Flag;
    private int predictedTeam1Score;
    private int predictedTeam2Score;
    private Long penaltyWinnerTeamId;
    private Integer actualTeam1Score;
    private Integer actualTeam2Score;
    private int pointsEarned;
    private String matchStatus;
    private String username;
}
