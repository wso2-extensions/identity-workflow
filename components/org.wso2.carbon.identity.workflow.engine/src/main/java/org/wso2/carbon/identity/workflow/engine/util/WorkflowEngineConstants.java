package org.wso2.carbon.identity.workflow.engine.util;

/**
 * This class holds the constants used in the module, identity-workflow-simple.
 */
public class WorkflowEngineConstants {

    public static final String CURRENT_STEP_COLUMN = "CURRENT_STEP";
    public static final String TASK_ID_COLUMN = "TASK_ID";
    public static final String APPROVER_NAME_COLUMN = "APPROVER_NAME";
    public static final String APPROVER_TYPE_COLUMN = "APPROVER_TYPE";
    public static final String EVENT_ID = "EVENT_ID";
    public static final String WORKFLOW_ID = "WORKFLOW_ID";
    public static final String WORKFLOW_NAME = "WF_NAME";
    public static final String CREATED_USER_COLUMN = "CREATED_BY";
    public static final String EVENT_TYPE_COLUMN = "OPERATION_TYPE";
    public static final String ENTITY_NAME = "ENTITY_NAME";
    public static final String TASK_STATUS_COLUMN = "TASK_STATUS";
    public static final String CREATED_AT_IN_MILL_COLUMN = "CREATED_AT";
    public static final String RELATIONSHIP_ID_IN_REQUEST_COLUMN = "RELATIONSHIP_ID";
    public static final String ROLE_ID_COLUMN = "UM_ROLE_ID";
    public static final String ROLE_NAME = "UM_ROLE_NAME";
    public static final String WORKFLOW_ASSOCIATION_NAME = "ASSOC_NAME";
    public static final String APPROVER_TYPE_USERS = "users";
    public static final String APPROVER_TYPE_ROLES = "roles";

    /**
     * SQL Query definitions.
     */
    public static class SqlQueries {

        public static final String ADD_APPROVAL_LIST_RELATED_TO_USER = "INSERT INTO WF_WORKFLOW_APPROVAL_RELATION " +
                "(TASK_ID,EVENT_ID,WORKFLOW_ID,APPROVER_TYPE,APPROVER_NAME, TASK_STATUS) VALUES (?,?,?,?,?,?)";
        public static final String GET_TASK_ID_RELATED_TO_USER = "SELECT DISTINCT TASK_ID FROM WF_WORKFLOW_" +
                "APPROVAL_RELATION WHERE EVENT_ID = ?";
        public static final String DELETE_APPROVAL_LIST_RELATED_TO_USER = "DELETE FROM WF_WORKFLOW_APPROVAL_RELATION" +
                " WHERE TASK_ID=?";
        public static final String ADD_CURRENT_STEP_FOR_EVENT = "INSERT INTO WF_WORKFLOW_APPROVAL_STATE " +
                "(EVENT_ID,WORKFLOW_ID, CURRENT_STEP) VALUES (?,?,?)";
        public static final String GET_CURRENT_STEP = "SELECT CURRENT_STEP FROM WF_WORKFLOW_APPROVAL_STATE WHERE " +
                "EVENT_ID = ? AND WORKFLOW_ID = ?";
        public static final String UPDATE_STATE_OF_REQUEST = "UPDATE WF_WORKFLOW_APPROVAL_STATE SET CURRENT_STEP=? " +
                "WHERE EVENT_ID = ? AND WORKFLOW_ID = ?";
        public static final String DELETE_CURRENT_STEP_OF_REQUEST = "DELETE FROM WF_WORKFLOW_APPROVAL_STATE " +
                "WHERE EVENT_ID=?";
        public static final String GET_APPROVER_NAME_RELATED_TO_CURRENT_TASK_ID = "SELECT DISTINCT APPROVER_NAME " +
                "FROM WF_WORKFLOW_APPROVAL_RELATION WHERE TASK_ID = ?";
        public static final String GET_APPROVER_TYPE_RELATED_TO_CURRENT_TASK_ID = "SELECT APPROVER_TYPE FROM" +
                " WF_WORKFLOW_APPROVAL_RELATION WHERE TASK_ID = ?";
        public static final String GET_WORKFLOW_ID = "SELECT DISTINCT WORKFLOW_ID FROM " +
                "WF_WORKFLOW_APPROVAL_RELATION " +
                "WHERE TASK_ID = ?";
        public static final String UPDATE_TASK_STATUS = "UPDATE WF_WORKFLOW_APPROVAL_RELATION SET TASK_STATUS=? " +
                "WHERE TASK_ID=?";
        public static final String GET_REQUEST_ID = "SELECT DISTINCT EVENT_ID FROM WF_WORKFLOW_APPROVAL_RELATION " +
                "WHERE TASK_ID = ?";
        public static final String GET_ENTITY_NAME = "SELECT ENTITY_NAME FROM WF_REQUEST_ENTITY_RELATIONSHIP " +
                "WHERE REQUEST_ID = ?";
        public static final String GET_TASK_ID_FROM_REQUEST = "SELECT TASK_ID FROM WF_WORKFLOW_APPROVAL_RELATION " +
                "WHERE EVENT_ID= ?";
        public static final String GET_REQUEST_ID_FROM_APPROVER = "SELECT EVENT_ID FROM " +
                "WF_WORKFLOW_APPROVAL_RELATION WHERE APPROVER_NAME= ?";
        public static final String GET_REQUEST_ID_FROM_APPROVER_AND_TYPE = "SELECT EVENT_ID FROM " +
                "WF_WORKFLOW_APPROVAL_RELATION WHERE APPROVER_NAME= ? AND APPROVER_TYPE= ?";
        public static final String GET_TASK_ID_FROM_APPROVER_AND_TYPE =
                "SELECT TASK_ID FROM WF_WORKFLOW_APPROVAL_RELATION " +
                        "WHERE APPROVER_NAME = ? AND APPROVER_TYPE = ?";
        public static final String GET_TASK_ID_FROM_APPROVER_AND_TYPE_BY_STATUS =
                "SELECT TASK_ID FROM WF_WORKFLOW_APPROVAL_RELATION " +
                        "WHERE APPROVER_NAME = ? AND APPROVER_TYPE = ? AND TASK_STATUS = ?";
        public static final String GET_REQUEST_ID_FROM_APPROVER_AND_TYPE_BY_STATUS = "SELECT EVENT_ID FROM " +
                "WF_WORKFLOW_APPROVAL_RELATION WHERE APPROVER_NAME= ? AND APPROVER_TYPE= ? AND TASK_STATUS= ?";
        public static final String GET_TASK_STATUS = "SELECT DISTINCT TASK_STATUS FROM WF_WORKFLOW_APPROVAL_RELATION " +
                "WHERE TASK_ID = ?";
        public static final String GET_STATUS = "SELECT DISTINCT TASK_STATUS FROM WF_WORKFLOW_APPROVAL_RELATION " +
                "WHERE EVENT_ID = ?";
        public static final String GET_CREATED_USER = "SELECT CREATED_BY FROM WF_REQUEST WHERE UUID= ?";
        public static final String GET_EVENT_TYPE = "SELECT OPERATION_TYPE FROM WF_REQUEST WHERE UUID= ?";
        public static final String GET_CREATED_TIME_IN_MILL = "SELECT CREATED_AT FROM WF_REQUEST WHERE UUID= ?";
        public static final String GET_REQUEST_ID_OF_RELATIONSHIP = "SELECT RELATIONSHIP_ID FROM " +
                "WF_WORKFLOW_REQUEST_RELATION WHERE REQUEST_ID = ?";
        public static final String GET_WORKFLOW_NAME = "SELECT WF_NAME FROM WF_WORKFLOW WHERE ID =? ";
        public static final String GET_ROLE_ID = "SELECT UM_ROLE_ID FROM UM_HYBRID_USER_ROLE WHERE UM_USER_NAME=?";
        public static final String GET_ROLE_NAME = "SELECT UM_ROLE_NAME FROM UM_HYBRID_ROLE WHERE UM_ID=?";
        public static final String GET_WORKFLOW_ASSOCIATION_NAME = "SELECT ASSOC_NAME FROM WF_WORKFLOW_ASSOCIATION " +
                "WHERE WORKFLOW_ID = ? AND EVENT_ID = ?";
    }

    /**
     * Holds constant parameter names.
     */
    public static class ParameterName {

        public static final String USER_AND_ROLE_STEP = "ApprovalSteps";
        public static final String TASK_STATUS_DEFAULT = "RESERVED";
        public static final String TASK_STATUS_READY = "READY";
        public static final String REQUEST_ID = "REQUEST ID";
        public static final String TASK_SUBJECT = "ApprovalTaskSubject";
        public static final String TASK_DESCRIPTION = "ApprovalTaskDescription";
        public static final String PRIORITY = "High";
        public static final String APPROVAL_TASK = "Approval task";
        public static final String INTERNAL_USER = "Internal/";
        public static final String APPLICATION_USER = "Application";
        public static final String SYSTEM_USER = "system";
        public static final String SYSTEM_PRIMARY_USER = "system_primary_";
        public static final String ASSIGNEE_TYPE = "Type";
        public static final String CREDENTIAL = "Credential";
        public static final String HT_SUBJECT = "ApprovalTaskSubject";
        public static final String INITIATED_BY = "Initiated_by ";
        public static final String ENTITY_NAME = " User_Name:";
        public static final String ENTITY_TYPE_ROLES = "roles";
        public static final String ENTITY_TYPE_USERS = "users";
        public static final String ENTITY_TYPE_CLAIMED_USERS = "claimedUsers";
    }

    /**
     * Represents the possible statuses of an approval task.
     */
    public enum TaskStatus {
        READY,
        RESERVED,
        COMPLETED,
        BLOCKED;

        TaskStatus() {

        }
    }

    /**
     * Holds constant parameter keys used in workflow configurations.
     */
    public static class ParameterHolder {

        public static final String WORKFLOW_IMPL = "WorkflowImpl";
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
        ASSOCIATION_NOT_FOUND("SWE_00002", "The associations are not connecting with any request"),
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
        ERROR_OCCURRED_WHILE_RETRIEVING_APPROVALS_FOR_USER("SWE_00008", "Unable to retrieve " +
                "approvals for the user, " +
                "Server encountered an error while retrieving approvals for user."),
        USER_ERROR_NON_EXISTING_TASK_ID("SWE_10001", "Task does not exist."),
        USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE("10005", "Unacceptable input provided, " +
                "Only [CLAIM, RELEASE, APPROVED, REJECTED] are acceptable.");
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
