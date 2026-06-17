package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "goal_scorer_predictions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"user_id", "match_id", "player_id"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GoalScorerPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "match_id", nullable = false)
    private Match match;

    @ManyToOne
    @JoinColumn(name = "player_id", nullable = false)
    private Player player;

    @Builder.Default
    private boolean isFirstGoalScorer = false;  // true = predicting first goal, false = anytime scorer

    @Builder.Default
    private int predictedGoals = 1;  // number of goals predicted for this player

    @Builder.Default
    private int pointsEarned = 0;

    @Builder.Default
    private boolean scored = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
