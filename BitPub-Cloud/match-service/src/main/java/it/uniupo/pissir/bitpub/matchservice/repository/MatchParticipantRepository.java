// Autore: Timothy Giolito 20054431
package it.uniupo.pissir.bitpub.matchservice.repository;

import it.uniupo.pissir.bitpub.matchservice.domain.MatchParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchParticipantRepository extends JpaRepository<MatchParticipant, String> {
}
