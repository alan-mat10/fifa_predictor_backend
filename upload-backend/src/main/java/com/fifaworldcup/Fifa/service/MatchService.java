package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.GroupStandingsResponse;
import com.fifaworldcup.Fifa.dto.MatchResponse;
import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.model.Team;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import com.fifaworldcup.Fifa.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;
    private final TeamRepository teamRepository;

    public List<MatchResponse> getAllMatches() {
        return matchRepository.findAllByOrderByMatchDateTimeAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MatchResponse> getMatchesByStage(Match.Stage stage) {
        return matchRepository.findByStage(stage).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MatchResponse> getMatchesByGroup(String group) {
        return matchRepository.findByGroupOrderByMatchDateTimeAsc(group).stream()
                .map(this::toResponse)
                .toList();
    }

    public List<MatchResponse> getTodaysMatches() {
        // Convert "today in IST" back to ET range for the DB query
        LocalDateTime startOfDayIST = LocalDateTime.now(IST).toLocalDate().atStartOfDay();
        LocalDateTime endOfDayIST = startOfDayIST.plusDays(1);
        // Convert IST boundaries to ET for querying against stored ET times
        LocalDateTime startET = startOfDayIST.atZone(IST).withZoneSameInstant(ET).toLocalDateTime();
        LocalDateTime endET = endOfDayIST.atZone(IST).withZoneSameInstant(ET).toLocalDateTime();
        return matchRepository.findByMatchDateTimeBetween(startET, endET).stream()
                .map(this::toResponse)
                .toList();
    }

    public MatchResponse getMatchById(Long id) {
        Match match = matchRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Match not found"));
        return toResponse(match);
    }

    public Map<String, List<GroupStandingsResponse>> computeAllStandings() {
        List<String> groups = List.of("A", "B", "C", "D", "E", "F", "G", "H", "I", "J", "K", "L");
        Map<String, List<GroupStandingsResponse>> allStandings = new LinkedHashMap<>();
        for (String group : groups) {
            allStandings.put(group, computeGroupStandings(group));
        }
        return allStandings;
    }

    public List<GroupStandingsResponse> computeGroupStandings(String group) {
        List<Team> teams = teamRepository.findByGroupOrderByNameAsc(group);
        List<Match> completedMatches = matchRepository.findByStageAndGroupAndStatus(
                Match.Stage.GROUP, group, Match.MatchStatus.COMPLETED);

        Map<Long, GroupStandingsResponse> standingsMap = new HashMap<>();
        for (Team team : teams) {
            standingsMap.put(team.getId(), GroupStandingsResponse.builder()
                    .teamName(team.getName())
                    .teamFlagUrl(team.getFlagUrl())
                    .teamId(team.getId())
                    .matchesPlayed(0).wins(0).draws(0).losses(0)
                    .goalsFor(0).goalsAgainst(0).goalDifference(0).points(0)
                    .build());
        }

        for (Match match : completedMatches) {
            if (match.getTeam1() == null || match.getTeam2() == null) continue;

            Long team1Id = match.getTeam1().getId();
            Long team2Id = match.getTeam2().getId();
            int score1 = match.getTeam1Score();
            int score2 = match.getTeam2Score();

            GroupStandingsResponse t1 = standingsMap.get(team1Id);
            GroupStandingsResponse t2 = standingsMap.get(team2Id);
            if (t1 == null || t2 == null) continue;

            t1.setMatchesPlayed(t1.getMatchesPlayed() + 1);
            t2.setMatchesPlayed(t2.getMatchesPlayed() + 1);
            t1.setGoalsFor(t1.getGoalsFor() + score1);
            t1.setGoalsAgainst(t1.getGoalsAgainst() + score2);
            t2.setGoalsFor(t2.getGoalsFor() + score2);
            t2.setGoalsAgainst(t2.getGoalsAgainst() + score1);

            if (score1 > score2) {
                t1.setWins(t1.getWins() + 1);
                t1.setPoints(t1.getPoints() + 3);
                t2.setLosses(t2.getLosses() + 1);
            } else if (score2 > score1) {
                t2.setWins(t2.getWins() + 1);
                t2.setPoints(t2.getPoints() + 3);
                t1.setLosses(t1.getLosses() + 1);
            } else {
                t1.setDraws(t1.getDraws() + 1);
                t1.setPoints(t1.getPoints() + 1);
                t2.setDraws(t2.getDraws() + 1);
                t2.setPoints(t2.getPoints() + 1);
            }
        }

        // Compute goal difference
        for (GroupStandingsResponse entry : standingsMap.values()) {
            entry.setGoalDifference(entry.getGoalsFor() - entry.getGoalsAgainst());
        }

        // Sort: points desc, then GD desc, then GF desc
        return standingsMap.values().stream()
                .sorted(Comparator.comparingInt(GroupStandingsResponse::getPoints).reversed()
                        .thenComparing(Comparator.comparingInt(GroupStandingsResponse::getGoalDifference).reversed())
                        .thenComparing(Comparator.comparingInt(GroupStandingsResponse::getGoalsFor).reversed()))
                .collect(Collectors.toList());
    }

    private static final ZoneId ET = ZoneId.of("America/New_York");
    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /**
     * Converts stored match time (ET) to IST for the response.
     */
    private LocalDateTime toIST(LocalDateTime etTime) {
        ZonedDateTime etZoned = etTime.atZone(ET);
        return etZoned.withZoneSameInstant(IST).toLocalDateTime();
    }

    private MatchResponse toResponse(Match match) {
        LocalDateTime matchTimeIST = toIST(match.getMatchDateTime());
        return MatchResponse.builder()
                .id(match.getId())
                .team1Id(match.getTeam1() != null ? match.getTeam1().getId() : null)
                .team2Id(match.getTeam2() != null ? match.getTeam2().getId() : null)
                .team1Name(match.getTeam1() != null ? match.getTeam1().getName() : "TBD")
                .team2Name(match.getTeam2() != null ? match.getTeam2().getName() : "TBD")
                .team1Flag(match.getTeam1() != null ? match.getTeam1().getFlagUrl() : null)
                .team2Flag(match.getTeam2() != null ? match.getTeam2().getFlagUrl() : null)
                .matchDateTime(matchTimeIST)
                .venue(match.getVenue())
                .stage(match.getStage().name())
                .group(match.getGroup())
                .team1Score(match.getTeam1Score())
                .team2Score(match.getTeam2Score())
                .team1PenaltyScore(match.getTeam1PenaltyScore())
                .team2PenaltyScore(match.getTeam2PenaltyScore())
                .status(match.getStatus().name())
                .predictionLocked(matchTimeIST.isBefore(LocalDateTime.now(IST)))
                .build();
    }
}
