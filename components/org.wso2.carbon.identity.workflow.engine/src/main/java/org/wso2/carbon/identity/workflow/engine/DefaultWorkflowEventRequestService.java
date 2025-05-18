package org.wso2.carbon.identity.workflow.engine;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.UserBasicInfo;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineClientException;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineServerException;
import org.wso2.carbon.identity.workflow.engine.internal.WorkflowEngineServiceDataHolder;
import org.wso2.carbon.identity.workflow.engine.internal.dao.WorkflowEventRequestDAO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.impl.WorkflowEventRequestDAOImpl;
import org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementServiceImpl;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociation;
import org.wso2.carbon.identity.workflow.mgt.dao.WorkflowRequestAssociationDAO;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Default Workflow Event Request service implementation.
 */
public class DefaultWorkflowEventRequestService implements DefaultWorkflowEventRequest {

    private final WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
    private static final Log log = LogFactory.getLog(DefaultWorkflowEventRequestService.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void addApproversOfRequests(WorkflowRequest request, List<Parameter> parameterList) {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        String eventId = getRequestId(request);
        String workflowId = getWorkflowId(request);
        String approverType;
        String approverName;
        int currentStepValue = getStateOfRequest(eventId, workflowId);
        if (currentStepValue == 0) {
            createStatesOfRequest(eventId, workflowId, currentStepValue);
        }
        currentStepValue += 1;
        updateStateOfRequest(eventId, workflowId);
        int approverCountInCurrentStep = 0;
        List<String> taskIdsOfCurrentStep = new ArrayList<>();
        for (Parameter parameter : parameterList) {
                if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                    String[] stepName = parameter.getqName().split("-");
                    int step = Integer.parseInt(stepName[1]);
                    if (currentStepValue == step) {
                        approverType = stepName[stepName.length - 1];

                        String approver = parameter.getParamValue();
                        if (approver != null && !approver.isEmpty()) {
                            String[] approvers = approver.split(",", 0);
                            approverCountInCurrentStep += approvers.length; // Efficient count here
                            for (String name : approvers) {
                                String taskId = UUID.randomUUID().toString();
                                approverName = name;
                                String taskStatus = WorkflowEngineConstants.ParameterName.TASK_STATUS_DEFAULT;
                                if (approverType.equals(WorkflowEngineConstants.ParameterName.ENTITY_TYPE_ROLES)) {
                                    try {
                                        List<UserBasicInfo> userListOfRole =
                                                WorkflowEngineServiceDataHolder.getInstance().getRoleManagementService()
                                                        .getUserListOfRole(name, tenantDomain);
                                        if (userListOfRole.size() == 1) {
                                            taskStatus = WorkflowEngineConstants.ParameterName.TASK_STATUS_DEFAULT;
                                        } else {
                                            taskStatus = WorkflowEngineConstants.ParameterName.TASK_STATUS_READY;
                                        }
                                    } catch (IdentityRoleManagementException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                                workflowEventRequestDAO.addApproversOfRequest(taskId, eventId, workflowId,
                                        approverType, approverName, taskStatus);
                                taskIdsOfCurrentStep.add(taskId);
                            }
                        }
                    }
                }
        }
        if (approverCountInCurrentStep > 1) {
            for (String taskId: taskIdsOfCurrentStep) {
                workflowEventRequestDAO.updateStatusOfRequest(taskId,
                        WorkflowEngineConstants.ParameterName.TASK_STATUS_READY);
            }
        }
    }

    private String getRequestId(WorkflowRequest request) {

        List<RequestParameter> requestParameter;
        Object event = null;
        for (int i = 0; i < request.getRequestParameters().size(); i++) {
            requestParameter = request.getRequestParameters();
            if (requestParameter.get(i).getName().equals(WorkflowEngineConstants.ParameterName.REQUEST_ID)) {
                event = requestParameter.get(i).getValue();
            }
        }
        return (String) event;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWorkflowId(WorkflowRequest request) {

        WorkflowManagementService workflowManagementService = new WorkflowManagementServiceImpl();
        List<WorkflowAssociation> associations = getAssociations(request);
        String workflowId = null;
        for (WorkflowAssociation association : associations) {
            try {
                Workflow  workflow = workflowManagementService.getWorkflow(association.getWorkflowId());
                workflowId = workflow.getWorkflowId();
            } catch (WorkflowException e) {
                throw new WorkflowEngineClientException(
                        WorkflowEngineConstants.ErrorMessages.ASSOCIATION_NOT_FOUND.getCode(),
                        WorkflowEngineConstants.ErrorMessages.WORKFLOW_ID_NOT_FOUND.getDescription());
            }
        }
        return workflowId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
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

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApprovalOfRequest(String eventId) {

        if (StringUtils.isEmpty(eventId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot retrieve task from eventID: " + eventId);
            }
            throw buildTaskNotFoundError();
        }
        return workflowEventRequestDAO.getApproversOfRequest(eventId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteApprovalOfRequest(String taskId) {

        if (StringUtils.isEmpty(taskId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot delete task from taskID: " + taskId);
            }
        }
        workflowEventRequestDAO.deleteApproversOfRequest(taskId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void createStatesOfRequest(String eventId, String workflowId, int currentStep) {

        if (StringUtils.isEmpty(eventId) || StringUtils.isEmpty(workflowId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot add task from event ID: " + eventId);
            }
            throw buildTaskNotFoundError();
        }
        workflowEventRequestDAO.createStatesOfRequest(eventId, workflowId, currentStep);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getStateOfRequest(String eventId, String workflowId) {

        if (StringUtils.isEmpty(eventId) || StringUtils.isEmpty(workflowId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot retrieve task step from eventID: " + eventId);
            }
            throw buildTaskNotFoundError();
        }
        return workflowEventRequestDAO.getStateOfRequest(eventId, workflowId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateStateOfRequest(String eventId, String workflowId) {

        if (StringUtils.isEmpty(eventId) || StringUtils.isEmpty(workflowId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot update task from eventID: " + eventId);
            }
            throw buildTaskNotFoundError();
        }
        int currentStep = getStateOfRequest(eventId, workflowId);
        currentStep += 1;
        workflowEventRequestDAO.updateStateOfRequest(eventId, workflowId, currentStep);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteStateOfRequest(String eventId) {

        if (StringUtils.isEmpty(eventId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot delete task from eventID: " + eventId);
            }
            throw buildTaskNotFoundError();
        }
        workflowEventRequestDAO.deleteCurrentStepOfRequest(eventId);
    }

    private WorkflowEngineServerException buildTaskNotFoundError() {

        return new WorkflowEngineServerException(
                WorkflowEngineConstants.ErrorMessages.TASK_NOT_FOUND.getCode(),
                WorkflowEngineConstants.ErrorMessages.TASK_NOT_FOUND.getDescription());
    }
}
