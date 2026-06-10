package mcp.server.domain.reference_data.application;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import mcp.server.domain.reference_data.web.DomainStaticReferenceDataDto.CompetencyLevelOptionView;
import mcp.server.domain.reference_data.web.DomainStaticReferenceDataDto.ReferenceOptionView;
import mcp.server.domain.reference_data.web.DomainStaticReferenceDataDto.ReferenceSkillOptionView;
import mcp.server.domain.reference_data.web.DomainStaticReferenceDataDto.StaticReferenceDataView;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupEntity;
import mcp.server.domain.reference_data.persistence.RoleEntity;
import mcp.server.domain.reference_data.persistence.SkillCatalogLookup;
import mcp.server.domain.reference_data.persistence.CompetencyLevelLookupJpaRepo;
import mcp.server.domain.reference_data.persistence.RoleJpaRepo;

import java.util.Comparator;

@Service
public class DomainStaticReferenceDataService {

        private final RoleJpaRepo roleRepo;
        private final SkillCatalogLookup skillCatalogLookup;
        private final CompetencyLevelLookupJpaRepo skillLevelRepo;

        public DomainStaticReferenceDataService(
                        RoleJpaRepo roleRepo,
                        SkillCatalogLookup skillCatalogLookup,
                        CompetencyLevelLookupJpaRepo skillLevelRepo) {
                this.roleRepo = roleRepo;
                this.skillCatalogLookup = skillCatalogLookup;
                this.skillLevelRepo = skillLevelRepo;
        }

        @Transactional(readOnly = true)
        public StaticReferenceDataView referenceData() {
                return new StaticReferenceDataView(
                                roleRepo.findAll().stream()
                                                .sorted(Comparator.comparing(RoleEntity::getRoleTitle))
                                                .map(role -> new ReferenceOptionView(
                                                                role.getId(),
                                                                role.getRoleTitle()))
                                                .toList(),
                                skillCatalogLookup.findAllSkills().stream()
                                                .map(skill -> new ReferenceSkillOptionView(
                                                                skill.id(),
                                                                skill.title(),
                                                                skill.category()))
                                                .sorted(Comparator
                                                                .comparing(ReferenceSkillOptionView::title))
                                                .toList(),
                                skillLevelRepo.findAll().stream()
                                                .sorted(Comparator.comparing(
                                                                CompetencyLevelLookupEntity::getCompetencyLevelLookupId))
                                                .map(level -> new CompetencyLevelOptionView(
                                                                level.getCompetencyLevelLookupId(),
                                                                level.getCompetencyLevelName()))
                                                .toList());
        }
}
