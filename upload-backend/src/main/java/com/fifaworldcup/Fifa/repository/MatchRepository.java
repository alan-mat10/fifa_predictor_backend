package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.Match;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface MatchRepository extends JpaRepository<Match, Long> {
    List<Match> findByStatus(Match.MatchStatus status);
    List<Match> findByStage(Match.Stage stage);
    List<Match> findByGroupOrderByMatchDateTimeAsc(String group);
    List<Match> findAllByOrderByMatchDateTimeAsc();
    List<Match> findByMatchDateTimeBetween(LocalDateTime start, LocalDateTime end);
    List<Match> findByStageAndGroupAndStatus(Match.Stage stage, String group, Match.MatchStatus status);
    List<Match> findByStageAndStatus(Match.Stage stage, Match.MatchStatus status);
    List<Match> findByStatusOrderByMatchDateTimeDesc(Match.MatchStatus status);
}
