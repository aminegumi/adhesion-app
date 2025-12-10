package com.projet.adhesionapp.analytics.model;

/**
 * A single risk factor contributing to non-adherence risk.
 */
public record RiskFactorDto(
        /**
         * Name of the risk factor (e.g., "Depression", "Complex Regimen")
         */
        String factor,

        /**
         * Description of the specific issue
         */
        String description,

        /**
         * Severity level: "low", "medium", "high"
         */
        String severity,

        /**
         * Actionable recommendation to address this factor
         */
        String recommendation) {
    /**
     * Get severity as a numeric weight for calculations
     */
    public double severityWeight() {
        return switch (severity) {
            case "high" -> 1.0;
            case "medium" -> 0.6;
            case "low" -> 0.3;
            default -> 0.5;
        };
    }

    /**
     * Get color code for UI display
     */
    public String colorCode() {
        return switch (severity) {
            case "high" -> "#E53935"; // Red
            case "medium" -> "#FB8C00"; // Orange
            case "low" -> "#FDD835"; // Yellow
            default -> "#9E9E9E"; // Gray
        };
    }
}
