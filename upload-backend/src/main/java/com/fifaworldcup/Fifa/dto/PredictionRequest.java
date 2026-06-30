package com.fifaworldcup.Fifa.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PredictionRequest {
    @NotNull
    private Long matchId;

    @Min(0)
    private int predictedTeam1Score;

    @Min(0)
    private int predictedTeam2Score;

    /**
     * For knockout matches: team ID of predicted penalty winner (required when scores are equal).
     */
    private Long penaltyWinnerTeamId;
}
