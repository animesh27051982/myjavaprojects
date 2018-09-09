package com.flowserve.system606.model;

/**
 *
 * @author kgraves
 */
public enum WorkflowStatus {
    DRAFT("Draft", "Draft", "fa fa-dot-circle-o", "color: grey; margin-right: 3px;"),
    PREPARED("Prepared", "Pending Review", "fa fa-dot-circle-o", "color: yellow; margin-right: 3px;"),
    REVIEWED("Reviewed", "Pending Approval", "fa fa-dot-circle-o", "color: yellow; margin-right: 3px;"),
    REJECTED("Rejected", "Rejected", "fa fa-dot-circle-o", "color: red; margin-right: 3px;"),
    APPROVED("Approved", "Approved", "fa fa-dot-circle-o", "color: green; margin-right: 3px;");

    private String name;
    private String description;
    private String icon;
    private String style;

    private WorkflowStatus(String name, String description, String icon, String style) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.style = style;
    }

    public String getName() {
        return name;
    }

    public String getIcon() {
        return icon;
    }

    public String getDescription() {
        return description;
    }

    public String getStyle() {
        return style;
    }
}
