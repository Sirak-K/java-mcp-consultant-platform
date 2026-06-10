package mcp.server.domain.reference_data.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CompanyIdentityLookupJpaRepo extends JpaRepository<CompanyIdentityLookupEntity, Long> {

        Optional<CompanyIdentityLookupEntity> findByOrganisationNumber(String organisationNumber);

        List<CompanyIdentityLookupEntity> findByOrganisationNameNormalizedOrderByOrganisationNameAsc(
                        String organisationNameNormalized);

        @Query("""
                        SELECT companyIdentity
                        FROM CompanyIdentityLookupEntity companyIdentity
                        WHERE companyIdentity.organisationNameNormalized LIKE CONCAT(:prefix, '%')
                        ORDER BY companyIdentity.organisationName ASC
                        """)
        List<CompanyIdentityLookupEntity> findByOrganisationNameNormalizedPrefix(
                        @Param("prefix") String prefix,
                        Pageable pageable);
}
