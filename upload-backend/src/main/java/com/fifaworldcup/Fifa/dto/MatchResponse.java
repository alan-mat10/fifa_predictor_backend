package com.fifaworldcup.Fifa.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class MatchResponse {
    private Long id;
    private Long team1Id;
    private Long team2Id;
    private String team1Name;
    private String team2Name;
    private String team1Flag;
    private String team2Flag;
    private LocalDateTime matchDateTime;
    private String venue;
    private String stage;
    private String group;
    private Integer team1Score;
    private Integer team2Score;
    private Integer team1PenaltyScore;
    private Integer team2PenaltyScore;
    private String status;
    private boolean predictionLocked;
}
