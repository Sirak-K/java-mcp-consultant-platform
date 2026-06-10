import type { SkillCategory } from "~/reference_data/types";

type SkillOption = {
  id: number;
  title: string;
  category?: SkillCategory;
};

type SkillOptionGroupsProps = {
  options: SkillOption[];
  emptyLabel?: string;
  compositeValue?: boolean;
};

const skillCategoryLabels: Record<SkillCategory, string> = {
  PRIMARY: "Primary skills",
  SECONDARY: "Secondary skills",
};

function optionValue(option: SkillOption, compositeValue: boolean | undefined) {
  return compositeValue ? `${option.category ?? "PRIMARY"}:${option.id}` : option.id;
}

export function SkillOptionGroups({ options, emptyLabel, compositeValue }: SkillOptionGroupsProps) {
  const primarySkills = options.filter((option) => option.category === "PRIMARY");
  const secondarySkills = options.filter((option) => option.category === "SECONDARY");
  const uncategorizedSkills = options.filter((option) => !option.category);

  return (
    <>
      {emptyLabel ? <option value="">{emptyLabel}</option> : null}
      {primarySkills.length > 0 ? (
        <optgroup label={skillCategoryLabels.PRIMARY}>
          {primarySkills.map((option) => (
            <option key={`${option.category}:${option.id}`} value={optionValue(option, compositeValue)}>
              {option.title}
            </option>
          ))}
        </optgroup>
      ) : null}
      {secondarySkills.length > 0 ? (
        <optgroup label={skillCategoryLabels.SECONDARY}>
          {secondarySkills.map((option) => (
            <option key={`${option.category}:${option.id}`} value={optionValue(option, compositeValue)}>
              {option.title}
            </option>
          ))}
        </optgroup>
      ) : null}
      {uncategorizedSkills.map((option) => (
        <option key={option.id} value={optionValue(option, compositeValue)}>
          {option.title}
        </option>
      ))}
    </>
  );
}
