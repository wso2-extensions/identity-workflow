package org.wso2.carbon.identity.workflow.engine.dto;

/**
 * DTO class for tasks list.
 */
public class TaskSummaryDTO {

    private String id = null;

    private String name = null;

    private String presentationSubject = null;

    private String presentationName = null;

    private String taskType = null;

    /**
     * Represents the various statuses in an approval task's lifecycle.
     */
    public enum StatusEnum {
        READY, RESERVED, COMPLETED, BLOCKED
    }

    private StatusEnum status = null;

    private String priority = null;

    private String createdTimeInMillis = null;

    /**
     * Unique ID to represent an Approval Task
     **/
    public String getId() {

        return id;
    }

    /**
     * Set Unique ID to represent an Approval Task
     **/
    public void setId(String id) {

        this.id = id;
    }

    /**
     * Unique name for the Approval Task
     **/
    public String getName() {

        return name;
    }

    /**
     * Set Unique name for the Approval Task
     **/
    public void setName(String name) {

        this.name = name;
    }

    /**
     * Display value for Approval Operation
     **/
    public String getPresentationSubject() {

        return presentationSubject;
    }

    /**
     * Set display value for Approval Operation
     **/
    public void setPresentationSubject(String presentationSubject) {

        this.presentationSubject = presentationSubject;
    }

    /**
     * Display value for Approval Task
     **/
    public String getPresentationName() {

        return presentationName;
    }

    /**
     * Set display value for Approval Task
     **/
    public void setPresentationName(String presentationName) {

        this.presentationName = presentationName;
    }

    /**
     * Type of the Approval
     **/
    public String getTaskType() {

        return taskType;
    }

    /**
     * Set type of the Approval
     **/
    public void setTaskType(String taskType) {

        this.taskType = taskType;
    }

    /**
     * State of the Approval task
     **/
    public StatusEnum getStatus() {

        return status;
    }

    /**
     * Set state of the Approval task
     **/
    public void setStatus(StatusEnum status) {

        this.status = status;
    }

    /**
     * Priority of the Approval task
     **/
    public String getPriority() {

        return priority;
    }

    /**
     * Set priority of the Approval task
     **/
    public void setPriority(String priority) {

        this.priority = priority;
    }

    /**
     * The time that the operation for approval initiated
     **/
    public String getCreatedTimeInMillis() {

        return createdTimeInMillis;
    }

    /**
     * Set the time that the operation for approval initiated
     **/
    public void setCreatedTimeInMillis(String createdTimeInMillis) {

        this.createdTimeInMillis = createdTimeInMillis;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class TaskSummaryDTO {\n");
        sb.append("  id: ").append(id).append("\n");
        sb.append("  name: ").append(name).append("\n");
        sb.append("  presentationSubject: ").append(presentationSubject).append("\n");
        sb.append("  presentationName: ").append(presentationName).append("\n");
        sb.append("  taskType: ").append(taskType).append("\n");
        sb.append("  status: ").append(status).append("\n");
        sb.append("  priority: ").append(priority).append("\n");
        sb.append("  createdTimeInMillis: ").append(createdTimeInMillis).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
