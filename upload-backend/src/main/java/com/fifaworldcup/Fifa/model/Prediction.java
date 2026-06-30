package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "predictions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "match_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Prediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @Column(name = "predicted_team1_score")
    private Integer predictedTeam1Score;

    @Column(name = "predicted_team2_score")
    private Integer predictedTeam2Score;

    /**
     * For knockout matches: if user predicts a draw, they must pick which team wins on penalties.
     * Null for group stage matches or non-draw predictions.
     */
    @Column(name = "penalty_winner_team_id")
    private Long penaltyWinnerTeamId;

    @Builder.Default
    private int pointsEarned = 0;

    @Builder.Default
    private boolean scored = false;  // whether points have been calculated

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
