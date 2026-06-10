package mcp.server.domain.reference_data.persistence;

import java.util.List;
import java.util.Optional;

import mcp.server.domain.reference_data.model.Role;
import mcp.server.domain.reference_data.model.RoleId;

public interface RoleRepo {

    Role save(Role role);

    Optional<Role> findById(RoleId id);

    List<Role> findAll();

    void delete(RoleId id);

    boolean existsById(RoleId id);
}
