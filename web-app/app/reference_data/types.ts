export interface ReferenceOption {
  id: number;
  title: string;
}

export type SkillCategory = "PRIMARY" | "SECONDARY";

export interface ReferenceSkillOption extends ReferenceOption {
  category: SkillCategory;
}

export interface ReferenceSkillLevelOption {
  id: number;
  name: string;
}

export interface MarketplaceReferenceOption {
  id: number;
  title: string;
}

export interface MarketplaceReferenceSkillOption extends MarketplaceReferenceOption {
  category: SkillCategory;
}

export interface MarketplaceSkillLevelOption {
  id: number;
  name: string;
}

export interface MarketplaceReferenceData {
  roles: MarketplaceReferenceOption[];
  skills: MarketplaceReferenceSkillOption[];
  skillLevels: MarketplaceSkillLevelOption[];
}
