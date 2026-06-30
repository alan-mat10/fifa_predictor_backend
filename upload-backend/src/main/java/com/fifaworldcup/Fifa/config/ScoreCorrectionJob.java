package com.fifaworldcup.Fifa.config;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import com.fifaworldcup.Fifa.service.FootballDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * On startup, re-fetches scores for all completed R32 matches from the football API
 * to correct any wrongly stored scores (e.g., from the reversed-teams bug).
 * 
 * Only runs once at startup, then does nothing.
 * Set app.correct-r32-scores=false to disable.
 */
@Component
@Order(3)
@RequiredArgsConstructor
@Slf4j
public class ScoreCorrectionJob implements CommandLineRunner {

    private final MatchRepository matchRepository;
    private final FootballDataService footballDataService;

    @Value("${app.correct-r32-scores:false}")
    private boolean correctR32Scores;

    @Override
    public void run(String... args) {
        if (!correctR32Scores) return;

        List<Match> completedR32 = matchRepository.findByStageAndStatus(
                Match.Stage.ROUND_OF_32, Match.MatchStatus.COMPLETED);

        if (completedR32.isEmpty()) {
            log.info("📊 Score correction: No completed R32 matches to correct.");
            return;
        }

        // First, clear the R16 team assignments that were set by the buggy advancement
        List<Match> r16Matches = matchRepository.findByStage(Match.Stage.ROUND_OF_16);
        for (Match r16 : r16Matches) {
            r16.setTeam1(null);
            r16.setTeam2(null);
            matchRepository.save(r16);
        }
        log.info("📊 Score correction: Reset {} R16 match team slots.", r16Matches.size());

        log.info("📊 Score correction: Re-fetching {} completed R32 match(es)...", completedR32.size());

        for (Match match : completedR32) {
            try {
                // Reset status to UPCOMING so fetchAndUpdateMatchResult will process it
                match.setStatus(Match.MatchStatus.UPCOMING);
                match.setTeam1Score(null);
                match.setTeam2Score(null);
                match.setTeam1PenaltyScore(null);
                match.setTeam2PenaltyScore(null);
                matchRepository.save(match);

                // Re-fetch from API (this will set correct scores and re-run advancement)
                footballDataService.fetchAndUpdateMatchResult(match.getId());

                String team1 = match.getTeam1() != null ? match.getTeam1().getName() : "TBD";
                String team2 = match.getTeam2() != null ? match.getTeam2().getName() : "TBD";
                log.info("   ✅ Corrected: {} vs {}", team1, team2);
            } catch (Exception e) {
                log.error("   ❌ Failed to correct match {}: {}", match.getId(), e.getMessage());
            }
        }

        log.info("📊 Score correction complete.");
    }
}
