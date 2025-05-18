package org.wso2.carbon.identity.workflow.engine.model;

import java.util.Map;

/**
 * Model Class for task.
 */
public class TaskModel {

    private String id;
    private Map<String, String> assignees;

    /**
     * Unique ID to represent a approval task
     **/
    public String getId() {

        return id;
    }

    /**
     * Set unique ID to represent a approval task
     **/
    public void setId(String id) {

        this.id = id;
    }

    /**
     * To whom the task is assigned:\n  * user - username(s) if the task is reserved for specific user(s).
     * group - role name(s) if the task is assignable for group(s).
     **/
    public Map<String, String> getAssignees() {

        return assignees;
    }

    /**
     * Set to whom the task is assigned:\n  * user - username(s) if the task is reserved for specific user(s).
     * group - role name(s) if the task is assignable for group(s).
     **/
    public void setAssignees(Map<String, String> assignees) {

        this.assignees = assignees;
    }
}
