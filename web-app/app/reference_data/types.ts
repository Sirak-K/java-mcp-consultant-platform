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

export interface ReferenceData {
  roles: ReferenceOption[];
  skills: ReferenceSkillOption[];
  skillLevels: ReferenceSkillLevelOption[];
}
