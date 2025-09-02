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
 * DTO class to represent an approval task data in WF_WORKFLOW_APPROVAL_RELATION.
 */
public class ApprovalTaskRelationDTO {

    private String taskId = null;

    private String eventId = null;

    private String workflowId = null;

    private String approverType = null;

    private String approverName = null;

    private String taskStatus = null;

    /**
     * Unique ID of the task.
     **/
    public String getTaskId() {

        return taskId;
    }

    /**
     * Set Unique ID of the task.
     **/
    public void setTaskId(String taskId) {

        this.taskId = taskId;
    }

    /**
     * The request ID corresponding to the task.
     **/
    public String getEventId() {

        return eventId;
    }

    /**
     * Set The request ID.
     **/
    public void setEventId(String eventId) {

        this.eventId = eventId;
    }

    /**
     * The workflow ID corresponding to the task.
     **/
    public String getWorkflowId() {

        return workflowId;
    }

    /**
     * Set The workflow ID.
     **/
    public void setWorkflowId(String workflowId) {

        this.workflowId = workflowId;
    }

    /**
     * The type of the approved user EX: user or Role.
     **/
    public String getApproverType() {

        return approverType;
    }

    /**
     * Set The type of the approved user EX: user or Role.
     **/
    public void setApproverType(String approverType) {

        this.approverType = approverType;
    }

    /**
     * The value of the approver type.
     **/
    public String getApproverName() {

        return approverName;
    }

    /**
     * Set The value of the approver type.
     **/
    public void setApproverName(String approverName) {

        this.approverName = approverName;
    }

    /**
     * State of the tasks [RESERVED, READY, BLOCKED, REJECTED or APPROVED].
     **/
    public String getTaskStatus() {

        return taskStatus;
    }

    /**
     * Set State of the tasks [RESERVED, READY, BLOCKED, REJECTED or APPROVED].
     **/
    public void setTaskStatus(String taskStatus) {

        this.taskStatus = taskStatus;
    }
}
