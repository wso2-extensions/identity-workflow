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
                        WorkflowEngineConstants.ParameterHolder.WORKFLOW_NAME);

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
