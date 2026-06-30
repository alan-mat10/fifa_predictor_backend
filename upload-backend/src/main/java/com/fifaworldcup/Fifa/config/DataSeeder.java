package com.fifaworldcup.Fifa.config;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.model.MatchGoalScorer;
import com.fifaworldcup.Fifa.model.GoalScorerPrediction;
import com.fifaworldcup.Fifa.model.MotmPrediction;
import com.fifaworldcup.Fifa.model.Player;
import com.fifaworldcup.Fifa.model.Prediction;
import com.fifaworldcup.Fifa.model.Team;
import com.fifaworldcup.Fifa.model.User;
import com.fifaworldcup.Fifa.repository.GoalScorerPredictionRepository;
import com.fifaworldcup.Fifa.repository.MatchGoalScorerRepository;
import com.fifaworldcup.Fifa.repository.MatchRepository;
import com.fifaworldcup.Fifa.repository.MotmPredictionRepository;
import com.fifaworldcup.Fifa.repository.PlayerRepository;
import com.fifaworldcup.Fifa.repository.PredictionRepository;
import com.fifaworldcup.Fifa.repository.TeamRepository;
import com.fifaworldcup.Fifa.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@Order(1)
@RequiredArgsConstructor
public class DataSeeder implements CommandLineRunner {

    private final TeamRepository teamRepository;
    private final MatchRepository matchRepository;
    private final MatchGoalScorerRepository matchGoalScorerRepository;
    private final UserRepository userRepository;
    private final PredictionRepository predictionRepository;
    private final GoalScorerPredictionRepository goalScorerPredictionRepository;
    private final MotmPredictionRepository motmPredictionRepository;
    private final PlayerRepository playerRepository;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Value("${app.seed-data:true}")
    private boolean seedData;

    @Override
    public void run(String... args) {
        if (!seedData) return;
        if (teamRepository.count() > 0) return; // Already seeded

        // Create admin user
        userRepository.save(User.builder()
                .username("admin")
                .email("admin@fifa2026.com")
                .password(passwordEncoder.encode("admin123"))
                .role(User.Role.ADMIN)
                .build());

        // ============================================================
        // FIFA WORLD CUP 2026 - ALL 48 TEAMS (12 Groups of 4)
        // ============================================================

        // Group A
        Team mexico = createTeam("Mexico", "A", "mx");
        Team southAfrica = createTeam("South Africa", "A", "za");
        Team southKorea = createTeam("South Korea", "A", "kr");
        Team czechia = createTeam("Czechia", "A", "cz");

        // Group B
        Team canada = createTeam("Canada", "B", "ca");
        Team switzerland = createTeam("Switzerland", "B", "ch");
        Team qatar = createTeam("Qatar", "B", "qa");
        Team bosnia = createTeam("Bosnia and Herzegovina", "B", "ba");

        // Group C
        Team brazil = createTeam("Brazil", "C", "br");
        Team morocco = createTeam("Morocco", "C", "ma");
        Team haiti = createTeam("Haiti", "C", "ht");
        Team scotland = createTeam("Scotland", "C", "gb-sct");

        // Group D
        Team usa = createTeam("USA", "D", "us");
        Team paraguay = createTeam("Paraguay", "D", "py");
        Team australia = createTeam("Australia", "D", "au");
        Team turkiye = createTeam("Türkiye", "D", "tr");

        // Group E
        Team germany = createTeam("Germany", "E", "de");
        Team curacao = createTeam("Curaçao", "E", "cw");
        Team ivoryCoast = createTeam("Ivory Coast", "E", "ci");
        Team ecuador = createTeam("Ecuador", "E", "ec");

        // Group F
        Team netherlands = createTeam("Netherlands", "F", "nl");
        Team japan = createTeam("Japan", "F", "jp");
        Team sweden = createTeam("Sweden", "F", "se");
        Team tunisia = createTeam("Tunisia", "F", "tn");

        // Group G
        Team belgium = createTeam("Belgium", "G", "be");
        Team egypt = createTeam("Egypt", "G", "eg");
        Team iran = createTeam("Iran", "G", "ir");
        Team newZealand = createTeam("New Zealand", "G", "nz");

        // Group H
        Team spain = createTeam("Spain", "H", "es");
        Team capeVerde = createTeam("Cape Verde", "H", "cv");
        Team saudiArabia = createTeam("Saudi Arabia", "H", "sa");
        Team uruguay = createTeam("Uruguay", "H", "uy");

        // Group I
        Team france = createTeam("France", "I", "fr");
        Team senegal = createTeam("Senegal", "I", "sn");
        Team norway = createTeam("Norway", "I", "no");
        Team iraq = createTeam("Iraq", "I", "iq");

        // Group J
        Team argentina = createTeam("Argentina", "J", "ar");
        Team algeria = createTeam("Algeria", "J", "dz");
        Team austria = createTeam("Austria", "J", "at");
        Team jordan = createTeam("Jordan", "J", "jo");

        // Group K
        Team portugal = createTeam("Portugal", "K", "pt");
        Team drCongo = createTeam("DR Congo", "K", "cd");
        Team uzbekistan = createTeam("Uzbekistan", "K", "uz");
        Team colombia = createTeam("Colombia", "K", "co");

        // Group L
        Team england = createTeam("England", "L", "gb-eng");
        Team croatia = createTeam("Croatia", "L", "hr");
        Team ghana = createTeam("Ghana", "L", "gh");
        Team panama = createTeam("Panama", "L", "pa");

        // ============================================================
        // MATCHDAY 1 (June 11 - 17) — All times ET
        // ============================================================

        // June 11
        Match mex_sa = createCompletedMatch(mexico, southAfrica, "2026-06-11T15:00", "Mexico City Stadium (Estadio Azteca)", "A", 2, 0);
        seedGoalScorer(mex_sa, "Julián Quiñones", 9, true, false, false);
        seedGoalScorer(mex_sa, "Raúl Jiménez", 77, false, false, true);

        Match kor_cze = createCompletedMatch(southKorea, czechia, "2026-06-11T22:00", "Guadalajara Stadium (Estadio Akron)", "A", 2, 1);
        seedGoalScorer(kor_cze, "Adam Hložek", 56, true, false, false);
        seedGoalScorer(kor_cze, "Hwang In-beom", 68, false, false, false);
        seedGoalScorer(kor_cze, "Oh Hyeon-gyu", 82, false, false, false);

        // June 12
        createMatch(canada, bosnia, "2026-06-12T15:00", "Toronto Stadium (BMO Field)", "B");
        createMatch(usa, paraguay, "2026-06-12T21:00", "Los Angeles Stadium (SoFi)", "D");

        // June 13
        createMatch(qatar, switzerland, "2026-06-13T15:00", "San Francisco Bay Area Stadium (Levi's)", "B");
        createMatch(brazil, morocco, "2026-06-13T18:00", "New York New Jersey Stadium (MetLife)", "C");
        createMatch(haiti, scotland, "2026-06-13T21:00", "Boston Stadium (Gillette)", "C");

        // June 14
        createMatch(australia, turkiye, "2026-06-14T00:00", "BC Place, Vancouver", "D");
        createMatch(germany, curacao, "2026-06-14T13:00", "Houston Stadium (NRG)", "E");
        createMatch(netherlands, japan, "2026-06-14T16:00", "Dallas Stadium (AT&T)", "F");
        createMatch(ivoryCoast, ecuador, "2026-06-14T19:00", "Philadelphia Stadium (Lincoln Financial)", "E");
        createMatch(sweden, tunisia, "2026-06-14T22:00", "Monterrey Stadium (Estadio BBVA)", "F");

        // June 15
        createMatch(spain, capeVerde, "2026-06-15T12:00", "Atlanta Stadium (Mercedes-Benz)", "H");
        createMatch(belgium, egypt, "2026-06-15T15:00", "Seattle Stadium (Lumen Field)", "G");
        createMatch(saudiArabia, uruguay, "2026-06-15T18:00", "Miami Stadium (Hard Rock)", "H");
        createMatch(iran, newZealand, "2026-06-15T21:00", "Los Angeles Stadium (SoFi)", "G");

        // June 16
        createMatch(france, senegal, "2026-06-16T15:00", "New York New Jersey Stadium (MetLife)", "I");
        createMatch(iraq, norway, "2026-06-16T18:00", "Boston Stadium (Gillette)", "I");
        createMatch(argentina, algeria, "2026-06-16T21:00", "Kansas City Stadium (Arrowhead)", "J");

        // June 17
        createMatch(austria, jordan, "2026-06-17T00:00", "San Francisco Bay Area Stadium (Levi's)", "J");
        createMatch(portugal, drCongo, "2026-06-17T13:00", "Houston Stadium (NRG)", "K");
        createMatch(england, croatia, "2026-06-17T16:00", "Dallas Stadium (AT&T)", "L");
        createMatch(ghana, panama, "2026-06-17T19:00", "Toronto Stadium (BMO Field)", "L");
        createMatch(uzbekistan, colombia, "2026-06-17T22:00", "Mexico City Stadium (Estadio Azteca)", "K");

        // ============================================================
        // MATCHDAY 2 (June 18 - 23)
        // ============================================================

        // June 18
        createMatch(czechia, southAfrica, "2026-06-18T12:00", "Atlanta Stadium (Mercedes-Benz)", "A");
        createMatch(switzerland, bosnia, "2026-06-18T15:00", "Los Angeles Stadium (SoFi)", "B");
        createMatch(canada, qatar, "2026-06-18T18:00", "BC Place, Vancouver", "B");
        createMatch(mexico, southKorea, "2026-06-18T21:00", "Guadalajara Stadium (Estadio Akron)", "A");

        // June 19
        createMatch(turkiye, paraguay, "2026-06-19T00:00", "San Francisco Bay Area Stadium (Levi's)", "D");
        createMatch(usa, australia, "2026-06-19T15:00", "Seattle Stadium (Lumen Field)", "D");
        createMatch(scotland, morocco, "2026-06-19T18:00", "Boston Stadium (Gillette)", "C");
        createMatch(brazil, haiti, "2026-06-19T20:30", "Philadelphia Stadium (Lincoln Financial)", "C");

        // June 20
        createMatch(netherlands, sweden, "2026-06-20T13:00", "Houston Stadium (NRG)", "F");
        createMatch(germany, ivoryCoast, "2026-06-20T16:00", "Toronto Stadium (BMO Field)", "E");
        createMatch(ecuador, curacao, "2026-06-20T20:00", "Kansas City Stadium (Arrowhead)", "E");

        // June 21
        createMatch(tunisia, japan, "2026-06-21T00:00", "Monterrey Stadium (Estadio BBVA)", "F");
        createMatch(spain, saudiArabia, "2026-06-21T12:00", "Atlanta Stadium (Mercedes-Benz)", "H");
        createMatch(belgium, iran, "2026-06-21T15:00", "Los Angeles Stadium (SoFi)", "G");
        createMatch(uruguay, capeVerde, "2026-06-21T18:00", "Miami Stadium (Hard Rock)", "H");
        createMatch(newZealand, egypt, "2026-06-21T21:00", "BC Place, Vancouver", "G");

        // June 22
        createMatch(argentina, austria, "2026-06-22T13:00", "Dallas Stadium (AT&T)", "J");
        createMatch(france, iraq, "2026-06-22T17:00", "Philadelphia Stadium (Lincoln Financial)", "I");
        createMatch(norway, senegal, "2026-06-22T20:00", "New York New Jersey Stadium (MetLife)", "I");
        createMatch(jordan, algeria, "2026-06-22T23:00", "San Francisco Bay Area Stadium (Levi's)", "J");

        // June 23
        createMatch(portugal, uzbekistan, "2026-06-23T13:00", "Houston Stadium (NRG)", "K");
        createMatch(england, ghana, "2026-06-23T16:00", "Boston Stadium (Gillette)", "L");
        createMatch(panama, croatia, "2026-06-23T19:00", "Toronto Stadium (BMO Field)", "L");
        createMatch(colombia, drCongo, "2026-06-23T22:00", "Guadalajara Stadium (Estadio Akron)", "K");

        // ============================================================
        // MATCHDAY 3 (June 24 - 27) - Simultaneous kickoffs per group
        // ============================================================

        // June 24
        createMatch(switzerland, canada, "2026-06-24T15:00", "BC Place, Vancouver", "B");
        createMatch(bosnia, qatar, "2026-06-24T15:00", "Seattle Stadium (Lumen Field)", "B");
        createMatch(scotland, brazil, "2026-06-24T18:00", "Miami Stadium (Hard Rock)", "C");
        createMatch(morocco, haiti, "2026-06-24T18:00", "Atlanta Stadium (Mercedes-Benz)", "C");
        createMatch(czechia, mexico, "2026-06-24T21:00", "Mexico City Stadium (Estadio Azteca)", "A");
        createMatch(southAfrica, southKorea, "2026-06-24T21:00", "Monterrey Stadium (Estadio BBVA)", "A");

        // June 25
        createMatch(curacao, ivoryCoast, "2026-06-25T16:00", "Philadelphia Stadium (Lincoln Financial)", "E");
        createMatch(ecuador, germany, "2026-06-25T16:00", "New York New Jersey Stadium (MetLife)", "E");
        createMatch(japan, sweden, "2026-06-25T19:00", "Dallas Stadium (AT&T)", "F");
        createMatch(tunisia, netherlands, "2026-06-25T19:00", "Kansas City Stadium (Arrowhead)", "F");
        createMatch(turkiye, usa, "2026-06-25T22:00", "Los Angeles Stadium (SoFi)", "D");
        createMatch(paraguay, australia, "2026-06-25T22:00", "San Francisco Bay Area Stadium (Levi's)", "D");

        // June 26
        createMatch(norway, france, "2026-06-26T15:00", "Boston Stadium (Gillette)", "I");
        createMatch(senegal, iraq, "2026-06-26T15:00", "Toronto Stadium (BMO Field)", "I");
        createMatch(capeVerde, saudiArabia, "2026-06-26T20:00", "Houston Stadium (NRG)", "H");
        createMatch(uruguay, spain, "2026-06-26T20:00", "Guadalajara Stadium (Estadio Akron)", "H");
        createMatch(egypt, iran, "2026-06-26T23:00", "Seattle Stadium (Lumen Field)", "G");
        createMatch(newZealand, belgium, "2026-06-26T23:00", "BC Place, Vancouver", "G");

        // June 27
        createMatch(panama, england, "2026-06-27T17:00", "New York New Jersey Stadium (MetLife)", "L");
        createMatch(croatia, ghana, "2026-06-27T17:00", "Philadelphia Stadium (Lincoln Financial)", "L");
        createMatch(colombia, portugal, "2026-06-27T19:30", "Miami Stadium (Hard Rock)", "K");
        createMatch(drCongo, uzbekistan, "2026-06-27T19:30", "Atlanta Stadium (Mercedes-Benz)", "K");
        createMatch(algeria, austria, "2026-06-27T22:00", "Kansas City Stadium (Arrowhead)", "J");
        createMatch(jordan, argentina, "2026-06-27T22:00", "Dallas Stadium (AT&T)", "J");

        // ============================================================
        // ROUND OF 32 (June 28 - July 3) — All times ET
        // ============================================================

        // Match 73 - June 28 (COMPLETED: Canada 1-0 South Africa)
        Match m73 = createCompletedKnockoutMatch(southAfrica, canada, "2026-06-28T12:00", "Los Angeles Stadium (SoFi)", Match.Stage.ROUND_OF_32, 0, 1);

        // Match 74 - June 29
        createKnockoutMatch(brazil, japan, "2026-06-29T13:00", "Houston Stadium (NRG)", Match.Stage.ROUND_OF_32);

        // Match 75 - June 29
        createKnockoutMatch(germany, paraguay, "2026-06-29T16:30", "Boston Stadium (Gillette)", Match.Stage.ROUND_OF_32);

        // Match 76 - June 29
        createKnockoutMatch(netherlands, morocco, "2026-06-29T21:00", "Monterrey Stadium (Estadio BBVA)", Match.Stage.ROUND_OF_32);

        // Match 77 - June 30
        createKnockoutMatch(france, sweden, "2026-06-30T17:00", "New York New Jersey Stadium (MetLife)", Match.Stage.ROUND_OF_32);

        // Match 78 - June 30
        createKnockoutMatch(ivoryCoast, norway, "2026-06-30T13:00", "Dallas Stadium (AT&T)", Match.Stage.ROUND_OF_32);

        // Match 79 - June 30
        createKnockoutMatch(mexico, ecuador, "2026-06-30T21:00", "Mexico City Stadium (Estadio Azteca)", Match.Stage.ROUND_OF_32);

        // Match 80 - July 1
        createKnockoutMatch(england, drCongo, "2026-07-01T12:00", "Atlanta Stadium (Mercedes-Benz)", Match.Stage.ROUND_OF_32);

        // Match 81 - July 1
        createKnockoutMatch(usa, bosnia, "2026-07-01T20:00", "San Francisco Bay Area Stadium (Levi's)", Match.Stage.ROUND_OF_32);

        // Match 82 - July 1
        createKnockoutMatch(belgium, senegal, "2026-07-01T16:00", "Seattle Stadium (Lumen Field)", Match.Stage.ROUND_OF_32);

        // Match 83 - July 2
        createKnockoutMatch(spain, austria, "2026-07-02T15:00", "Los Angeles Stadium (SoFi)", Match.Stage.ROUND_OF_32);

        // Match 84 - July 2
        createKnockoutMatch(portugal, croatia, "2026-07-02T19:00", "Toronto Stadium (BMO Field)", Match.Stage.ROUND_OF_32);

        // Match 85 - July 2
        createKnockoutMatch(switzerland, algeria, "2026-07-02T23:00", "BC Place, Vancouver", Match.Stage.ROUND_OF_32);

        // Match 86 - July 3
        createKnockoutMatch(australia, egypt, "2026-07-03T14:00", "Dallas Stadium (AT&T)", Match.Stage.ROUND_OF_32);

        // Match 87 - July 3
        createKnockoutMatch(argentina, capeVerde, "2026-07-03T18:00", "Miami Stadium (Hard Rock)", Match.Stage.ROUND_OF_32);

        // Match 88 - July 3
        createKnockoutMatch(colombia, ghana, "2026-07-03T21:30", "Kansas City Stadium (Arrowhead)", Match.Stage.ROUND_OF_32);

        // ============================================================
        // ROUND OF 16 (July 4 - July 7) — Placeholder matches
        // ============================================================

        // Match 89 - July 4: Winner M75 vs Winner M77
        createPlaceholderMatch("2026-07-04T16:00", "Philadelphia Stadium (Lincoln Financial)", Match.Stage.ROUND_OF_16);

        // Match 90 - July 4: Winner M73 (Canada) vs Winner M76
        // Since M73 is completed, Canada is already placed as team1
        matchRepository.save(Match.builder()
                .team1(canada)
                .matchDateTime(LocalDateTime.parse("2026-07-04T20:00"))
                .venue("Houston Stadium (NRG)")
                .stage(Match.Stage.ROUND_OF_16)
                .status(Match.MatchStatus.UPCOMING)
                .build());

        // Match 91 - July 5: Winner M74 vs Winner M78
        createPlaceholderMatch("2026-07-05T17:00", "New York New Jersey Stadium (MetLife)", Match.Stage.ROUND_OF_16);

        // Match 92 - July 5: Winner M79 vs Winner M80
        createPlaceholderMatch("2026-07-05T21:00", "Mexico City Stadium (Estadio Azteca)", Match.Stage.ROUND_OF_16);

        // Match 93 - July 6: Winner M84 vs Winner M83
        createPlaceholderMatch("2026-07-06T16:00", "Dallas Stadium (AT&T)", Match.Stage.ROUND_OF_16);

        // Match 94 - July 6: Winner M81 vs Winner M82
        createPlaceholderMatch("2026-07-06T20:00", "Seattle Stadium (Lumen Field)", Match.Stage.ROUND_OF_16);

        // Match 95 - July 7: Winner M87 vs Winner M86
        createPlaceholderMatch("2026-07-07T17:00", "Atlanta Stadium (Mercedes-Benz)", Match.Stage.ROUND_OF_16);

        // Match 96 - July 7: Winner M85 vs Winner M88
        createPlaceholderMatch("2026-07-07T21:00", "BC Place, Vancouver", Match.Stage.ROUND_OF_16);

        System.out.println("✅ Database seeded: 48 teams, 72 group + 16 R32 + 8 R16 matches, admin user created.");
        System.out.println("   Results loaded: Mexico 2-0 SA, S.Korea 2-1 Czechia | R32: Canada 1-0 South Africa");

        // ============================================================
        // TEST DATA: akshay predictions for completed matches
        // ============================================================
        User akshay = userRepository.save(User.builder()
                .username("akshay")
                .email("akshay@test.com")
                .password(passwordEncoder.encode("akshay123"))
                .role(User.Role.USER)
                .build());

        // Create test players for predictions (these will also be loaded by PlayerSeeder from CSV)
        Player testPlayerMex1 = playerRepository.save(Player.builder()
                .name("Julián Quiñones").team(mexico).position(Player.Position.FORWARD).build());
        Player testPlayerMex2 = playerRepository.save(Player.builder()
                .name("Raúl Jiménez").team(mexico).position(Player.Position.FORWARD).build());
        Player testPlayerKor1 = playerRepository.save(Player.builder()
                .name("Hwang In-beom").team(southKorea).position(Player.Position.MIDFIELDER).build());

        // ── Match 1: Mexico 2-0 South Africa — ALL CORRECT ──
        // Score: predicted 2-0, actual 2-0 → +3 (exact)
        predictionRepository.save(Prediction.builder()
                .user(akshay).match(mex_sa)
                .predictedTeam1Score(2).predictedTeam2Score(0)
                .pointsEarned(3).scored(true)
                .build());

        // Goal scorer: predicted Julián Quiñones (actually scored!) → +2
        goalScorerPredictionRepository.save(GoalScorerPrediction.builder()
                .user(akshay).match(mex_sa).player(testPlayerMex1)
                .isFirstGoalScorer(true).predictedGoals(1)
                .pointsEarned(2).scored(true)
                .build());

        // MOTM: predicted Julián Quiñones → +3 (correct)
        mex_sa.setManOfTheMatch("Julián Quiñones");
        matchRepository.save(mex_sa);
        motmPredictionRepository.save(MotmPrediction.builder()
                .user(akshay).match(mex_sa).player(testPlayerMex1)
                .pointsEarned(3).scored(true)
                .build());

        // ── Match 2: South Korea 2-1 Czechia — ALL WRONG ──
        // Score: predicted 0-1, actual 2-1 → 0 (wrong result)
        predictionRepository.save(Prediction.builder()
                .user(akshay).match(kor_cze)
                .predictedTeam1Score(0).predictedTeam2Score(1)
                .pointsEarned(0).scored(true)
                .build());

        // Goal scorer: predicted Hwang In-beom as first scorer, but he wasn't first → 0
        // (he did score, but let's say we give 0 for testing "wrong" scenario)
        goalScorerPredictionRepository.save(GoalScorerPrediction.builder()
                .user(akshay).match(kor_cze).player(testPlayerKor1)
                .isFirstGoalScorer(true).predictedGoals(1)
                .pointsEarned(0).scored(true)
                .build());

        // MOTM: predicted Hwang In-beom but actual MOTM was different → 0
        kor_cze.setManOfTheMatch("Oh Hyeon-gyu");
        matchRepository.save(kor_cze);
        motmPredictionRepository.save(MotmPrediction.builder()
                .user(akshay).match(kor_cze).player(testPlayerKor1)
                .pointsEarned(0).scored(true)
                .build());

        // Update akshay's total: 3 (score) + 2 (scorer) + 3 (motm) = 8 pts
        akshay.setTotalPoints(8);
        userRepository.save(akshay);

        System.out.println("   Test data: akshay with 2 matches — Match 1: all correct (+8), Match 2: all wrong (0)");
    }

    private Team createTeam(String name, String group, String countryCode) {
        return teamRepository.save(Team.builder()
                .name(name)
                .group(group)
                .flagUrl("https://flagcdn.com/48x36/" + countryCode + ".png")
                .build());
    }

    private void createMatch(Team team1, Team team2, String dateTime, String venue, String group) {
        matchRepository.save(Match.builder()
                .team1(team1)
                .team2(team2)
                .matchDateTime(LocalDateTime.parse(dateTime))
                .venue(venue)
                .stage(Match.Stage.GROUP)
                .group(group)
                .status(Match.MatchStatus.UPCOMING)
                .build());
    }

    private Match createCompletedMatch(Team team1, Team team2, String dateTime, String venue, String group, int score1, int score2) {
        return matchRepository.save(Match.builder()
                .team1(team1)
                .team2(team2)
                .matchDateTime(LocalDateTime.parse(dateTime))
                .venue(venue)
                .stage(Match.Stage.GROUP)
                .group(group)
                .team1Score(score1)
                .team2Score(score2)
                .status(Match.MatchStatus.COMPLETED)
                .build());
    }

    private Match createKnockoutMatch(Team team1, Team team2, String dateTime, String venue, Match.Stage stage) {
        return matchRepository.save(Match.builder()
                .team1(team1)
                .team2(team2)
                .matchDateTime(LocalDateTime.parse(dateTime))
                .venue(venue)
                .stage(stage)
                .status(Match.MatchStatus.UPCOMING)
                .build());
    }

    private Match createCompletedKnockoutMatch(Team team1, Team team2, String dateTime, String venue, Match.Stage stage, int score1, int score2) {
        return matchRepository.save(Match.builder()
                .team1(team1)
                .team2(team2)
                .matchDateTime(LocalDateTime.parse(dateTime))
                .venue(venue)
                .stage(stage)
                .team1Score(score1)
                .team2Score(score2)
                .status(Match.MatchStatus.COMPLETED)
                .build());
    }

    private Match createPlaceholderMatch(String dateTime, String venue, Match.Stage stage) {
        return matchRepository.save(Match.builder()
                .matchDateTime(LocalDateTime.parse(dateTime))
                .venue(venue)
                .stage(stage)
                .status(Match.MatchStatus.UPCOMING)
                .build());
    }

    private void seedGoalScorer(Match match, String playerName, int minute, boolean isFirst, boolean isOwnGoal, boolean isPenalty) {
        matchGoalScorerRepository.save(MatchGoalScorer.builder()
                .match(match)
                .playerName(playerName)
                .minute(minute)
                .firstGoal(isFirst)
                .ownGoal(isOwnGoal)
                .penalty(isPenalty)
                .build());
    }
}
