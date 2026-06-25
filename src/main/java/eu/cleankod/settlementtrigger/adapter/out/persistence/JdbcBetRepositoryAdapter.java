package eu.cleankod.settlementtrigger.adapter.out.persistence;

import eu.cleankod.settlementtrigger.application.port.out.BetRepository;
import eu.cleankod.settlementtrigger.domain.Bet;
import eu.cleankod.settlementtrigger.domain.UnsavedBet;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class JdbcBetRepositoryAdapter implements BetRepository {

    private final SpringDataBetRepository springDataBetRepository;

    public JdbcBetRepositoryAdapter(SpringDataBetRepository springDataBetRepository) {
        this.springDataBetRepository = springDataBetRepository;
    }

    @Override
    public Bet save(UnsavedBet unsavedBet) {
        return springDataBetRepository.save(BetEntity.from(unsavedBet)).toDomain();
    }

    @Override
    public void save(Bet bet) {
        springDataBetRepository.save(BetEntity.from(bet));
    }

    @Override
    public Optional<Bet> findById(long id) {
        return springDataBetRepository.findById(id).map(BetEntity::toDomain);
    }

    @Override
    public List<Bet> findPendingByEventId(String eventId) {
        return springDataBetRepository.findPendingByEventId(eventId)
                .stream()
                .map(BetEntity::toDomain)
                .toList();
    }
}
