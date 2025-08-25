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

package org.wso2.carbon.identity.workflow.engine.dto;

/**
 * DTO class to represent an approver in the workflow engine.
 */
public class ApproverDTO {

    private String approverName;
    private String approverType;
    private String taskStatus;

    public String getApproverName() {

        return approverName;
    }

    public void setApproverName(String approverName) {

        this.approverName = approverName;
    }

    public String getApproverType() {

        return approverType;
    }

    public void setApproverType(String approverType) {

        this.approverType = approverType;
    }

    public String getTaskStatus() {

        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {

        this.taskStatus = taskStatus;
    }
}
