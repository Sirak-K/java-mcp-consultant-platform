package mcp.server.domain.candidate_profiles.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Spring Data JPA repository for the live {@code cand_profile} operational
 * profile table.
 */
public interface CandidateProfileJpaRepo extends JpaRepository<CandidateProfileEntity, Long> {

    List<CandidateProfileEntity> findAllByOrderByCreatedAtDesc();
}
