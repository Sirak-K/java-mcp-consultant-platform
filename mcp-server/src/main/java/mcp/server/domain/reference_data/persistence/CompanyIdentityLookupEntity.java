package mcp.server.domain.reference_data.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "company_identity_lookup", schema = "consultant_platform", indexes = {
        @Index(name = "idx_company_identity_lookup_name_norm", columnList = "organisation_name_normalized")
})
public class CompanyIdentityLookupEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "company_identity_lookup_id")
    private Long id;

    @Column(name = "organisation_number", nullable = false, length = 30, unique = true)
    private String organisationNumber;

    @Column(name = "organisation_name", nullable = false, length = 200)
    private String organisationName;

    @Column(name = "organisation_name_normalized", nullable = false, length = 200)
    private String organisationNameNormalized;

    @Column(name = "organisation_city", length = 100)
    private String organisationCity;

    @Column(name = "source", nullable = false, length = 40)
    private String source;

    @Column(name = "imported_at", nullable = false)
    private Instant importedAt;

    protected CompanyIdentityLookupEntity() {
    }

    public CompanyIdentityLookupEntity(
            Long id,
            String organisationNumber,
            String organisationName,
            String organisationNameNormalized,
            String organisationCity,
            String source,
            Instant importedAt) {
        this.id = id;
        this.organisationNumber = organisationNumber;
        this.organisationName = organisationName;
        this.organisationNameNormalized = organisationNameNormalized;
        this.organisationCity = organisationCity;
        this.source = source;
        this.importedAt = importedAt;
    }

    public Long getId() {
        return id;
    }

    public String getOrganisationNumber() {
        return organisationNumber;
    }

    public void setOrganisationNumber(String organisationNumber) {
        this.organisationNumber = organisationNumber;
    }

    public String getOrganisationName() {
        return organisationName;
    }

    public void setOrganisationName(String organisationName) {
        this.organisationName = organisationName;
    }

    public String getOrganisationNameNormalized() {
        return organisationNameNormalized;
    }

    public void setOrganisationNameNormalized(String organisationNameNormalized) {
        this.organisationNameNormalized = organisationNameNormalized;
    }

    public String getOrganisationCity() {
        return organisationCity;
    }

    public void setOrganisationCity(String organisationCity) {
        this.organisationCity = organisationCity;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Instant getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(Instant importedAt) {
        this.importedAt = importedAt;
    }
}
