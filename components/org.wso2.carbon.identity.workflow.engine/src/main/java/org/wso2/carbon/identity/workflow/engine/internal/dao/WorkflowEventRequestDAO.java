package org.wso2.carbon.identity.workflow.engine.internal.dao;

import java.sql.Timestamp;
import java.util.List;

/**
 * Perform CRUD operations for workflow Event Request properties.
 */
public interface WorkflowEventRequestDAO {

    /**
     * Add who approves the relevant request with unique task ID, event ID, workflow ID, approverType, approverName and
     * taskStatus.
     *
     * @param taskId       random generated unique Id.
     * @param eventId      the request ID that need to be checked.
     * @param workflowId   workflow ID.
     * @param approverType the type of the approved user EX: user or Role.
     * @param approverName the value of the approver type.
     * @param taskStatus   state of the tasks [RESERVED, READY or COMPLETED].
     */
    void addApproverDetailsOfEvent(String taskId, String eventId, String workflowId, String approverType,
                                   String approverName, String taskStatus);

    /**
     * Get taskId from table.
     *
     * @param eventId the request ID that need to be checked.
     * @return task Id.
     */
    String getTaskIDOfEvent(String eventId);

    /**
     * Delete approver details using task Id.
     *
     * @param taskId random generated unique Id.
     */
    void deleteApproversOfTask(String taskId);

    /**
     * Add what step to approve.
     *
     * @param eventId     the request ID that need to be checked.
     * @param workflowId  workflow ID.
     * @param currentStep the current step.
     */
    void createCurrentStepOfEvent(String eventId, String workflowId, int currentStep);

    /**
     * Returns the current step given the event ID and workflow ID.
     *
     * @param eventId    the request ID that need to be checked.
     * @param workflowId workflow ID.
     * @return current step value.
     */
    int getCurrentStepOfEvent(String eventId, String workflowId);

    /**
     * Updates a state of request given the event ID, workflow ID and current step.
     *
     * @param eventId     the request ID that need to be checked.
     * @param workflowId  workflow ID.
     * @param currentStep the current step.
     */
    void updateCurrentStepOfEvent(String eventId, String workflowId, int currentStep);

    /**
     * Returns the request ID given the task ID.
     *
     * @param taskId random generated unique Id.
     * @return request Id.
     */
    String getRequestIDFromTask(String taskId);

    /**
     * Returns the initiator given the request ID.
     *
     * @param requestId the request ID that need to be checked.
     * @return string initiator.
     */
    String getInitiatedUserOfRequest(String requestId);

    /**
     * Retrieve the role id list giving the username.
     *
     * @param userName the username that need to be checked.
     * @return role ID list.
     */
    List<Integer> getRolesIDOfUser(String userName);

    /**
     * Get the role name giving the role ID.
     *
     * @param roleId the roleID that need to be checked.
     * @return role name list.
     */
    List<String> getRoleNamesOfRoleID(int roleId);

    /**
     * Returns the events list according to the user.
     *
     * @param approverName admin user.
     * @return events list.
     */
    List<String> getEventsListOfApprover(String approverName);

    /**
     * Returns the event type given the request ID.
     *
     * @param eventId the request ID that need to be checked.
     * @return event type of the request.
     */
    String getEventTypeOfEvent(String eventId);

    /**
     * Returns the task status given the task ID [RESERVED, READY or COMPLETED].
     *
     * @param taskId the task ID that need to be checked.
     * @return task Status.
     */
    String getTaskStatusOfTask(String taskId);

    /**
     * Update the task status given the task ID.
     *
     * @param taskId the task ID that need to be checked.
     * @param taskStatus state of the tasks [RESERVED, READY or COMPLETED].
     */
    void updateStatusOfTask(String taskId, String taskStatus);

    /**
     * Returns the created time of the request.
     *
     * @param requestId the request ID that need to be checked.
     * @return the created time.
     */
    Timestamp getCreatedAtTimeInMill(String requestId);

    /**
     * Returns the relationship ID given the request ID.
     *
     * @param requestId the event ID that need to be checked.
     * @return the relationship ID.
     */
    String getRelationshipId(String requestId);

    /**
     * Returns the approvers list given the taskId.
     *
     * @param taskId the task ID that need to be checked.
     * @return approvers list.
     */
    List<String> listApprovers(String taskId);

    /**
     * Returns the task status given the request ID [RESERVED, READY or COMPLETED].
     *
     * @param requestId the request ID that need to be checked.
     * @return task status.
     */
    String getStatusOfRequest(String requestId);

    /**
     * Retrieve the tasks list giving event ID.
     *
     * @param eventId the request ID that need to be checked.
     * @return tasks list.
     */
    List<String> getTaskId(String eventId);

    /**
     * Delete the current step using giving event ID.
     *
     * @param eventId the request ID that need to be checked.
     */
    void deleteCurrentStepOfRequest(String eventId);

    /**
     * Retrieve the workflow ID giving task ID.
     *
     * @param taskId the task ID that need to be checked.
     * @return workflow ID.
     */
    String getWorkflowID(String taskId);

    /**
     * Retrieve the workflow name giving workflow ID.
     *
     * @param workflowID workflow ID.
     * @return workflow definition name.
     */
    String getWorkflowName(String workflowID);

    /**
     * Retrieve the entity name giving request ID.
     *
     * @param requestID the request ID that need to be checked.
     * @return entity name of the request
     */
    String getEntityNameOfRequest(String requestID);

    /**
     * Retrieve the task giving taskID.
     *
     * @param taskId the task ID that need to be checked.
     * @return task.
     */
    String getTask(String taskId);
}
