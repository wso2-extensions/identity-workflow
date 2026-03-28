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

package org.wso2.carbon.identity.workflow.engine.dto;

import java.util.Collections;
import java.util.List;

/**
 * DTO class to represent filter criteria for listing approval tasks.
 */
public class ApprovalTaskFilterDTO {

    private List<String> statusList;
    private List<String> operationTypeList;
    private List<FilterCondition> filterConditions;

    public List<String> getStatusList() {

        return statusList;
    }

    public void setStatusList(List<String> statusList) {

        this.statusList = statusList;
    }

    public List<String> getOperationTypeList() {

        return operationTypeList;
    }

    public void setOperationTypeList(List<String> operationTypeList) {

        this.operationTypeList = operationTypeList;
    }

    public List<FilterCondition> getFilterConditions() {

        return filterConditions != null ? filterConditions : Collections.emptyList();
    }

    public void setFilterConditions(List<FilterCondition> filterConditions) {

        this.filterConditions = filterConditions;
    }
}
