/*
 * Copyright (c) 2025, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.wso2.carbon.identity.workflow.engine.internal.dao;

import java.util.List;

/**
 * ApprovalTaskDAO interface provides methods to manage approval tasks
 */
public interface ApprovalTaskDAO {

    /**
     * Add who approves the relevant request.
     *
     * @param taskId       random generated unique Id.
     * @param eventId      the request ID that need to be checked.
     * @param workflowId   workflow ID.
     * @param approverType the type of the approved user EX: user or Role.
     * @param approverName the value of the approver type.
     * @param taskStatus   state of the tasks [RESERVED, READY or COMPLETED].
     */
    void addApproversOfRequest(String taskId, String eventId, String workflowId, String approverType,
                               String approverName, String taskStatus);

    /**
     * Get taskId from table.
     *
     * @param eventId the request ID that need to be checked.
     * @return task Id.
     */
    String getApproversOfRequest(String eventId);

    /**
     * Delete approver details using task Id.
     *
     * @param taskId random generated unique Id.
     */
    void deleteApproversOfRequest(String taskId);

    /**
     * Add what step to approve.
     *
     * @param eventId     the request ID that need to be checked.
     * @param workflowId  workflow ID.
     */
    void addApprovalTaskStep(String eventId, String workflowId);

    /**
     * Returns the current step given the event ID and workflow ID.
     *
     * @param requestId  the request ID that need to be checked.
     * @param workflowId workflow ID.
     * @return current step value.
     */
    int getCurrentApprovalStepOfWorkflowRequest(String requestId, String workflowId);

    /**
     * Updates a state of request given the event ID, workflow ID and current step.
     *
     * @param eventId     the request ID that need to be checked.
     * @param workflowId  workflow ID.
     * @param currentStep the current step.
     */
    void updateStateOfRequest(String eventId, String workflowId, int currentStep);

    /**
     * Returns the request ID given the task ID.
     *
     * @param taskId random generated unique Id.
     * @return request Id.
     */
    String getWorkflowRequestIdByTaskId(String taskId);


    /**
     * Returns the events list according to the user.
     *
     * @param approverName admin user.
     * @return events list.
     */
    List<String> getRequestsList(String approverName);

    /**
     * Returns the events list according to the user and type.
     *
     * @param approverType entity type.
     * @param approverName entity value.
     * @return events list.
     */
    List<String> getRequestsList(String approverType, String approverName);

    /**
     * Returns the task id list according to the user and type.
     *
     * @param approverType entity type.
     * @param approverName entity value.
     * @return events list.
     */
    List<String> getTaskIDList(String approverType, String approverName);

    /**
     * Returns the task id list according to the user, type and status.
     *
     * @param approverType entity type.
     * @param approverName entity value.
     * @param status       request status
     * @return events list.
     */
    List<String> getTaskIDListByStatus(String approverType, String approverName, String status);

    /**
     * Returns the approval task status given the task ID [RESERVED, READY or COMPLETED].
     *
     * @param taskId the task ID that need to be checked.
     * @return task Status.
     */
    String getApprovalTaskStatus(String taskId);

    /**
     * Update the task status given the task ID.
     *
     * @param taskId     the task ID that need to be checked.
     * @param taskStatus state of the tasks [RESERVED, READY or COMPLETED].
     */
    void updateApprovalTaskStatus(String taskId, String taskStatus);

    /**
     * Returns the approvers list given the authenticated approver name.
     *
     * @param taskId the task ID that need to be checked.
     * @return approvers list.
     */
    List<String> listApprovers(String taskId);

    /**
     * Returns the approver type given the task ID.
     *
     * @param taskId the task ID that need to be checked.
     * @return approver type.
     */
    String getApproverType(String taskId);

    /**
     * Retrieve the task list giving event ID.
     *
     * @param eventId the request ID that need to be checked.
     * @return tasks list.
     */
    List<String> getTaskIds(String eventId);

    /**
     * Retrieve the workflow ID giving task ID.
     *
     * @param taskId the task ID that need to be checked.
     * @return workflow ID.
     */
    String getWorkflowID(String taskId);

}

