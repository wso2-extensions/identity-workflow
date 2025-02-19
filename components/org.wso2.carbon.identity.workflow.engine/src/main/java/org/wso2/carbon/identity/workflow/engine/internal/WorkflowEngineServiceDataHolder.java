package org.wso2.carbon.identity.workflow.engine.internal;

import org.osgi.framework.BundleContext;
import org.wso2.carbon.identity.workflow.mgt.WorkflowManagementService;
import org.wso2.carbon.identity.workflow.mgt.workflow.AbstractWorkflow;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleManagementService;

import java.util.HashMap;
import java.util.Map;

/**
 * A class to keep the data of the workflow engine component.
 */
public class WorkflowEngineServiceDataHolder {

    private static WorkflowEngineServiceDataHolder instance = new WorkflowEngineServiceDataHolder();

    private WorkflowManagementService workflowManagementService;

    private RoleManagementService roleManagementService;

    private BundleContext bundleContext;

    private WorkflowEngineServiceDataHolder() {

    }

    public static WorkflowEngineServiceDataHolder getInstance() {

        return instance;
    }

    public WorkflowManagementService getWorkflowManagementService() {

        return workflowManagementService;
    }

    public BundleContext getBundleContext() {

        return bundleContext;
    }

    public void setBundleContext(BundleContext bundleContext) {

        this.bundleContext = bundleContext;
    }

    public void setWorkflowManagementService(
            WorkflowManagementService workflowManagementService) {

        this.workflowManagementService = workflowManagementService;
    }

    public RoleManagementService getRoleManagementService() {
        return roleManagementService;
    }

    public void setRoleManagementService(RoleManagementService roleManagementService){
        this.roleManagementService = roleManagementService;
    }

    private Map<String, Map<String, AbstractWorkflow>> workflowImpls = new HashMap<String, Map<String, AbstractWorkflow>>();

    public Map<String, Map<String, AbstractWorkflow>> getWorkflowImpls() {

        return workflowImpls;
    }

}
