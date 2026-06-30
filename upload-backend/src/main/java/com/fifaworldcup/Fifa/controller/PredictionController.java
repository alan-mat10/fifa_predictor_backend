package com.fifaworldcup.Fifa.controller;

import com.fifaworldcup.Fifa.dto.*;
import com.fifaworldcup.Fifa.service.PredictionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final PredictionService predictionService;
    private final com.fifaworldcup.Fifa.service.TournamentSettingsService tournamentSettingsService;

    // === Score Predictions ===

    @PostMapping
    public ResponseEntity<PredictionResponse> makePrediction(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PredictionRequest request) {
        return ResponseEntity.ok(predictionService.makePrediction(userDetails.getUsername(), request));
    }

    @GetMapping("/my")
    public ResponseEntity<List<PredictionResponse>> getMyPredictions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(predictionService.getMyPredictions(userDetails.getUsername()));
    }

    @GetMapping("/match/{matchId}")
    public ResponseEntity<List<PredictionResponse>> getPredictionsForMatch(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(predictionService.getPredictionsForMatch(matchId, userDetails.getUsername()));
    }

    // === Goal Scorer Predictions ===

    @PostMapping("/goal-scorers")
    public ResponseEntity<List<GoalScorerPredictionResponse>> predictGoalScorers(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody GoalScorerPredictionRequest request) {
        return ResponseEntity.ok(predictionService.predictGoalScorers(userDetails.getUsername(), request));
    }

    @GetMapping("/goal-scorers/match/{matchId}")
    public ResponseEntity<List<GoalScorerPredictionResponse>> getMyGoalScorerPredictions(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(predictionService.getGoalScorerPredictions(userDetails.getUsername(), matchId));
    }

    @GetMapping("/goal-scorers/match/{matchId}/all")
    public ResponseEntity<List<GoalScorerPredictionResponse>> getAllGoalScorerPredictions(
            @PathVariable Long matchId,
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(predictionService.getAllGoalScorerPredictionsForMatch(matchId, userDetails.getUsername()));
    }

    // === Special Predictions (Tournament) ===

    @PostMapping("/top-scorer")
    public ResponseEntity<String> predictTopScorer(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SpecialPredictionRequest request) {
        if (tournamentSettingsService.areTournamentPredictionsLocked()) {
            return ResponseEntity.badRequest().body("Tournament predictions are locked.");
        }
        predictionService.predictTopScorer(userDetails.getUsername(), request);
        return ResponseEntity.ok("Top scorer prediction saved");
    }

    @PostMapping("/golden-ball")
    public ResponseEntity<String> predictGoldenBall(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SpecialPredictionRequest request) {
        if (tournamentSettingsService.areTournamentPredictionsLocked()) {
            return ResponseEntity.badRequest().body("Tournament predictions are locked.");
        }
        predictionService.predictGoldenBall(userDetails.getUsername(), request);
        return ResponseEntity.ok("Golden Ball prediction saved");
    }

    @PostMapping("/golden-glove")
    public ResponseEntity<String> predictGoldenGlove(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody SpecialPredictionRequest request) {
        if (tournamentSettingsService.areTournamentPredictionsLocked()) {
            return ResponseEntity.badRequest().body("Tournament predictions are locked.");
        }
        predictionService.predictGoldenGlove(userDetails.getUsername(), request);
        return ResponseEntity.ok("Golden Glove prediction saved");
    }

    @GetMapping("/tournament")
    public ResponseEntity<java.util.Map<String, Object>> getMyTournamentPredictions(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(predictionService.getMyTournamentPredictions(userDetails.getUsername()));
    }

    @PostMapping("/motm")
    public ResponseEntity<String> predictMotm(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long matchId,
            @RequestParam Long playerId) {
        predictionService.predictMotm(userDetails.getUsername(), matchId, playerId);
        return ResponseEntity.ok("Man of the Match prediction saved");
    }

    @GetMapping("/motm/match/{matchId}")
    public ResponseEntity<List<java.util.Map<String, Object>>> getMotmPredictionsForMatch(
            @PathVariable Long matchId) {
        return ResponseEntity.ok(predictionService.getMotmPredictionsForMatch(matchId));
    }

    @PostMapping("/world-cup-winner")
    public ResponseEntity<String> predictWorldCupWinner(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam Long teamId) {
        if (tournamentSettingsService.areTournamentPredictionsLocked()) {
            return ResponseEntity.badRequest().body("Tournament predictions are locked.");
        }
        predictionService.predictWorldCupWinner(userDetails.getUsername(), teamId);
        return ResponseEntity.ok("World Cup Winner prediction saved");
    }

    @GetMapping("/tournament-lock-status")
    public ResponseEntity<java.util.Map<String, Object>> getTournamentLockStatus() {
        var settings = tournamentSettingsService.getSettings();
        return ResponseEntity.ok(java.util.Map.of(
                "locked", tournamentSettingsService.areTournamentPredictionsLocked(),
                "lockTime", settings.getTournamentPredictionLockTime() != null ? settings.getTournamentPredictionLockTime().toString() : ""
        ));
    }
}
