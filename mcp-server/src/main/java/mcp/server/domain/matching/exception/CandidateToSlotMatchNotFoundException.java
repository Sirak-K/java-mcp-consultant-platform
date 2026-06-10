package mcp.server.domain.matching.exception;

import java.util.NoSuchElementException;

public final class CandidateToSlotMatchNotFoundException extends NoSuchElementException {

  public CandidateToSlotMatchNotFoundException(long matchId) {
    super("candidateToSlotMatch not found: " + matchId);
  }
}
