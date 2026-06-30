package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.MatchResultRequest;
import com.fifaworldcup.Fifa.model.*;
import com.fifaworldcup.Fifa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final MatchRepository matchRepository;
    private final PredictionRepository predictionRepository;
    private final GoalScorerPredictionRepository goalScorerPredictionRepository;
    private final MotmPredictionRepository motmPredictionRepository;
    private final UserRepository userRepository;
    private final KnockoutAdvancementService knockoutAdvancementService;

    private static final int MATCH_WINNER_POINTS = 1;       // Correct result (win/draw)
    private static final int EXACT_SCORE_POINTS = 2;        // Exact score (bonus on top of match winner)
    private static final int GOAL_SCORER_POINTS = 2;        // Per correct goal scorer
    private static final int MOTM_POINTS = 3;               // Correct man of the match
    private static final int TOP_SCORER_POINTS = 4;         // Tournament top scorer
    private static final int GOLDEN_BALL_POINTS = 4;        // Tournament golden ball
    private static final int GOLDEN_GLOVE_POINTS = 4;       // Tournament golden glove
    private static final int WORLD_CUP_WINNER_POINTS = 5;   // Correct world cup winner

    @Transactional
    public void submitMatchResult(MatchResultRequest request) {
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        match.setTeam1Score(request.getTeam1Score());
        match.setTeam2Score(request.getTeam2Score());
        match.setStatus(Match.MatchStatus.COMPLETED);
        matchRepository.save(match);

        // Advance winner to next round if this is a knockout match
        if (match.getStage() != Match.Stage.GROUP) {
            knockoutAdvancementService.advanceWinner(match);
        }

        calculateScorePoints(match);
    }

    @Transactional
    public void submitMotm(Long matchId, String playerName) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new RuntimeException("Match not found"));

        match.setManOfTheMatch(playerName);
        matchRepository.save(match);

        // Award MOTM points
        List<MotmPrediction> predictions = motmPredictionRepository.findByMatchAndScored(match, false);
        for (MotmPrediction prediction : predictions) {
            String predictedName = prediction.getPlayer().getName().toLowerCase();
            if (namesMatch(predictedName, playerName.toLowerCase())) {
                prediction.setPointsEarned(MOTM_POINTS);
            }
            prediction.setScored(true);
            motmPredictionRepository.save(prediction);

            if (prediction.getPointsEarned() > 0) {
                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + prediction.getPointsEarned());
                userRepository.save(user);
            }
        }
    }

    @Transactional
    public void submitMatchGoalScorers(com.fifaworldcup.Fifa.dto.MatchGoalScorersRequest request) {
        Match match = matchRepository.findById(request.getMatchId())
                .orElseThrow(() -> new RuntimeException("Match not found"));

        if (match.getStatus() != Match.MatchStatus.COMPLETED) {
            throw new RuntimeException("Submit match result first before adding goal scorers");
        }

        // Get actual scorer player IDs and count occurrences (for multi-goal scoring)
        java.util.Map<Long, Integer> actualGoalCounts = new java.util.HashMap<>();
        for (Long scorerId : request.getScorerPlayerIds()) {
            actualGoalCounts.merge(scorerId, 1, Integer::sum);
        }

        List<GoalScorerPrediction> predictions = goalScorerPredictionRepository.findByMatchAndScored(match, false);

        for (GoalScorerPrediction prediction : predictions) {
            int points = 0;
            Long predictedPlayerId = prediction.getPlayer().getId();

            if (actualGoalCounts.containsKey(predictedPlayerId)) {
                int actualGoals = actualGoalCounts.get(predictedPlayerId);
                int predictedGoals = prediction.getPredictedGoals();
                // Award points multiplied by min(actual goals, predicted goals)
                int goalsToReward = Math.min(actualGoals, predictedGoals);
                points = GOAL_SCORER_POINTS * goalsToReward;
            }

            prediction.setPointsEarned(points);
            prediction.setScored(true);
            goalScorerPredictionRepository.save(prediction);

            if (points != 0) {
                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + points);
                userRepository.save(user);
            }
        }
    }

    private void calculateScorePoints(Match match) {
        List<Prediction> predictions = predictionRepository.findByMatchAndScored(match, false);

        for (Prediction prediction : predictions) {
            int points = calculatePredictionPoints(prediction, match);
            prediction.setPointsEarned(points);
            prediction.setScored(true);
            predictionRepository.save(prediction);

            User user = prediction.getUser();
            user.setTotalPoints(user.getTotalPoints() + points);
            userRepository.save(user);
        }
    }

    private int calculatePredictionPoints(Prediction prediction, Match match) {
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

        return points;
    }

    private String getResult(int score1, int score2) {
        if (score1 > score2) return "WIN1";
        if (score1 < score2) return "WIN2";
        return "DRAW";
    }

    private boolean namesMatch(String name1, String name2) {
        if (name1 == null || name2 == null) return false;
        String a = java.text.Normalizer.normalize(name1.toLowerCase().trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        String b = java.text.Normalizer.normalize(name2.toLowerCase().trim(), java.text.Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        if (a.equals(b)) return true;
        // Check last name match
        String[] partsA = a.split("\\s+");
        String[] partsB = b.split("\\s+");
        String lastA = partsA[partsA.length - 1];
        String lastB = partsB[partsB.length - 1];
        return lastA.equals(lastB) && lastA.length() > 3;
    }

    /**
     * Returns all scored predictions for a user grouped by type, for admin editing.
     */
    public java.util.Map<String, Object> getUserPredictionsForAdmin(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        java.util.Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("username", user.getUsername());
        result.put("totalPoints", user.getTotalPoints());

        // Match predictions
        List<Prediction> predictions = predictionRepository.findByUser(user);
        List<java.util.Map<String, Object>> matchPreds = new java.util.ArrayList<>();
        for (Prediction p : predictions) {
            if (p.isScored()) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", p.getId());
                m.put("match", p.getMatch().getTeam1().getName() + " vs " + p.getMatch().getTeam2().getName());
                m.put("predicted", p.getPredictedTeam1Score() + "-" + p.getPredictedTeam2Score());
                m.put("actual", p.getMatch().getTeam1Score() + "-" + p.getMatch().getTeam2Score());
                m.put("points", p.getPointsEarned());
                matchPreds.add(m);
            }
        }
        result.put("matchPredictions", matchPreds);

        // Goal scorer predictions
        List<GoalScorerPrediction> gsPreds = goalScorerPredictionRepository.findByUser(user);
        List<java.util.Map<String, Object>> gsList = new java.util.ArrayList<>();
        for (GoalScorerPrediction gs : gsPreds) {
            if (gs.isScored()) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", gs.getId());
                m.put("match", gs.getMatch().getTeam1().getName() + " vs " + gs.getMatch().getTeam2().getName());
                m.put("player", gs.getPlayer().getName());
                m.put("predictedGoals", gs.getPredictedGoals());
                m.put("points", gs.getPointsEarned());
                gsList.add(m);
            }
        }
        result.put("goalScorerPredictions", gsList);

        // MOTM predictions
        List<MotmPrediction> motmPreds = motmPredictionRepository.findByUser(user);
        List<java.util.Map<String, Object>> motmList = new java.util.ArrayList<>();
        for (MotmPrediction mp : motmPreds) {
            if (mp.isScored()) {
                java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
                m.put("id", mp.getId());
                m.put("match", mp.getMatch().getTeam1().getName() + " vs " + mp.getMatch().getTeam2().getName());
                m.put("player", mp.getPlayer().getName());
                m.put("points", mp.getPointsEarned());
                motmList.add(m);
            }
        }
        result.put("motmPredictions", motmList);

        return result;
    }

    /**
     * Updates points on a specific prediction by type and ID.
     * Also updates the user's total points accordingly.
     */
    @Transactional
    public void updatePredictionPoints(String type, Long predictionId, int newPoints) {
        switch (type) {
            case "match" -> {
                Prediction p = predictionRepository.findById(predictionId)
                        .orElseThrow(() -> new RuntimeException("Prediction not found"));
                int diff = newPoints - p.getPointsEarned();
                p.setPointsEarned(newPoints);
                predictionRepository.save(p);
                User user = p.getUser();
                user.setTotalPoints(user.getTotalPoints() + diff);
                userRepository.save(user);
            }
            case "goalScorer" -> {
                GoalScorerPrediction p = goalScorerPredictionRepository.findById(predictionId)
                        .orElseThrow(() -> new RuntimeException("Goal scorer prediction not found"));
                int diff = newPoints - p.getPointsEarned();
                p.setPointsEarned(newPoints);
                goalScorerPredictionRepository.save(p);
                User user = p.getUser();
                user.setTotalPoints(user.getTotalPoints() + diff);
                userRepository.save(user);
            }
            case "motm" -> {
                MotmPrediction p = motmPredictionRepository.findById(predictionId)
                        .orElseThrow(() -> new RuntimeException("MOTM prediction not found"));
                int diff = newPoints - p.getPointsEarned();
                p.setPointsEarned(newPoints);
                motmPredictionRepository.save(p);
                User user = p.getUser();
                user.setTotalPoints(user.getTotalPoints() + diff);
                userRepository.save(user);
            }
            default -> throw new RuntimeException("Unknown prediction type: " + type);
        }
    }

    /**
     * Recalculates ALL points from scratch for every user.
     * Resets all user points to 0, then re-scores every completed match.
     */
    @Transactional
    public String recalculateAllPoints() {
        // Reset all user points to 0
        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            user.setTotalPoints(0);
            userRepository.save(user);
        }

        // Reset all prediction scores
        List<Prediction> allPreds = predictionRepository.findAll();
        for (Prediction p : allPreds) {
            p.setPointsEarned(0);
            p.setScored(false);
            predictionRepository.save(p);
        }

        List<GoalScorerPrediction> allGs = goalScorerPredictionRepository.findAll();
        for (GoalScorerPrediction p : allGs) {
            p.setPointsEarned(0);
            p.setScored(false);
            goalScorerPredictionRepository.save(p);
        }

        List<MotmPrediction> allMotm = motmPredictionRepository.findAll();
        for (MotmPrediction p : allMotm) {
            p.setPointsEarned(0);
            p.setScored(false);
            motmPredictionRepository.save(p);
        }

        // Re-score all completed matches
        List<Match> completedMatches = matchRepository.findByStatus(Match.MatchStatus.COMPLETED);
        int matchesScored = 0;

        for (Match match : completedMatches) {
            // Score predictions
            calculateScorePoints(match);

            // Score goal scorers if we have actual scorer data
            if (match.getTeam1Score() != null) {
                List<GoalScorerPrediction> gsPreds = goalScorerPredictionRepository.findByMatchAndScored(match, false);
                // We need the actual scorers from match_goal_scorers table - handled via submitMatchGoalScorers
                // For recalculation, mark them as scored with 0 unless we have data
            }

            // Score MOTM
            if (match.getManOfTheMatch() != null && !match.getManOfTheMatch().isBlank()) {
                List<MotmPrediction> motmPreds = motmPredictionRepository.findByMatchAndScored(match, false);
                for (MotmPrediction pred : motmPreds) {
                    String predictedName = pred.getPlayer().getName().toLowerCase();
                    if (namesMatch(predictedName, match.getManOfTheMatch().toLowerCase())) {
                        pred.setPointsEarned(MOTM_POINTS);
                    }
                    pred.setScored(true);
                    motmPredictionRepository.save(pred);

                    if (pred.getPointsEarned() > 0) {
                        User user = pred.getUser();
                        user.setTotalPoints(user.getTotalPoints() + pred.getPointsEarned());
                        userRepository.save(user);
                    }
                }
            }

            matchesScored++;
        }

        return "Recalculated points for " + matchesScored + " completed matches. All user totals reset and re-scored.";
    }
}
