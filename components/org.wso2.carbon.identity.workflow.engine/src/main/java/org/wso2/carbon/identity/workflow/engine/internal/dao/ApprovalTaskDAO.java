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

import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskSummaryDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApproverDTO;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineServerException;

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
                               String approverName, String taskStatus) throws WorkflowEngineServerException;

    /**
     * Return ths approval task details given the approval task ID.
     *
     * @param approvalTaskId the request ID that need to be checked.
     * @return The approver details corresponding to the approval task ID.
     */
    ApproverDTO getApproverDetailForApprovalTask(String approvalTaskId) throws WorkflowEngineServerException;

    /**
     * Delete approval tasks correspond to the given workflow request ID.
     *
     * @param workflowRequestId The workflow request ID that need to be checked.
     */
    void deleteApprovalTasksOfWorkflowRequest(String workflowRequestId) throws WorkflowEngineServerException;

    /**
     * Add what step to approve.
     *
     * @param eventId    the request ID that need to be checked.
     * @param workflowId workflow ID.
     */
    void addApprovalTaskStep(String eventId, String workflowId) throws WorkflowEngineServerException;

    /**
     * Returns the current step given the event ID and workflow ID.
     *
     * @param requestId  the request ID that need to be checked.
     * @param workflowId workflow ID.
     * @return current step value.
     */
    int getCurrentApprovalStepOfWorkflowRequest(String requestId, String workflowId)
            throws WorkflowEngineServerException;

    /**
     * Updates a state of request given the event ID, workflow ID and current step.
     *
     * @param eventId     the request ID that need to be checked.
     * @param workflowId  workflow ID.
     * @param currentStep the current step.
     */
    void updateStateOfRequest(String eventId, String workflowId, int currentStep) throws WorkflowEngineServerException;

    /**
     * Returns the workflow request ID of the given approval task ID.
     *
     * @param approvalTaskId The approval task ID that need to be checked.
     * @return Workflow request ID.
     */
    String getWorkflowRequestIdByApprovalTaskId(String approvalTaskId) throws WorkflowEngineServerException;

    /**
     * Returns the approval task details given the approver type and name.
     *
     * @param approverType entity type.
     * @param approverName entity value.
     * @return events list.
     */
    List<ApprovalTaskSummaryDTO> getApprovalTaskDetailsList(String approverType, String approverName)
            throws WorkflowEngineServerException;

    /**
     * Returns the approval task details given the approver type, name and status.
     *
     * @param approverType entity type.
     * @param approverName entity value.
     * @param statusList       request status
     * @return events list.
     */
    List<ApprovalTaskSummaryDTO> getApprovalTaskDetailsListByStatus(String approverType, String approverName,
                                                                    List<String> statusList)
            throws WorkflowEngineServerException;

    /**
     * Returns the approval task status given the task ID [RESERVED, READY or COMPLETED].
     *
     * @param taskId the task ID that need to be checked.
     * @return task Status.
     */
    String getApprovalTaskStatus(String taskId) throws WorkflowEngineServerException;

    /**
     * Update the task status given the task ID.
     *
     * @param taskId     the task ID that need to be checked.
     * @param taskStatus state of the tasks [RESERVED, READY or COMPLETED].
     */
    void updateApprovalTaskStatus(String taskId, String taskStatus) throws WorkflowEngineServerException;

    /**
     * Returns the approvers list given the authenticated approver name.
     *
     * @param taskId the task ID that need to be checked.
     * @return approvers list.
     */
    List<String> listApprovers(String taskId) throws WorkflowEngineServerException;

    /**
     * Returns the approver type given the task ID.
     *
     * @param taskId the task ID that need to be checked.
     * @return approver type.
     */
    String getApproverType(String taskId) throws WorkflowEngineServerException;

    /**
     * Retrieve the approval tasks corresponding to a workflow request ID.
     *
     * @param workflowRequestId the workflow request ID that need to be checked.
     * @return approval tasks.
     */
    List<String> getApprovalTasksByWorkflowRequestId(String workflowRequestId) throws WorkflowEngineServerException;

    /**
     * Retrieve the workflow ID giving task ID.
     *
     * @param taskId the task ID that need to be checked.
     * @return workflow ID.
     */
    String getWorkflowID(String taskId) throws WorkflowEngineServerException;

}

