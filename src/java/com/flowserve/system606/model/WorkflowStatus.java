package com.flowserve.system606.model;

/**
 *
 * @author kgraves
 */
public enum WorkflowStatus {
    DRAFT("D", "Draft", "", ""),
    PENDING_REVIEW("PR", "Pending Review", "", ""),
    PENDING_APPROVAL("PA", "Pending Approval", "", ""),
    REJECTED("R", "Rejected", "", ""),
    APPROVED("A", "Approved", "", "");

    private String shortName;
    private String description;
    private String icon;
    private String color;

    private WorkflowStatus(String shortName, String description, String icon, String color) {
        this.shortName = shortName;
        this.description = description;
        this.icon = icon;
        this.color = color;
    }

    public String getShortName() {
        return shortName;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public static WorkflowStatus fromShortName(String shortName) {
        switch (shortName) {
            case "D":
                return WorkflowStatus.DRAFT;

            case "PR":
                return WorkflowStatus.PENDING_REVIEW;

            case "PA":
                return WorkflowStatus.PENDING_APPROVAL;

            case "R":
                return WorkflowStatus.REJECTED;

            case "A":
                return WorkflowStatus.APPROVED;

            default:
                throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
        }
    }
}
