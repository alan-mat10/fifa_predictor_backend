package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.GoalScorerPrediction;
import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GoalScorerPredictionRepository extends JpaRepository<GoalScorerPrediction, Long> {
    List<GoalScorerPrediction> findByUserAndMatch(User user, Match match);
    List<GoalScorerPrediction> findByMatch(Match match);
    List<GoalScorerPrediction> findByMatchAndScored(Match match, boolean scored);
    List<GoalScorerPrediction> findByUser(User user);
    void deleteByUserAndMatch(User user, Match match);
}
