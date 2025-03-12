package org.wso2.carbon.identity.workflow.engine;

import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.workflow.WorkFlowExecutor;

import java.util.List;

/**
 * Implementation of Workflow Executor Interface.
 */
public class DefaultWorkflowExecutor implements WorkFlowExecutor {

    List<Parameter> parameterList;
    private static final String EXECUTOR_NAME = "WorkflowEngineSimple";

    /**
     *{@inheritDoc}
     */
    @Override
    public boolean canHandle(WorkflowRequest workflowRequest) {

        return true;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void initialize(List<Parameter> parameterList) {

        this.parameterList = parameterList;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void execute(WorkflowRequest request) {

        DefaultWorkflowEventRequestService defaultWorkflowEventRequest = new DefaultWorkflowEventRequestServiceImpl();
        defaultWorkflowEventRequest.addParamDetailsOfEvent(request, parameterList);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public String getName() {

        return EXECUTOR_NAME;
    }
}
