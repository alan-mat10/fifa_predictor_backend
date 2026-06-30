package com.fifaworldcup.Fifa.config;

import com.fifaworldcup.Fifa.model.Player;
import com.fifaworldcup.Fifa.model.Player.Position;
import com.fifaworldcup.Fifa.model.Team;
import com.fifaworldcup.Fifa.repository.PlayerRepository;
import com.fifaworldcup.Fifa.repository.TeamRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Component
@Order(2)
@RequiredArgsConstructor
@Slf4j
public class PlayerSeeder implements CommandLineRunner {

    private final PlayerRepository playerRepository;
    private final TeamRepository teamRepository;

    @org.springframework.beans.factory.annotation.Value("${app.seed-data:true}")
    private boolean seedData;

    // Map CSV country names to our DB team names
    private static final Map<String, String> TEAM_NAME_MAP = new HashMap<>() {{
        put("Korea Republic", "South Korea");
        put("IR Iran", "Iran");
        put("Congo DR", "DR Congo");
        put("Côte D'Ivoire", "Ivory Coast");
        put("Cabo Verde", "Cape Verde");
        put("Bosnia And Herzegovina", "Bosnia and Herzegovina");
    }};

    @Override
    public void run(String... args) {
        if (!seedData) return;
        if (playerRepository.count() > 10) return;  // Only skip if full CSV already loaded

        try {
            ClassPathResource resource = new ClassPathResource("FIFA_World_Cup_2026_Squads)_new.csv");
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8));

            String line;
            int loaded = 0;
            int skipped = 0;

            // Skip header
            reader.readLine();

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                try {
                    String[] parts = parseCsvLine(line);
                    if (parts.length < 3) {
                        skipped++;
                        continue;
                    }

                    String csvCountry = parts[0].trim();
                    String positionStr = parts[1].trim();
                    String playerName = parts[2].trim();

                    // Map country name to DB team name
                    String dbTeamName = TEAM_NAME_MAP.getOrDefault(csvCountry, csvCountry);

                    // Find team in DB
                    Optional<Team> teamOpt = teamRepository.findByName(dbTeamName);
                    if (teamOpt.isEmpty()) {
                        skipped++;
                        continue;
                    }

                    // Map position
                    Position position = mapPosition(positionStr);

                    // Save player
                    playerRepository.save(Player.builder()
                            .name(playerName)
                            .team(teamOpt.get())
                            .position(position)
                            .build());
                    loaded++;
                } catch (Exception e) {
                    skipped++;
                }
            }

            reader.close();
            log.info("✅ Player database loaded from CSV: {} players loaded, {} skipped.", loaded, skipped);

        } catch (Exception e) {
            log.error("Failed to load players from CSV: {}", e.getMessage(), e);
        }
    }

    private Position mapPosition(String pos) {
        return switch (pos.toUpperCase()) {
            case "GK" -> Position.GOALKEEPER;
            case "DF" -> Position.DEFENDER;
            case "MF" -> Position.MIDFIELDER;
            case "FW" -> Position.FORWARD;
            default -> Position.MIDFIELDER;
        };
    }

    private String[] parseCsvLine(String line) {
        java.util.List<String> fields = new java.util.ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields.toArray(new String[0]);
    }
}
