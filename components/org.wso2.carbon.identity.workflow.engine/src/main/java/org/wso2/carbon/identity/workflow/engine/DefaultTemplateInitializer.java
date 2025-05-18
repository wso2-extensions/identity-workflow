package org.wso2.carbon.identity.workflow.engine;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowManagementUtil;
import org.wso2.carbon.identity.workflow.mgt.workflow.TemplateInitializer;
import org.wso2.carbon.utils.multitenancy.MultitenantConstants;

import java.util.List;

/**
 * Implementation of TemplateInitializer interface that serves as the extension point for external workflow templates.
 */
public class DefaultTemplateInitializer implements TemplateInitializer {

    private String processName;
    private String htName;
    private String role;
    private String tenantContext = "";
    private static final String HT_SUFFIX = "Task";

    /**
     * Initialize template at start up.
     *
     * @return false.
     */
    @Override
    public boolean initNeededAtStartUp() {

        return false;
    }

    /**
     * Initialize template.
     *
     * @param list parameters of template.
     */
    @Override
    public void initialize(List<Parameter> list) {

        Parameter wfNameParameter = WorkflowManagementUtil
                .getParameter(list, WorkflowEngineConstants.ParameterValue.WORKFLOW_NAME,
                        WorkflowEngineConstants.ParameterHolder
                        .WORKFLOW_IMPL);

        if (wfNameParameter != null) {
            processName = StringUtils.deleteWhitespace(wfNameParameter.getParamValue());
            role = WorkflowManagementUtil
                    .createWorkflowRoleName(StringUtils.deleteWhitespace(wfNameParameter.getParamValue()));
        }

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        if (!MultitenantConstants.SUPER_TENANT_DOMAIN_NAME.equals(tenantDomain)) {
            tenantContext = "t/" + tenantDomain + "/";
        }
        htName = processName + DefaultTemplateInitializer.HT_SUFFIX;
    }
}
