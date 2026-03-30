/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

import org.wso2.carbon.identity.workflow.engine.dto.FilterCondition;
import org.wso2.carbon.identity.workflow.engine.dto.FilterOperator;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineClientException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Utility class to parse raw filter strings into structured {@link FilterCondition} objects for approval task queries.
 */
public class FilterParser {

    private static final String AND_SEPARATOR_REGEX = "(?i)\\s+and\\s+";
    private static final Set<String> SUPPORTED_ATTRIBUTES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    WorkflowEngineConstants.FILTER_ATTRIBUTE_WORKFLOW_ID,
                    WorkflowEngineConstants.FILTER_ATTRIBUTE_REQUEST_ID
            ))
    );

    private FilterParser() {
    }

    /**
     * Parse a filter string into a list of {@link FilterCondition} objects.
     *
     * @param filterString the raw filter expression, or {@code null} / empty.
     * @return an unmodifiable list of filter conditions; empty if {@code filterString} is blank.
     * @throws WorkflowEngineClientException if the expression is syntactically invalid or references
     *                                       an unsupported attribute or operator.
     */
    public static List<FilterCondition> parse(String filterString) throws WorkflowEngineClientException {

        if (filterString == null || filterString.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String[] clauses = filterString.trim().split(AND_SEPARATOR_REGEX);
        List<FilterCondition> conditions = new ArrayList<>(clauses.length);

        for (String clause : clauses) {
            // Split into at most 3 tokens: attribute, operator, value (value may contain spaces)
            String[] tokens = clause.trim().split("\\s+", 3);
            if (tokens.length != 3) {
                throw new WorkflowEngineClientException(
                        WorkflowEngineConstants.ErrorMessages.INVALID_FILTER_EXPRESSION.getDescription() +
                                " Invalid clause: '" + clause.trim() + "'",
                        WorkflowEngineConstants.ErrorMessages.INVALID_FILTER_EXPRESSION.getCode());
            }

            String attribute = tokens[0];
            String operatorStr = tokens[1];
            String value = tokens[2];

            if (!SUPPORTED_ATTRIBUTES.contains(attribute)) {
                throw new WorkflowEngineClientException(
                        WorkflowEngineConstants.ErrorMessages.INVALID_FILTER_EXPRESSION.getDescription() +
                                " Unsupported filter attribute: '" + attribute + "'",
                        WorkflowEngineConstants.ErrorMessages.INVALID_FILTER_EXPRESSION.getCode());
            }

            FilterOperator operator;
            try {
                operator = FilterOperator.fromString(operatorStr);
            } catch (IllegalArgumentException e) {
                throw new WorkflowEngineClientException(
                        WorkflowEngineConstants.ErrorMessages.INVALID_FILTER_EXPRESSION.getDescription() +
                                " Unsupported filter operator: '" + operatorStr + "'",
                        WorkflowEngineConstants.ErrorMessages.INVALID_FILTER_EXPRESSION.getCode());
            }

            conditions.add(new FilterCondition(attribute, operator, value));
        }

        return Collections.unmodifiableList(conditions);
    }
}
