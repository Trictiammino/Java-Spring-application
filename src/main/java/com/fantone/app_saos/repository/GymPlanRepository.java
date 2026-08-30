package com.fantone.app_saos.repository;

import com.fantone.app_saos.model.GymPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GymPlanRepository extends JpaRepository<GymPlan, Long> {

    // Trova un piano dello shop tramite il nome (es: "Mensile Open")
    Optional<GymPlan> findByName(String name);
}