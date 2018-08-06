package com.flowserve.system606.model;

/**
 *
 * @author kgraves
 */
public enum ApprovalWorkflowStatus {
    DRAFT("D", "Draft", "", ""),
    PENDING_REVIEW("PR", "Pending Review", "", ""),
    PENDING_APPROVAL("PA", "Pending Approval", "", ""),
    REJECTED("R", "Rejected", "", ""),
    APPROVED("A", "Approved", "", "");

    private String shortName;
    private String description;
    private String icon;
    private String color;

    private ApprovalWorkflowStatus(String shortName, String description, String icon, String color) {
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

    public static ApprovalWorkflowStatus fromShortName(String shortName) {
        switch (shortName) {
            case "D":
                return ApprovalWorkflowStatus.DRAFT;

            case "PR":
                return ApprovalWorkflowStatus.PENDING_REVIEW;

            case "PA":
                return ApprovalWorkflowStatus.PENDING_APPROVAL;

            case "R":
                return ApprovalWorkflowStatus.REJECTED;

            case "A":
                return ApprovalWorkflowStatus.APPROVED;

            default:
                throw new IllegalArgumentException("ShortName [" + shortName + "] not supported.");
        }
    }
}
