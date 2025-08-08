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

package org.wso2.carbon.identity.workflow.engine.internal;

import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to keep the data of the workflow engine component.
 */
public class WorkflowEngineServiceDataHolder {

    private static WorkflowEngineServiceDataHolder instance = new WorkflowEngineServiceDataHolder();

    private WorkflowManagementService workflowManagementService;
    private RoleManagementService roleManagementService;


    private WorkflowEngineServiceDataHolder() {

    }

    public static WorkflowEngineServiceDataHolder getInstance() {

        return instance;
    }

    public WorkflowManagementService getWorkflowManagementService() {

        return workflowManagementService;
    }

    public void setWorkflowManagementService(
            WorkflowManagementService workflowManagementService) {

        this.workflowManagementService = workflowManagementService;
    }

    public RoleManagementService getRoleManagementService() {
        return roleManagementService;
    }

    public void setRoleManagementService(RoleManagementService roleManagementService) {
        this.roleManagementService = roleManagementService;
    }

    private Map<String, Map<String, AbstractWorkflow>> workflowImpls
            = new HashMap<String, Map<String, AbstractWorkflow>>();

    public Map<String, Map<String, AbstractWorkflow>> getWorkflowImpls() {

        return workflowImpls;
    }

}
