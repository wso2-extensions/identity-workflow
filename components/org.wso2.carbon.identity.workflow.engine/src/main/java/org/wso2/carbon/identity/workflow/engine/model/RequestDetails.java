package org.wso2.carbon.identity.workflow.engine.model;

/**
 * model class for task subject & task description.
 */
public class RequestDetails {

    String taskDescription;
    String taskSubject;

    /**
     * The subject of the task.
     */
    public String getTaskDescription() {

        return taskDescription;
    }

    /**
     * Set the subject of the task.
     */
    public void setTaskDescription(String taskDescription) {

        this.taskDescription = taskDescription;
    }

    /**
     * The description of the task.
     */
    public String getTaskSubject() {

        return taskSubject;
    }

    /**
     * Set the description of the task.
     */
    public void setTaskSubject(String taskSubject) {

        this.taskSubject = taskSubject;
    }
}
