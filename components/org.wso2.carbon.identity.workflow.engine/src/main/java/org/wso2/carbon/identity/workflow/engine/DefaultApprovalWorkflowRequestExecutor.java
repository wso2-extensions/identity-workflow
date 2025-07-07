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
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineException;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.workflow.WorkFlowExecutor;

import java.util.List;

/**
 * Default implementation of WorkFlowExecutor for handling approval workflows.
 */
public class DefaultApprovalWorkflowRequestExecutor implements WorkFlowExecutor {

    private List<Parameter> parameterList;
    private static final String EXECUTOR_NAME = "Approval Workflow Engine";

    private final ApprovalTaskService approvalTaskService = new ApprovalTaskServiceImpl();

    private static final Logger LOG = LoggerFactory.getLogger(DefaultApprovalWorkflowRequestExecutor.class);

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

        try {
            approvalTaskService.addApprovalTasksForWorkflowRequest(request, parameterList);
        } catch (WorkflowEngineException e) {
            LOG.error("Error while executing approval workflow request: {}", request.getUuid(), e);
        }
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public String getName() {

        return EXECUTOR_NAME;
    }
}
