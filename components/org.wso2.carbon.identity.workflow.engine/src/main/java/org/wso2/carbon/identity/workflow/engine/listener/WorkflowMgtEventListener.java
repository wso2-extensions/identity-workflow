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

package org.wso2.carbon.identity.workflow.engine.listener;

import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineException;
import org.wso2.carbon.identity.workflow.engine.internal.WorkflowEngineServiceDataHolder;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.listener.AbstractWorkflowListener;

import java.util.List;

/**
 * WorkflowMgtEventListener is a listener class for workflow management events.
 */
public class WorkflowMgtEventListener extends AbstractWorkflowListener {

    /**
     * Triggered after updating a workflow.
     * @param workflowId      workflow Id.
     * @param newParameterList List of new parameters added to the workflow.
     * @param oldParameterList  List of old parameters before the update.
     * @throws WorkflowException If an error occurs during the post-update process.
     */
    @Override
    public void doPostAddWorkflow(String workflowId, List<Parameter> newParameterList,
                                  List<Parameter> oldParameterList) throws WorkflowException {
        try {
            // Update the approval tasks on workflow update.
            WorkflowEngineServiceDataHolder.getInstance().getApprovalTaskService()
                    .updateApprovalTasksOnWorkflowUpdate(workflowId, newParameterList, oldParameterList);
        } catch (WorkflowEngineException e) {
            throw new WorkflowException("Error while updating the approval tasks for the workflow: " +
                    workflowId, e);
        }
    }
}
