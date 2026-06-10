package mcp.server.domain.reference_data.persistence;

import org.springframework.stereotype.Repository;

import mcp.server.domain.reference_data.model.Role;
import mcp.server.domain.reference_data.model.RoleId;

import java.util.List;
import java.util.Optional;

@Repository
public class RoleRepoAdapter implements RoleRepo {

    private final RoleJpaRepo jpaRepo;

    public RoleRepoAdapter(RoleJpaRepo jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Role save(Role role) {
        RoleEntity entity = toEntity(role);
        RoleEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Role> findById(RoleId id) {
        return jpaRepo.findById(id.value()).map(this::toDomain);
    }

    @Override
    public List<Role> findAll() {
        return jpaRepo.findAll().stream().map(this::toDomain).toList();
    }

    @Override
    public void delete(RoleId id) {
        jpaRepo.deleteById(id.value());
    }

    @Override
    public boolean existsById(RoleId id) {
        return jpaRepo.existsById(id.value());
    }

    private RoleEntity toEntity(Role domain) {
        Long entityId = domain.getId().value() == 0 ? null : domain.getId().value();
        return new RoleEntity(entityId, domain.getTitle());
    }

    private Role toDomain(RoleEntity entity) {
        return new Role(
                new RoleId(entity.getId()),
                entity.getRoleTitle());
    }
}
