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

import org.osgi.framework.BundleContext;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;
import org.wso2.carbon.identity.workflow.engine.ApprovalEventService;
import org.wso2.carbon.identity.workflow.engine.DefaultApprovalWorkflow;
import org.wso2.carbon.identity.workflow.engine.DefaultTemplateInitializer;
import org.wso2.carbon.identity.workflow.engine.DefaultWorkflowExecutor;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;

/**
 * OSGi declarative services component which handles registration and un-registration of workflow engine management
 * service.
 */
@Component(
        name = "workflow.engine",
        immediate = true)
public class WorkflowEngineServiceComponent {

    /**
     * Register Default Approval Workflow as an OSGi service.
     *
     * @param context OSGi service component context.
     */
    @Activate
    protected void activate(ComponentContext context) {

        BundleContext bundleContext = context.getBundleContext();
        bundleContext.registerService(AbstractWorkflow.class,
                new DefaultApprovalWorkflow(DefaultTemplateInitializer.class,
                DefaultWorkflowExecutor.class, getMetaDataXML()), null);
        ApprovalEventService approvalEventService = new ApprovalEventService();
        bundleContext.registerService(ApprovalEventService.class, approvalEventService, null);
    }

    private String getMetaDataXML() {

        return "<met:MetaData xmlns:met=\"http://metadata.bean.mgt.workflow.identity.carbon.wso2.org\">\n" +
                "<met:WorkflowImpl>\n" +
                "    <met:WorkflowImplId>WorkflowEngine</met:WorkflowImplId>\n" +
                "    <met:WorkflowImplName>WorkflowEngine</met:WorkflowImplName>\n" +
                "    <met:WorkflowImplDescription>WorkflowEngine</met:WorkflowImplDescription>\n" +
                "    <met:TemplateId>MultiStepApprovalTemplate</met:TemplateId>\n" +
                "    <met:ParametersMetaData>\n" +
                "        <met:ParameterMetaData Name=\"HTSubject\" DataType=\"String\" InputType=\"Text\" " +
                "               isRequired=\"true\">\n" +
                "            <met:DisplayName> Subject(Approval task subject to display)</met:DisplayName>\n" +
                "        </met:ParameterMetaData>\n" +
                "        <met:ParameterMetaData Name=\"HTDescription\" DataType=\"String\" InputType=\"TextArea\">\n" +
                "            <met:DisplayName> Detail(Approval task description)</met:DisplayName>\n" +
                "        </met:ParameterMetaData>\n" +
                "    </met:ParametersMetaData>\n" +
                "</met:WorkflowImpl>\n" +
                "</met:MetaData>";
    }

    @Reference(
            name = "org.wso2.carbon.identity.workflow.mgt",
            service = org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetWorkflowManagementService")
    protected void setWorkflowManagementService(WorkflowManagementService workflowManagementService) {

        WorkflowEngineServiceDataHolder.getInstance().setWorkflowManagementService(workflowManagementService);
    }

    protected void unsetWorkflowManagementService(WorkflowManagementService workflowManagementService) {

        WorkflowEngineServiceDataHolder.getInstance().setWorkflowManagementService(null);
    }

    @Reference(
            name = "RoleManagementServiceComponent",
            service = RoleManagementService.class,
            cardinality = ReferenceCardinality.MANDATORY,
            policy = ReferencePolicy.DYNAMIC,
            unbind = "unsetRoleManagementService"
    )
    private void setRoleManagementService(RoleManagementService roleManagementService) {

        WorkflowEngineServiceDataHolder.getInstance().setRoleManagementService(roleManagementService);
    }

    private void unsetRoleManagementService(RoleManagementService roleManagementService) {

        WorkflowEngineServiceDataHolder.getInstance().setRoleManagementService(null);
    }
}
