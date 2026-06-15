package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.dto.MatchResponse;
import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MatchService {

    private final MatchRepository matchRepository;

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
                .team1Id(match.getTeam1().getId())
                .team2Id(match.getTeam2().getId())
                .team1Name(match.getTeam1().getName())
                .team2Name(match.getTeam2().getName())
                .team1Flag(match.getTeam1().getFlagUrl())
                .team2Flag(match.getTeam2().getFlagUrl())
                .matchDateTime(matchTimeIST)
                .venue(match.getVenue())
                .stage(match.getStage().name())
                .group(match.getGroup())
                .team1Score(match.getTeam1Score())
                .team2Score(match.getTeam2Score())
                .status(match.getStatus().name())
                .predictionLocked(matchTimeIST.isBefore(LocalDateTime.now(IST)))
                .build();
    }
}
