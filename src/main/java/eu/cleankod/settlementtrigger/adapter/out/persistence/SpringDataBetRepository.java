package eu.cleankod.settlementtrigger.adapter.out.persistence;

import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

interface SpringDataBetRepository extends CrudRepository<BetEntity, Long> {

    @Query("SELECT * FROM bets WHERE event_id = :eventId AND status = 'PENDING'")
    List<BetEntity> findPendingByEventId(String eventId);
}
