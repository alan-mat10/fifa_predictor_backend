package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "tournament_settings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TournamentSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Lock datetime for tournament predictions (World Cup Winner, Golden Ball, Golden Boot, Golden Glove).
     * After this time, users cannot submit or modify tournament predictions.
     * Stored in IST.
     */
    private LocalDateTime tournamentPredictionLockTime;

    /**
     * Manual lock flag — if true, predictions are locked regardless of the lock time.
     */
    @Builder.Default
    private boolean tournamentPredictionsLocked = false;
}
