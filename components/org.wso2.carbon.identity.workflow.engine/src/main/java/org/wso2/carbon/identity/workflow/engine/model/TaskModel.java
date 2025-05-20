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

package org.wso2.carbon.identity.workflow.engine.model;

import java.util.Map;

/**
 * Model Class for task.
 */
public class TaskModel {

    private String id;
    private Map<String, String> assignees;

    /**
     * Unique ID to represent a approval task
     **/
    public String getId() {

        return id;
    }

    /**
     * Set unique ID to represent a approval task
     **/
    public void setId(String id) {

        this.id = id;
    }

    /**
     * To whom the task is assigned:\n  * user - username(s) if the task is reserved for specific user(s).
     * group - role name(s) if the task is assignable for group(s).
     **/
    public Map<String, String> getAssignees() {

        return assignees;
    }

    /**
     * Set to whom the task is assigned:\n  * user - username(s) if the task is reserved for specific user(s).
     * group - role name(s) if the task is assignable for group(s).
     **/
    public void setAssignees(Map<String, String> assignees) {

        this.assignees = assignees;
    }
}
