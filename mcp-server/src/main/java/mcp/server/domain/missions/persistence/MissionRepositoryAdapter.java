package mcp.server.domain.missions.persistence;

import mcp.server.domain.customers.model.CustomerId;
import mcp.server.domain.missions.model.Mission;
import mcp.server.domain.missions.model.MissionAvailability;
import mcp.server.domain.missions.model.MissionId;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MissionRepositoryAdapter implements MissionRepository {

    private final MissionJpaRepository jpaRepo;

    public MissionRepositoryAdapter(MissionJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Mission save(Mission mission) {
        MissionEntity entity = toEntity(mission);
        MissionEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Mission> findById(MissionId id) {
        return jpaRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Mission> findByAvailability(MissionAvailability availability) {
        return jpaRepo.findByMissionAvailability(availability.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public List<Mission> findAll() {
        return jpaRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(MissionId id) {
        jpaRepo.deleteById(id.value());
    }

    @Override
    public boolean existsById(MissionId id) {
        return jpaRepo.existsById(id.value());
    }

    // -------------------------------------------------------------------------
    // Mapping
    // -------------------------------------------------------------------------

    private MissionEntity toEntity(Mission domain) {
        Long entityId = domain.getId().value() == 0 ? null : domain.getId().value();
        MissionEntity existing = entityId == null
                ? null
                : jpaRepo.findById(entityId).orElse(null);
        String customerName = existing != null
                ? existing.getCustomerName()
                : domain.getCustomerName();
        String customerEmail = existing != null
                ? existing.getCustomerEmail()
                : domain.getCustomerEmail();
        return new MissionEntity(
                entityId,
                domain.getCustomerId().value(),
                customerName,
                customerEmail,
                domain.getTitle(),
                domain.getAvailability().name());
    }

    private Mission toDomain(MissionEntity entity) {
        return new Mission(
                new MissionId(entity.getId()),
                new CustomerId(entity.getCustomerId()),
                entity.getCustomerName(),
                entity.getCustomerEmail(),
                entity.getMissionTitle(),
                MissionAvailability.valueOf(entity.getMissionAvailability()));
    }
}
