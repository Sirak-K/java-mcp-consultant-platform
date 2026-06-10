package mcp.server.domain.reference_data.persistence;

public final class CompetencyLevelLookupSupport {

    public static final short JUNIOR_ID = 1;
    public static final short INTERMEDIATE_ID = 2;
    public static final short SENIOR_ID = 3;

    private CompetencyLevelLookupSupport() {
    }

    public static short lookupIdForYears(Short years) {
        int value = years == null ? 0 : Math.max(0, years);
        if (value >= 5) {
            return SENIOR_ID;
        }
        if (value >= 3) {
            return INTERMEDIATE_ID;
        }
        return JUNIOR_ID;
    }

    public static short yearsForLookupId(Short lookupId) {
        if (lookupId == null) {
            return 0;
        }
        return switch (lookupId) {
            case SENIOR_ID -> 5;
            case INTERMEDIATE_ID -> 3;
            default -> 0;
        };
    }
}
