export function MatchViewerSkillChips({
  skills,
  emptyText,
  align = "left",
}: {
  skills: string[];
  emptyText: string;
  align?: "left" | "right";
}) {
  const alignmentClass =
    align === "right" ? "justify-end text-right" : "justify-start text-left";

  if (skills.length === 0) {
    return (
      <span className={`block text-soft text-xs ${alignmentClass}`}>
        {emptyText}
      </span>
    );
  }

  return (
    <div className={`flex min-w-0 flex-wrap gap-1.5 ${alignmentClass}`}>
      {skills.map((skill) => (
        <span
          key={skill}
          className="chip max-w-full rounded-full px-2.5 py-1 text-xs leading-5 [overflow-wrap:anywhere]"
        >
          {skill}
        </span>
      ))}
    </div>
  );
}
