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

package org.wso2.carbon.identity.workflow.engine;

import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskSummaryDTO;
import org.wso2.carbon.identity.workflow.engine.dto.StateDTO;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineException;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;

import java.util.List;

/**
 * Approval Task Service interface for handling approval tasks.
 * This service provides methods to list, retrieve, and update the state of approval tasks.
 */
public interface ApprovalTaskService {

    /**
     * Search available approval tasks for the current authenticated user.
     *
     * @param limit  number of records to be returned.
     * @param offset start page.
     * @param status state of the tasks [RESERVED, READY or COMPLETED].
     * @return ApprovalTaskSummaryDTO list.
     */
    List<ApprovalTaskSummaryDTO> listApprovalTasks(Integer limit, Integer offset, List<String> status)
            throws WorkflowEngineException;

    /**
     * Get details of a task identified by the taskId.
     *
     * @param taskId the unique ID.
     * @return ApprovalTaskDTO object.
     */
    ApprovalTaskDTO getApprovalTaskByTaskId(String taskId) throws WorkflowEngineException;

    /**
     * Update the state of an approval task. User can get assigned a task by claiming or releasing an already assigned
     * task to himself or user can approve or reject a task.
     *
     * @param approvalTaskId    the unique ID to update the state.
     * @param nextState The next status of the approval task.
     */
    void updateApprovalTaskStatus(String approvalTaskId, StateDTO nextState) throws WorkflowEngineException;

    /**
     * Add approval tasks for a workflow request.
     *
     * @param workflowRequest the workflow request to which the approval tasks are added.
     * @param parameterList   list of parameters associated with the workflow request.
     */
    void addApprovalTasksForWorkflowRequest(WorkflowRequest workflowRequest, List<Parameter> parameterList)
            throws WorkflowEngineException;

    /**
     * Delete pending approval tasks for a given workflow ID.
     *
     * @param workflowId the ID of the workflow for which pending approval tasks are to be deleted.
     * @throws WorkflowEngineException if an error occurs while deleting pending approval tasks or if the workflow ID
     *                                 is invalid.
     */
    void deletePendingApprovalTasks(String workflowId) throws WorkflowEngineException;

    /**
     * Update the pending approval tasks when there is a change in the workflow associated with a request.
     *
     * @param workflowId the ID of the old workflow.
     * @param newWorkflowParams the parameters of the new workflow.
     * @param oldWorkflowParams the parameters of the old workflow.
     * @throws WorkflowEngineException if an error occurs while updating approval tasks or if the workflow IDs are
     *                                 invalid.
     */
    void updatePendingApprovalTasksOnWorkflowUpdate(String workflowId, List<Parameter> newWorkflowParams,
                                                    List<Parameter> oldWorkflowParams) throws WorkflowEngineException;
}
