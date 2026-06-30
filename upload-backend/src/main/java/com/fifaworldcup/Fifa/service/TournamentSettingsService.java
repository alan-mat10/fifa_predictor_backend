package com.fifaworldcup.Fifa.service;

import com.fifaworldcup.Fifa.model.TournamentSettings;
import com.fifaworldcup.Fifa.repository.TournamentSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class TournamentSettingsService {

    private final TournamentSettingsRepository settingsRepository;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    /**
     * Returns the singleton settings record, creating one if none exists.
     */
    public TournamentSettings getSettings() {
        return settingsRepository.findAll().stream().findFirst()
                .orElseGet(() -> settingsRepository.save(TournamentSettings.builder()
                        .tournamentPredictionsLocked(false)
                        .build()));
    }

    /**
     * Checks if tournament predictions are currently locked.
     * Locked if: manual lock is true OR current time is past the configured lock time.
     */
    public boolean areTournamentPredictionsLocked() {
        TournamentSettings settings = getSettings();

        // Manual lock takes precedence
        if (settings.isTournamentPredictionsLocked()) {
            return true;
        }

        // Check time-based lock
        if (settings.getTournamentPredictionLockTime() != null) {
            LocalDateTime nowIST = LocalDateTime.now(IST);
            return nowIST.isAfter(settings.getTournamentPredictionLockTime());
        }

        return false;
    }

    /**
     * Admin: set the lock datetime for tournament predictions.
     */
    public TournamentSettings setLockTime(LocalDateTime lockTime) {
        TournamentSettings settings = getSettings();
        settings.setTournamentPredictionLockTime(lockTime);
        return settingsRepository.save(settings);
    }

    /**
     * Admin: manually lock tournament predictions immediately.
     */
    public TournamentSettings lockNow() {
        TournamentSettings settings = getSettings();
        settings.setTournamentPredictionsLocked(true);
        return settingsRepository.save(settings);
    }

    /**
     * Admin: unlock tournament predictions (remove manual lock).
     */
    public TournamentSettings unlock() {
        TournamentSettings settings = getSettings();
        settings.setTournamentPredictionsLocked(false);
        return settingsRepository.save(settings);
    }
}
