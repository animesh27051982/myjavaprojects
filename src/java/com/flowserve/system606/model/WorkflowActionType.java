package com.flowserve.system606.model;

public enum WorkflowActionType {
    CREATE("Create"),
    INITIALIZE("Initialize"),
    UPDATE("Update"),
    REQUEST_REVIEW("Request Review"),
    REQUEST_APPROVAL("Request Approval"),
    REVIEW("Review"),
    APPROVE("Approve"),
    REJECT("Reject"),
    REMOVE("Remove"),
    CANCEL("Cancel"),
    CLOSE("Close");

    private String name;

    private WorkflowActionType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
