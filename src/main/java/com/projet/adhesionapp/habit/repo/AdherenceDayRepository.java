package com.projet.adhesionapp.habit.repo;


import com.projet.adhesionapp.habit.domain.AdherenceDay;
import com.projet.adhesionapp.identity.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AdherenceDayRepository extends JpaRepository<AdherenceDay, Long> {

    List<AdherenceDay> findByUserAndDateBetweenOrderByDateAsc(
            User user,
            LocalDate start,
            LocalDate end
    );
}
