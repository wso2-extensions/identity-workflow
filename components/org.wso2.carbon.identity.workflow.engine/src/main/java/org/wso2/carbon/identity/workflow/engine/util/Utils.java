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

package org.wso2.carbon.identity.workflow.engine.util;

import org.apache.commons.lang.StringUtils;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.organization.management.organization.user.sharing.util.OrganizationSharedUserUtil;
import org.wso2.carbon.identity.organization.management.service.exception.OrganizationManagementException;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineException;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Utility class for workflow-related operations.
 */
public class Utils {

    /**
     * Extracts parameter values for approval steps from the given list of parameters.
     *
     * @param parameterList List of parameters to extract values from.
     * @return A map where the key: approval step number, value: a list of parameter values for that step.
     */
    public static Map<Integer, List<String>> getParamValuesForApprovalSteps(List<Parameter> parameterList) {

        // Approval step number as key and list of parameter values as value.
        Map<Integer, List<String>> paramValuesForApprovalSteps = new HashMap<>();
        if (parameterList != null) {
            for (Parameter parameter : parameterList) {
                if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                    String[] stepName = parameter.getqName().split(WorkflowEngineConstants.Q_NAME_STEP_SEPARATOR);
                    int step = Integer.parseInt(stepName[1]);
                    String approverName = parameter.getParamValue();
                    paramValuesForApprovalSteps.computeIfAbsent(step, k -> new ArrayList<>())
                            .add(approverName);
                }
            }
        }
        return paramValuesForApprovalSteps;
    }

    /**
     * Compares two lists of parameters and identifies modified approval steps.
     *
     * @param newParams List of new parameters.
     * @param oldParams List of old parameters.
     * @return A list of approval step numbers that have been modified.
     */
    public static List<Integer> getModifiedApprovalSteps(List<Parameter> newParams, List<Parameter> oldParams) {

        Map<Integer, List<String>> newParamValuesForApprovalSteps = getParamValuesForApprovalSteps(newParams);
        Map<Integer, List<String>> existingParamValuesForApprovalSteps = getParamValuesForApprovalSteps(oldParams);
        List<Integer> modifiedSteps = new java.util.ArrayList<>();

        // Check for modified and new steps.
        for (Map.Entry<Integer, List<String>> approvalStep : newParamValuesForApprovalSteps.entrySet()) {
            Integer step = approvalStep.getKey();
            List<String> newApproverTypes = approvalStep.getValue();
            List<String> existingApproverTypes = existingParamValuesForApprovalSteps.get(step);
            Set<String> existingSet = existingApproverTypes == null ? null : new HashSet<>(existingApproverTypes);
            Set<String> newSet = new HashSet<>(newApproverTypes);
            if (existingSet == null || !existingSet.equals(newSet)) {
                modifiedSteps.add(step);
            }
        }

        // Check for removed steps.
        for (Integer step : existingParamValuesForApprovalSteps.keySet()) {
            if (!newParamValuesForApprovalSteps.containsKey(step)) {
                modifiedSteps.add(step);
            }
        }
        return modifiedSteps;
    }

    /**
     * Resolves the user ID based on the organization context.
     *
     * @param userId The original user ID.
     * @return The resolved user ID.
     * @throws WorkflowEngineException If there is an error during the resolution process.
     */
    public static String resolveUserID(String userId) throws WorkflowEngineException {

        String orgId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getOrganizationId();
        String userResidentOrgId = PrivilegedCarbonContext.getThreadLocalCarbonContext()
                .getUserResidentOrganizationId();
        if (!StringUtils.equals(orgId, userResidentOrgId)) {
            try {
                Optional<String> optionalUserId = OrganizationSharedUserUtil
                        .getUserIdOfAssociatedUserByOrgId(userId, orgId);
                if (optionalUserId.isPresent()) {
                    userId = optionalUserId.get();
                }
            } catch (OrganizationManagementException e) {
                throw new WorkflowEngineException(
                        WorkflowEngineConstants.ErrorMessages.ERROR_RETRIEVING_ASSOCIATED_USER_ID.getDescription(), e);
            }
        }

        return userId;
    }
}
