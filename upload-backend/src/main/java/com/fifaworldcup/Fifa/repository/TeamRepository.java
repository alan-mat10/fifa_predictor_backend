package com.fifaworldcup.Fifa.repository;

import com.fifaworldcup.Fifa.model.Team;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {
    Optional<Team> findByName(String name);
    List<Team> findByGroupOrderByNameAsc(String group);
}
