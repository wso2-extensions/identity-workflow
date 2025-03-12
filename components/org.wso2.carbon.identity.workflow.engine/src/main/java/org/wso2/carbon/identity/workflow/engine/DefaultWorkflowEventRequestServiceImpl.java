package org.wso2.carbon.identity.workflow.engine;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineClientException;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineServerException;
import org.wso2.carbon.identity.workflow.engine.internal.dao.WorkflowEventRequestDAO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.impl.WorkflowEventRequestDAOImpl;
import org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants;
import org.wso2.carbon.identity.workflow.mgt.WorkflowExecutorManagerService;
import org.wso2.carbon.identity.workflow.mgt.WorkflowExecutorManagerServiceImpl;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementServiceImpl;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.bean.Workflow;
import org.wso2.carbon.identity.workflow.mgt.bean.WorkflowAssociation;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.InternalWorkflowException;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;

import java.util.List;
import java.util.UUID;

/**
 * Default Workflow Event Request service implementation.
 */
public class DefaultWorkflowEventRequestServiceImpl implements DefaultWorkflowEventRequestService {

    private WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
    private static final Log log = LogFactory.getLog(DefaultWorkflowEventRequestServiceImpl.class);

    /**
     * {@inheritDoc}
     */
    @Override
    public void addParamDetailsOfEvent(WorkflowRequest request, List<Parameter> parameterList) {

        String taskId = UUID.randomUUID().toString();
        String eventId = getRequestId(request);
        String workflowId = getWorkflowId(request);
        String approverType;
        String approverName;
        int currentStepValue = getCurrentStepOfEvent(eventId, workflowId);
        if (currentStepValue == 0) {
            addCurrentStepOfEvent(eventId, workflowId, currentStepValue);
        }
        currentStepValue += 1;
        updateCurrentStepOfEvent(eventId, workflowId);
            for (Parameter parameter : parameterList) {
                if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                    String[] stepName = parameter.getqName().split("-");
                    int step = Integer.parseInt(stepName[2]);
                    if (currentStepValue == step) {
                        approverType = stepName[stepName.length - 1];

                        String approver = parameter.getParamValue();
                        if (approver != null && !approver.isEmpty()) {
                            String[] approvers = approver.split(",", 0);
                            for (String name : approvers) {
                                approverName = name;
                                String taskStatus= WorkflowEngineConstants.ParameterName.TASK_STATUS_DEFAULT;
                                workflowEventRequestDAO.addApproverDetailsOfEvent(taskId, eventId, workflowId,
                                        approverType, approverName,taskStatus);
                            }
                        }
                    }
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

        List<WorkflowAssociation> associations;
        WorkflowExecutorManagerService workFlowExecutorManagerService = new WorkflowExecutorManagerServiceImpl();
        try {
            associations = workFlowExecutorManagerService.getWorkflowAssociationsForRequest(
                    workflowRequest.getEventType(), workflowRequest.getTenantId());
        } catch (InternalWorkflowException e) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.ASSOCIATION_NOT_FOUND.getCode(),
                    WorkflowEngineConstants.ErrorMessages.ASSOCIATION_NOT_FOUND.getDescription());
        }
        return associations;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getTaskIDOfEvent(String eventId) {

        if (StringUtils.isEmpty(eventId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot retrieve task from eventID: " + eventId);
            }
            throw buildTaskNotFoundError();
        }
        return workflowEventRequestDAO.getTaskIDOfEvent(eventId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteTask(String taskId) {

        if (StringUtils.isEmpty(taskId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot delete task from taskID: " + taskId);
            }
        }
        workflowEventRequestDAO.deleteApproversOfTask(taskId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCurrentStepOfEvent(String eventId, String workflowId, int currentStep) {

        if (StringUtils.isEmpty(eventId) || StringUtils.isEmpty(workflowId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot add current step from event ID: " + eventId);
            }
            throw buildTaskNotFoundError();
        }
        workflowEventRequestDAO.createCurrentStepOfEvent(eventId, workflowId, currentStep);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getCurrentStepOfEvent(String eventId, String workflowId) {

        if (StringUtils.isEmpty(eventId) || StringUtils.isEmpty(workflowId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot retrieve current task step from eventID: " + eventId);
            }
            throw buildTaskNotFoundError();
        }
        return workflowEventRequestDAO.getCurrentStepOfEvent(eventId, workflowId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateCurrentStepOfEvent(String eventId, String workflowId) {

        if (StringUtils.isEmpty(eventId) || StringUtils.isEmpty(workflowId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot update task step from eventID: " + eventId);
            }
            throw buildTaskNotFoundError();
        }
        int currentStep = getCurrentStepOfEvent(eventId, workflowId);
        currentStep += 1;
        workflowEventRequestDAO.updateCurrentStepOfEvent(eventId, workflowId, currentStep);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void deleteCurrentStepOfEvent(String eventId) {

        if (StringUtils.isEmpty(eventId)) {
            if (log.isDebugEnabled()) {
                log.debug("Cannot delete task step from eventID: " + eventId);
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
