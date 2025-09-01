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

import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Utility class for workflow-related operations.
 */
public class Utils {

    public static Map<Integer, List<String>> getParamValuesForApprovalSteps(List<Parameter> parameterList) {

        // Approval step number as key and list of parameter values as value.
        Map<Integer, List<String>> paramValuesForApprovalSteps = new HashMap<>();
        String approverName;
        int step;
        if (parameterList != null) {
            for (Parameter parameter : parameterList) {
                if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                    String[] stepName = parameter.getqName().split("-");
                    step = Integer.parseInt(stepName[1]);
                    approverName = parameter.getParamValue();
                    paramValuesForApprovalSteps.computeIfAbsent(step, k -> new java.util.ArrayList<>())
                            .add(approverName);
                }
            }
        }
        return paramValuesForApprovalSteps;
    }

    public static List<Integer> getModifiedApprovalSteps(List<Parameter> newParams, List<Parameter> oldParams) {

        Map<Integer, List<String>> newParamValuesForApprovalSteps = getParamValuesForApprovalSteps(newParams);
        Map<Integer, List<String>> existingParamValuesForApprovalSteps = getParamValuesForApprovalSteps(oldParams);
        List<Integer> modifiedSteps = new java.util.ArrayList<>();

        // Check for modified and new steps.
        for (Map.Entry<Integer, List<String>> entry : newParamValuesForApprovalSteps.entrySet()) {
            Integer step = entry.getKey();
            List<String> newApproverTypes = entry.getValue();
            List<String> existingApproverTypes = existingParamValuesForApprovalSteps.get(step);
            if (existingApproverTypes == null || !new HashSet<>(newApproverTypes).containsAll(existingApproverTypes) ||
                    !new HashSet<>(existingApproverTypes).containsAll(newApproverTypes)) {
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
}
