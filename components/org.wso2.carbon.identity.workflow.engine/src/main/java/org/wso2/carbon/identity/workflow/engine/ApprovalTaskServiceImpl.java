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
import org.wso2.carbon.identity.role.v2.mgt.core.model.UserBasicInfo;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskDTO;
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
import org.wso2.carbon.identity.workflow.engine.model.TaskDetails;
import org.wso2.carbon.identity.workflow.engine.model.TaskModel;
import org.wso2.carbon.identity.workflow.engine.model.TaskParam;
import org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementServiceImpl;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociation;
import org.wso2.carbon.identity.workflow.mgt.callback.WSWorkflowCallBackService;
import org.wso2.carbon.identity.workflow.mgt.callback.WSWorkflowResponse;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestAssociationDAO;
import org.wso2.carbon.identity.workflow.mgt.dto.Association;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowClientException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.sql.Timestamp;
import java.util.ArrayList;
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
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.CLAIMS_UI_PROPERTY_NAME;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.ENTITY_TYPE_CLAIMED_USERS;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.ENTITY_TYPE_ROLES;
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
    private static final String USERNAME_SEPARATOR = " , ";

    @Override
    public List<ApprovalTaskSummaryDTO> listApprovalTasks(Integer limit, Integer offset, List<String> statusList)
            throws WorkflowEngineException {

        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();

        List<ApprovalTaskSummaryDTO> approvalTaskSummaryDTOS = getAllAssignedTasks(statusList, userId);
        List<String> reservedWorkflowRequests = approvalTaskSummaryDTOS.stream()
                .filter(approvalTask -> WorkflowEngineConstants.TaskStatus.RESERVED.name()
                        .equals(approvalTask.getApprovalStatus())).map(ApprovalTaskSummaryDTO::getRequestId)
                .collect(Collectors.toList());
        Set<String> duplicatedWorkflowRequestIds = new HashSet<>();
        Iterator<ApprovalTaskSummaryDTO> iterator = approvalTaskSummaryDTOS.iterator();
        while (iterator.hasNext()) {
            ApprovalTaskSummaryDTO approvalTaskSummaryDTO = iterator.next();
            if (duplicatedWorkflowRequestIds.contains(approvalTaskSummaryDTO.getRequestId())) {
                iterator.remove();
                continue;
            }
            if (reservedWorkflowRequests.contains(approvalTaskSummaryDTO.getRequestId()) &&
                    !WorkflowEngineConstants.TaskStatus.RESERVED.name()
                            .equals(approvalTaskSummaryDTO.getApprovalStatus())) {
                iterator.remove();
                continue;
            }

            duplicatedWorkflowRequestIds.add(approvalTaskSummaryDTO.getRequestId());

            WorkflowRequest request = getWorkflowRequest(approvalTaskSummaryDTO.getRequestId());
            TaskDetails taskDetails = getTaskDetails(request);

            String eventType = request.getEventType();

            String workflowID = approvalTaskDAO.getWorkflowID(approvalTaskSummaryDTO.getId());
            String workflowAssociationName = findAssociationNameByWorkflowAndEvent(workflowID, eventType);

            Timestamp createdTime = workflowRequestDAO.getCreatedAtTimeInMill(request.getUuid());
            approvalTaskSummaryDTO.setId(approvalTaskSummaryDTO.getId());
            approvalTaskSummaryDTO.setName(workflowAssociationName);
            approvalTaskSummaryDTO.setTaskType(eventType);
            approvalTaskSummaryDTO.setPresentationName(taskDetails.getTaskSubject());
            approvalTaskSummaryDTO.setPresentationSubject(taskDetails.getTaskDescription());
            approvalTaskSummaryDTO.setCreatedTimeInMillis(String.valueOf(createdTime.getTime()));
            approvalTaskSummaryDTO.setPriority(WorkflowEngineConstants.ParameterName.PRIORITY);
            approvalTaskSummaryDTO.setApprovalStatus(approvalTaskSummaryDTO.getApprovalStatus());
        }

        if (limit == null || limit < 0) {
            limit = LIMIT;
        }
        if (offset == null || offset < 0) {
            offset = OFFSET;
        }
        approvalTaskSummaryDTOS.sort((taskA, taskB) -> {
            long createdTimeForTaskA = Long.parseLong(taskA.getCreatedTimeInMillis());
            long createdTimeForTaskB = Long.parseLong(taskB.getCreatedTimeInMillis());
            return Long.compare(createdTimeForTaskB, createdTimeForTaskA); // Descending order
        });
        return approvalTaskSummaryDTOS.subList(Math.min(offset, approvalTaskSummaryDTOS.size()),
                Math.min(offset + limit, approvalTaskSummaryDTOS.size()));
    }

    @Override
    public ApprovalTaskDTO getApprovalTaskByTaskId(String taskId) throws WorkflowEngineException {

        taskId = taskId.trim();
        String requestId = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(taskId);
        WorkflowRequest request = getWorkflowRequest(requestId);
        TaskDetails taskDetails = getTaskDetails(request);
        String initiator = workflowRequestDAO.getInitiatedUser(requestId);
        List<String> approvers = approvalTaskDAO.listApprovers(taskId);
        Map<String, String> assigneeMap = new HashMap<>();
        for (String assignee : approvers) {
            assigneeMap.put(WorkflowEngineConstants.ParameterName.ASSIGNEE_TYPE, assignee);
        }
        List<TaskParam> params = getRequestParameters(request);
        List<PropertyDTO> properties = getPropertyDTOs(params);
        ApprovalTaskDTO approvalTaskDTO = new ApprovalTaskDTO();
        approvalTaskDTO.setId(taskId);
        approvalTaskDTO.setSubject(taskDetails.getTaskSubject());
        approvalTaskDTO.setDescription(taskDetails.getTaskDescription());
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

        String tenantDomain = IdentityTenantUtil.getTenantDomain(workflowRequest.getTenantId());
        String workflowRequestId = getWorkflowRequestId(workflowRequest);
        String workflowId = getWorkflowId(workflowRequest);
        String approverType;

        int currentStep = approvalTaskDAO.getCurrentApprovalStepOfWorkflowRequest(workflowRequestId, workflowId);
        if (currentStep == 0) {
            approvalTaskDAO.addApprovalTaskStep(workflowRequestId, workflowId);
            currentStep = 1;
        } else {
            currentStep += 1;
            approvalTaskDAO.updateStateOfRequest(workflowRequestId, workflowId, currentStep);
        }

        int approverCountInCurrentStep = 0;
        List<String> taskIdsOfCurrentStep = new ArrayList<>();
        for (Parameter parameter : parameterList) {
            if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                String[] stepName = parameter.getqName().split("-");
                int step = Integer.parseInt(stepName[1]);
                if (currentStep == step) {
                    approverType = stepName[stepName.length - 1];

                    String approverIdentifiers = parameter.getParamValue();
                    if (approverIdentifiers != null && !approverIdentifiers.isEmpty()) {
                        String[] approverIdentifierList = approverIdentifiers.split(",", 0);
                        approverCountInCurrentStep += approverIdentifierList.length; // Efficient count here
                        for (String approverIdentifier : approverIdentifierList) {
                            String taskId = UUID.randomUUID().toString();
                            String taskStatus = WorkflowEngineConstants.ParameterName.TASK_STATUS_DEFAULT;
                            if (approverType.equals(ENTITY_TYPE_ROLES)) {
                                try {
                                    List<UserBasicInfo> userListOfRole =
                                            WorkflowEngineServiceDataHolder.getInstance().getRoleManagementService()
                                                    .getUserListOfRole(approverIdentifier, tenantDomain);
                                    if (userListOfRole.size() == 1) {
                                        taskStatus = WorkflowEngineConstants.ParameterName.TASK_STATUS_DEFAULT;
                                    } else {
                                        taskStatus = WorkflowEngineConstants.ParameterName.TASK_STATUS_READY;
                                    }
                                } catch (IdentityRoleManagementException e) {
                                    throw new RuntimeException(e);
                                }
                            }
                            approvalTaskDAO.addApproversOfRequest(taskId, workflowRequestId, workflowId,
                                    approverType, approverIdentifier, taskStatus);
                            taskIdsOfCurrentStep.add(taskId);
                        }

                    }
                }
            }
        }
        if (approverCountInCurrentStep > 1) {
            for (String taskId : taskIdsOfCurrentStep) {
                approvalTaskDAO.updateApprovalTaskStatus(taskId,
                        WorkflowEngineConstants.ParameterName.TASK_STATUS_READY);
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
    private List<ApprovalTaskSummaryDTO> getAllAssignedTasks(List<String> statusList, String userId)
            throws WorkflowEngineException {

        List<ApprovalTaskSummaryDTO> allTaskIDs = new ArrayList<>();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        // Get role-based task IDs.
        List<String> roleIds = getAssignedRoleIds(userId, tenantDomain);
        for (String roleId : roleIds) {
            List<ApprovalTaskSummaryDTO> tasksAssignedByRole =
                    getTaskIDsByEntity(ENTITY_TYPE_ROLES, roleId, statusList);
            allTaskIDs.addAll(tasksAssignedByRole);
        }

        // Get user-based task IDs.
        List<ApprovalTaskSummaryDTO> tasksIDsByUser = getTaskIDsByEntity(ENTITY_TYPE_USERS, userId, statusList);
        allTaskIDs.addAll(tasksIDsByUser);

        // Get task ids of the request that is claimed by the user.
        List<ApprovalTaskSummaryDTO> tasksClaimedByUser =
                getTaskIDsByEntity(ENTITY_TYPE_CLAIMED_USERS, userId, statusList);
        allTaskIDs.addAll(tasksClaimedByUser);
        return allTaskIDs;
    }

    private List<ApprovalTaskSummaryDTO> getTaskIDsByEntity(String entityType, String entityId, List<String> statusList)
            throws WorkflowEngineServerException {

        if (statusList == null || statusList.isEmpty()) {
            return approvalTaskDAO.getApprovalTaskDetailsList(entityType, entityId);
        } else {
            return approvalTaskDAO.getApprovalTaskDetailsListByStatus(entityType, entityId, statusList);
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

    private int numOfStates(WorkflowRequest request) throws WorkflowEngineException {

        List<Parameter> parameterList = getParameterList(request);
        int maxStep = 0;

        for (Parameter parameter : parameterList) {
            if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                String value = parameter.getqName();
                if (value != null && !value.isEmpty()) {
                    String[] parts = value.split("-");
                    if (parts.length > 1) {
                        try {
                            int step = Integer.parseInt(parts[1]);
                            if (step > maxStep) {
                                maxStep = step;
                            }
                        } catch (NumberFormatException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
        return maxStep;
    }


    private void handleApproval(String approvalTaskId) throws WorkflowEngineException {

        String workflowRequestId = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(approvalTaskId);
        WorkflowRequest workflowRequest = getWorkflowRequest(workflowRequestId);
        List<Parameter> parameterList = getParameterList(workflowRequest);
        String workflowId = approvalTaskDAO.getWorkflowID(approvalTaskId);

        approvalTaskDAO.updateApprovalTaskStatus(approvalTaskId, ApprovalTaskServiceImpl.APPROVED);

        int stepValue = approvalTaskDAO.getCurrentApprovalStepOfWorkflowRequest(workflowRequestId, workflowId);
        if (stepValue < numOfStates(workflowRequest)) {
            addApprovalTasksForWorkflowRequest(workflowRequest, parameterList);
        } else {
            completeWorkflowRequest(workflowRequestId, ApprovalTaskServiceImpl.APPROVED);
        }
    }

    private void handleReject(String approvalTaskId) throws WorkflowEngineServerException {

        String requestID = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(approvalTaskId);
        approvalTaskDAO.updateApprovalTaskStatus(approvalTaskId, REJECTED);
        completeWorkflowRequest(requestID, REJECTED);
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
                    String newTaskId = UUID.randomUUID().toString();
                    approvalTaskDAO.addApproversOfRequest(newTaskId, workflowRequestID, workflowId,
                            ENTITY_TYPE_CLAIMED_USERS, userId, reservedStatus);
                    approvalTaskDAO.updateApprovalTaskStatus(newTaskId, reservedStatus);
                    approvalTaskDAO.updateApprovalTaskStatus(updatedApprovalTaskId, blockedStatus);
                }
            } else {
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

    private TaskDetails getTaskDetails(WorkflowRequest workflowRequest) throws WorkflowEngineException {

        List<Parameter> parameterList = getParameterList(workflowRequest);
        TaskDetails taskDetails = new TaskDetails();
        if (CollectionUtils.isEmpty(parameterList)) {
            return taskDetails;
        }
        for (Parameter parameter : parameterList) {
            if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.TASK_SUBJECT)) {
                taskDetails.setTaskSubject(parameter.getParamValue());
            }
            if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.TASK_DESCRIPTION)) {
                taskDetails.setTaskDescription(parameter.getParamValue());
            }
        }
        return taskDetails;
    }

    private List<Parameter> getParameterList(WorkflowRequest workflowRequest) throws WorkflowEngineException {

        List<WorkflowAssociation> associations = getAssociations(workflowRequest);
        List<Parameter> parameterList = null;
        for (WorkflowAssociation association : associations) {
            try {
                parameterList = WorkflowEngineServiceDataHolder.getInstance().getWorkflowManagementService().
                        getWorkflowParameters(association.getWorkflowId());
            } catch (WorkflowException e) {
                throw new WorkflowEngineException(WorkflowEngineConstants.ErrorMessages
                        .ERROR_OCCURRED_WHILE_RETRIEVING_PARAMETER_LIST.getDescription(), e);
            }
        }
        return parameterList;
    }

    private List<TaskParam> getRequestParameters(WorkflowRequest workflowRequest) throws WorkflowEngineException {

        List<TaskParam> taskParamsList = new ArrayList<>();

        for (RequestParameter param : workflowRequest.getRequestParameters()) {
            if (param.getName().equals(WorkflowEngineConstants.ParameterName.CREDENTIAL)) {
                continue;
            }
            Object value = param.getValue();
            if (value != null) {
                String valueString = value.toString().trim();
                String paramString = param.getName().trim();
                TaskParam taskParam = new TaskParam();
                if (ROLE_ID_PARAM_NAME.equals(param.getName())) {
                    String tenantDomain = IdentityTenantUtil.getTenantDomain(workflowRequest.getTenantId());
                    try {
                        paramString = ROLE_NAME_PARAM_NAME;
                        RoleBasicInfo roleBasicInfo = WorkflowEngineServiceDataHolder.getInstance()
                                .getRoleManagementService().getRoleBasicInfoById(valueString, tenantDomain);
                        valueString = roleBasicInfo.getName();
                        if (RoleConstants.APPLICATION.equals(roleBasicInfo.getAudience())) {
                            TaskParam taskParam1 = new TaskParam();
                            taskParam1.setItemValue(roleBasicInfo.getAudienceName());
                            taskParam1.setItemName(ROLE_ASSOCIATED_APPLICATION_PARAM_NAME);
                            taskParamsList.add(taskParam1);
                        }
                    } catch (IdentityRoleManagementException e) {
                        throw new WorkflowEngineException(e.getMessage(), e);
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
                                valueString = String.join(USERNAME_SEPARATOR, userNames);
                            }
                        }
                    } catch (UserStoreException e) {
                        throw new WorkflowEngineException(e.getMessage(), e);
                    }
                }
                taskParam.setItemValue(valueString);
                taskParam.setItemName(paramString);
                taskParamsList.add(taskParam);
            }
        }
        return taskParamsList;
    }

    private List<PropertyDTO> getPropertyDTOs(Map<String, String> props) {

        return props.entrySet().stream().map(p -> getPropertyDTO(p.getKey(), p.getValue()))
                .collect(Collectors.toList());
    }

    private List<PropertyDTO> getPropertyDTOs(List<TaskParam> props) {

        List<PropertyDTO> propertyDTO = props.stream().map(p -> getPropertyDTO(p.getItemName(), p.getItemValue()))
                .collect(Collectors.toList());

        // Check if the claim property exists in the properties list and add new claim_UI property.
        propertyDTO.stream().filter(property -> CLAIMS_PROPERTY_NAME.equalsIgnoreCase((property.getKey()))).findFirst().
                ifPresent(claimProperty -> {
                    PropertyDTO claimUIProperty = new PropertyDTO();
                    claimUIProperty.setKey(CLAIMS_UI_PROPERTY_NAME);
                    claimUIProperty.setValue(getClaimsDisplayNames(claimProperty.getValue()));
                    propertyDTO.add(claimUIProperty);
                });

        return propertyDTO;
    }

    private String getClaimsDisplayNames(String claims) {

        StringBuilder claimDisplayNames = new StringBuilder();

        // Remove the square brackets and split the claims by comma.
        if (claims.startsWith("{") && claims.endsWith("}")) {
            claims = claims.substring(1, claims.length() - 1);
        }

        // Split the claims by comma and iterate through each claim.
        String[] claimArray = claims.split(",");

        List<LocalClaim> localClaims;
        try {
            localClaims = claimMetadataManagementService.getLocalClaims(
                            CarbonContext.getThreadLocalCarbonContext().getTenantDomain());
        } catch (ClaimMetadataException e) {
            log.error("Error while retrieving local claims for tenant: {}",
                    CarbonContext.getThreadLocalCarbonContext().getTenantDomain(), e);
            return claims; // Return original claims if unable to retrieve local claims.
        }

        for (String claim: claimArray) {
            String[] claimParts = claim.split("=");
            String claimUri = claimParts[0].trim();
            String claimValue = claimParts[1].trim();

            String displayName = localClaims.stream()
                            .filter(localClaim -> localClaim.getClaimURI().equals(claimUri))
                            .map(localClaim -> localClaim.getClaimProperty(DISPLAY_NAME_PROPERTY))
                            .findFirst()
                            .orElse(claimUri);
            claimDisplayNames.append(displayName).append("=").append(claimValue).append(", ");
        }

        // Remove the last comma and space if present.
        if (claimDisplayNames.length() > 0) {
            claimDisplayNames.setLength(claimDisplayNames.length() - 2);
        }
        return claimDisplayNames.toString();
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

    public String getWorkflowId(WorkflowRequest request) throws WorkflowEngineException {

        WorkflowManagementService workflowManagementService = new WorkflowManagementServiceImpl();
        List<WorkflowAssociation> associations = getAssociations(request);
        String workflowId = null;
        for (WorkflowAssociation association : associations) {
            try {
                Workflow workflow = workflowManagementService.getWorkflow(association.getWorkflowId());
                workflowId = workflow.getWorkflowId();
            } catch (WorkflowClientException e) {
                throw new WorkflowEngineClientException(
                        WorkflowEngineConstants.ErrorMessages.WORKFLOW_ID_NOT_FOUND.getCode(),
                        WorkflowEngineConstants.ErrorMessages.WORKFLOW_ID_NOT_FOUND.getDescription());
            } catch (WorkflowException e) {
                throw new WorkflowEngineServerException(e.getErrorCode(), e.getMessage());
            }
        }
        return workflowId;
    }

    public List<WorkflowAssociation> getAssociations(WorkflowRequest workflowRequest) {

        List<WorkflowAssociation> associations = null;
        WorkflowRequestAssociationDAO requestAssociationDAO = new WorkflowRequestAssociationDAO();
        try {
            associations = requestAssociationDAO.getWorkflowAssociationsForRequest(
                    workflowRequest.getEventType(), workflowRequest.getTenantId());
        } catch (InternalWorkflowException e) {
            log.error("Error while retrieving workflow associations for event type: {} and tenant ID: {}",
                    workflowRequest.getEventType(), workflowRequest.getTenantId(), e);
        }
        return associations;
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
}
