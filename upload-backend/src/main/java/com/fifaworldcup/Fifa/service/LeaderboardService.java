package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.LeaderboardEntry;
import com.fifaworldcup.Fifa.model.*;
import com.fifaworldcup.Fifa.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;
    private final GoalScorerPredictionRepository goalScorerPredictionRepository;
    private final MotmPredictionRepository motmPredictionRepository;
    private final TopScorerPredictionRepository topScorerPredictionRepository;
    private final GoldenBallPredictionRepository goldenBallPredictionRepository;
    private final GoldenGlovePredictionRepository goldenGlovePredictionRepository;
    private final WorldCupWinnerPredictionRepository worldCupWinnerPredictionRepository;

    public List<LeaderboardEntry> getLeaderboard() {
        List<User> users = userRepository.findAllByOrderByTotalPointsDesc();
        List<LeaderboardEntry> leaderboard = new ArrayList<>();

        int rank = 1;
        for (User user : users) {
            // Exclude admin from leaderboard
            if (user.getRole() == User.Role.ADMIN) continue;

            List<Prediction> predictions = predictionRepository.findByUser(user);
            int correctScores = (int) predictions.stream()
                    .filter(p -> p.getPointsEarned() == 3)  // exact score = 1 (result) + 2 (exact) = 3
                    .count();
            int correctResults = (int) predictions.stream()
                    .filter(p -> p.getPointsEarned() == 1)  // correct result only = 1
                    .count();

            leaderboard.add(new LeaderboardEntry(
                    rank++,
                    user.getUsername(),
                    user.getTotalPoints(),
                    correctScores,
                    correctResults
            ));
        }
        return leaderboard;
    }

    public Map<String, Object> getPointsBreakdown(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Map<String, Object> breakdown = new LinkedHashMap<>();
        breakdown.put("username", user.getUsername());
        breakdown.put("totalPoints", user.getTotalPoints());

        // Match predictions breakdown
        List<Prediction> predictions = predictionRepository.findByUser(user);
        int matchResultPoints = 0;
        int exactScorePoints = 0;
        int matchResultCount = 0;
        int exactScoreCount = 0;
        List<Map<String, Object>> matchDetails = new ArrayList<>();

        for (Prediction p : predictions) {
            if (p.getPointsEarned() > 0) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("match", p.getMatch().getTeam1().getName() + " vs " + p.getMatch().getTeam2().getName());
                detail.put("predicted", p.getPredictedTeam1Score() + "-" + p.getPredictedTeam2Score());
                detail.put("actual", p.getMatch().getTeam1Score() + "-" + p.getMatch().getTeam2Score());
                detail.put("points", p.getPointsEarned());
                if (p.getPointsEarned() == 3) {
                    detail.put("type", "Exact Score (+1 Result +2 Exact)");
                    // Exact score includes 1 for result + 2 for exact
                    matchResultPoints += 1;
                    matchResultCount++;
                    exactScorePoints += 2;
                    exactScoreCount++;
                } else {
                    detail.put("type", "Correct Result");
                    matchResultPoints += p.getPointsEarned();
                    matchResultCount++;
                }
                matchDetails.add(detail);
            }
        }

        // Goal scorer predictions breakdown
        List<GoalScorerPrediction> gsPredictions = goalScorerPredictionRepository.findByUser(user);
        int goalScorerPoints = 0;
        int goalScorerCorrectCount = 0;
        int goalScorerWrongCount = 0;
        List<Map<String, Object>> gsDetails = new ArrayList<>();

        for (GoalScorerPrediction gs : gsPredictions) {
            if (gs.isScored()) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("match", gs.getMatch().getTeam1().getName() + " vs " + gs.getMatch().getTeam2().getName());
                detail.put("player", gs.getPlayer().getName());
                detail.put("predictedGoals", gs.getPredictedGoals());
                detail.put("points", gs.getPointsEarned());
                detail.put("correct", gs.getPointsEarned() > 0);
                gsDetails.add(detail);
                goalScorerPoints += gs.getPointsEarned();
                if (gs.getPointsEarned() > 0) {
                    goalScorerCorrectCount++;
                } else if (gs.getPointsEarned() < 0) {
                    goalScorerWrongCount++;
                }
            }
        }

        // MOTM predictions breakdown
        List<MotmPrediction> motmPredictions = motmPredictionRepository.findByUser(user);
        int motmPoints = 0;
        int motmCount = 0;
        List<Map<String, Object>> motmDetails = new ArrayList<>();

        for (MotmPrediction motm : motmPredictions) {
            if (motm.getPointsEarned() > 0) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("match", motm.getMatch().getTeam1().getName() + " vs " + motm.getMatch().getTeam2().getName());
                detail.put("player", motm.getPlayer().getName());
                detail.put("points", motm.getPointsEarned());
                motmDetails.add(detail);
                motmPoints += motm.getPointsEarned();
                motmCount++;
            }
        }

        // Tournament predictions
        int tournamentPoints = 0;
        List<Map<String, Object>> tournamentDetails = new ArrayList<>();

        topScorerPredictionRepository.findByUser(user).ifPresent(p -> {
            if (p.getPointsEarned() > 0) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("type", "Golden Boot");
                detail.put("prediction", p.getPlayerName());
                detail.put("points", p.getPointsEarned());
                tournamentDetails.add(detail);
            }
        });

        goldenBallPredictionRepository.findByUser(user).ifPresent(p -> {
            if (p.getPointsEarned() > 0) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("type", "Golden Ball");
                detail.put("prediction", p.getPlayerName());
                detail.put("points", p.getPointsEarned());
                tournamentDetails.add(detail);
            }
        });

        goldenGlovePredictionRepository.findByUser(user).ifPresent(p -> {
            if (p.getPointsEarned() > 0) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("type", "Golden Glove");
                detail.put("prediction", p.getPlayerName());
                detail.put("points", p.getPointsEarned());
                tournamentDetails.add(detail);
            }
        });

        worldCupWinnerPredictionRepository.findByUser(user).ifPresent(p -> {
            if (p.getPointsEarned() > 0) {
                Map<String, Object> detail = new LinkedHashMap<>();
                detail.put("type", "World Cup Winner");
                detail.put("prediction", p.getTeam().getName());
                detail.put("points", p.getPointsEarned());
                tournamentDetails.add(detail);
            }
        });

        for (Map<String, Object> td : tournamentDetails) {
            tournamentPoints += (int) td.get("points");
        }

        // Summary
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("matchResults", Map.of("points", matchResultPoints, "count", matchResultCount));
        summary.put("exactScores", Map.of("points", exactScorePoints, "count", exactScoreCount));
        summary.put("goalScorers", Map.of("points", goalScorerPoints, "correct", goalScorerCorrectCount, "wrong", goalScorerWrongCount));
        summary.put("motm", Map.of("points", motmPoints, "count", motmCount));
        summary.put("tournament", Map.of("points", tournamentPoints, "count", tournamentDetails.size()));

        breakdown.put("summary", summary);
        breakdown.put("matchDetails", matchDetails);
        breakdown.put("goalScorerDetails", gsDetails);
        breakdown.put("motmDetails", motmDetails);
        breakdown.put("tournamentDetails", tournamentDetails);

        return breakdown;
    }
}
