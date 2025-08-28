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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementServiceImpl;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskRelationDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskSummaryDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApproverDTO;
import org.wso2.carbon.identity.workflow.engine.dto.PropertyDTO;
import org.wso2.carbon.identity.workflow.engine.dto.StateDTO;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineClientException;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineException;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineServerException;
import org.wso2.carbon.identity.workflow.engine.internal.WorkflowEngineServiceDataHolder;
import org.wso2.carbon.identity.workflow.engine.internal.dao.ApprovalTaskDAO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.WorkflowRequestDAO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.impl.ApprovalTaskDAOImpl;
import org.wso2.carbon.identity.workflow.engine.internal.dao.impl.WorkflowRequestDAOImpl;
import org.wso2.carbon.identity.workflow.engine.model.TaskModel;
import org.wso2.carbon.identity.workflow.engine.util.Utils;
import org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.callback.WSWorkflowCallBackService;
import org.wso2.carbon.identity.workflow.mgt.callback.WSWorkflowResponse;
import org.wso2.carbon.identity.workflow.mgt.dto.Association;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowDataType;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.DISPLAY_NAME_PROPERTY;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.CLAIMS_PROPERTY_NAME;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.ENTITY_TYPE_CLAIMED_USERS;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.ENTITY_TYPE_USERS;

/**
 * Implementation of the ApprovalTaskService interface.
 */
public class ApprovalTaskServiceImpl implements ApprovalTaskService {

    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final Integer LIMIT = 20;
    private static final Integer OFFSET = 0;
    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskServiceImpl.class);

    private final ApprovalTaskDAO approvalTaskDAO = new ApprovalTaskDAOImpl();
    private final WorkflowRequestDAO workflowRequestDAO = new WorkflowRequestDAOImpl();
    private final WSWorkflowCallBackService wsWorkflowCallBackService = new WSWorkflowCallBackService();
    private final ClaimMetadataManagementServiceImpl claimMetadataManagementService =
            new ClaimMetadataManagementServiceImpl();

    private static final String ROLE_ID_PARAM_NAME = "Role ID";
    private static final String ROLE_NAME_PARAM_NAME = "Role Name";
    private static final String USERS_TO_BE_ADDED_PARAM_NAME = "Users to be Added";
    private static final String USERS_TO_BE_DELETED_PARAM_NAME = "Users to be Deleted";
    private static final String ROLE_ASSOCIATED_APPLICATION_PARAM_NAME = "Role Associated Application";
    private static final String TENANT_DOMAIN_PARAM_NAME = "Tenant Domain";
    private static final String AUDIENCE_ID_PARAM_NAME = "Audience ID";
    private static final String COMMA_SEPARATOR = ",";
    private static final String ROLE_NOT_FOUND_ERROR_CODE = "RMA-60007";

    @Override
    public List<ApprovalTaskSummaryDTO> listApprovalTasks(Integer limit, Integer offset, List<String> statusList)
            throws WorkflowEngineException {

        if (limit == null || limit < 0) {
            limit = LIMIT;
        }
        if (offset == null || offset < 0) {
            offset = OFFSET;
        }

        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();

        List<ApprovalTaskSummaryDTO> approvalTaskSummaryDTOS = getAllAssignedTasks(statusList, userId, limit, offset);
        // Filter the reserved workflow requests to filter out the BLOCKED tasks corresponding to the same request.
        List<String> reservedWorkflowRequests = approvalTaskSummaryDTOS.stream()
                .filter(approvalTask -> WorkflowEngineConstants.TaskStatus.RESERVED.name()
                        .equals(approvalTask.getApprovalStatus())).map(ApprovalTaskSummaryDTO::getRequestId)
                .collect(Collectors.toList());
        Set<String> processedRequestIds = new HashSet<>();
        Iterator<ApprovalTaskSummaryDTO> iterator = approvalTaskSummaryDTOS.iterator();
        while (iterator.hasNext()) {
            ApprovalTaskSummaryDTO approvalTaskSummaryDTO = iterator.next();
            if (processedRequestIds.contains(approvalTaskSummaryDTO.getRequestId())) {
                iterator.remove();
                continue;
            }
            /* The tasks with BLOCKED state where the corresponding workflow request already has a RESERVED task should
               be skipped to avoid duplication in the list. */
            if (reservedWorkflowRequests.contains(approvalTaskSummaryDTO.getRequestId()) &&
                    WorkflowEngineConstants.TaskStatus.BLOCKED.name()
                            .equals(approvalTaskSummaryDTO.getApprovalStatus())) {
                iterator.remove();
                continue;
            }

            /* If the task is in APPROVED state, skip adding it to the processedRequestIds set as there can be tasks in
               READY / RESERVED state for the same workflow request when it is a multistep approval process. */
            if (!WorkflowEngineConstants.TaskStatus.APPROVED.name()
                    .equals(approvalTaskSummaryDTO.getApprovalStatus())) {
                processedRequestIds.add(approvalTaskSummaryDTO.getRequestId());
            }

            WorkflowRequest request = getWorkflowRequest(approvalTaskSummaryDTO.getRequestId());

            String eventType = request.getEventType();

            String workflowID = approvalTaskDAO.getWorkflowID(approvalTaskSummaryDTO.getId());
            String workflowAssociationName = findAssociationNameByWorkflowAndEvent(workflowID, eventType);

            Timestamp createdTime = workflowRequestDAO.getCreatedAtTimeInMill(request.getUuid());
            approvalTaskSummaryDTO.setId(approvalTaskSummaryDTO.getId());
            approvalTaskSummaryDTO.setName(workflowAssociationName);
            approvalTaskSummaryDTO.setTaskType(eventType);
            approvalTaskSummaryDTO.setCreatedTimeInMillis(String.valueOf(createdTime.getTime()));
            approvalTaskSummaryDTO.setPriority(WorkflowEngineConstants.ParameterName.PRIORITY);
            approvalTaskSummaryDTO.setApprovalStatus(approvalTaskSummaryDTO.getApprovalStatus());
        }

        return approvalTaskSummaryDTOS.subList(Math.min(offset, approvalTaskSummaryDTOS.size()),
                Math.min(offset + limit, approvalTaskSummaryDTOS.size()));
    }

    @Override
    public ApprovalTaskDTO getApprovalTaskByTaskId(String taskId) throws WorkflowEngineException {

        taskId = taskId.trim();
        String requestId = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(taskId);
        WorkflowRequest request = getWorkflowRequest(requestId);
        String initiator = workflowRequestDAO.getInitiatedUser(requestId);
        List<String> approvers = approvalTaskDAO.listApprovers(taskId);
        Map<String, String> assigneeMap = new HashMap<>();
        for (String assignee : approvers) {
            assigneeMap.put(WorkflowEngineConstants.ParameterName.ASSIGNEE_TYPE, assignee);
        }
        List<PropertyDTO> properties = getRequestParameters(request);
        ApprovalTaskDTO approvalTaskDTO = new ApprovalTaskDTO();
        approvalTaskDTO.setId(taskId);
        String statusValue = approvalTaskDAO.getApprovalTaskStatus(taskId);
        approvalTaskDTO.setApprovalStatus(statusValue);
        approvalTaskDTO.setInitiator(initiator);
        approvalTaskDTO.setPriority(WorkflowEngineConstants.ParameterName.PRIORITY);
        TaskModel taskModel = new TaskModel();
        taskModel.setAssignees(assigneeMap);
        approvalTaskDTO.setAssignees(getPropertyDTOs(taskModel.getAssignees()));
        approvalTaskDTO.setProperties(properties);
        return approvalTaskDTO;
    }

    @Override
    public void updateApprovalTaskStatus(String approvalTaskId, StateDTO nextState) throws WorkflowEngineException {

        validateApprovers(approvalTaskId);

        switch (nextState.getAction()) {
            case APPROVE:
                handleApproval(approvalTaskId);
                break;
            case REJECT:
                handleReject(approvalTaskId);
                break;
            case RELEASE:
                handleRelease(approvalTaskId);
                break;
            case CLAIM:
                handleClaim(approvalTaskId);
                break;
            default:
                throw new WorkflowEngineClientException(
                        WorkflowEngineConstants.ErrorMessages.USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE.
                                getCode(),
                        WorkflowEngineConstants.ErrorMessages.USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE.
                                getDescription());
        }
    }

    @Override
    public void addApprovalTasksForWorkflowRequest(WorkflowRequest workflowRequest, List<Parameter> parameterList)
            throws WorkflowEngineException {

        if (CollectionUtils.isEmpty(parameterList)) {
            return;
        }

        String workflowRequestId = getWorkflowRequestId(workflowRequest);
        /* The workflow parameter list has the workflow ID for each property object. Retrieve the workflow ID from
           the first. */
        String workflowId = parameterList.get(0).getWorkflowId();
        String approverType;

        int currentStep = approvalTaskDAO.getCurrentApprovalStepOfWorkflowRequest(workflowRequestId, workflowId);
        if (currentStep == 0) {
            approvalTaskDAO.addApprovalTaskStep(workflowRequestId, workflowId);
            currentStep = 1;
        } else {
            currentStep += 1;
            approvalTaskDAO.updateStateOfRequest(workflowRequestId, workflowId, currentStep);
        }

        for (Parameter parameter : parameterList) {
            if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                String[] stepName = parameter.getqName().split("-");
                int step = Integer.parseInt(stepName[1]);
                if (currentStep == step) {
                    approverType = stepName[stepName.length - 1];
                    String approverIdentifiers = parameter.getParamValue();
                    if (approverIdentifiers != null && !approverIdentifiers.isEmpty()) {
                        String[] approverIdentifierList = approverIdentifiers.split(COMMA_SEPARATOR, 0);
                        for (String approverIdentifier : approverIdentifierList) {
                            String taskId = UUID.randomUUID().toString();
                            approvalTaskDAO.addApproversOfRequest(taskId, workflowRequestId, workflowId,
                                    approverType,
                                    approverIdentifier, WorkflowEngineConstants.TaskStatus.READY.toString());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void deletePendingApprovalTasks(String workflowId) throws WorkflowEngineException {

        if (StringUtils.isBlank(workflowId)) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.WORKFLOW_ID_NOT_FOUND.getCode(),
                    WorkflowEngineConstants.ErrorMessages.WORKFLOW_ID_NOT_FOUND.getDescription());
        }
        approvalTaskDAO.deletePendingApprovalTasks(workflowId);
    }

    @Override
    public void updateApprovalTasksOnWorkflowUpdate(String workflowId, List<Parameter> newWorkflowParams,
                                                    List<Parameter> oldWorkflowParams) throws WorkflowEngineException {

        // Get the list of pending requests corresponding to given workflow ID.
        List<String> requestList = approvalTaskDAO.getPendingRequestsByWorkflowId(workflowId);

        // APPROVER_NAME list for each step
        Map<Integer, List<String>> newParamValuesForApprovalSteps =
                Utils.getParamValuesForApprovalSteps(newWorkflowParams);
        // Get the modified steps.
        List<Integer> modifiedSteps = Utils.getModifiedApprovalSteps(newWorkflowParams, oldWorkflowParams);

        // Retrieving the tenant domain to validate reserved task users.
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        // For each request, delete the existing approval tasks and
        // add new tasks based on the updated workflow parameters.
        for (String requestId : requestList) {
            int currentStep = approvalTaskDAO.getCurrentApprovalStepOfWorkflowRequest(requestId, workflowId);

            // Check if the request has been affected by the workflow update.
            // First check if the current step is one of the modified steps.
            if (!modifiedSteps.contains(currentStep)) {
                // If not, no need to change the approval tasks for this request.
                continue;
            }

            // Get the tasks corresponding to the request ID with status READY, BLOCKED or RESERVED.
            List<ApprovalTaskRelationDTO> approvalTaskRelationDTOS =
                    approvalTaskDAO.getApprovalTaskRelationsByRequestId(requestId);

            // Get reserved task in the task list if exists.
            ApprovalTaskRelationDTO reservedTask =
                    approvalTaskRelationDTOS.stream()
                            .filter(dto -> "RESERVED".equals(dto.getTaskStatus()))
                            .findFirst()
                            .orElse(null);

            // Delete existing pending approval tasks.
            approvalTaskDAO.deletePendingApprovalTasks(requestId);
            // Update the current step to (step-1) to reset the pending step.
            approvalTaskDAO.updateStateOfRequest(requestId, workflowId, currentStep - 1);
            // Get corresponding workflow request.
            WorkflowRequest request = getWorkflowRequest(requestId);
            // Add new approval tasks based on updated workflow parameters.
            addApprovalTasksForWorkflowRequest(request, newWorkflowParams);

            if (reservedTask != null) {
                /*
                If there is a RESERVED task, need to re-perform the reservation for the same user.
                The reservation is made successfully for the request if the user is valid for the new workflow as well.
                 */
                String userId = reservedTask.getApproverName();

                // Get the new workflow APPROVER_NAME list for the current step.
                List<String> approverNamesForCurrentStep =
                        newParamValuesForApprovalSteps.get(currentStep);
                // Get the user's roles.
                List<String> entityIds = getAssignedRoleIds(userId, tenantDomain);
                // Add userId as eligible entity if the workflow has USER.
                entityIds.add(userId);

                // Check if the user is still eligible to approve the request,
                // by checking if any entityId is present in the approverNamesForCurrentStep.
                for (String entityId : entityIds) {
                    if (approverNamesForCurrentStep != null && approverNamesForCurrentStep.contains(entityId)) {
                        // Get the tasks respect to the request ID with status 'READY'.
                        List<ApprovalTaskRelationDTO> approvalTasks =
                                approvalTaskDAO.getApprovalTaskRelationsByRequestId(requestId);

                        // Get the task id with the entityId.
                        String taskId = approvalTasks.stream()
                                .filter(dto -> entityId.equals(dto.getApproverName()) && "READY".equals(dto
                                        .getTaskStatus()))
                                .map(ApprovalTaskRelationDTO::getTaskId)
                                .findFirst()
                                .orElse(null);

                        // If a task is found, perform reservation.
                        if (taskId != null) {
                            handleClaim(taskId);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Retrieves all the approval tasks assigned to the user.
     *
     * @param statusList List of task statuses to filter by (e.g., READY, RESERVED).
     * @param userId The ID of the user whose related task IDs should be retrieved.
     * @return List of task IDs assigned to the user, filtered by the specified statuses.
     */
    private List<ApprovalTaskSummaryDTO> getAllAssignedTasks(List<String> statusList, String userId, int limit,
                                                             int offset) throws WorkflowEngineException {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        List<String> entityIds = new ArrayList<>();
        entityIds.add(userId);

        List<String> roleIds = getAssignedRoleIds(userId, tenantDomain);
        entityIds.addAll(roleIds);
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        if (statusList == null || statusList.isEmpty()) {
            return approvalTaskDAO.getApprovalTaskDetailsList(entityIds, limit, offset, tenantId);
        } else {
            return approvalTaskDAO.getApprovalTaskDetailsListByStatus(entityIds, statusList, limit, offset, tenantId);
        }
    }

    private List<String> getAssignedRoleIds(String userId, String tenantDomain) throws WorkflowEngineException {


        String orgId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
        String userResidentOrgId = PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getUserResidentOrganizationId();
        if (!StringUtils.equals(orgId, userResidentOrgId)) {
            try {
                Optional<String> optionalUserId = OrganizationSharedUserUtil
                        .getUserIdOfAssociatedUserByOrgId(userId, orgId);
                if (optionalUserId.isPresent()) {
                    userId = optionalUserId.get();
                }
            } catch (OrganizationManagementException e) {
                throw new WorkflowEngineException(
                        WorkflowEngineConstants.ErrorMessages.ERROR_RETRIEVING_ASSOCIATED_USER_ID.getDescription(), e);
            }
        }
        try {
            List<String> roleIDList = WorkflowEngineServiceDataHolder.getInstance().getRoleManagementService().
                    getRoleIdListOfUser(userId, tenantDomain);
            return new ArrayList<>(roleIDList);
        } catch (IdentityRoleManagementException e) {
            throw new WorkflowEngineException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVAL_TASKS_FOR_USER.
                            getDescription(), e);
        }

    }

    private WorkflowRequest getWorkflowRequest(String requestId) throws WorkflowEngineException {

        try {
            return WorkflowEngineServiceDataHolder.getInstance().getWorkflowManagementService()
                    .getWorkflowRequest(requestId);
        } catch (WorkflowException e) {
            throw new WorkflowEngineException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_WORKFLOW_REQUEST.getCode(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_WORKFLOW_REQUEST.
                            getDescription());
        }
    }


    private void validateApprovers(String taskId) throws WorkflowEngineException {

        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        boolean isAssignedApprovalTask = false;
        ApproverDTO approverDTO = approvalTaskDAO.getApproverDetailForApprovalTask(taskId);
        if (ENTITY_TYPE_USERS.equals(approverDTO.getApproverType()) ||
                ENTITY_TYPE_CLAIMED_USERS.equals(approverDTO.getApproverType())) {
            if (approverDTO.getApproverName().equals(userId)) {
                isAssignedApprovalTask = true;
            }
        } else {
            List<String> roleIds = getAssignedRoleIds(userId, tenantDomain);
            if (roleIds.contains(approverDTO.getApproverName())) {
                isAssignedApprovalTask = true;
            }
        }
        if (!isAssignedApprovalTask) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_APPROVAL_TASK_IS_NOT_ASSIGNED.getCode(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_APPROVAL_TASK_IS_NOT_ASSIGNED.
                            getDescription());
        }
        if (WorkflowEngineConstants.TaskStatus.BLOCKED.name().equals(approvalTaskDAO.getApprovalTaskStatus(taskId))) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getCode(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getDescription());
        }
    }

    private int getNumberOfApprovalStepsFromWorkflowParameters(List<Parameter> workflowParameterList) {

        int maxStep = 0;

        for (Parameter parameter : workflowParameterList) {
            if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                String value = parameter.getqName();
                if (value != null && !value.isEmpty()) {
                    String[] parts = value.split("-");
                    if (parts.length > 1) {
                        int step = Integer.parseInt(parts[1]);
                        if (step > maxStep) {
                            maxStep = step;
                        }
                    }
                }
            }
        }
        return maxStep;
    }


    private void handleApproval(String approvalTaskId) throws WorkflowEngineException {

        String workflowRequestId = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(approvalTaskId);
        String workflowId = approvalTaskDAO.getWorkflowID(approvalTaskId);

        handleApprovalTaskCompletion(approvalTaskId, workflowRequestId, ApprovalTaskServiceImpl.APPROVED);

        int stepValue = approvalTaskDAO.getCurrentApprovalStepOfWorkflowRequest(workflowRequestId, workflowId);
        List<Parameter> approvalWorkflowParameterList = getApprovalWorkflowParameters(workflowId);

        /* If the current step value is less than the total number of approval steps defined in the workflow
           parameters, then we need to add more approval tasks for the next step. Otherwise,
           we can complete the workflow request with an approved status. */
        if (stepValue < getNumberOfApprovalStepsFromWorkflowParameters(approvalWorkflowParameterList)) {
            WorkflowRequest workflowRequest = buildWorkflowRequest(workflowRequestId);
            addApprovalTasksForWorkflowRequest(workflowRequest, approvalWorkflowParameterList);
        } else {
            completeWorkflowRequest(workflowRequestId, ApprovalTaskServiceImpl.APPROVED);
        }
    }

    private static WorkflowRequest buildWorkflowRequest(String workflowRequestId) {

        WorkflowRequest workflowRequest = new WorkflowRequest();
        RequestParameter requestParameter = new RequestParameter();
        requestParameter.setName(WorkflowEngineConstants.ParameterName.REQUEST_ID);
        requestParameter.setValue(workflowRequestId);
        workflowRequest.setRequestParameters(Collections.singletonList(requestParameter));
        workflowRequest.setUuid(workflowRequestId);
        return workflowRequest;
    }

    private void handleReject(String approvalTaskId) throws WorkflowEngineServerException {

        String workflowRequestId = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(approvalTaskId);
        handleApprovalTaskCompletion(approvalTaskId, workflowRequestId, ApprovalTaskServiceImpl.REJECTED);
        completeWorkflowRequest(workflowRequestId, REJECTED);
    }

    private void handleRelease(String taskId) throws WorkflowEngineServerException {

        String readyStatus = WorkflowEngineConstants.TaskStatus.READY.toString();
        String requestID = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(taskId);
        String approverType = approvalTaskDAO.getApproverType(taskId);
        List<String> taskIds = approvalTaskDAO.getApprovalTasksByWorkflowRequestId(requestID);

        for (String id : taskIds) {
            approvalTaskDAO.updateApprovalTaskStatus(id, readyStatus);

            if (taskId.equals(id) && ENTITY_TYPE_CLAIMED_USERS.equals(approverType)) {
                approvalTaskDAO.deleteApprovalTasksOfWorkflowRequest(id);
            }
        }
    }

    private void handleClaim(String updatedApprovalTaskId) throws WorkflowEngineException {

        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();
        String reservedStatus = WorkflowEngineConstants.TaskStatus.RESERVED.toString();
        String blockedStatus = WorkflowEngineConstants.TaskStatus.BLOCKED.toString();

        if (blockedStatus.equals(approvalTaskDAO.getApprovalTaskStatus(updatedApprovalTaskId))) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getCode(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getDescription());
        }

        String workflowRequestID = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(updatedApprovalTaskId);
        String approverType = approvalTaskDAO.getApproverType(updatedApprovalTaskId);
        String workflowId = approvalTaskDAO.getWorkflowID(updatedApprovalTaskId);
        List<String> existingApprovalTasks = approvalTaskDAO.getApprovalTasksByWorkflowRequestId(workflowRequestID);

        for (String existingApprovalTaskId : existingApprovalTasks) {
            if (existingApprovalTaskId.equals(updatedApprovalTaskId)) {
                if (WorkflowEngineConstants.APPROVER_TYPE_USERS.equals(approverType)) {
                    approvalTaskDAO.updateApprovalTaskStatus(updatedApprovalTaskId, reservedStatus);
                } else if (WorkflowEngineConstants.APPROVER_TYPE_ROLES.equals(approverType)) {
                    // Create a new task for the user who claimed the task.
                    String newTaskId = UUID.randomUUID().toString();
                    approvalTaskDAO.addApproversOfRequest(newTaskId, workflowRequestID, workflowId,
                            ENTITY_TYPE_CLAIMED_USERS, userId, reservedStatus);

                    // Update the status of the existing task to BLOCKED.
                    approvalTaskDAO.updateApprovalTaskStatus(updatedApprovalTaskId, blockedStatus);
                }
            } else {
                // Update the status of all existing tasks to BLOCKED.
                approvalTaskDAO.updateApprovalTaskStatus(existingApprovalTaskId, blockedStatus);
            }
        }
    }

    private void completeWorkflowRequest(String workflowRequestId, String status) throws WorkflowEngineServerException {

        WSWorkflowResponse wsWorkflowResponse = new WSWorkflowResponse();
        String relationshipId = workflowRequestDAO.getRelationshipId(workflowRequestId);
        wsWorkflowResponse.setUuid(relationshipId);
        wsWorkflowResponse.setStatus(status);
        wsWorkflowCallBackService.onCallback(wsWorkflowResponse);
    }

    private List<Parameter> getApprovalWorkflowParameters(String workflowId) throws WorkflowEngineException {

        try {
            return WorkflowEngineServiceDataHolder.getInstance().getWorkflowManagementService().
                    getWorkflowParameters(workflowId);
        } catch (WorkflowException e) {
            throw new WorkflowEngineException(WorkflowEngineConstants.ErrorMessages
                    .ERROR_OCCURRED_WHILE_RETRIEVING_PARAMETER_LIST.getDescription(), e);
        }
    }

    private List<PropertyDTO> getRequestParameters(WorkflowRequest workflowRequest) throws WorkflowEngineException {

        List<PropertyDTO> workflowRequestProperties = new ArrayList<>();

        for (RequestParameter param : workflowRequest.getRequestParameters()) {
            if (param.getName().equals(WorkflowEngineConstants.ParameterName.CREDENTIAL)) {
                continue;
            }
            Object value = param.getValue();
            if (value != null) {
                String valueString = value.toString().trim();
                String paramString = param.getName().trim();
                if (ROLE_ID_PARAM_NAME.equals(param.getName())) {
                    String tenantDomain = IdentityTenantUtil.getTenantDomain(workflowRequest.getTenantId());
                    try {
                        paramString = ROLE_NAME_PARAM_NAME;
                        RoleBasicInfo roleBasicInfo = WorkflowEngineServiceDataHolder.getInstance()
                                .getRoleManagementService().getRoleBasicInfoById(valueString, tenantDomain);
                        valueString = roleBasicInfo.getName();
                        if (RoleConstants.APPLICATION.equals(roleBasicInfo.getAudience())) {
                            PropertyDTO propertyDTO = new PropertyDTO();
                            propertyDTO.setKey(ROLE_ASSOCIATED_APPLICATION_PARAM_NAME);
                            propertyDTO.setValue(roleBasicInfo.getAudienceName());
                            workflowRequestProperties.add(propertyDTO);
                        }
                    } catch (IdentityRoleManagementException e) {
                        if (StringUtils.equals(ROLE_NOT_FOUND_ERROR_CODE, e.getErrorCode())) {
                            valueString = StringUtils.EMPTY;
                        } else {
                            throw new WorkflowEngineException(e.getMessage(), e);
                        }
                    }
                } else if (USERS_TO_BE_ADDED_PARAM_NAME.equals(paramString)
                        || USERS_TO_BE_DELETED_PARAM_NAME.equals(paramString)) {
                    try {
                        AbstractUserStoreManager userStoreManager =
                                (AbstractUserStoreManager) WorkflowEngineServiceDataHolder.getInstance()
                                .getRealmService().getTenantUserRealm(workflowRequest.getTenantId())
                                        .getUserStoreManager();
                        if (value instanceof List) {
                            List<String> userNames = userStoreManager.getUserNamesFromUserIDs((List<String>) value);
                            if (CollectionUtils.isNotEmpty(userNames)) {
                                valueString = String.join(COMMA_SEPARATOR, userNames);
                            } else {
                                valueString = StringUtils.EMPTY;
                            }
                        }
                    } catch (UserStoreException e) {
                        throw new WorkflowEngineException(e.getMessage(), e);
                    }
                } else if (CLAIMS_PROPERTY_NAME.equals(paramString)) {
                    if (WorkflowDataType.STRING_STRING_MAP_TYPE.equals(param.getValueType()) &&
                            param.getValue() != null) {
                        Map<String, String> claimsMap = (Map<String, String>) param.getValue();

                        List<LocalClaim> localClaims;
                        try {
                            localClaims = claimMetadataManagementService.getLocalClaims(
                                    CarbonContext.getThreadLocalCarbonContext().getTenantDomain());
                        } catch (ClaimMetadataException e) {
                            log.error("Error while retrieving local claims for tenant: {}",
                                    CarbonContext.getThreadLocalCarbonContext().getTenantDomain(), e);
                            continue;
                        }

                        for (Map.Entry<String, String> entry : claimsMap.entrySet()) {
                            String claimUri = entry.getKey();
                            String claimValue = entry.getValue();
                            String displayName = localClaims.stream()
                                    .filter(localClaim -> localClaim.getClaimURI().equals(claimUri))
                                    .map(localClaim -> localClaim.getClaimProperty(DISPLAY_NAME_PROPERTY))
                                    .findFirst()
                                    .orElse(claimUri);
                            PropertyDTO propertyDTO = new PropertyDTO();
                            propertyDTO.setKey(displayName);
                            propertyDTO.setValue(claimValue);
                            workflowRequestProperties.add(propertyDTO);
                        }

                    }
                } else if (TENANT_DOMAIN_PARAM_NAME.equals(paramString)) {
                    // Skip these parameters as they are not required in the task parameters.
                    continue;
                }
                PropertyDTO propertyDTO = new PropertyDTO();
                propertyDTO.setKey(paramString);
                propertyDTO.setValue(valueString);
                workflowRequestProperties.add(propertyDTO);
            }
        }
        return workflowRequestProperties;
    }

    private List<PropertyDTO> getPropertyDTOs(Map<String, String> props) {

        return props.entrySet().stream().map(p -> getPropertyDTO(p.getKey(), p.getValue()))
                .collect(Collectors.toList());
    }

    private PropertyDTO getPropertyDTO(String key, String value) {

        PropertyDTO prop = new PropertyDTO();
        prop.setKey(key);
        prop.setValue(value);
        return prop;
    }

    private String getWorkflowRequestId(WorkflowRequest request) {

        List<RequestParameter> requestParameter;
        for (int i = 0; i < request.getRequestParameters().size(); i++) {
            requestParameter = request.getRequestParameters();
            if (requestParameter.get(i).getName().equals(WorkflowEngineConstants.ParameterName.REQUEST_ID)) {
                return (String) requestParameter.get(i).getValue();
            }
        }
        return null;
    }

    private String findAssociationNameByWorkflowAndEvent(String workflowID, String eventType) {

        try {
            return WorkflowEngineServiceDataHolder.getInstance().getWorkflowManagementService()
                    .getAssociationsForWorkflow(workflowID).stream()
                    .filter(association -> association.getEventId().equals(eventType))
                    .findFirst()
                    .map(Association::getAssociationName)
                    .orElse(null);
        } catch (WorkflowException e) {
            log.error("Error while retrieving association name for workflow ID: {} and event type: {}", workflowID,
                    eventType, e);
            return null;
        }
    }

    private void handleApprovalTaskCompletion(String approvalTaskId, String workflowRequestId, String status)
            throws WorkflowEngineServerException {

        // Update the approval task status to APPROVED / REJECTED and delete other tasks of the same workflow request.
        approvalTaskDAO.updateApprovalTaskStatus(approvalTaskId, status);
        approvalTaskDAO.deleteApprovalTasksOfWorkflowRequestExceptGivenId(workflowRequestId, approvalTaskId);

        /* Update the entity of the approval task to the current user.
           This is to ensure that the task is marked as completed by the user who approved it
           and to maintain the integrity of the task history. */
        approvalTaskDAO.updateApprovalTaskEntityDetail(approvalTaskId, ENTITY_TYPE_USERS,
                CarbonContext.getThreadLocalCarbonContext().getUserId());
    }
}
