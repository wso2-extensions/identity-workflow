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

import java.util.ArrayList;
import java.util.List;

/**
 * DTO class to represent an approval task.
 */
public class ApprovalTaskDTO extends ApprovalTaskSummaryDTO {

    private String subject = null;

    private String description = null;

    private String priority = null;

    private String initiator = null;

    private String createdTimeInMillis = null;

    private List<PropertyDTO> assignees = new ArrayList<PropertyDTO>();

    private List<PropertyDTO> properties = new ArrayList<PropertyDTO>();

    /**
     * Subject of the Approval.
     **/
    public String getSubject() {

        return subject;
    }

    /**
     * Set Subject of the Approval.
     **/
    public void setSubject(String subject) {

        this.subject = subject;
    }

    /**
     * Description on the Approval task.
     **/

    public String getDescription() {

        return description;
    }

    /**
     * Set Description on the Approval task.
     **/
    public void setDescription(String description) {

        this.description = description;
    }

    /**
     * Priority of the Approval task.
     **/
    public String getPriority() {

        return priority;
    }

    /**
     * Set Priority of the Approval task.
     **/
    public void setPriority(String priority) {

        this.priority = priority;
    }

    /**
     * The user who initiated the task.
     **/

    public String getInitiator() {

        return initiator;
    }

    /**
     * Set the user who initiated the task.
     **/
    public void setInitiator(String initiator) {

        this.initiator = initiator;
    }

    /**
     * To whom the task is assigned:\n  * user - username(s) if the task is reserved for specific user(s).\n
     * * group - role name(s) if the task is assignable for group(s).\n
     **/
    public List<PropertyDTO> getAssignees() {

        return assignees;
    }

    public void setAssignees(List<PropertyDTO> assignees) {

        this.assignees = assignees;
    }

    /**
     * Task parameters: username, role, claims, requestId.
     **/
    public List<PropertyDTO> getProperties() {

        return properties;
    }

    /**
     * Set Task parameters: username, role, claims, requestId.
     **/
    public void setProperties(List<PropertyDTO> properties) {

        this.properties = properties;
    }

    /**
     * The created time of the request.
     **/
    public String getCreatedTimeInMillis() {

        return createdTimeInMillis;
    }

    /**
     * Set the created time of the request.
     **/
    public void setCreatedTimeInMillis(String createdTimeInMillis) {

        this.createdTimeInMillis = createdTimeInMillis;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class TaskDataDTO {\n");
        sb.append("  subject: ").append(subject).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  priority: ").append(priority).append("\n");
        sb.append("  initiator: ").append(initiator).append("\n");
        sb.append("  assignees: ").append(assignees).append("\n");
        sb.append("  properties: ").append(properties).append("\n");
        sb.append("  createdTimeInMillis: ").append(createdTimeInMillis).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
