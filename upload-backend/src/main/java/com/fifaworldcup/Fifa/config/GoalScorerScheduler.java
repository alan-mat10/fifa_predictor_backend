package com.fifaworldcup.Fifa.config;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.repository.MatchGoalScorerRepository;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import com.fifaworldcup.Fifa.service.ApiFootballService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Scheduler that fetches goal scorers from API-Football every day at 9 AM IST.
 * Only processes the last 4 completed matches that don't already have scorer data.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class GoalScorerScheduler {

    private final MatchRepository matchRepository;
    private final MatchGoalScorerRepository matchGoalScorerRepository;
    private final ApiFootballService apiFootballService;

    /**
     * Runs daily at 9:00 AM IST (3:30 AM UTC).
     * Fetches goal scorers for the last 4 completed matches that are missing scorer data.
     */
    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Kolkata")
    @Transactional
    public void fetchGoalScorersForRecentMatches() {
        log.info("⚽ Goal Scorer Scheduler: Checking for missing goal scorer data...");

        // Get last 4 completed matches that DON'T have scorer data
        List<Match> completedMatches = matchRepository.findByStatusOrderByMatchDateTimeDesc(Match.MatchStatus.COMPLETED);

        int processed = 0;
        int fetched = 0;

        for (Match match : completedMatches) {
            if (processed >= 4) break;

            // Skip if already has scorer data
            List<?> existingScorers = matchGoalScorerRepository.findByMatchOrderByMinuteAsc(match);
            if (!existingScorers.isEmpty()) {
                continue;
            }

            processed++;

            // Skip 0-0 draws — no goals to fetch
            if (match.getTeam1Score() != null && match.getTeam2Score() != null &&
                match.getTeam1Score() == 0 && match.getTeam2Score() == 0) {
                log.info("   Skipping {} vs {} (0-0 draw)", match.getTeam1().getName(), match.getTeam2().getName());
                continue;
            }

            List<Map<String, Object>> scorers = apiFootballService.fetchGoalScorersFromApi(match);
            if (!scorers.isEmpty()) {
                apiFootballService.saveGoalScorers(match, scorers);
                fetched++;
                log.info("   ✅ Saved {} scorer(s) for {} vs {}",
                        scorers.size(), match.getTeam1().getName(), match.getTeam2().getName());
            } else {
                log.warn("   ⚠️ No scorer data from API for {} vs {}",
                        match.getTeam1().getName(), match.getTeam2().getName());
            }
        }

        log.info("⚽ Goal Scorer Scheduler complete: {}/{} matches updated.", fetched, processed);
    }
}
