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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.UserBasicInfo;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskSummaryDTO;
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
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.ENTITY_TYPE_CLAIMED_USERS;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.ENTITY_TYPE_ROLES;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.ENTITY_TYPE_USERS;

/**
 * Implementation of the ApprovalTaskService interface.
 */
public class ApprovalTaskServiceImpl implements ApprovalTaskService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String RELEASED = "RELEASED";
    private static final String CLAIMED = "CLAIMED";
    private static final Integer LIMIT = 20;
    private static final Integer OFFSET = 0;
    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskServiceImpl.class);
    protected long localCreatedTime;

    private final ApprovalTaskDAO approvalTaskDAO = new ApprovalTaskDAOImpl();
    private final WorkflowRequestDAO workflowRequestDAO = new WorkflowRequestDAOImpl();
    private final WSWorkflowCallBackService wsWorkflowCallBackService = new WSWorkflowCallBackService();

    @Override
    public List<ApprovalTaskSummaryDTO> listApprovalTasks(Integer limit, Integer offset, List<String> status) {

        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();

        List<String> allTaskIDsList = new ArrayList<>(getAllAssignedTasks(status, userId));
        List<ApprovalTaskSummaryDTO> approvalTaskSummaryDTOList = new ArrayList<>();
        List<String> workflowRequestIds = new ArrayList<>();
        for (String taskID : allTaskIDsList) {
            ApprovalTaskSummaryDTO summeryDTO = new ApprovalTaskSummaryDTO();
            String requestID = approvalTaskDAO.getWorkflowRequestIdByTaskId(taskID);
            if (workflowRequestIds.contains(requestID)) {
                continue;
            }
            workflowRequestIds.add(requestID);
            WorkflowRequest request = getWorkflowRequest(requestID);
            TaskDetails taskDetails = getTaskDetails(request);

            String eventType = request.getEventType();

            String taskStatus = approvalTaskDAO.getApprovalTaskStatus(taskID);
            String[] taskStatusValue = taskStatus.split(",", 0);
            String workflowID = approvalTaskDAO.getWorkflowID(taskID);
            String workflowAssociationName = findAssociationNameByWorkflowAndEvent(workflowID, eventType);

            Timestamp createdTime = workflowRequestDAO.getCreatedAtTimeInMill(request.getUuid());

            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(createdTime.getTime());
            long cal = calendar.getTimeInMillis();
            setCreatedTime(cal);
            summeryDTO.setId(taskID);
            summeryDTO.setName(WorkflowEngineConstants.ParameterName.APPROVAL_TASK);
            summeryDTO.setName(workflowAssociationName);
            summeryDTO.setTaskType(eventType);
            summeryDTO.setPresentationName(taskDetails.getTaskSubject());
            summeryDTO.setPresentationSubject(taskDetails.getTaskDescription());
            summeryDTO.setCreatedTimeInMillis(String.valueOf(getCreatedTime()));
            summeryDTO.setPriority(WorkflowEngineConstants.ParameterName.PRIORITY);
            summeryDTO.setStatus(ApprovalTaskSummaryDTO.StatusEnum.valueOf(taskStatusValue[0]));
            approvalTaskSummaryDTOList.add(summeryDTO);
        }

        if (limit == null || limit < 0) {
            limit = LIMIT;
        }
        if (offset == null || offset < 0) {
            offset = OFFSET;
        }
        return approvalTaskSummaryDTOList.subList(Math.min(offset, approvalTaskSummaryDTOList.size()),
                Math.min(offset + limit, approvalTaskSummaryDTOList.size()));
    }

    @Override
    public ApprovalTaskDTO getApprovalTaskByTaskId(String taskId) {

        try {
            String[] taskArray = taskId.split(" ", 0);
            taskId = taskArray[0];
            String requestId = approvalTaskDAO.getWorkflowRequestIdByTaskId(taskId);
            WorkflowRequest request = getWorkflowRequest(requestId);
            TaskDetails taskDetails = getTaskDetails(request);
            String initiator = workflowRequestDAO.getInitiatedUser(requestId);
            List<String> approvers = approvalTaskDAO.listApprovers(taskId);
            Map<String, String> assigneeMap = null;
            for (String assignee : approvers) {
                assigneeMap = new HashMap<>();
                assigneeMap.put(WorkflowEngineConstants.ParameterName.ASSIGNEE_TYPE, assignee);
            }
            List<TaskParam> params = getRequestParameters(request);
            List<PropertyDTO> properties = getPropertyDTOs(params);
            ApprovalTaskDTO approvalTaskDTO = new ApprovalTaskDTO();
            approvalTaskDTO.setId(taskId);
            approvalTaskDTO.setSubject(taskDetails.getTaskSubject());
            approvalTaskDTO.setDescription(taskDetails.getTaskDescription());
            String statusValue = setStatusOfTask(taskId);
            approvalTaskDTO.setApprovalStatus(ApprovalTaskDTO.ApprovalStatusEnum.valueOf(statusValue));
            approvalTaskDTO.setInitiator(WorkflowEngineConstants.ParameterName.INITIATED_BY + initiator);
            approvalTaskDTO.setPriority(WorkflowEngineConstants.ParameterName.PRIORITY);
            TaskModel taskModel = new TaskModel();
            taskModel.setAssignees(assigneeMap);
            approvalTaskDTO.setAssignees(getPropertyDTOs(taskModel.getAssignees()));
            approvalTaskDTO.setProperties(properties);
            return approvalTaskDTO;
        } catch (WorkflowEngineClientException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_NON_EXISTING_TASK_ID.getCode(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_NON_EXISTING_TASK_ID.getDescription());
        } catch (WorkflowEngineServerException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVAL_OF_USER.getCode(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVAL_OF_USER.
                            getDescription());
        }
    }

    @Override
    public void updateApprovalTaskStatus(String taskId, StateDTO nextState) {

        validateApprovers(taskId);
        try {
            switch (nextState.getAction()) {
                case APPROVE:
                    handleApproval(taskId);
                    updateStepDetailsOfRequest(taskId);
                    break;
                case REJECT:
                    String eventId = approvalTaskDAO.getWorkflowRequestIdByTaskId(taskId);
                    handleRejection(taskId);
                    String requestID = approvalTaskDAO.getWorkflowRequestIdByTaskId(taskId);
                    List<String> taskIdsOfRequest = approvalTaskDAO.getTaskIds(requestID);
                    for (String id : taskIdsOfRequest) {
                        approvalTaskDAO.deleteApproversOfRequest(id);
                    }
                    completeRequest(eventId, REJECTED);
                    break;
                case RELEASE:
                    handleRelease(taskId);
                    break;
                case CLAIM:
                    handleClaim(taskId);
                    break;
                default:
                    throw new WorkflowEngineClientException(
                            WorkflowEngineConstants.ErrorMessages.USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE.
                                    getCode(),
                            WorkflowEngineConstants.ErrorMessages.USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE.
                                    getDescription());
            }
        } catch (WorkflowEngineClientException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_NON_EXISTING_TASK_ID.getCode(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_NON_EXISTING_TASK_ID.getDescription());
        } catch (WorkflowEngineServerException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_CHANGING_APPROVALS_STATE.getCode(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_CHANGING_APPROVALS_STATE.
                            getDescription());
        }
    }

    @Override
    public void addApprovalTasksForWorkflowRequest(WorkflowRequest workflowRequest, List<Parameter> parameterList) {

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

    private List<String> getAllRequestRelatedUserAndRole() {

        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        List<String> roleIds = getAssignedRoleIds(userId, tenantDomain);
        List<String> roleRequestsList;
        List<String> lst = new ArrayList<>();
        for (String roleId : roleIds) {
            roleRequestsList = approvalTaskDAO.getRequestsList(
                    ENTITY_TYPE_ROLES, roleId);
            lst.addAll(roleRequestsList);
        }
        List<String> userRequestList = approvalTaskDAO.getRequestsList(
                ENTITY_TYPE_USERS, userId);

        return Stream.concat(lst.stream(), userRequestList.stream()).collect(Collectors.toList());
    }

    /**
     * Retrieves all task IDs assigned to the user.
     *
     * @param status List of task statuses to filter by (e.g., READY, RESERVED).
     * @param userId The ID of the user whose related task IDs should be retrieved.
     * @return Set of task IDs assigned to the user, filtered by the specified statuses.
     */
    private Set<String> getAllAssignedTasks(List<String> status, String userId) {

        Set<String> allTaskIDs = new HashSet<>();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        // Get role-based task IDs
        List<String> roleIds = getAssignedRoleIds(userId, tenantDomain);
        for (String roleId : roleIds) {
            List<String> tasksAssignedByRole = getTaskIDsByEntity(ENTITY_TYPE_ROLES, roleId, status);
            allTaskIDs.addAll(tasksAssignedByRole);
        }

        // Get user-based task IDs.
        List<String> tasksIDsByUser = getTaskIDsByEntity(ENTITY_TYPE_USERS, userId, status);
        allTaskIDs.addAll(tasksIDsByUser);

        // Get task ids of the request that is claimed by the user.
        List<String> tasksClaimedByUser = getTaskIDsByEntity(ENTITY_TYPE_CLAIMED_USERS, userId, status);
        allTaskIDs.addAll(tasksClaimedByUser);

        return allTaskIDs;
    }

    private List<String> getTaskIDsByEntity(String entityType, String entityId, List<String> status) {

        if (status == null || status.isEmpty()) {
            return approvalTaskDAO.getTaskIDList(entityType, entityId);
        } else {
            return approvalTaskDAO.getTaskIDListByStatus(entityType, entityId, status.get(0));
        }
    }

    private List<String> getAssignedRoleIds(String userId, String tenantDomain) {

        try {
            List<String> roleIDList = WorkflowEngineServiceDataHolder.getInstance().getRoleManagementService().
                    getRoleIdListOfUser(userId, tenantDomain);
            return new ArrayList<>(roleIDList);
        } catch (IdentityRoleManagementException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVAL_TASKS_FOR_USER
                            .getCode(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVAL_TASKS_FOR_USER.
                            getDescription());
        }

    }

    private long getCreatedTime() {

        return this.localCreatedTime;
    }

    private void setCreatedTime(long param) {

        this.localCreatedTime = param;
    }

    private String setStatusOfTask(String taskId) {

        String status = approvalTaskDAO.getApprovalTaskStatus(taskId);
        String statusValue;
        if (status.equals(String.valueOf(WorkflowEngineConstants.TaskStatus.RESERVED))) {
            statusValue = PENDING;
        } else if (status.equals(String.valueOf(WorkflowEngineConstants.TaskStatus.READY))) {
            statusValue = PENDING;
        } else if (status.equals(String.valueOf(WorkflowEngineConstants.TaskStatus.COMPLETED))) {
            statusValue = APPROVED;
        } else {
            statusValue = REJECTED;
        }
        return statusValue;
    }

    private WorkflowRequest getWorkflowRequest(String requestId) {

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

    private void updateStepDetailsOfRequest(String taskId) {

        String eventId = approvalTaskDAO.getWorkflowRequestIdByTaskId(taskId);
        WorkflowRequest request = getWorkflowRequest(eventId);
        List<Parameter> parameterList = getParameterList(request);
        String workflowId = approvalTaskDAO.getWorkflowID(taskId);
        String requestID = approvalTaskDAO.getWorkflowRequestIdByTaskId(taskId);
        List<String> taskIdsOfRequest = approvalTaskDAO.getTaskIds(requestID);
        for (String task : taskIdsOfRequest) {
            approvalTaskDAO.deleteApproversOfRequest(task);
        }

        int stepValue = approvalTaskDAO.getCurrentApprovalStepOfWorkflowRequest(eventId, workflowId);
        if (stepValue < numOfStates(request)) {
            addApprovalTasksForWorkflowRequest(request, parameterList);
        } else {
            completeRequest(eventId, ApprovalTaskServiceImpl.APPROVED);
        }
    }

    private void validateApprovers(String taskId) {

        List<String> eventList = getAllRequestRelatedUserAndRole();
        List<String> taskList;
        List<String> lst = new ArrayList<>();
        for (String event : eventList) {
            taskList = approvalTaskDAO.getTaskIds(event);
            lst.addAll(taskList);
        }

        for (String task : lst) {
            if (taskId.equals(task)) {
                return;
            }
        }
    }

    private int numOfStates(WorkflowRequest request) {

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

    private void handleApproval(String taskId) throws WorkflowEngineServerException {

        String requestID = approvalTaskDAO.getWorkflowRequestIdByTaskId(taskId);
        List<String> taskIds = approvalTaskDAO.getTaskIds(requestID);
        String completedStatus = WorkflowEngineConstants.TaskStatus.COMPLETED.toString();

        for (String id : taskIds) {
            approvalTaskDAO.updateApprovalTaskStatus(id, completedStatus);
        }
    }

    private void handleRejection(String taskId) throws WorkflowEngineServerException {

        String rejectedStatus = WorkflowEngineConstants.TaskStatus.COMPLETED + "," + REJECTED;
        approvalTaskDAO.updateApprovalTaskStatus(taskId, rejectedStatus);
    }

    private void handleRelease(String taskId) throws WorkflowEngineServerException {

        String readyStatus = WorkflowEngineConstants.TaskStatus.READY.toString();
        String requestID = approvalTaskDAO.getWorkflowRequestIdByTaskId(taskId);
        String approverType = approvalTaskDAO.getApproverType(taskId);
        List<String> taskIds = approvalTaskDAO.getTaskIds(requestID);

        for (String id : taskIds) {
            approvalTaskDAO.updateApprovalTaskStatus(id, readyStatus);

            if (taskId.equals(id) && ENTITY_TYPE_CLAIMED_USERS.equals(approverType)) {
                approvalTaskDAO.deleteApproversOfRequest(id);
            }
        }
    }

    private void handleClaim(String taskId) throws WorkflowEngineServerException {

        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();
        String reservedStatus = WorkflowEngineConstants.TaskStatus.RESERVED.toString();
        String blockedStatus = WorkflowEngineConstants.TaskStatus.BLOCKED.toString();

        if (blockedStatus.equals(approvalTaskDAO.getApprovalTaskStatus(taskId))) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getCode(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getDescription());
        }

        String workflowRequestID = approvalTaskDAO.getWorkflowRequestIdByTaskId(taskId);
        String approverType = approvalTaskDAO.getApproverType(taskId);
        String workflowId = approvalTaskDAO.getWorkflowID(taskId);
        List<String> existingTaskIds = approvalTaskDAO.getTaskIds(workflowRequestID);

        for (String existingTaskId : existingTaskIds) {
            if (existingTaskId.equals(taskId)) {
                if (WorkflowEngineConstants.APPROVER_TYPE_USERS.equals(approverType)) {
                    approvalTaskDAO.updateApprovalTaskStatus(taskId, reservedStatus);
                } else if (WorkflowEngineConstants.APPROVER_TYPE_ROLES.equals(approverType)) {
                    String newTaskId = UUID.randomUUID().toString();
                    approvalTaskDAO.addApproversOfRequest(newTaskId, workflowRequestID, workflowId,
                            ENTITY_TYPE_CLAIMED_USERS, userId, reservedStatus);
                    approvalTaskDAO.updateApprovalTaskStatus(newTaskId, reservedStatus);
                    approvalTaskDAO.updateApprovalTaskStatus(taskId, blockedStatus);
                }
            } else {
                approvalTaskDAO.updateApprovalTaskStatus(taskId, blockedStatus);
            }
        }
    }

    private void completeRequest(String eventId, String status) {

        WSWorkflowResponse wsWorkflowResponse = new WSWorkflowResponse();
        String relationshipId = workflowRequestDAO.getRelationshipId(eventId);
        wsWorkflowResponse.setUuid(relationshipId);
        wsWorkflowResponse.setStatus(status);
        wsWorkflowCallBackService.onCallback(wsWorkflowResponse);
        // workflowEventRequestHandler.deleteStateOfRequest(eventId);
    }

    private TaskDetails getTaskDetails(WorkflowRequest workflowRequest) {

        List<Parameter> parameterList = getParameterList(workflowRequest);
        TaskDetails taskDetails = new TaskDetails();
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

    private List<Parameter> getParameterList(WorkflowRequest request) {

        List<WorkflowAssociation> associations = getAssociations(request);
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

    private List<TaskParam> getRequestParameters(WorkflowRequest request) {

        List<RequestParameter> requestParameter;
        List<TaskParam> taskParamsList = new ArrayList<>();
        for (int i = 0; i < request.getRequestParameters().size(); i++) {
            requestParameter = request.getRequestParameters();
            TaskParam taskParam = new TaskParam();
            Object value = requestParameter.get(i).getValue();
            if (requestParameter.get(i).getName().equals(WorkflowEngineConstants.ParameterName.CREDENTIAL)) {
                continue;
            }
            if (value != null) {
                taskParam.setItemValue(requestParameter.get(i).getValue().toString());
                taskParam.setItemName(requestParameter.get(i).getName());
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

        return props.stream().map(p -> getPropertyDTO(p.getItemName(), p.getItemValue()))
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

    public String getWorkflowId(WorkflowRequest request) {

        WorkflowManagementService workflowManagementService = new WorkflowManagementServiceImpl();
        List<WorkflowAssociation> associations = getAssociations(request);
        String workflowId = null;
        for (WorkflowAssociation association : associations) {
            try {
                Workflow workflow = workflowManagementService.getWorkflow(association.getWorkflowId());
                workflowId = workflow.getWorkflowId();
            } catch (WorkflowException e) {
                throw new WorkflowEngineClientException(
                        WorkflowEngineConstants.ErrorMessages.ASSOCIATION_NOT_FOUND.getCode(),
                        WorkflowEngineConstants.ErrorMessages.WORKFLOW_ID_NOT_FOUND.getDescription());
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
