package com.fifaworldcup.Fifa.config;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import com.fifaworldcup.Fifa.service.FootballDataService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler that checks for matches that should have ended
 * and fetches their results from the football API.
 *
 * Logic: A match is ~2 hours long. We check every 15 minutes for
 * matches whose kickoff was 2+ hours ago and are still UPCOMING.
 * This means we only call the API when a match is expected to be finished.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MatchResultScheduler {

    private final MatchRepository matchRepository;
    private final FootballDataService footballDataService;

    /**
     * Runs every 15 minutes. Finds matches that kicked off 2+ hours ago
     * but haven't been marked as COMPLETED yet, and fetches their results.
     */
    @Scheduled(fixedRate = 900000)  // 15 minutes in milliseconds
    public void checkForFinishedMatches() {
        // A match lasts ~90 min + halftime + injury time ≈ 2 hours
        // Convert current IST time to ET for comparison with stored ET match times
        LocalDateTime nowET = LocalDateTime.now(java.time.ZoneId.of("America/New_York"));
        LocalDateTime twoHoursAgoET = nowET.minusHours(2);

        // Find matches that kicked off between 2 and 24 hours ago and are still not COMPLETED
        // 24h window ensures missed matches get retried (e.g., after API outage or server restart)
        List<Match> matchesToCheck = matchRepository
                .findByMatchDateTimeBetween(twoHoursAgoET.minusHours(22), twoHoursAgoET)
                .stream()
                .filter(m -> m.getStatus() != Match.MatchStatus.COMPLETED)
                .toList();

        if (matchesToCheck.isEmpty()) return;

        log.info("🔍 Checking {} match(es) that should be finished...", matchesToCheck.size());

        for (Match match : matchesToCheck) {
            log.info("   Fetching result: {} vs {} (kicked off {})",
                    match.getTeam1() != null ? match.getTeam1().getName() : "TBD",
                    match.getTeam2() != null ? match.getTeam2().getName() : "TBD",
                    match.getMatchDateTime());
            footballDataService.fetchAndUpdateMatchResult(match.getId());
        }
    }
}
