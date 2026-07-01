package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.model.MatchGoalScorer;
import com.fifaworldcup.Fifa.repository.MatchGoalScorerRepository;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service to fetch goal scorer data from api-football.com (API-Sports).
 * Free tier: 100 requests/day. We only use this for goal events.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ApiFootballService {

    private final MatchRepository matchRepository;
    private final MatchGoalScorerRepository matchGoalScorerRepository;

    @Value("${app.api-football.key:}")
    private String apiKey;

    private static final String BASE_URL = "https://v3.football.api-sports.io";
    private static final int WORLD_CUP_LEAGUE_ID = 1;
    private static final int WORLD_CUP_SEASON = 2026;

    /**
     * Fetches goal scorers from API-Football for a specific match.
     * Returns a list of scorer info maps (playerName, minute, ownGoal, penalty).
     * Does NOT save to DB — caller decides what to do with the data.
     */
    public List<Map<String, Object>> fetchGoalScorersFromApi(Match match) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("API-Football key not configured. Cannot fetch goal scorers.");
            return List.of();
        }

        try {
            // Find the fixture ID from API-Football by matching teams and date
            Integer fixtureId = findFixtureId(match);
            if (fixtureId == null) {
                log.warn("Could not find API-Football fixture for {} vs {}",
                        match.getTeam1().getName(), match.getTeam2().getName());
                return List.of();
            }

            // Fetch events for this fixture
            Map<String, Object> response = callApi("/fixtures/events?fixture=" + fixtureId + "&type=Goal");
            if (response == null || !response.containsKey("response")) {
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> events = (List<Map<String, Object>>) response.get("response");
            List<Map<String, Object>> scorers = new ArrayList<>();

            for (Map<String, Object> event : events) {
                @SuppressWarnings("unchecked")
                Map<String, Object> time = (Map<String, Object>) event.get("time");
                @SuppressWarnings("unchecked")
                Map<String, Object> player = (Map<String, Object>) event.get("player");
                String detail = (String) event.get("detail");

                int minute = time != null && time.get("elapsed") != null
                        ? ((Number) time.get("elapsed")).intValue() : 0;
                String playerName = player != null ? (String) player.get("name") : "Unknown";
                boolean isOwnGoal = "Own Goal".equalsIgnoreCase(detail);
                boolean isPenalty = "Penalty".equalsIgnoreCase(detail);

                Map<String, Object> scorer = new LinkedHashMap<>();
                scorer.put("playerName", playerName);
                scorer.put("minute", minute);
                scorer.put("ownGoal", isOwnGoal);
                scorer.put("penalty", isPenalty);
                scorers.add(scorer);
            }

            log.info("Fetched {} goal(s) from API-Football for {} vs {}",
                    scorers.size(), match.getTeam1().getName(), match.getTeam2().getName());
            return scorers;

        } catch (Exception e) {
            log.error("Error fetching from API-Football: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Saves goal scorers to the match_goal_scorers table.
     * Clears existing scorers for the match first (allows re-saving).
     */
    public void saveGoalScorers(Match match, List<Map<String, Object>> scorers) {
        // Delete existing scorers for this match
        matchGoalScorerRepository.deleteByMatch(match);

        boolean isFirst = true;
        for (Map<String, Object> scorer : scorers) {
            matchGoalScorerRepository.save(MatchGoalScorer.builder()
                    .match(match)
                    .playerName((String) scorer.get("playerName"))
                    .minute(scorer.get("minute") != null ? ((Number) scorer.get("minute")).intValue() : 0)
                    .firstGoal(isFirst)
                    .ownGoal(Boolean.TRUE.equals(scorer.get("ownGoal")))
                    .penalty(Boolean.TRUE.equals(scorer.get("penalty")))
                    .build());
            isFirst = false;
        }
    }

    /**
     * Finds the API-Football fixture ID for a given match by searching
     * World Cup fixtures by date range.
     */
    private Integer findFixtureId(Match match) {
        String date = match.getMatchDateTime().toLocalDate().toString(); // yyyy-MM-dd

        Map<String, Object> response = callApi(
                "/fixtures?league=" + WORLD_CUP_LEAGUE_ID +
                "&season=" + WORLD_CUP_SEASON +
                "&date=" + date);

        if (response == null || !response.containsKey("response")) return null;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> fixtures = (List<Map<String, Object>>) response.get("response");

        String team1 = match.getTeam1().getName().toLowerCase();
        String team2 = match.getTeam2().getName().toLowerCase();

        for (Map<String, Object> fixture : fixtures) {
            @SuppressWarnings("unchecked")
            Map<String, Object> teams = (Map<String, Object>) fixture.get("teams");
            @SuppressWarnings("unchecked")
            Map<String, Object> home = (Map<String, Object>) teams.get("home");
            @SuppressWarnings("unchecked")
            Map<String, Object> away = (Map<String, Object>) teams.get("away");

            String homeName = ((String) home.get("name")).toLowerCase();
            String awayName = ((String) away.get("name")).toLowerCase();

            if ((fuzzyMatch(team1, homeName) && fuzzyMatch(team2, awayName)) ||
                (fuzzyMatch(team1, awayName) && fuzzyMatch(team2, homeName))) {
                @SuppressWarnings("unchecked")
                Map<String, Object> fixtureInfo = (Map<String, Object>) fixture.get("fixture");
                return ((Number) fixtureInfo.get("id")).intValue();
            }
        }
        return null;
    }

    private boolean fuzzyMatch(String name1, String name2) {
        if (name1.equals(name2)) return true;
        if (name1.contains(name2) || name2.contains(name1)) return true;
        // Common aliases
        Map<String, List<String>> aliases = Map.ofEntries(
                Map.entry("south korea", List.of("korea republic", "korea")),
                Map.entry("usa", List.of("united states", "us")),
                Map.entry("türkiye", List.of("turkey", "turkiye")),
                Map.entry("ivory coast", List.of("côte d'ivoire", "cote d'ivoire", "cote divoire")),
                Map.entry("dr congo", List.of("congo dr", "dem. republic congo", "democratic republic of the congo", "dr congo")),
                Map.entry("bosnia and herzegovina", List.of("bosnia", "bosnia-herzegovina")),
                Map.entry("curaçao", List.of("curacao")),
                Map.entry("cape verde", List.of("cabo verde"))
        );
        for (var entry : aliases.entrySet()) {
            if ((entry.getKey().equals(name1) || entry.getValue().contains(name1)) &&
                (entry.getKey().equals(name2) || entry.getValue().contains(name2))) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callApi(String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("x-apisports-key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                BASE_URL + endpoint,
                HttpMethod.GET,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );
        return response.getBody();
    }
}
