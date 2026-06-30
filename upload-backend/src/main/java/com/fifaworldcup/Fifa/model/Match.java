package com.fifaworldcup.Fifa.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "matches")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Match {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "team1_id", nullable = true)
    private Team team1;

    @ManyToOne
    @JoinColumn(name = "team2_id", nullable = true)
    private Team team2;

    private LocalDateTime matchDateTime;

    private String venue;

    @Enumerated(EnumType.STRING)
    private Stage stage;

    @Column(name = "match_group")
    private String group;

    private Integer team1Score;  // actual score (null until match ends)

    private Integer team2Score;  // actual score (null until match ends)

    private Integer team1PenaltyScore;  // penalty shootout score (null if no shootout)

    private Integer team2PenaltyScore;  // penalty shootout score (null if no shootout)

    private String manOfTheMatch;  // MOTM player name (set by admin after match)

    @Builder.Default
    @Enumerated(EnumType.STRING)
    private MatchStatus status = MatchStatus.UPCOMING;

    public enum Stage {
        GROUP, ROUND_OF_32, ROUND_OF_16, QUARTER_FINAL, SEMI_FINAL, THIRD_PLACE, FINAL
    }

    public enum MatchStatus {
        UPCOMING, LIVE, COMPLETED
    }
}
