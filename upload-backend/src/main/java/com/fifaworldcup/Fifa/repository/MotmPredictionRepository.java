package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.Match;
import com.fifaworldcup.Fifa.model.MotmPrediction;
import com.fifaworldcup.Fifa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MotmPredictionRepository extends JpaRepository<MotmPrediction, Long> {
    Optional<MotmPrediction> findByUserAndMatch(User user, Match match);
    List<MotmPrediction> findByMatchAndScored(Match match, boolean scored);
    List<MotmPrediction> findByUser(User user);
}
