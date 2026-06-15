package com.fifaworldcup.Fifa.controller;

import com.fifaworldcup.Fifa.dto.InviteCodeResponse;
import com.fifaworldcup.Fifa.dto.MatchGoalScorersRequest;
import com.fifaworldcup.Fifa.dto.MatchResultRequest;
import com.fifaworldcup.Fifa.model.*;
import com.fifaworldcup.Fifa.repository.GoldenBallPredictionRepository;
import com.fifaworldcup.Fifa.repository.GoldenGlovePredictionRepository;
import com.fifaworldcup.Fifa.repository.InviteCodeRepository;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import com.fifaworldcup.Fifa.repository.TopScorerPredictionRepository;
import com.fifaworldcup.Fifa.repository.UserRepository;
import com.fifaworldcup.Fifa.repository.WorldCupWinnerPredictionRepository;
import com.fifaworldcup.Fifa.service.AdminService;
import com.fifaworldcup.Fifa.service.FootballDataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;
    private final FootballDataService footballDataService;
    private final MatchRepository matchRepository;
    private final InviteCodeRepository inviteCodeRepository;
    private final TopScorerPredictionRepository topScorerPredictionRepository;
    private final GoldenBallPredictionRepository goldenBallPredictionRepository;
    private final GoldenGlovePredictionRepository goldenGlovePredictionRepository;
    private final WorldCupWinnerPredictionRepository worldCupWinnerPredictionRepository;
    private final UserRepository userRepository;

    private static final int TOP_SCORER_BONUS = 4;
    private static final int GOLDEN_BALL_BONUS = 4;
    private static final int GOLDEN_GLOVE_BONUS = 4;
    private static final int WORLD_CUP_WINNER_BONUS = 5;

    @PostMapping("/pull-results")
    public ResponseEntity<String> pullResultsFromAPI() {
        // Find all matches that should have ended (kickoff 2+ hours ago) and are not completed
        // Match times are stored in ET
        LocalDateTime twoHoursAgoET = LocalDateTime.now(java.time.ZoneId.of("America/New_York")).minusHours(2);
        List<Match> pendingMatches = matchRepository.findAllByOrderByMatchDateTimeAsc().stream()
                .filter(m -> m.getStatus() != Match.MatchStatus.COMPLETED)
                .filter(m -> m.getMatchDateTime().isBefore(twoHoursAgoET))
                .toList();

        if (pendingMatches.isEmpty()) {
            return ResponseEntity.ok("No pending matches to update. All finished matches are already recorded.");
        }

        int updated = 0;
        for (Match match : pendingMatches) {
            try {
                footballDataService.fetchAndUpdateMatchResult(match.getId());
                // Re-check if it was updated
                Match refreshed = matchRepository.findById(match.getId()).orElse(match);
                if (refreshed.getStatus() == Match.MatchStatus.COMPLETED) {
                    updated++;
                }
            } catch (Exception e) {
                // Continue with next match
            }
        }

        return ResponseEntity.ok("Pulled results: " + updated + " match(es) updated out of " + pendingMatches.size() + " pending.");
    }

    @PostMapping("/pull-result/{matchId}")
    public ResponseEntity<String> pullSingleMatchResult(@PathVariable Long matchId) {
        try {
            // Force re-pull even if already completed (reset status temporarily)
            Match match = matchRepository.findById(matchId)
                    .orElseThrow(() -> new RuntimeException("Match not found"));

            Match.MatchStatus originalStatus = match.getStatus();
            if (originalStatus == Match.MatchStatus.COMPLETED) {
                // Temporarily set to UPCOMING so the service processes it
                match.setStatus(Match.MatchStatus.UPCOMING);
                match.setTeam1Score(null);
                match.setTeam2Score(null);
                matchRepository.save(match);
            }

            footballDataService.fetchAndUpdateMatchResult(matchId);

            match = matchRepository.findById(matchId).orElse(match);
            if (match.getStatus() == Match.MatchStatus.COMPLETED) {
                return ResponseEntity.ok("Match result pulled: " +
                        match.getTeam1().getName() + " " + match.getTeam1Score() +
                        " - " + match.getTeam2Score() + " " + match.getTeam2().getName());
            } else {
                // Restore original status if pull failed
                match.setStatus(originalStatus);
                matchRepository.save(match);
                return ResponseEntity.ok("Match result not available from API yet. Try again later.");
            }
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Failed: " + e.getMessage());
        }
    }

    @PostMapping("/match-result")
    public ResponseEntity<String> submitMatchResult(@Valid @RequestBody MatchResultRequest request) {
        adminService.submitMatchResult(request);
        return ResponseEntity.ok("Match result submitted and score prediction points calculated");
    }

    @PostMapping("/match-goal-scorers")
    public ResponseEntity<String> submitMatchGoalScorers(@Valid @RequestBody MatchGoalScorersRequest request) {
        adminService.submitMatchGoalScorers(request);
        return ResponseEntity.ok("Goal scorer points calculated");
    }

    @PostMapping("/submit-motm")
    public ResponseEntity<String> submitMotm(@RequestParam Long matchId, @RequestParam String playerName) {
        adminService.submitMotm(matchId, playerName);
        return ResponseEntity.ok("Man of the Match set and points awarded");
    }

    @PostMapping("/recalculate-points")
    public ResponseEntity<String> recalculateAllPoints() {
        String result = adminService.recalculateAllPoints();
        return ResponseEntity.ok(result);
    }

    @PostMapping("/award-top-scorer")
    public ResponseEntity<String> awardTopScorer(@RequestParam String playerName) {
        List<TopScorerPrediction> predictions = topScorerPredictionRepository.findAll();
        int awarded = 0;
        for (TopScorerPrediction prediction : predictions) {
            if (prediction.getPlayerName().equalsIgnoreCase(playerName) && !prediction.isScored()) {
                prediction.setPointsEarned(TOP_SCORER_BONUS);
                prediction.setScored(true);
                topScorerPredictionRepository.save(prediction);

                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + TOP_SCORER_BONUS);
                userRepository.save(user);
                awarded++;
            }
        }
        return ResponseEntity.ok("Top scorer points awarded to " + awarded + " users");
    }

    @PostMapping("/award-golden-ball")
    public ResponseEntity<String> awardGoldenBall(@RequestParam String playerName) {
        List<GoldenBallPrediction> predictions = goldenBallPredictionRepository.findAll();
        int awarded = 0;
        for (GoldenBallPrediction prediction : predictions) {
            if (prediction.getPlayerName().equalsIgnoreCase(playerName) && !prediction.isScored()) {
                prediction.setPointsEarned(GOLDEN_BALL_BONUS);
                prediction.setScored(true);
                goldenBallPredictionRepository.save(prediction);

                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + GOLDEN_BALL_BONUS);
                userRepository.save(user);
                awarded++;
            }
        }
        return ResponseEntity.ok("Golden Ball points awarded to " + awarded + " users");
    }

    @PostMapping("/award-golden-glove")
    public ResponseEntity<String> awardGoldenGlove(@RequestParam String playerName) {
        List<GoldenGlovePrediction> predictions = goldenGlovePredictionRepository.findAll();
        int awarded = 0;
        for (GoldenGlovePrediction prediction : predictions) {
            if (prediction.getPlayerName().equalsIgnoreCase(playerName) && !prediction.isScored()) {
                prediction.setPointsEarned(GOLDEN_GLOVE_BONUS);
                prediction.setScored(true);
                goldenGlovePredictionRepository.save(prediction);

                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + GOLDEN_GLOVE_BONUS);
                userRepository.save(user);
                awarded++;
            }
        }
        return ResponseEntity.ok("Golden Glove points awarded to " + awarded + " users");
    }

    @PostMapping("/award-world-cup-winner")
    public ResponseEntity<String> awardWorldCupWinner(@RequestParam Long teamId) {
        List<WorldCupWinnerPrediction> predictions = worldCupWinnerPredictionRepository.findAll();
        int awarded = 0;
        for (WorldCupWinnerPrediction prediction : predictions) {
            if (prediction.getTeam().getId().equals(teamId) && !prediction.isScored()) {
                prediction.setPointsEarned(WORLD_CUP_WINNER_BONUS);
                prediction.setScored(true);
                worldCupWinnerPredictionRepository.save(prediction);

                User user = prediction.getUser();
                user.setTotalPoints(user.getTotalPoints() + WORLD_CUP_WINNER_BONUS);
                userRepository.save(user);
                awarded++;
            }
        }
        return ResponseEntity.ok("World Cup Winner points awarded to " + awarded + " users");
    }

    // ─── Invite Code Management ─────────────────────────────

    @PostMapping("/invite-codes/generate")
    public ResponseEntity<InviteCodeResponse> generateInviteCode(@RequestParam(required = false) String label,
                                                                  @RequestParam(defaultValue = "1") int count) {
        if (count == 1) {
            InviteCode code = createInviteCode(label);
            return ResponseEntity.ok(toInviteResponse(code));
        }
        // For bulk, return the last one (use /invite-codes/generate-bulk for multiple)
        InviteCode code = createInviteCode(label);
        return ResponseEntity.ok(toInviteResponse(code));
    }

    @PostMapping("/invite-codes/generate-bulk")
    public ResponseEntity<java.util.List<InviteCodeResponse>> generateBulkInviteCodes(
            @RequestParam(defaultValue = "5") int count,
            @RequestParam(required = false) String labelPrefix) {
        java.util.List<InviteCodeResponse> codes = new java.util.ArrayList<>();
        for (int i = 1; i <= Math.min(count, 50); i++) {
            String lbl = labelPrefix != null ? labelPrefix + " #" + i : null;
            InviteCode code = createInviteCode(lbl);
            codes.add(toInviteResponse(code));
        }
        return ResponseEntity.ok(codes);
    }

    @GetMapping("/invite-codes")
    public ResponseEntity<java.util.List<InviteCodeResponse>> getAllInviteCodes() {
        return ResponseEntity.ok(
            inviteCodeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toInviteResponse)
                .toList()
        );
    }

    @DeleteMapping("/invite-codes/{id}")
    public ResponseEntity<String> deleteInviteCode(@PathVariable Long id) {
        InviteCode code = inviteCodeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Invite code not found"));
        if (code.isUsed()) {
            return ResponseEntity.badRequest().body("Cannot delete a used invite code");
        }
        inviteCodeRepository.delete(code);
        return ResponseEntity.ok("Invite code deleted");
    }

    private InviteCode createInviteCode(String label) {
        String code = generateCode();
        InviteCode inviteCode = InviteCode.builder()
                .code(code)
                .label(label)
                .build();
        return inviteCodeRepository.save(inviteCode);
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";  // no I, O, 0, 1 to avoid confusion
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 8; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    private InviteCodeResponse toInviteResponse(InviteCode code) {
        return InviteCodeResponse.builder()
                .id(code.getId())
                .code(code.getCode())
                .label(code.getLabel())
                .used(code.isUsed())
                .usedByUsername(code.getUsedByUsername())
                .createdAt(code.getCreatedAt())
                .usedAt(code.getUsedAt())
                .build();
    }
}
