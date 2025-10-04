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

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.CarbonConstants;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.identity.central.log.mgt.utils.LoggerUtils;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.user.core.util.UserCoreUtil;
import org.wso2.carbon.utils.AuditLog;
import org.wso2.carbon.utils.multitenancy.MultitenantUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Approval task V2 audit logger class.
 * This class is responsible for logging approval task related operations with proper validation,
 * error handling, and structured audit logging.
 */
public class ApprovalTaskAuditLogger {

    private static final Log log = LogFactory.getLog(ApprovalTaskAuditLogger.class);

    private static final String TASK_ID = "TaskId";
    private static final String WORKFLOW_REQUEST_ID = "WorkflowRequestId";
    private static final String WORKFLOW_ID = "WorkflowId";
    private static final String APPROVER_TYPE = "ApproverType";
    private static final String APPROVER_ID = "ApproverId";
    private static final String TASK_EXISTING_STATUS = "TaskExistingStatus";
    private static final String TASK_NEW_STATUS = "TaskNewStatus";
    private static final String STEP_VALUE = "StepValue";

    /**
     * Create a new audit log builder for configuring and triggering audit logs.
     *
     * @return AuditLogBuilder instance for fluent configuration
     */
    public AuditLogBuilder auditBuilder() {

        return new AuditLogBuilder();
    }

    /**
     * Build audit log with the given operation and data map.
     *
     * @param operation The operation performed.
     * @param targetId  The target ID (usually task ID).
     * @param dataMap   The data map containing relevant information.
     */
    public void triggerAuditLog(Operation operation, String targetId, Map<String, Object> dataMap) {

        if (operation == null) {
            log.warn("Operation is null. Cannot trigger audit log.");
            return;
        }
        if (StringUtils.isBlank(targetId)) {
            log.warn("Target ID is blank. Cannot trigger audit log.");
            return;
        }
        String initiatorId = getInitiatorId();
        AuditLog.AuditLogBuilder auditLogBuilder = new AuditLog.AuditLogBuilder(
                initiatorId,
                LoggerUtils.getInitiatorType(initiatorId),
                targetId,
                LoggerUtils.Target.ApprovalTask.name(),
                operation.getLogAction()).
                data(dataMap);
        LoggerUtils.triggerAuditLogEvent(auditLogBuilder);
    }

    /**
     * To get the current user, who is doing the current task.
     *
     * @return current logged-in user.
     */
    private String getUser() {

        String user = CarbonContext.getThreadLocalCarbonContext().getUsername();
        if (StringUtils.isNotEmpty(user)) {
            user = UserCoreUtil.addTenantDomainToEntry(
                    user, CarbonContext.getThreadLocalCarbonContext().getTenantDomain());
        } else {
            user = CarbonConstants.REGISTRY_SYSTEM_USERNAME;
        }
        return user;
    }

    /**
     * Get the initiator for audit logs.
     *
     * @return Initiator id despite masking.
     */
    private String getInitiatorId() {

        String initiator = null;
        String username = MultitenantUtils.getTenantAwareUsername(getUser());
        String tenantDomain = MultitenantUtils.getTenantDomain(getUser());
        if (StringUtils.isNotBlank(username) && StringUtils.isNotBlank(tenantDomain)) {
            initiator = IdentityUtil.getInitiatorId(username, tenantDomain);
        }
        if (StringUtils.isBlank(initiator)) {
            if (username.equals(CarbonConstants.REGISTRY_SYSTEM_USERNAME)) {
                // If the initiator is wso2.system, the username need not be masked.
                return LoggerUtils.Initiator.System.name();
            }
            initiator = LoggerUtils.getMaskedContent(getUser());
        }
        return initiator;
    }

    /**
     * Create data map for audit logs with enhanced metadata.
     *
     * @param taskId            The task ID
     * @param workflowRequestId The workflow request ID
     * @param workflowId        The workflow ID
     * @param approverType      The approver type
     * @param approverId        The approver ID
     * @param taskExistingStatus The existing task status
     * @param taskNewStatus     The new task status
     * @param stepValue         The workflow step value
     * @return Enhanced data map with additional metadata
     */
    private Map<String, Object> getDataMap(String taskId, String workflowRequestId, String workflowId,
        String approverType, String approverId, String taskExistingStatus, String taskNewStatus, Integer stepValue) {

        Map<String, Object> dataMap = new HashMap<>();
        if (StringUtils.isNotBlank(taskId)) {
            dataMap.put(TASK_ID, taskId);
        }
        if (StringUtils.isNotBlank(workflowRequestId)) {
            dataMap.put(WORKFLOW_REQUEST_ID, workflowRequestId);
        }
        if (StringUtils.isNotBlank(workflowId)) {
            dataMap.put(WORKFLOW_ID, workflowId);
        }
        if (StringUtils.isNotBlank(approverType)) {
            dataMap.put(APPROVER_TYPE, approverType);
        }
        if (StringUtils.isNotBlank(approverId)) {
            dataMap.put(APPROVER_ID, approverId);
        }
        if (StringUtils.isNotBlank(taskExistingStatus)) {
            dataMap.put(TASK_EXISTING_STATUS, taskExistingStatus);
        }
        if (StringUtils.isNotBlank(taskNewStatus)) {
            dataMap.put(TASK_NEW_STATUS, taskNewStatus);
        }
        if (stepValue != null) {
            dataMap.put(STEP_VALUE, stepValue);
        }
        return dataMap;
    }

    /**
     * Operations to be logged.
     */
    public enum Operation {

        RESERVE("reserve-approval"),
        RELEASE("release-approval"),
        APPROVE("approve-approval"),
        REJECT("reject-approval"),
        COMPLETE("complete-approval");

        private final String logAction;

        Operation(String logAction) {

            this.logAction = logAction;
        }

        public String getLogAction() {

            return this.logAction;
        }
    }

    /**
     * Print audit log using the builder data.
     *
     * @param builder The audit log builder containing the data
     */
    public void printAuditLog(AuditLogBuilder builder) {

        if (builder == null) {
            log.warn("Audit log builder is null. Cannot print audit log.");
            return;
        }

        builder.validate();
        if (!isV2AuditLogsEnabled()) {
            if (log.isDebugEnabled()) {
                log.debug("V2 audit logs are disabled. Skipping audit log for operation: " +
                         (builder.operation != null ? builder.operation.getLogAction() : "unknown"));
            }
            return;
        }

        Map<String, Object> dataMap = builder.buildDataMap();
        triggerAuditLog(builder.operation, builder.taskId, dataMap);
    }

    /**
     * Check whether V2 audit logs are enabled.
     *
     * @return true if V2 audit logs are enabled, false otherwise.
     */
    private boolean isV2AuditLogsEnabled() {

        return LoggerUtils.isEnableV2AuditLogs();
    }


    /**
     * Builder class for creating audit logs with fluent API and validation.
     */
    public class AuditLogBuilder {

        private Operation operation;
        private String taskId;
        private String workflowRequestId;
        private String workflowId;
        private String approverType;
        private String approverId;
        private String taskExistingStatus;
        private String taskNewStatus;
        private Integer stepValue;

        /**
         * Set the operation being performed.
         *
         * @param operation The operation (required)
         * @return This builder for method chaining
         */
        public AuditLogBuilder operation(Operation operation) {

            this.operation = operation;
            return this;
        }

        /**
         * Set the task ID.
         *
         * @param taskId The task ID (required)
         * @return This builder for method chaining
         */
        public AuditLogBuilder taskId(String taskId) {

            this.taskId = taskId;
            return this;
        }

        /**
         * Set the workflow request ID.
         *
         * @param workflowRequestId The workflow request ID
         * @return This builder for method chaining
         */
        public AuditLogBuilder workflowRequestId(String workflowRequestId) {

            this.workflowRequestId = workflowRequestId;
            return this;
        }

        /**
         * Set the workflow ID.
         *
         * @param workflowId The workflow ID
         * @return This builder for method chaining
         */
        public AuditLogBuilder workflowId(String workflowId) {

            this.workflowId = workflowId;
            return this;
        }

        /**
         * Set the approver type.
         *
         * @param approverType The approver type (e.g., USER, ROLE)
         * @return This builder for method chaining
         */
        public AuditLogBuilder approverType(String approverType) {

            this.approverType = approverType;
            return this;
        }

        /**
         * Set the approver ID.
         *
         * @param approverId The approver ID
         * @return This builder for method chaining
         */
        public AuditLogBuilder approverId(String approverId) {

            this.approverId = approverId;
            return this;
        }

        /**
         * Set the existing task status.
         *
         * @param existingStatus The existing status
         * @return This builder for method chaining
         */
        public AuditLogBuilder existingStatus(String existingStatus) {

            this.taskExistingStatus = existingStatus;
            return this;
        }

        /**
         * Set the new task status.
         *
         * @param newStatus The new status
         * @return This builder for method chaining
         */
        public AuditLogBuilder newStatus(String newStatus) {

            this.taskNewStatus = newStatus;
            return this;
        }

        /**
         * Set the step value.
         *
         * @param stepValue The workflow step value
         * @return This builder for method chaining
         */
        public AuditLogBuilder stepValue(Integer stepValue) {

            this.stepValue = stepValue;
            return this;
        }

        /**
         * Build the data map for audit logging.
         *
         * @return Map containing audit log data
         * @throws IllegalArgumentException if required fields are missing
         */
        public Map<String, Object> buildDataMap() {

            return getDataMap(taskId, workflowRequestId, workflowId,
                    approverType, approverId, taskExistingStatus, taskNewStatus, stepValue);
        }

        /**
         * Validate required fields.
         *
         * @throws IllegalArgumentException if validation fails
         */
        private void validate() {

            if (operation == null) {
                throw new IllegalArgumentException("Operation is required for audit logging");
            }
            if (StringUtils.isBlank(taskId)) {
                throw new IllegalArgumentException("Task ID is required for audit logging");
            }
        }
    }
}
