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

import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.workflow.engine.dto.PropertyDTO;
import org.wso2.carbon.identity.workflow.engine.dto.StateDTO;
import org.wso2.carbon.identity.workflow.engine.dto.TaskDataDTO;
import org.wso2.carbon.identity.workflow.engine.dto.TaskSummaryDTO;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineClientException;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineException;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineServerException;
import org.wso2.carbon.identity.workflow.engine.internal.WorkflowEngineServiceDataHolder;
import org.wso2.carbon.identity.workflow.engine.internal.dao.WorkflowEventRequestDAO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.impl.WorkflowEventRequestDAOImpl;
import org.wso2.carbon.identity.workflow.engine.model.PagePagination;
import org.wso2.carbon.identity.workflow.engine.model.TaskDetails;
import org.wso2.carbon.identity.workflow.engine.model.TaskModel;
import org.wso2.carbon.identity.workflow.engine.model.TaskParam;
import org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementServiceImpl;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociation;
import org.wso2.carbon.identity.workflow.mgt.callback.WSWorkflowCallBackService;
import org.wso2.carbon.identity.workflow.mgt.callback.WSWorkflowResponse;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestDAO;
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

/**
 * Call internal osgi services to perform user's approval task related operations.
 */
public class ApprovalEventService {

    private static final String PENDING = "PENDING";
    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final String RELEASED = "RELEASED";
    private static final String CLAIMED = "CLAIMED";
    private static final Integer LIMIT = 20;
    private static final Integer OFFSET = 0;
    protected long localCreatedTime;

    /**
     * Search available approval tasks for the current authenticated user.
     *
     * @param limit  number of records to be returned.
     * @param offset start page.
     * @param status state of the tasks [RESERVED, READY or COMPLETED].
     * @return taskSummaryDTO list.
     */
    public List<TaskSummaryDTO> listTasks(Integer limit, Integer offset, List<String> status) {

        try {
            PagePagination pagePagination = new PagePagination();
            if (limit == null || offset == null) {
                pagePagination.setPageSize(LIMIT);
                pagePagination.setPageNumber(OFFSET);
            }

            if (limit != null && limit > 0) {
                pagePagination.setPageSize(limit);
            }
            if (offset != null && offset > 0) {
                pagePagination.setPageNumber(offset);
            }

            Set<TaskSummaryDTO> taskSummaryDTOs = null;
            List<TaskSummaryDTO> tasks = listTasksOfApprovers(status);
            int taskListSize = tasks.size();
            for (int i = 0; i < taskListSize; ++i) {
                taskSummaryDTOs = new HashSet<>(tasks);
            }
            if (taskSummaryDTOs == null) {
                return new ArrayList<>(0);
            }
            return new ArrayList<>(taskSummaryDTOs);
        } catch (WorkflowEngineServerException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVALS_FOR_USER.getCode(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVALS_FOR_USER.
                            getDescription());
        }
    }

    private List<String> filterBlockedTasks(List<String> allTaskIDsList) {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        Map<String, String> requestIdToTaskId = new HashMap<>();
        Map<String, String> requestIdToStatus = new HashMap<>();

        for (String taskID : allTaskIDsList) {
            String requestID = workflowEventRequestDAO.getRequestID(taskID);
            String taskStatus = workflowEventRequestDAO.getTaskStatusOfRequest(taskID);

            // Keep only RESERVED task if exists
            if (!requestIdToTaskId.containsKey(requestID)) {
                requestIdToTaskId.put(requestID, taskID);
                requestIdToStatus.put(requestID, taskStatus);
            } else {
                String existingStatus = requestIdToStatus.get(requestID);
                if ("BLOCKED".equals(existingStatus) && !"BLOCKED".equals(taskStatus)) {
                    // Replace BLOCKED with higher-priority task (RESERVED)
                    requestIdToTaskId.put(requestID, taskID);
                    requestIdToStatus.put(requestID, taskStatus);
                }
            }
        }

        return new ArrayList<>(requestIdToTaskId.values());
    }


    private List<TaskSummaryDTO> listTasksOfApprovers(List<String> status) {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();
        List<String> allTaskIDsList = getAllTaskIDsRelatedUserAndRole(status, userId);
        allTaskIDsList = filterBlockedTasks(allTaskIDsList);
        List<TaskSummaryDTO> taskSummaryDTOList = new ArrayList<>();
        for (String taskID : allTaskIDsList) {
            TaskSummaryDTO summeryDTO = new TaskSummaryDTO();
            if (taskID != null) {
                String requestID = workflowEventRequestDAO.getRequestID(taskID);
                WorkflowRequest request = getWorkflowRequest(requestID);
                TaskDetails taskDetails = getTaskDetails(request);
                String eventType = workflowEventRequestDAO.getEventType(request.getUuid());
                String taskStatus = workflowEventRequestDAO.getTaskStatusOfRequest(taskID);
                String[] taskStatusValue = taskStatus.split(",", 0);
                String workflowID = workflowEventRequestDAO.getWorkflowID(taskID);
                String workflowAssociationName = workflowEventRequestDAO.getAssociationName(workflowID, eventType);
                Timestamp createdTime = workflowEventRequestDAO.getCreatedAtTimeInMill(request.getUuid());
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
                summeryDTO.setStatus(TaskSummaryDTO.StatusEnum.valueOf(taskStatusValue[0]));
                taskSummaryDTOList.add(summeryDTO);
            }
        }
        return taskSummaryDTOList;
    }

    private List<String> getAllRequestRelatedUserAndRole() {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        List<String> roleIds = getRoleIdsFromUser(userId, tenantDomain);
        List<String> roleRequestsList;
        List<String> lst = new ArrayList<>();
        for (String roleId : roleIds) {
            roleRequestsList = workflowEventRequestDAO.getRequestsList(
                    WorkflowEngineConstants.ParameterName.ENTITY_TYPE_ROLES, roleId);
            lst.addAll(roleRequestsList);
        }
        List<String> userRequestList = workflowEventRequestDAO.getRequestsList(
                WorkflowEngineConstants.ParameterName.ENTITY_TYPE_USERS, userId);

        return Stream.concat(lst.stream(), userRequestList.stream()).collect(Collectors.toList());
    }

    /**
     * Retrieves all task IDs assigned to a user either directly or indirectly (via roles or claimed tasks),
     * filtered by the given task statuses.
     *
     * This method checks for tasks:
     * - Assigned directly to the user (ENTITY_TYPE_USERS)
     * - Assigned to any of the user's roles (ENTITY_TYPE_ROLES)
     * - Claimed by the user (ENTITY_TYPE_CLAIMED_USERS)
     *
     * @param status  List of task statuses to filter by (e.g., READY, RESERVED)
     * @param userId  The ID of the user whose related task IDs should be retrieved
     * @return        A combined list of task IDs associated with the user's roles, user ID, and claimed tasks
     */
    private List<String> getAllTaskIDsRelatedUserAndRole(List<String> status, String userId) {

        List<String> allTaskIDs = new ArrayList<>();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        // Get role-based task IDs
        List<String> roleIds = getRoleIdsFromUser(userId, tenantDomain);
        for (String roleId : roleIds) {
            allTaskIDs.addAll(getTaskIDsByEntity(WorkflowEngineConstants.ParameterName.ENTITY_TYPE_ROLES, roleId,
                    status));
        }

        // Get user-based task IDs
        allTaskIDs.addAll(getTaskIDsByEntity(WorkflowEngineConstants.ParameterName.ENTITY_TYPE_USERS, userId, status));

        // Get task ids of the request that is claimed by the user
        allTaskIDs.addAll(getTaskIDsByEntity(WorkflowEngineConstants.ParameterName.ENTITY_TYPE_CLAIMED_USERS, userId,
                status));

        return allTaskIDs;
    }

    private List<String> getTaskIDsByEntity(String entityType, String entityId, List<String> status) {
        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();

        if (status == null || status.isEmpty()) {
            return workflowEventRequestDAO.getTaskIDList(entityType, entityId);
        } else {
            return workflowEventRequestDAO.getTaskIDListByStatus(entityType, entityId, status.get(0));
        }
    }

    private List<String> getRoleIdsFromUser(String userId, String tenantDomain) {

        try {
            List<String> roleIDList = WorkflowEngineServiceDataHolder.getInstance().getRoleManagementService().
                    getRoleIdListOfUser(userId, tenantDomain);
            return new ArrayList<>(roleIDList);

        } catch (IdentityRoleManagementException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVALS_FOR_USER.getCode(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVALS_FOR_USER.
                            getDescription());        }

    }

    private long getCreatedTime() {

        return this.localCreatedTime;
    }

    private void setCreatedTime(long param) {

        this.localCreatedTime = param;
    }

    /**
     * Get details of a task identified by the taskId.
     *
     * @param task the unique ID.
     * @return TaskDataDto object.
     */
    public TaskDataDTO getTaskData(String task) {

        try {
            WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
            String[] taskArray = task.split(" ", 0);
            String taskId = taskArray[0];
            String requestId = workflowEventRequestDAO.getRequestID(taskId);
            WorkflowRequest request = getWorkflowRequest(requestId);
            TaskDetails taskDetails = getTaskDetails(request);
            String initiator = workflowEventRequestDAO.getInitiatedUser(requestId);
            List<String> approvers = workflowEventRequestDAO.listApprovers(taskId);
            Map<String, String> assigneeMap = null;
            for (String assignee : approvers) {
                assigneeMap = new HashMap<>();
                assigneeMap.put(WorkflowEngineConstants.ParameterName.ASSIGNEE_TYPE, assignee);
            }
            List<TaskParam> params = getRequestParameters(request);
            List<PropertyDTO> properties = getPropertyDTOs(params);
            TaskDataDTO taskDataDTO = new TaskDataDTO();
            taskDataDTO.setId(taskId);
            taskDataDTO.setSubject(taskDetails.getTaskSubject());
            taskDataDTO.setDescription(taskDetails.getTaskDescription());
            String statusValue = setStatusOfTask(taskId);
            taskDataDTO.setApprovalStatus(TaskDataDTO.ApprovalStatusEnum.valueOf(statusValue));
            taskDataDTO.setInitiator(WorkflowEngineConstants.ParameterName.INITIATED_BY + initiator);
            taskDataDTO.setPriority(WorkflowEngineConstants.ParameterName.PRIORITY);
            TaskModel taskModel = new TaskModel();
            taskModel.setAssignees(assigneeMap);
            taskDataDTO.setAssignees(getPropertyDTOs(taskModel.getAssignees()));
            taskDataDTO.setProperties(properties);
            return taskDataDTO;
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

    private String setStatusOfTask(String taskId) {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        String status = workflowEventRequestDAO.getTaskStatusOfRequest(taskId);
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

    /**
     * Update the state of a task identified by the task id.
     * User can reserve the task by claiming, or release a reserved task to himself.
     * Or user can approve or reject a task.
     *
     * @param taskId    the unique ID to update the state.
     * @param nextState event status.
     */
    public void updateStatus(String taskId, StateDTO nextState) {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        DefaultWorkflowEventRequest defaultWorkflowEventRequest = new DefaultWorkflowEventRequestService();
        validateApprovers(taskId);
        try {
            switch (nextState.getAction()) {
                case APPROVE:
                    updateTaskStatusOfRequest(taskId, APPROVED);
                    updateStepDetailsOfRequest(taskId);
                    break;
                case REJECT:
                    String eventId = workflowEventRequestDAO.getRequestID(taskId);
                    updateTaskStatusOfRequest(taskId, REJECTED);
                    String requestID = workflowEventRequestDAO.getRequestID(taskId);
                    List<String> taskIdsOfRequest = workflowEventRequestDAO.getTaskId(requestID);
                    for (String id: taskIdsOfRequest) {
                        defaultWorkflowEventRequest.deleteApprovalOfRequest(id);
                    }
                    completeRequest(eventId, REJECTED);
                    break;
                case RELEASE:
                    updateTaskStatusOfRequest(taskId, RELEASED);
                    break;
                case CLAIM:
                    updateTaskStatusOfRequest(taskId, CLAIMED);
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

    private WorkflowRequest getWorkflowRequest(String requestId) {

        WorkflowRequestDAO requestDAO = new WorkflowRequestDAO();
        WorkflowRequest request;
        try {
            request = requestDAO.retrieveWorkflow(requestId);
        } catch (InternalWorkflowException e) {
            throw new WorkflowEngineException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_WORKFLOW_REQUEST.getCode(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_WORKFLOW_REQUEST.
                            getDescription());
        }
        return request;
    }

    private void updateStepDetailsOfRequest(String taskId) {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        String eventId = workflowEventRequestDAO.getRequestID(taskId);
        WorkflowRequest request = getWorkflowRequest(eventId);
        DefaultWorkflowEventRequest defaultWorkflowEventRequest = new DefaultWorkflowEventRequestService();
        List<Parameter> parameterList = getParameterList(request);
        String workflowId = defaultWorkflowEventRequest.getWorkflowId(request);
        String requestID = workflowEventRequestDAO.getRequestID(taskId);
        List<String> taskIdsOfRequest = workflowEventRequestDAO.getTaskId(requestID);
        for (String id: taskIdsOfRequest) {
            defaultWorkflowEventRequest.deleteApprovalOfRequest(id);
        }
        int stepValue = defaultWorkflowEventRequest.getStateOfRequest(eventId, workflowId);
        if (stepValue < numOfStates(request)) {
            defaultWorkflowEventRequest.addApproversOfRequests(request, parameterList);
        } else {
            completeRequest(eventId, ApprovalEventService.APPROVED);
        }
    }

    private void validateApprovers(String taskId) {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        List<String> eventList = getAllRequestRelatedUserAndRole();
        List<String> taskList;
        List<String> lst = new ArrayList<>();
        for (String event : eventList) {
            taskList = workflowEventRequestDAO.getTaskId(event);
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

    private void updateTaskStatusOfRequest(String taskId, String status) {

        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();

        try {
            switch (status) {
                case APPROVED:
                    handleApproval(taskId, workflowEventRequestDAO);
                    break;

                case REJECTED:
                    handleRejection(taskId, workflowEventRequestDAO);
                    break;

                case RELEASED:
                    handleRelease(taskId, workflowEventRequestDAO);
                    break;

                case CLAIMED:
                    handleClaim(taskId, workflowEventRequestDAO);
                    break;

                default:
                    throw new WorkflowEngineClientException(
                            WorkflowEngineConstants.ErrorMessages.
                                    USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE.getCode(),
                            WorkflowEngineConstants.ErrorMessages.
                                    USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE.getDescription()
                    );
            }
        } catch (WorkflowEngineClientException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_NON_EXISTING_TASK_ID.getCode(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_NON_EXISTING_TASK_ID.getDescription()
            );
        } catch (WorkflowEngineServerException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_CHANGING_APPROVALS_STATE.getCode(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_CHANGING_APPROVALS_STATE.getDescription()
            );
        }
    }

    private void handleApproval(String taskId, WorkflowEventRequestDAO dao) throws WorkflowEngineServerException {

        String requestID = dao.getRequestID(taskId);
        List<String> taskIds = dao.getTaskId(requestID);
        String completedStatus = WorkflowEngineConstants.TaskStatus.COMPLETED.toString();

        for (String id : taskIds) {
            dao.updateStatusOfRequest(id, completedStatus);
        }
    }

    private void handleRejection(String taskId, WorkflowEventRequestDAO dao) throws WorkflowEngineServerException {

        String rejectedStatus = WorkflowEngineConstants.TaskStatus.COMPLETED + "," + REJECTED;
        dao.updateStatusOfRequest(taskId, rejectedStatus);
    }

    private void handleRelease(String taskId, WorkflowEventRequestDAO dao) throws WorkflowEngineServerException {

        String readyStatus = WorkflowEngineConstants.TaskStatus.READY.toString();
        String requestID = dao.getRequestID(taskId);
        String approverType = dao.getApproverType(taskId);
        List<String> taskIds = dao.getTaskId(requestID);

        for (String id : taskIds) {
            dao.updateStatusOfRequest(id, readyStatus);

            if (taskId.equals(id) && WorkflowEngineConstants.ParameterName.
                    ENTITY_TYPE_CLAIMED_USERS.equals(approverType)) {
                dao.deleteApproversOfRequest(id);
            }
        }
    }

    private void handleClaim(String taskId, WorkflowEventRequestDAO dao) throws WorkflowEngineServerException {

        String userId = CarbonContext.getThreadLocalCarbonContext().getUserId();
        String reservedStatus = WorkflowEngineConstants.TaskStatus.RESERVED.toString();
        String blockedStatus = WorkflowEngineConstants.TaskStatus.BLOCKED.toString();

        String requestID = dao.getRequestID(taskId);
        String approverType = dao.getApproverType(taskId);
        String workflowId = dao.getWorkflowID(taskId);
        List<String> taskIds = dao.getTaskId(requestID);

        for (String id : taskIds) {
            if (taskId.equals(id)) {
                if (WorkflowEngineConstants.APPROVER_TYPE_USERS.equals(approverType)) {
                    dao.updateStatusOfRequest(id, reservedStatus);
                } else if (WorkflowEngineConstants.APPROVER_TYPE_ROLES.equals(approverType)) {
                    String newTaskId = UUID.randomUUID().toString();

                    dao.addApproversOfRequest(
                            newTaskId,
                            requestID,
                            workflowId,
                            WorkflowEngineConstants.ParameterName.ENTITY_TYPE_CLAIMED_USERS,
                            userId,
                            reservedStatus
                    );
                    dao.updateStatusOfRequest(newTaskId, reservedStatus);
                    dao.updateStatusOfRequest(id, blockedStatus);
                }
            } else {
                dao.updateStatusOfRequest(id, blockedStatus);
            }
        }
    }


    private void completeRequest(String eventId, String status) {

        WSWorkflowResponse wsWorkflowResponse = new WSWorkflowResponse();
        WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
        DefaultWorkflowEventRequest defaultWorkflowEventRequest = new DefaultWorkflowEventRequestService();
        String relationshipId = workflowEventRequestDAO.getRelationshipId(eventId);
        wsWorkflowResponse.setUuid(relationshipId);
        wsWorkflowResponse.setStatus(status);
        WSWorkflowCallBackService wsWorkflowCallBackService = new WSWorkflowCallBackService();
        wsWorkflowCallBackService.onCallback(wsWorkflowResponse);
        defaultWorkflowEventRequest.deleteStateOfRequest(eventId);
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

        WorkflowManagementService workflowManagementService = new WorkflowManagementServiceImpl();
        DefaultWorkflowEventRequest defaultWorkflowEventRequest = new DefaultWorkflowEventRequestService();
        List<WorkflowAssociation> associations = defaultWorkflowEventRequest.getAssociations(request);
        List<Parameter> parameterList = null;
        for (WorkflowAssociation association : associations) {
            try {
                parameterList = workflowManagementService.getWorkflowParameters(association.getWorkflowId());
            } catch (WorkflowException e) {
                throw new WorkflowEngineException(
                        WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_PARAMETER_LIST.getCode(),
                        WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_PARAMETER_LIST.
                                getDescription()
                );
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

}
