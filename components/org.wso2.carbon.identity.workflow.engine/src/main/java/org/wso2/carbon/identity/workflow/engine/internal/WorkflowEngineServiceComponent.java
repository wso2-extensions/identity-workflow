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
        name = "simple.workflow.engine",
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
                "    <met:WorkflowImplId>workflowImplSimple</met:WorkflowImplId>\n" +
                "    <met:WorkflowImplName>SimpleWorkflowEngine</met:WorkflowImplName>\n" +
                "    <met:WorkflowImplDescription>Simple WorkflowEngine</met:WorkflowImplDescription>\n" +
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
