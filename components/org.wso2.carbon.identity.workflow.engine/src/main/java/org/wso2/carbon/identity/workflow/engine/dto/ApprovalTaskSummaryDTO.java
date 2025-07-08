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
 * DTO class to represent a summary of an approval task.
 */
public class ApprovalTaskSummaryDTO {

    private String id = null;

    private String name = null;

    private String presentationSubject = null;

    private String presentationName = null;

    private String taskType = null;

    private String requestId = null;

    private String approvalStatus = null;

    private String priority = null;

    private String createdTimeInMillis = null;

    /**
     * Unique ID to represent an Approval Task
     **/
    public String getId() {

        return id;
    }

    /**
     * Set Unique ID to represent an Approval Task
     **/
    public void setId(String id) {

        this.id = id;
    }

    /**
     * Unique name for the Approval Task
     **/
    public String getName() {

        return name;
    }

    /**
     * Set Unique name for the Approval Task
     **/
    public void setName(String name) {

        this.name = name;
    }

    /**
     * Display value for Approval Operation
     **/
    public String getPresentationSubject() {

        return presentationSubject;
    }

    /**
     * Set display value for Approval Operation
     **/
    public void setPresentationSubject(String presentationSubject) {

        this.presentationSubject = presentationSubject;
    }

    /**
     * Display value for Approval Task
     **/
    public String getPresentationName() {

        return presentationName;
    }

    /**
     * Set display value for Approval Task
     **/
    public void setPresentationName(String presentationName) {

        this.presentationName = presentationName;
    }

    /**
     * Type of the Approval
     **/
    public String getTaskType() {

        return taskType;
    }

    /**
     * Set type of the Approval
     **/
    public void setTaskType(String taskType) {

        this.taskType = taskType;
    }

    /**
     * get the status of the approval task.
     **/
    public String getApprovalStatus() {

        return approvalStatus;
    }

    /**
     * Set the status of the approval task.
     **/
    public void setApprovalStatus(String approvalStatus) {

        this.approvalStatus = approvalStatus;
    }

    /**
     * Priority of the Approval task
     **/
    public String getPriority() {

        return priority;
    }

    /**
     * Set priority of the Approval task
     **/
    public void setPriority(String priority) {

        this.priority = priority;
    }

    /**
     * The time that the operation for approval initiated
     **/
    public String getCreatedTimeInMillis() {

        return createdTimeInMillis;
    }

    /**
     * Set the time that the operation for approval initiated
     **/
    public void setCreatedTimeInMillis(String createdTimeInMillis) {

        this.createdTimeInMillis = createdTimeInMillis;
    }

    public String getRequestId() {

        return requestId;
    }

    public void setRequestId(String requestId) {

        this.requestId = requestId;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class TaskSummaryDTO {\n");
        sb.append("  id: ").append(id).append("\n");
        sb.append("  name: ").append(name).append("\n");
        sb.append("  presentationSubject: ").append(presentationSubject).append("\n");
        sb.append("  presentationName: ").append(presentationName).append("\n");
        sb.append("  taskType: ").append(taskType).append("\n");
        sb.append("  approvalStatus: ").append(approvalStatus).append("\n");
        sb.append("  priority: ").append(priority).append("\n");
        sb.append("  createdTimeInMillis: ").append(createdTimeInMillis).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
