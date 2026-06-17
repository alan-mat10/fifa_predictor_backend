package com.fifaworldcup.Fifa.controller;

import com.fifaworldcup.Fifa.dto.LeaderboardEntry;
import com.fifaworldcup.Fifa.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    @GetMapping
    public ResponseEntity<List<LeaderboardEntry>> getLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getLeaderboard());
    }

    @GetMapping("/breakdown/{username}")
    public ResponseEntity<Map<String, Object>> getPointsBreakdown(@PathVariable String username) {
        return ResponseEntity.ok(leaderboardService.getPointsBreakdown(username));
    }
}
