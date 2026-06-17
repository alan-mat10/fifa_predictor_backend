package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.model.GoalScorerPrediction;
import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.model.MatchGoalScorer;
import com.fifaworldcup.Fifa.model.User;
import com.fifaworldcup.Fifa.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Service to fetch match results from football-data.org API
 * and automatically update scores and goal scorers.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FootballDataService {

    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;
    private final GoalScorerPredictionRepository goalScorerPredictionRepository;
    private final MatchGoalScorerRepository matchGoalScorerRepository;
    private final UserRepository userRepository;

    @Value("${app.football-api.token:}")
    private String apiToken;

    private static final String BASE_URL = "https://api.football-data.org/v4";
    private static final String WC_COMPETITION_CODE = "WC";

    private static final int MATCH_WINNER_POINTS = 1;       // Correct result (win/draw)
    private static final int EXACT_SCORE_POINTS = 2;        // Exact score bonus (on top of match winner = +3 total)
    private static final int GOAL_SCORER_POINTS = 2;        // Per correct goal scorer

    /**
     * Fetches results for a specific match from the API and updates the database.
     * Called by the scheduler after a match's expected end time.
     */
    @Transactional
    public void fetchAndUpdateMatchResult(Long matchId) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

        if (match.getStatus() == Match.MatchStatus.COMPLETED) {
            log.info("Match {} already completed, skipping.", matchId);
            return;
        }

        if (apiToken == null || apiToken.isBlank()) {
            log.warn("Football API token not configured. Skipping auto-update for match {}.", matchId);
            return;
        }

        try {
            // Fetch today's finished World Cup matches from football-data.org
            Map<String, Object> apiResponse = callFootballApi("/competitions/" + WC_COMPETITION_CODE + "/matches?status=FINISHED");

            if (apiResponse == null || !apiResponse.containsKey("matches")) {
                log.warn("No match data returned from API for match {}", matchId);
                return;
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> apiMatches = (List<Map<String, Object>>) apiResponse.get("matches");

            // Find the matching fixture by teams
            String team1Name = match.getTeam1().getName();
            String team2Name = match.getTeam2().getName();

            log.info("Looking for match: '{}' vs '{}' among {} API matches", team1Name, team2Name, apiMatches.size());

            for (Map<String, Object> apiMatch : apiMatches) {
                @SuppressWarnings("unchecked")
                Map<String, Object> homeTeam = (Map<String, Object>) apiMatch.get("homeTeam");
                @SuppressWarnings("unchecked")
                Map<String, Object> awayTeam = (Map<String, Object>) apiMatch.get("awayTeam");

                String apiHome = (String) homeTeam.get("name");
                String apiAway = (String) awayTeam.get("name");
                String apiHomeShort = (String) homeTeam.get("shortName");
                String apiAwayShort = (String) awayTeam.get("shortName");

                log.debug("  API match: '{}' ({}) vs '{}' ({})", apiHome, apiHomeShort, apiAway, apiAwayShort);

                boolean homeMatch = teamsMatch(team1Name, apiHome) || teamsMatch(team1Name, apiHomeShort);
                boolean awayMatch = teamsMatch(team2Name, apiAway) || teamsMatch(team2Name, apiAwayShort);

                if (homeMatch && awayMatch) {
                    // Found our match — extract score
                    @SuppressWarnings("unchecked")
                    Map<String, Object> score = (Map<String, Object>) apiMatch.get("score");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> fullTime = (Map<String, Object>) score.get("fullTime");

                    int homeScore = ((Number) fullTime.get("home")).intValue();
                    int awayScore = ((Number) fullTime.get("away")).intValue();

                    // Update match result
                    match.setTeam1Score(homeScore);
                    match.setTeam2Score(awayScore);
                    match.setStatus(Match.MatchStatus.COMPLETED);
                    matchRepository.save(match);

                    log.info("✅ Match updated: {} {} - {} {}", team1Name, homeScore, awayScore, team2Name);

                    // Calculate score prediction points
                    calculateScorePoints(match);

                    // Extract goal scorers and calculate bonus points
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> goals = (List<Map<String, Object>>) apiMatch.get("goals");
                    if (goals != null && !goals.isEmpty()) {
                        processGoalScorers(match, goals);
                    } else {
                        log.warn("   No goals data in API response for this match. Goals array: {}", goals);
                    }

                    return;
                }
            }

            log.warn("Match {} ({} vs {}) not found in {} API matches. First 3 API matches: {}",
                    matchId, team1Name, team2Name, apiMatches.size(),
                    apiMatches.stream().limit(3).map(m -> {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> h = (Map<String, Object>) m.get("homeTeam");
                        @SuppressWarnings("unchecked")
                        Map<String, Object> a = (Map<String, Object>) m.get("awayTeam");
                        return h.get("name") + " vs " + a.get("name");
                    }).toList());

        } catch (Exception e) {
            log.error("Error fetching match result for match {}: {}", matchId, e.getMessage());
        }
    }

    private void processGoalScorers(Match match, List<Map<String, Object>> goals) {
        // Save actual goal scorers to DB
        Set<String> scorerNames = new HashSet<>();
        boolean isFirst = true;

        for (Map<String, Object> goal : goals) {
            @SuppressWarnings("unchecked")
            Map<String, Object> scorer = (Map<String, Object>) goal.get("scorer");
            if (scorer != null) {
                String name = (String) scorer.get("name");
                int minute = goal.get("minute") != null ? ((Number) goal.get("minute")).intValue() : 0;
                String type = goal.get("type") != null ? (String) goal.get("type") : "REGULAR";

                // Save to DB
                matchGoalScorerRepository.save(MatchGoalScorer.builder()
                        .match(match)
                        .playerName(name)
                        .minute(minute)
                        .firstGoal(isFirst)
                        .ownGoal("OWN".equalsIgnoreCase(type))
                        .penalty("PENALTY".equalsIgnoreCase(type))
                        .build());

                if (!"OWN".equalsIgnoreCase(type)) {
                    scorerNames.add(name.toLowerCase());
                }
                isFirst = false;
            }
        }

        log.info("   Saved {} goal(s). Scorers: {}", goals.size(), scorerNames);

        // Count actual goals per player name (for multi-goal multiplier)
        java.util.Map<String, Integer> scorerGoalCounts = new java.util.HashMap<>();
        for (String name : scorerNames) {
            scorerGoalCounts.merge(name, 1, Integer::sum);
        }
        // Re-count properly from goals list (scorerNames is a Set, loses duplicates)
        java.util.Map<String, Integer> actualGoalsByName = new java.util.HashMap<>();
        for (Map<String, Object> goal : goals) {
            @SuppressWarnings("unchecked")
            Map<String, Object> scorer2 = (Map<String, Object>) goal.get("scorer");
            if (scorer2 != null) {
                String gName = (String) scorer2.get("name");
                String gType = goal.get("type") != null ? (String) goal.get("type") : "REGULAR";
                if (!"OWN".equalsIgnoreCase(gType)) {
                    actualGoalsByName.merge(gName.toLowerCase(), 1, Integer::sum);
                }
            }
        }

        // Now check goal scorer predictions
        List<GoalScorerPrediction> predictions = goalScorerPredictionRepository.findByMatchAndScored(match, false);

        for (GoalScorerPrediction prediction : predictions) {
            int points = 0;
            String predictedPlayerName = prediction.getPlayer().getName().toLowerCase();

            // Check if predicted player actually scored (fuzzy match) and get goal count
            int actualGoals = 0;
            for (Map.Entry<String, Integer> entry : actualGoalsByName.entrySet()) {
                if (namesMatch(predictedPlayerName, entry.getKey())) {
                    actualGoals = entry.getValue();
                    break;
                }
            }

            if (actualGoals > 0) {
                int predictedGoals = prediction.getPredictedGoals();
                int goalsToReward = Math.min(actualGoals, predictedGoals);
                points = GOAL_SCORER_POINTS * goalsToReward;
            }

            prediction.setPointsEarned(points);
            prediction.setScored(true);
            goalScorerPredictionRepository.save(prediction);

            if (points > 0) {
                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + points);
                userRepository.save(user);
                log.info("   +{} pts to {} (predicted: {})", points, user.getUsername(), prediction.getPlayer().getName());
            }
        }
    }

    private void calculateScorePoints(Match match) {
        var predictions = predictionRepository.findByMatchAndScored(match, false);

        for (var prediction : predictions) {
            int actualTeam1 = match.getTeam1Score();
            int actualTeam2 = match.getTeam2Score();
            int predictedTeam1 = prediction.getPredictedTeam1Score();
            int predictedTeam2 = prediction.getPredictedTeam2Score();

            int points = 0;

            // Check result (win/draw)
            String actualResult = getResult(actualTeam1, actualTeam2);
            String predictedResult = getResult(predictedTeam1, predictedTeam2);

            if (actualResult.equals(predictedResult)) {
                points += MATCH_WINNER_POINTS;  // +1 for correct result

                // Exact score bonus
                if (predictedTeam1 == actualTeam1 && predictedTeam2 == actualTeam2) {
                    points += EXACT_SCORE_POINTS;  // +2 for exact score (total +3)
                }
            }

            prediction.setPointsEarned(points);
            prediction.setScored(true);
            predictionRepository.save(prediction);

            User user = prediction.getUser();
            user.setTotalPoints(user.getTotalPoints() + points);
            userRepository.save(user);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callFootballApi(String endpoint) {
        RestTemplate restTemplate = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Auth-Token", apiToken);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                BASE_URL + endpoint,
                HttpMethod.GET,
                entity,
                (Class<Map<String, Object>>) (Class<?>) Map.class
        );

        return response.getBody();
    }

    /**
     * Fuzzy team name matching to handle variations
     * (e.g., "South Korea" vs "Korea Republic", "Türkiye" vs "Turkey")
     */
    private boolean teamsMatch(String ourName, String apiName) {
        if (ourName == null || apiName == null) return false;
        String a = ourName.toLowerCase().trim();
        String b = apiName.toLowerCase().trim();
        if (a.equals(b)) return true;
        if (a.contains(b) || b.contains(a)) return true;

        // Common variations
        Map<String, List<String>> aliases = Map.ofEntries(
                Map.entry("south korea", List.of("korea republic", "korea")),
                Map.entry("usa", List.of("united states", "us")),
                Map.entry("türkiye", List.of("turkey", "turkiye")),
                Map.entry("ivory coast", List.of("côte d'ivoire", "cote d'ivoire")),
                Map.entry("bosnia and herzegovina", List.of("bosnia", "bosnia-herzegovina")),
                Map.entry("dr congo", List.of("congo dr", "dem. republic congo", "democratic republic of the congo")),
                Map.entry("curaçao", List.of("curacao")),
                Map.entry("cape verde", List.of("cabo verde"))
        );

        List<String> ourAliases = aliases.getOrDefault(a, List.of());
        if (ourAliases.stream().anyMatch(alias -> alias.equals(b) || b.contains(alias))) return true;

        // Reverse check
        for (var entry : aliases.entrySet()) {
            if (entry.getValue().contains(a) && (entry.getKey().equals(b) || b.contains(entry.getKey()))) {
                return true;
            }
        }

        return false;
    }

    /**
     * Fuzzy player name matching (handles partial matches, accents, etc.)
     */
    private boolean namesMatch(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        String a = stripAccents(name1.toLowerCase().trim());
        String b = stripAccents(name2.toLowerCase().trim());
        if (a.equals(b)) return true;
        // Check if last name matches
        String[] partsA = a.split("\\s+");
        String[] partsB = b.split("\\s+");
        String lastA = partsA[partsA.length - 1];
        String lastB = partsB[partsB.length - 1];
        return lastA.equals(lastB) && lastA.length() > 3;
    }

    private String stripAccents(String input) {
        return java.text.Normalizer.normalize(input, java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    }

    private String getResult(int score1, int score2) {
        if (score1 > score2) return "WIN1";
        if (score1 < score2) return "WIN2";
        return "DRAW";
    }
}
