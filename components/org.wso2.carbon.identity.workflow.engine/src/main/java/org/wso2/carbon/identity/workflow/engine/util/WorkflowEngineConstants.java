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

/**
 * This class holds the constants used in the module, identity-workflow.
 */
public class WorkflowEngineConstants {

    public static final String CURRENT_STEP_COLUMN = "CURRENT_STEP";
    public static final String TASK_ID_COLUMN = "TASK_ID";
    public static final String APPROVER_NAME_COLUMN = "APPROVER_NAME";
    public static final String APPROVER_TYPE_COLUMN = "APPROVER_TYPE";
    public static final String EVENT_ID = "EVENT_ID";
    public static final String WORKFLOW_ID = "WORKFLOW_ID";
    public static final String CREATED_USER_COLUMN = "CREATED_BY";
    public static final String TASK_STATUS_COLUMN = "TASK_STATUS";
    public static final String CREATED_AT_IN_MILL_COLUMN = "CREATED_AT";
    public static final String RELATIONSHIP_ID_IN_REQUEST_COLUMN = "RELATIONSHIP_ID";
    public static final String APPROVER_TYPE_USERS = "users";
    public static final String APPROVER_TYPE_ROLES = "roles";

    /**
     * SQL Query definitions.
     */
    public static class SqlQueries {

        public static final String ADD_APPROVAL_LIST_RELATED_TO_USER = "INSERT INTO WF_WORKFLOW_APPROVAL_RELATION " +
                "(TASK_ID,EVENT_ID,WORKFLOW_ID,APPROVER_TYPE,APPROVER_NAME, TASK_STATUS) VALUES (?,?,?,?,?,?)";
        public static final String GET_APPROVER_DETAILS_BY_TASK_ID = "SELECT APPROVER_TYPE, APPROVER_NAME FROM " +
                "WF_WORKFLOW_APPROVAL_RELATION WHERE TASK_ID = ?";
        public static final String DELETE_APPROVAL_TASK_BY_TASK_ID = "DELETE FROM WF_WORKFLOW_APPROVAL_RELATION " +
                "WHERE EVENT_ID = ? AND TASK_ID != ?";
        public static final String DELETE_APPROVAL_TASKS_OF_WORKFLOW_REQUEST = "DELETE FROM " +
                "WF_WORKFLOW_APPROVAL_RELATION WHERE EVENT_ID = ?";
        public static final String DELETE_PENDING_APPROVAL_TASKS_OF_WORKFLOW_REQUEST = "DELETE FROM " +
                "WF_WORKFLOW_APPROVAL_RELATION WHERE EVENT_ID = ? AND TASK_STATUS IN ('READY', 'RESERVED', 'BLOCKED')";
        public static final String ADD_CURRENT_STEP_FOR_EVENT = "INSERT INTO WF_WORKFLOW_APPROVAL_STATE " +
                "(EVENT_ID,WORKFLOW_ID, CURRENT_STEP) VALUES (?,?,1)";
        public static final String GET_CURRENT_STEP = "SELECT CURRENT_STEP FROM WF_WORKFLOW_APPROVAL_STATE WHERE " +
                "EVENT_ID = ? AND WORKFLOW_ID = ?";
        public static final String UPDATE_STATE_OF_REQUEST = "UPDATE WF_WORKFLOW_APPROVAL_STATE SET CURRENT_STEP=? " +
                "WHERE EVENT_ID = ? AND WORKFLOW_ID = ?";
        public static final String GET_APPROVER_NAME_RELATED_TO_CURRENT_TASK_ID = "SELECT DISTINCT APPROVER_NAME " +
                "FROM WF_WORKFLOW_APPROVAL_RELATION WHERE TASK_ID = ?";
        public static final String GET_APPROVER_TYPE_RELATED_TO_CURRENT_TASK_ID = "SELECT APPROVER_TYPE FROM" +
                " WF_WORKFLOW_APPROVAL_RELATION WHERE TASK_ID = ?";
        public static final String GET_WORKFLOW_ID = "SELECT DISTINCT WORKFLOW_ID FROM " +
                "WF_WORKFLOW_APPROVAL_RELATION " +
                "WHERE TASK_ID = ?";
        public static final String UPDATE_TASK_STATUS = "UPDATE WF_WORKFLOW_APPROVAL_RELATION SET TASK_STATUS=? " +
                "WHERE TASK_ID = ?";
        public static final String UPDATE_TASK_ENTITY_DETAILS = "UPDATE WF_WORKFLOW_APPROVAL_RELATION SET " +
                "APPROVER_TYPE = ?, APPROVER_NAME = ? WHERE TASK_ID = ?";
        public static final String GET_APPROVAL_TASK_BY_TASK_ID = "SELECT EVENT_ID, WORKFLOW_ID, APPROVER_TYPE, " +
                "APPROVER_NAME, TASK_STATUS FROM WF_WORKFLOW_APPROVAL_RELATION WHERE TASK_ID = ?";
        public static final String GET_TASK_ID_FROM_REQUEST = "SELECT TASK_ID FROM WF_WORKFLOW_APPROVAL_RELATION " +
                "WHERE EVENT_ID= ?";
        public static final String GET_APPROVAL_TASK_DETAILS_FROM_APPROVER =
                "SELECT TASK_ID, EVENT_ID, TASK_STATUS FROM WF_WORKFLOW_APPROVAL_RELATION INNER JOIN WF_REQUEST " +
                        "ON WF_WORKFLOW_APPROVAL_RELATION.EVENT_ID = WF_REQUEST.UUID " +
                        "WHERE WF_REQUEST.TENANT_ID = :" + SQLPlaceholders.TENANT_ID_PLACEHOLDER + "; AND " +
                        "APPROVER_NAME IN (" + SQLPlaceholders.ENTITY_ID_LIST_PLACEHOLDER + ") " +
                        "ORDER BY WF_REQUEST.UPDATED_AT DESC";
        public static final String GET_APPROVER_TASK_DETAILS_FROM_APPROVER_AND_TYPE_AND_STATUSES =
                "SELECT TASK_ID, EVENT_ID, TASK_STATUS FROM WF_WORKFLOW_APPROVAL_RELATION INNER JOIN WF_REQUEST " +
                        "ON WF_WORKFLOW_APPROVAL_RELATION.EVENT_ID = WF_REQUEST.UUID " +
                        "WHERE WF_REQUEST.TENANT_ID = :" + SQLPlaceholders.TENANT_ID_PLACEHOLDER + "; AND " +
                        "APPROVER_NAME IN (" + SQLPlaceholders.ENTITY_ID_LIST_PLACEHOLDER + ") AND " +
                        "TASK_STATUS IN (" + SQLPlaceholders.STATUS_LIST_PLACEHOLDER + ") " +
                        "ORDER BY WF_REQUEST.UPDATED_AT DESC";
        public static final String GET_TASK_STATUS = "SELECT DISTINCT TASK_STATUS FROM WF_WORKFLOW_APPROVAL_RELATION " +
                "WHERE TASK_ID = ?";
        public static final String GET_CREATED_USER = "SELECT CREATED_BY FROM WF_REQUEST WHERE UUID= ?";
        public static final String GET_CREATED_TIME_IN_MILL = "SELECT CREATED_AT FROM WF_REQUEST WHERE UUID= ?";
        public static final String GET_REQUEST_ID_OF_RELATIONSHIP = "SELECT RELATIONSHIP_ID FROM " +
                "WF_WORKFLOW_REQUEST_RELATION WHERE REQUEST_ID = ?";
    }

    /**
     * SQL Placeholders.
     * */
    public static final class SQLPlaceholders {

        public static final String ENTITY_ID_LIST_PLACEHOLDER = "_ENTITY_ID_LIST_";
        public static final String ENTITY_ID_PLACEHOLDER_PREFIX = "ENTITY_ID_";
        public static final String STATUS_LIST_PLACEHOLDER = "_STATUS_LIST_";
        public static final String STATUS_PLACEHOLDER_PREFIX = "STATUS_";
        public static final String TENANT_ID_PLACEHOLDER = "TENANT_ID";
    }

    /**
     * Holds constant parameter names.
     */
    public static class ParameterName {

        public static final String USER_AND_ROLE_STEP = "ApprovalSteps";
        public static final String REQUEST_ID = "REQUEST ID";
        public static final String PRIORITY = "High";
        public static final String APPROVAL_TASK = "Approval task";
        public static final String ASSIGNEE_TYPE = "Type";
        public static final String CREDENTIAL = "Credential";
        public static final String HT_SUBJECT = "ApprovalTaskSubject";
        public static final String INITIATED_BY = "Initiated_by ";
        public static final String ENTITY_TYPE_ROLES = "roles";
        public static final String ENTITY_TYPE_USERS = "users";
        public static final String ENTITY_TYPE_CLAIMED_USERS = "claimedUsers";
        public static final String CLAIMS_PROPERTY_NAME = "Claims";
    }

    /**
     * Represents the possible statuses of an approval task.
     */
    public enum TaskStatus {

        /** Task is ready to be assigned and worked on */
        READY,

        /** The Task has been reserved/claimed by an approver but not yet completed */
        RESERVED,

        /** The Task has been approved by the assigned approver */
        APPROVED,

        /** Task is blocked and cannot proceed */
        BLOCKED,

        /** The Task has been rejected by the assigned approver */
        REJECTED;
    }

    /**
     * Holds constant parameter keys used in workflow configurations.
     */
    public static class ParameterHolder {

        public static final String WORKFLOW_NAME = "WorkflowEngine";
    }

    /**
     * Holds constant parameter values used in workflow configurations.
     */
    public static class ParameterValue {

        public static final String WORKFLOW_NAME = "WorkflowName";
    }

    /**
     * Enum contains Error Codes and Error Messages.
     */
    public enum ErrorMessages {

        TASK_NOT_FOUND("SWE_00001", "Invalid event ID"),
        WORKFLOW_ID_NOT_FOUND("SWE_00003", "The workflow Id is not valid"),
        ERROR_OCCURRED_WHILE_RETRIEVING_WORKFLOW_REQUEST("SWE_00004",
                "Cannot get the workflow request given the request Id"),
        ERROR_OCCURRED_WHILE_RETRIEVING_PARAMETER_LIST("SWE_00005",
                "The parameterList can't get given the associated workflowId"),
        ERROR_OCCURRED_WHILE_CHANGING_APPROVALS_STATE("SWE_00006", "Unable to update the " +
                "approval status, " +
                "Server encountered an error while updating the approval task status."),
        ERROR_OCCURRED_WHILE_RETRIEVING_APPROVAL_OF_USER("SWE_00007", "Unable to retrieve the " +
                "user approval, " +
                "Server encountered an error while retrieving information on the approval task."),
        ERROR_OCCURRED_WHILE_RETRIEVING_APPROVAL_TASKS_FOR_USER("SWE_00008", "Unable to retrieve " +
                "approvals for the user, " +
                "Server encountered an error while retrieving approvals for user."),
        ERROR_RETRIEVING_ASSOCIATED_USER_ID("SWE_00009", "Unable to retrieve the user ID. " +
                "Server encountered an error while retrieving the user ID associated with the task."),
        USER_ERROR_NON_EXISTING_TASK_ID("SWE_10001", "Task does not exist."),
        USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE("10005", "Unacceptable input provided, " +
                "Only [CLAIM, RELEASE, APPROVED, REJECTED] are acceptable."),
        USER_ERROR_TASK_ALREADY_CLAIMED("SWE_10002", "Task already claimed by another user."),
        USER_ERROR_APPROVAL_TASK_IS_NOT_ASSIGNED("SWE_10003", "Approval task is not assigned to the " +
                "user.");

        private final String code;
        private final String description;

        /**
         * Error Messages
         *
         * @param code        Code of the error message.
         * @param description Error message string.
         */
        ErrorMessages(String code, String description) {

            this.code = code;
            this.description = description;
        }

        public String getCode() {

            return code;
        }

        public String getDescription() {

            return this.description;
        }

        @Override
        public String toString() {

            return this.code + " - " + this.description;
        }
    }

}
