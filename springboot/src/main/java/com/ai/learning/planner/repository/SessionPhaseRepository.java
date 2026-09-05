package com.ai.learning.planner.repository;

import com.ai.learning.planner.entity.SessionPhase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SessionPhaseRepository extends JpaRepository<SessionPhase, Long> {

    List<SessionPhase> findBySession_IdOrderByCreatedAtAsc(Long sessionId);

    Optional<SessionPhase> findBySession_IdAndPhaseId(Long sessionId, String phaseId);
}