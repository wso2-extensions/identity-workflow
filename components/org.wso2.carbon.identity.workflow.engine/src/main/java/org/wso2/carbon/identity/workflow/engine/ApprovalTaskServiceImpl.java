/*
 * Copyright (c) 2025-2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.workflow.engine;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.wso2.carbon.context.CarbonContext;
import org.wso2.carbon.context.PrivilegedCarbonContext;
import org.wso2.carbon.identity.application.authentication.framework.util.FrameworkConstants;
import org.wso2.carbon.identity.claim.metadata.mgt.ClaimMetadataManagementServiceImpl;
import org.wso2.carbon.identity.claim.metadata.mgt.exception.ClaimMetadataException;
import org.wso2.carbon.identity.claim.metadata.mgt.model.LocalClaim;
import org.wso2.carbon.identity.core.ServiceURLBuilder;
import org.wso2.carbon.identity.core.ThreadLocalAwareExecutors;
import org.wso2.carbon.identity.core.URLBuilderException;
import org.wso2.carbon.identity.core.util.IdentityTenantUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.event.IdentityEventConstants;
import org.wso2.carbon.identity.event.IdentityEventException;
import org.wso2.carbon.identity.event.event.Event;
import org.wso2.carbon.identity.governance.IdentityGovernanceUtil;
import org.wso2.carbon.identity.governance.service.notification.NotificationChannels;
import org.wso2.carbon.identity.role.v2.mgt.core.RoleConstants;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementClientException;
import org.wso2.carbon.identity.role.v2.mgt.core.exception.IdentityRoleManagementException;
import org.wso2.carbon.identity.role.v2.mgt.core.model.RoleBasicInfo;
import org.wso2.carbon.identity.role.v2.mgt.core.model.UserBasicInfo;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskFilterDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskRelationDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApprovalTaskSummaryDTO;
import org.wso2.carbon.identity.workflow.engine.dto.ApproverDTO;
import org.wso2.carbon.identity.workflow.engine.dto.PropertyDTO;
import org.wso2.carbon.identity.workflow.engine.dto.StateDTO;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineClientException;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineException;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineServerException;
import org.wso2.carbon.identity.workflow.engine.internal.WorkflowEngineServiceDataHolder;
import org.wso2.carbon.identity.workflow.engine.internal.dao.ApprovalTaskDAO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.WorkflowRequestDAO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.impl.ApprovalTaskDAOImpl;
import org.wso2.carbon.identity.workflow.engine.internal.dao.impl.WorkflowRequestDAOImpl;
import org.wso2.carbon.identity.workflow.engine.model.TaskModel;
import org.wso2.carbon.identity.workflow.engine.util.ApprovalTaskAuditLogger;
import org.wso2.carbon.identity.workflow.engine.util.Utils;
import org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants;
import org.wso2.carbon.identity.workflow.mgt.bean.Parameter;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.callback.WSWorkflowCallBackService;
import org.wso2.carbon.identity.workflow.mgt.callback.WSWorkflowResponse;
import org.wso2.carbon.identity.workflow.mgt.dto.Association;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import org.wso2.carbon.identity.workflow.mgt.exception.WorkflowException;
import org.wso2.carbon.identity.workflow.mgt.util.WorkflowDataType;
import org.wso2.carbon.user.api.UserStoreException;
import org.wso2.carbon.user.core.common.AbstractUserStoreManager;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import static org.wso2.carbon.identity.claim.metadata.mgt.util.ClaimConstants.DISPLAY_NAME_PROPERTY;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.CLAIMS_PROPERTY_NAME;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.ENTITY_TYPE_CLAIMED_USERS;
import static org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants.ParameterName.ENTITY_TYPE_USERS;

/**
 * Implementation of the ApprovalTaskService interface.
 */
public class ApprovalTaskServiceImpl implements ApprovalTaskService {

    private static final String APPROVED = "APPROVED";
    private static final String REJECTED = "REJECTED";
    private static final Integer LIMIT = 20;
    private static final Integer OFFSET = 0;
    private static final Logger log = LoggerFactory.getLogger(ApprovalTaskServiceImpl.class);

    private final ApprovalTaskDAO approvalTaskDAO = new ApprovalTaskDAOImpl();
    private final WorkflowRequestDAO workflowRequestDAO = new WorkflowRequestDAOImpl();
    private final WSWorkflowCallBackService wsWorkflowCallBackService = new WSWorkflowCallBackService();
    private final ClaimMetadataManagementServiceImpl claimMetadataManagementService =
            new ClaimMetadataManagementServiceImpl();
    private final ApprovalTaskAuditLogger auditLogger = new ApprovalTaskAuditLogger();

    private static final String ROLE_ID_PARAM_NAME = "Role ID";
    private static final String ROLE_NAME_PARAM_NAME = "Role Name";
    private static final String USERS_TO_BE_ADDED_PARAM_NAME = "Users to be Added";
    private static final String USERS_TO_BE_DELETED_PARAM_NAME = "Users to be Deleted";
    private static final String ROLE_ASSOCIATED_APPLICATION_PARAM_NAME = "Role Associated Application";
    private static final String TENANT_DOMAIN_PARAM_NAME = "Tenant Domain";
    private static final String COMMA_SEPARATOR = ",";
    private static final String ROLE_NOT_FOUND_ERROR_CODE = "RMA-60007";
    private static final String Q_NAME_INITIATOR_CHANNELS_PREFIX = "NotificationForInitiator-channels";
    private static final String CHANNEL_SMS = "sms";
    private static final String CLAIM_MOBILE = "http://wso2.org/claims/mobile";
    private static final String PARAM_NOTIFICATION = "Notification";
    private static final String Q_NAME_APPROVER_CHANNELS_PREFIX = "NotificationForApprovers-channels";
    private static final String NOTIFICATION_EVENT_NAME_PREFIX = "TRIGGER_";
    private static final String NOTIFICATION_EVENT_NAME_SUFFIX = "_NOTIFICATION";
    private static final String NOTIFICATION_EVENT_NAME_SUFFIX_LOCAL = "_LOCAL";
    private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
    private final ExecutorService executorService = ThreadLocalAwareExecutors.newFixedThreadPool(THREAD_POOL_SIZE);

    @Override
    public List<ApprovalTaskSummaryDTO> listApprovalTasks(Integer limit, Integer offset, ApprovalTaskFilterDTO filter)
            throws WorkflowEngineException {

        if (limit == null || limit < 0) {
            limit = LIMIT;
        }
        if (offset == null || offset < 0) {
            offset = OFFSET;
        }

        String userId = Utils.resolveUserID(CarbonContext.getThreadLocalCarbonContext().getUserId());

        // Filter the reserved workflow requests to filter out the BLOCKED tasks corresponding to the same request.
        List<ApprovalTaskSummaryDTO> approvalTaskSummaryDTOS = getAllAssignedTasksWithFilter(filter, userId, limit,
                offset);

        List<String> reservedWorkflowRequests = approvalTaskSummaryDTOS.stream()
                .filter(approvalTask -> WorkflowEngineConstants.TaskStatus.RESERVED.name()
                        .equals(approvalTask.getApprovalStatus()))
                .map(approvalTask -> approvalTask.getRequestId() + ":" + approvalTask.getWorkflowId())
                .collect(Collectors.toList());
        Set<String> processedRequestIds = new HashSet<>();
        Iterator<ApprovalTaskSummaryDTO> iterator = approvalTaskSummaryDTOS.iterator();
        while (iterator.hasNext()) {
            ApprovalTaskSummaryDTO approvalTaskSummaryDTO = iterator.next();
            String uniqueKey = approvalTaskSummaryDTO.getRequestId() + ":" + approvalTaskSummaryDTO.getWorkflowId();
            if (processedRequestIds.contains(uniqueKey)) {
                iterator.remove();
                continue;
            }

            /* The tasks with BLOCKED state where the corresponding workflow request already has a RESERVED task should
               be skipped to avoid duplication in the list. */
            if (reservedWorkflowRequests.contains(uniqueKey) && WorkflowEngineConstants.TaskStatus.BLOCKED.name()
                    .equals(approvalTaskSummaryDTO.getApprovalStatus())) {
                iterator.remove();
                continue;
            }

            /* If the task is in APPROVED state, skip adding it to the processedRequestIds set as there can be tasks in
               READY / RESERVED state for the same workflow request when it is a multistep approval process. */
            if (!WorkflowEngineConstants.TaskStatus.APPROVED.name()
                    .equals(approvalTaskSummaryDTO.getApprovalStatus())) {
                processedRequestIds.add(uniqueKey);
            }

            WorkflowRequest request = getWorkflowRequest(approvalTaskSummaryDTO.getRequestId());
            String eventType = request.getEventType();
            String workflowID = approvalTaskSummaryDTO.getWorkflowId();
            String workflowAssociationName = findAssociationNameByWorkflowAndEvent(workflowID, eventType);

            Timestamp createdTime = workflowRequestDAO.getCreatedAtTimeInMill(request.getUuid());
            approvalTaskSummaryDTO.setName(workflowAssociationName);
            approvalTaskSummaryDTO.setTaskType(eventType);
            approvalTaskSummaryDTO.setCreatedTimeInMillis(String.valueOf(createdTime.getTime()));
            approvalTaskSummaryDTO.setPriority(WorkflowEngineConstants.ParameterName.PRIORITY);
        }

        return approvalTaskSummaryDTOS.subList(Math.min(offset, approvalTaskSummaryDTOS.size()),
                Math.min(offset + limit, approvalTaskSummaryDTOS.size()));
    }

    @Override
    public ApprovalTaskDTO getApprovalTaskByTaskId(String taskId) throws WorkflowEngineException {

        taskId = taskId.trim();
        String requestId = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(taskId);
        if (StringUtils.isEmpty(requestId)) {
            return null;
        }
        WorkflowRequest request = getWorkflowRequest(requestId);
        String initiator = workflowRequestDAO.getInitiatedUser(requestId);
        List<String> approvers = approvalTaskDAO.listApprovers(taskId);
        Map<String, String> assigneeMap = new HashMap<>();
        for (String assignee : approvers) {
            assigneeMap.put(WorkflowEngineConstants.ParameterName.ASSIGNEE_TYPE, assignee);
        }
        List<PropertyDTO> properties = getRequestParameters(request);
        ApprovalTaskDTO approvalTaskDTO = new ApprovalTaskDTO();
        approvalTaskDTO.setId(taskId);
        String statusValue = approvalTaskDAO.getApprovalTaskStatus(taskId);
        approvalTaskDTO.setApprovalStatus(statusValue);
        approvalTaskDTO.setInitiator(initiator);
        approvalTaskDTO.setPriority(WorkflowEngineConstants.ParameterName.PRIORITY);
        TaskModel taskModel = new TaskModel();
        taskModel.setAssignees(assigneeMap);
        approvalTaskDTO.setAssignees(getPropertyDTOs(taskModel.getAssignees()));
        approvalTaskDTO.setProperties(properties);
        return approvalTaskDTO;
    }

    @Override
    public void updateApprovalTaskStatus(String approvalTaskId, StateDTO nextState) throws WorkflowEngineException {

        validateApprovers(approvalTaskId);

        switch (nextState.getAction()) {
            case APPROVE:
                handleApproval(approvalTaskId);
                break;
            case REJECT:
                handleReject(approvalTaskId);
                break;
            case RELEASE:
                handleRelease(approvalTaskId);
                break;
            case CLAIM:
                handleClaim(approvalTaskId);
                break;
            default:
                throw new WorkflowEngineClientException(
                        WorkflowEngineConstants.ErrorMessages.USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE.
                                getDescription(),
                        WorkflowEngineConstants.ErrorMessages.USER_ERROR_NOT_ACCEPTABLE_INPUT_FOR_NEXT_STATE.
                                getCode());
        }
    }

    /**
     * Retrieves user claim value by user ID.
     *
     * @param tenantId The tenant ID.
     * @param userId   The user ID.
     * @param claimUri The claim URI.
     * @return The claim value.
     * @throws WorkflowEngineServerException If claim retrieval fails.
     */
    private String getUserClaimValue(int tenantId, String userId, String claimUri)
            throws WorkflowEngineServerException {

        try {
            AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) WorkflowEngineServiceDataHolder
                    .getInstance().getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
            return userStoreManager.getUserClaimValueWithID(userId, claimUri, null);
        } catch (UserStoreException e) {
            throw new WorkflowEngineServerException("Error while retrieving user claim: " + claimUri, e);
        }
    }

    /**
     * Retrieves user claim value by username.
     *
     * @param tenantId The tenant ID.
     * @param username The username.
     * @param claimUri The claim URI.
     * @return The claim value.
     * @throws WorkflowEngineServerException If claim retrieval fails.
     */
    private String getUserClaimValueByUsername(int tenantId, String username, String claimUri)
            throws WorkflowEngineServerException {

        try {
            AbstractUserStoreManager userStoreManager = (AbstractUserStoreManager) WorkflowEngineServiceDataHolder
                    .getInstance().getRealmService().getTenantUserRealm(tenantId).getUserStoreManager();
            return userStoreManager.getUserClaimValue(username, claimUri, null);
        } catch (UserStoreException e) {
            throw new WorkflowEngineServerException("Error while retrieving user claim: " + claimUri, e);
        }
    }

    /**
     * Resolve event name according to the notification channel.
     *
     * @param notificationChannel Notification channel
     * @return Resolved event name
     */
    private String resolveEventName(String notificationChannel) {

        if (NotificationChannels.SMS_CHANNEL.getChannelType().equalsIgnoreCase(notificationChannel)) {
            return NOTIFICATION_EVENT_NAME_PREFIX + NotificationChannels.SMS_CHANNEL.getChannelType() +
                    NOTIFICATION_EVENT_NAME_SUFFIX + NOTIFICATION_EVENT_NAME_SUFFIX_LOCAL;
        } else {
            return IdentityEventConstants.Event.TRIGGER_NOTIFICATION;
        }
    }

    private WorkflowRequest buildWorkflowRequest(String workflowRequestId) {

        WorkflowRequest workflowRequest = new WorkflowRequest();
        RequestParameter requestParameter = new RequestParameter();
        requestParameter.setName(WorkflowEngineConstants.ParameterName.REQUEST_ID);
        requestParameter.setValue(workflowRequestId);
        workflowRequest.setRequestParameters(Collections.singletonList(requestParameter));
        workflowRequest.setUuid(workflowRequestId);
        workflowRequest.setTenantId(CarbonContext.getThreadLocalCarbonContext().getTenantId());
        return workflowRequest;
    }

    @Override
    public void addApprovalTasksForWorkflowRequest(WorkflowRequest workflowRequest, List<Parameter> parameterList)
            throws WorkflowEngineException {

        if (CollectionUtils.isEmpty(parameterList)) {
            return;
        }

        String workflowRequestId = getWorkflowRequestId(workflowRequest);
        /* The workflow parameter list has the workflow ID for each property object. Retrieve the workflow ID from
           the first. */
        String workflowId = parameterList.get(0).getWorkflowId();
        String approverType;
        String approverNotificationChannels = null;

        int currentStep = approvalTaskDAO.getCurrentApprovalStepOfWorkflowRequest(workflowRequestId, workflowId);
        if (currentStep == WorkflowEngineConstants.NO_CURRENT_STEP) {
            approvalTaskDAO.addApprovalTaskStep(workflowRequestId, workflowId);
            currentStep = 1;
        } else {
            currentStep += 1;
            approvalTaskDAO.updateStateOfRequest(workflowRequestId, workflowId, currentStep);
        }

        // Collect approvers to notify after processing all parameters.
        Set<String> approversToNotify = new HashSet<>();
        String tenantDomain = IdentityTenantUtil.getTenantDomain(workflowRequest.getTenantId());

        // Single loop to extract notification channels and create approval tasks.
        for (Parameter parameter : parameterList) {
            // Extract notification channels.
            if (approverNotificationChannels == null && parameter.getParamName().equalsIgnoreCase(PARAM_NOTIFICATION)) {
                if (StringUtils.isNotBlank(parameter.getqName()) &&
                        parameter.getqName().startsWith(Q_NAME_APPROVER_CHANNELS_PREFIX)) {
                    approverNotificationChannels = parameter.getParamValue();
                }
            }

            // Process USER_AND_ROLE_STEP parameters.
            if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                String[] stepName = parameter.getqName().split(WorkflowEngineConstants.Q_NAME_STEP_SEPARATOR);
                int step = Integer.parseInt(stepName[1]);
                if (currentStep == step) {
                    approverType = stepName[stepName.length - 1];
                    String approverIdentifiers = parameter.getParamValue();
                    if (StringUtils.isNotBlank(approverIdentifiers)) {
                        String[] approverIdentifierList = approverIdentifiers.split(COMMA_SEPARATOR, 0);
                        for (String approverIdentifier : approverIdentifierList) {
                            String taskId = UUID.randomUUID().toString();
                            approvalTaskDAO.addApproversOfRequest(taskId, workflowRequestId, workflowId,
                                    approverType,
                                    approverIdentifier, WorkflowEngineConstants.TaskStatus.READY.toString());

                            // Collect approvers for notification after all parameters are processed.
                            collectApproversForNotification(approverType, approverIdentifier, tenantDomain,
                                    approversToNotify);
                        }
                    }
                }
            }
        }

        // Trigger notifications asynchronously to avoid blocking the main thread.
        if (CollectionUtils.isNotEmpty(approversToNotify) && StringUtils.isNotBlank(approverNotificationChannels)) {
            int approverCount = approversToNotify.size();
            int maxApproverNotifications = IdentityUtil.getMaxApproverNotificationsForWorkflow();
            int notificationCount = Math.min(approverCount, maxApproverNotifications);

            if (approverCount > maxApproverNotifications) {
                log.warn("Number of approvers ({}) exceeds the maximum allowed limit ({}). " +
                        "Notifications will be sent to only the first {} approvers to prevent memory issues. " +
                        "WorkflowRequestId: {}",
                        approverCount, maxApproverNotifications, maxApproverNotifications, workflowRequestId);
            }

            if (log.isDebugEnabled()) {
                log.debug("Triggering notifications for {} approvers asynchronously. WorkflowRequestId: {}",
                        notificationCount, workflowRequestId);
            }

            int count = 0;
            for (String approverUserId : approversToNotify) {
                if (count >= maxApproverNotifications) {
                    break;
                }
                executeNotificationAsync(approverUserId, workflowId, workflowRequestId, true, null,
                        approverNotificationChannels);
                count++;
            }
        }
    }

    /**
     * Executes a single notification asynchronously with proper tenant context propagation.
     *
     * @param recipientUserId        The recipient user ID.
     * @param workflowId             The workflow ID.
     * @param workflowRequestId      The workflow request ID.
     * @param isApproverNotification True for approver notification, false for initiator notification.
     * @param decision               The approval decision (for initiator notifications).
     * @param notificationChannels   The notification channels configuration.
     */
    private void executeNotificationAsync(String recipientUserId, String workflowId, String workflowRequestId,
                                          boolean isApproverNotification, String decision,
                                          String notificationChannels) {

        // Capture tenant context before async execution.
        // Note: ThreadLocalAwareExecutors only propagates MDC context, not CarbonContext.
        // CompletableFuture.supplyAsync() also bypasses the execute() override, so we need manual propagation.
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        CompletableFuture.supplyAsync(() -> {
            try {
                // Set the tenant context in the async thread.
                PrivilegedCarbonContext.startTenantFlow();
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantId(tenantId);
                PrivilegedCarbonContext.getThreadLocalCarbonContext().setTenantDomain(tenantDomain);

                triggerNotification(recipientUserId, workflowId, workflowRequestId, isApproverNotification, decision,
                        notificationChannels);
                return null;
            } catch (Exception e) {
                String recipientType = isApproverNotification ? "approver" : "initiator";
                log.error("Error while triggering notification for {}: {}", recipientType, recipientUserId, e);
                return null;
            } finally {
                // Clean up tenant context.
                PrivilegedCarbonContext.endTenantFlow();
            }
        }, executorService);
    }

    /**
     * Collects approver user IDs for notification purposes.
     * For role-based approvers, expands the role to individual user IDs.
     * For user-based approvers, adds the user ID directly.
     *
     * @param approverType       The approver type (users or roles).
     * @param approverIdentifier The approver identifier (user ID or role ID).
     * @param tenantDomain       The tenant domain.
     * @param approversToNotify  The set to collect approver user IDs.
     */
    private void collectApproversForNotification(String approverType, String approverIdentifier,
                                                 String tenantDomain, Set<String> approversToNotify) {

        if (WorkflowEngineConstants.APPROVER_TYPE_ROLES.equalsIgnoreCase(approverType)) {
            List<String> roleMembers = new ArrayList<>();
            try {
                roleMembers = getUserIdsAssignedToRole(approverIdentifier, tenantDomain);
            } catch (WorkflowEngineException e) {
                log.error("Error while retrieving assigned user IDs for role: {} in tenant: {}. " +
                                "Continuing without adding notifications for this role.",
                        approverIdentifier, tenantDomain, e);
            }
            if (CollectionUtils.isEmpty(roleMembers)) {
                if (log.isDebugEnabled()) {
                    log.debug("Role approver '{}' in tenant '{}' has no assigned users. " +
                                    "No notifications will be sent for this role.",
                            approverIdentifier, tenantDomain);
                }
            } else {
                approversToNotify.addAll(roleMembers);
            }
        } else {
            approversToNotify.add(approverIdentifier);
        }
    }

    private List<String> parseChannels(String channel) {

        if (StringUtils.isBlank(channel)) {
            log.debug("Notification channel is not configured. No notifications will be sent.");
            return Collections.emptyList();
        }
        return Arrays.stream(channel.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Get server supported notification channel.
     *
     * @param channel Notification channel
     * @return Server supported notification channel
     */
    private String getServerSupportedNotificationChannel(String channel) {

        // Validate notification channels.
        if (NotificationChannels.EMAIL_CHANNEL.getChannelType().equalsIgnoreCase(channel)) {
            return NotificationChannels.EMAIL_CHANNEL.getChannelType();
        } else if (NotificationChannels.SMS_CHANNEL.getChannelType().equalsIgnoreCase(channel)) {
            return NotificationChannels.SMS_CHANNEL.getChannelType();
        } else {
            String defaultNotificationChannel = IdentityGovernanceUtil.getDefaultNotificationChannel();
            if (log.isDebugEnabled()) {
                String message = String.format("Not a server supported notification channel : %s. Therefore "
                                + "default notification channel : %s will be used.", channel,
                        defaultNotificationChannel);
                log.debug(message);
            }
            return defaultNotificationChannel;
        }
    }

    /**
     * Resolves the contact claim URI based on the notification channel.
     *
     * @param channel The notification channel (sms or email).
     * @return The claim URI for the channel.
     */
    private String getClaimUriForChannel(String channel) {

        return CHANNEL_SMS.equalsIgnoreCase(channel) ? CLAIM_MOBILE : FrameworkConstants.EMAIL_ADDRESS_CLAIM;
    }

    /**
     * Triggers a notification for workflow events.
     *
     * @param approverUserId       The user ID of the notification recipient.
     * @param workflowId           The workflow ID.
     * @param workflowRequestId     The workflow request ID.
     * @param isApproverNotification True if notifying approver about new request, false if notifying workflow initiator
     *                              about decision.
     * @param decision              The approval decision (APPROVED/REJECTED), null for initial notifications.
     * @param channel               The notification channel (e.g., email, SMS), null for default channel.
     */
    private void triggerNotification(String approverUserId, String workflowId, String workflowRequestId,
                                     boolean isApproverNotification, String decision, String channel) {

        List<String> channels = parseChannels(channel);

        // Trigger a separate notification for each channel.
        for (String ch : channels) {
            try {
                // Build properties specific to this channel.
                Map<String, Object> notificationProperties = new HashMap<>();
                String notificationChannel = getServerSupportedNotificationChannel(ch);
                notificationProperties.put("notification-channel", notificationChannel);
                buildNotificationPropertiesForChannel(workflowId, workflowRequestId, approverUserId,
                        isApproverNotification, decision, ch, notificationProperties);

                String eventName = resolveEventName(notificationChannel);
                if (StringUtils.isBlank(eventName)) {
                    log.debug("Unsupported notification channel: {}", ch);
                    continue;
                }
                Event notificationEvent = new Event(eventName, notificationProperties);
                WorkflowEngineServiceDataHolder.getInstance().getIdentityEventService()
                        .handleEvent(notificationEvent);
            } catch (IdentityEventException e) {
                // Continue with other channels even if one fails.
                log.error("Error while sending the notification for channel: {}", ch, e);
            } catch (WorkflowEngineException e) {
                // Continue with other channels even if one fails.
                log.error("Error while building notification properties for channel: {}", ch, e);
            }
        }
    }

    /**
     * Builds notification properties for a specific channel.
     *
     * @param workflowId            The workflow ID.
     * @param workflowRequestId     The workflow request ID.
     * @param approverUserId       The user ID of the workflow approver.
     * @param isApproverNotification True for approver notification, false for workflow initiator notification.
     * @param decision              The approval decision.
     * @param channel               The notification channel (sms or email).
     * @param properties            The properties map to populate.
     * @throws WorkflowEngineException If property building fails.
     */
    private void buildNotificationPropertiesForChannel(String workflowId, String workflowRequestId,
                                                       String approverUserId, boolean isApproverNotification,
                                                       String decision, String channel, Map<String, Object> properties)
            throws WorkflowEngineException {

        org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest workflowRequest =
                getWorkflowRequestBean(workflowRequestId);

        if (isApproverNotification) {
            buildApproverNotificationProperties(approverUserId, workflowId, workflowRequestId, workflowRequest,
                    channel, properties);
        } else {
            buildInitiatorNotificationProperties(workflowRequestId, workflowRequest, decision, channel, properties);
        }
    }

    /**
     * Builds notification properties for approvers.
     *
     * @param approverUserId    The approver's user ID.
     * @param workflowId        The workflow ID.
     * @param workflowRequestId The workflow request ID.
     * @param workflowRequest   The workflow request bean.
     * @param channel           The notification channel (sms or email).
     * @param properties        The properties map to populate.
     * @throws WorkflowEngineException If property building fails.
     */
    private void buildApproverNotificationProperties(String approverUserId, String workflowId,
                                                     String workflowRequestId,
                                                     org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest
                                                             workflowRequest,
                                                     String channel, Map<String, Object> properties)
            throws WorkflowEngineException {

        String approvalUrl = StringUtils.EMPTY;
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();

        try {
            approvalUrl = ServiceURLBuilder.create().setTenant(tenantDomain, true)
                    .addPath(new String[]{"/myaccount"}).build().getAbsolutePublicURL() + "/approvals?workflowId=" +
                    workflowId + "&workflowRequestId=" + workflowRequestId;
        } catch (URLBuilderException e) {
            log.error("Error while building approval notification URL in tenant: {}. " +
                    "WorkflowRequestId: {}", tenantId, workflowRequestId, e);
        }

        // Determine the claim URI based on channel.
        String claimUri = getClaimUriForChannel(channel);

        // Get approver's contact information (email or mobile) based on the notification channel.
        String approverContact = getUserClaimValue(tenantId, approverUserId, claimUri);

        // Get approver's username for display purposes.
        String approverUsername = getUserClaimValue(tenantId, approverUserId, FrameworkConstants.USERNAME_CLAIM);

        properties.put("TEMPLATE_TYPE", "WorkflowApproverNotification");
        properties.put("approverName", approverUsername);
        properties.put("send-to", approverContact);
        properties.put("tenant-domain", tenantDomain);
        properties.put("approvalActionUrl", approvalUrl);
        properties.put("workflowRequestId", workflowRequestId);
        properties.put("initiatorName", workflowRequest.getCreatedBy());
        properties.put("workflowType", workflowRequest.getOperationType());
    }

    @Override
    public void deletePendingApprovalTasks(String workflowId) throws WorkflowEngineException {

        if (StringUtils.isBlank(workflowId)) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.WORKFLOW_ID_NOT_FOUND.getDescription(),
                    WorkflowEngineConstants.ErrorMessages.WORKFLOW_ID_NOT_FOUND.getCode());
        }
        approvalTaskDAO.deletePendingApprovalTasks(workflowId);
    }

    @Override
    public void updatePendingApprovalTasksOnWorkflowUpdate(String workflowId, List<Parameter> newWorkflowParams,
                                                           List<Parameter> oldWorkflowParams)
            throws WorkflowEngineException {

        // Get the list of pending requests corresponding to given workflow ID.
        List<String> pendingRequestList = approvalTaskDAO.getPendingRequestsByWorkflowId(workflowId);

        // APPROVER_NAME list for each step.
        Map<Integer, List<String>> newParamValuesForApprovalSteps =
                Utils.getParamValuesForApprovalSteps(newWorkflowParams);
        // Get the modified steps.
        List<Integer> modifiedSteps = Utils.getModifiedApprovalSteps(newWorkflowParams, oldWorkflowParams);

        // For each request, delete the existing approval tasks and
        // add new tasks based on the updated workflow parameters.
        for (String requestId : pendingRequestList) {
            int currentStep = approvalTaskDAO.getCurrentApprovalStepOfWorkflowRequest(requestId, workflowId);

            // Check if the request has been affected by the workflow update.
            // First check if the current step is one of the modified steps.
            if (!modifiedSteps.contains(currentStep)) {
                // If not, no need to change the approval tasks for this request.
                continue;
            }

            // Get the tasks corresponding to the request ID with status READY, BLOCKED or RESERVED.
            List<ApprovalTaskRelationDTO> approvalTaskRelationDTOS =
                    approvalTaskDAO.getApprovalTaskRelationsByWorkflowRequestId(requestId);

            // Get reserved task in the task list if exists.
            ApprovalTaskRelationDTO reservedTask =
                    approvalTaskRelationDTOS.stream()
                            .filter(dto -> WorkflowEngineConstants.TaskStatus.RESERVED.toString()
                                    .equals(dto.getTaskStatus()))
                            .findFirst()
                            .orElse(null);

            // Delete existing pending approval tasks.
            approvalTaskDAO.deletePendingApprovalTasks(requestId);
            // Update the current step to (step-1) to reset the pending step.
            approvalTaskDAO.updateStateOfRequest(requestId, workflowId, currentStep - 1);
            // Get corresponding workflow request.
            WorkflowRequest request = getWorkflowRequest(requestId);
            // Add new approval tasks based on updated workflow parameters.
            addApprovalTasksForWorkflowRequest(request, newWorkflowParams);

            if (reservedTask != null) {
                /*
                If there is a RESERVED task, need to re-perform the reservation for the same user.
                The reservation is made successfully for the request if the user is valid for the new workflow as well.
                 */
                String userId = reservedTask.getApproverName();

                // Get the new workflow APPROVER_NAME list for the current step.
                List<String> approverNamesForCurrentStep =
                        newParamValuesForApprovalSteps.get(currentStep);

                // Retrieving the tenant domain of the user corresponding to the userId to validate reserved task users.
                String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
                // Get the user's roles.
                List<String> entityIds = getAssignedRoleIds(userId, tenantDomain);
                // Add userId as eligible entity if the workflow has USER.
                entityIds.add(userId);

                // Check if the user is still eligible to approve the request,
                // by checking if any entityId is present in the approverNamesForCurrentStep.
                for (String entityId : entityIds) {
                    if (approverNamesForCurrentStep != null && approverNamesForCurrentStep.contains(entityId)) {
                        // Get the tasks respect to the request ID with status 'READY'.
                        List<ApprovalTaskRelationDTO> approvalTaskRelationsDTOs =
                                approvalTaskDAO.getApprovalTaskRelationsByWorkflowRequestId(requestId);

                        // Get the task id with the entityId.
                        String taskId = approvalTaskRelationsDTOs.stream()
                                .filter(dto -> entityId.equals(dto.getApproverName())
                                        && WorkflowEngineConstants.TaskStatus.READY.toString().equals(dto
                                        .getTaskStatus()))
                                .map(ApprovalTaskRelationDTO::getTaskId)
                                .findFirst()
                                .orElse(null);

                        // If a task is found, perform reservation.
                        if (taskId != null) {
                            handleClaim(taskId);
                        }
                        break;
                    }
                }
            }
        }
    }

    /**
     * Retrieves all the approval tasks assigned to the user.
     *
     * @param filter   The filter criteria to apply when retrieving the approval tasks.
     * @param userId The ID of the user whose related task IDs should be retrieved.
     * @param limit    The maximum number of task IDs to return.
     * @param offset   The starting point from which to return task IDs.
     * @return List of task IDs assigned to the user, filtered by the specified statuses.
     */
    private List<ApprovalTaskSummaryDTO> getAllAssignedTasksWithFilter(ApprovalTaskFilterDTO filter, String userId,
                                                                       int limit, int offset)
            throws WorkflowEngineException {

        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        List<String> entityIds = new ArrayList<>();
        entityIds.add(userId);

        List<String> roleIds = getAssignedRoleIds(userId, tenantDomain);
        entityIds.addAll(roleIds);
        int tenantId = CarbonContext.getThreadLocalCarbonContext().getTenantId();
        return approvalTaskDAO.getFilteredApprovalTaskDetails(entityIds, filter, limit, offset, tenantId);
    }

    private List<String> getAssignedRoleIds(String userId, String tenantDomain) throws WorkflowEngineException {

        try {
            List<String> roleIDList = WorkflowEngineServiceDataHolder.getInstance().getRoleManagementService().
                    getRoleIdListOfUser(userId, tenantDomain);
            return new ArrayList<>(roleIDList);
        } catch (IdentityRoleManagementClientException e) {
            if (log.isDebugEnabled()) {
                log.debug(e.getMessage(), e);
            }
            return new ArrayList<>();
        } catch (IdentityRoleManagementException e) {
            throw new WorkflowEngineException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_APPROVAL_TASKS_FOR_USER.
                            getDescription(), e);
        }

    }

    /**
     * Builds notification properties for initiators.
     *
     * @param workflowRequestId The workflow request ID.
     * @param workflowRequest   The workflow request bean.
     * @param decision          The approval decision.
     * @param channel           The notification channel (sms or email).
     * @param properties        The properties map to populate.
     * @throws WorkflowEngineException If property building fails.
     */
    private void buildInitiatorNotificationProperties(String workflowRequestId,
                                                      org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest
                                                              workflowRequest,
                                                      String decision, String channel,
                                                      Map<String, Object> properties)
            throws WorkflowEngineException {

        String initiatorUsername = workflowRequest.getCreatedBy();
        String tenantDomain = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantDomain();
        int tenantId = PrivilegedCarbonContext.getThreadLocalCarbonContext().getTenantId();

        // Determine the claim URI based on channel.
        String claimUri = getClaimUriForChannel(channel);
        String initiatorContact = getUserClaimValueByUsername(tenantId, initiatorUsername, claimUri);

        properties.put("TEMPLATE_TYPE", "WorkflowInitiatorNotification");
        properties.put("send-to", initiatorContact);
        properties.put("tenant-domain", tenantDomain);
        properties.put("workflowRequestId", workflowRequestId);
        properties.put("workflowType", workflowRequest.getOperationType());
        // Convert decision to title case (e.g., "APPROVED" -> "Approved").
        String formattedDecision = StringUtils.isNotBlank(decision)
                ? decision.substring(0, 1).toUpperCase() + decision.substring(1).toLowerCase()
                : decision;
        properties.put("decision", formattedDecision);
        properties.put("initiatorName", initiatorUsername);
    }

    private WorkflowRequest getWorkflowRequest(String requestId) throws WorkflowEngineException {

        try {
            return WorkflowEngineServiceDataHolder.getInstance().getWorkflowManagementService()
                    .getWorkflowRequest(requestId);
        } catch (WorkflowException e) {
            throw new WorkflowEngineException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_WORKFLOW_REQUEST.
                            getDescription(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_WORKFLOW_REQUEST.getCode());
        }
    }

    private List<String> getUserIdsAssignedToRole(String roleId, String tenantDomain) throws WorkflowEngineException {

        try {
            List<UserBasicInfo> userBasicInfoList = WorkflowEngineServiceDataHolder.getInstance().
                    getRoleManagementService().getUserListOfRole(roleId, tenantDomain);
            if (log.isDebugEnabled()) {
                log.debug("Retrieved users for role: {} in tenant: {}. User count: {}", roleId, tenantDomain,
                        userBasicInfoList.size());
            }
            return userBasicInfoList.stream().map(UserBasicInfo::getId).collect(Collectors.toList());
        } catch (IdentityRoleManagementException e) {
            throw new WorkflowEngineException("Error occurred while retrieving users assigned to role.", e);
        }
    }

    private void validateApprovers(String taskId) throws WorkflowEngineException {

        String userId = Utils.resolveUserID(CarbonContext.getThreadLocalCarbonContext().getUserId());
        String tenantDomain = CarbonContext.getThreadLocalCarbonContext().getTenantDomain();

        boolean isAssignedApprovalTask = false;
        ApproverDTO approverDTO = approvalTaskDAO.getApproverDetailForApprovalTask(taskId);
        if (ENTITY_TYPE_USERS.equals(approverDTO.getApproverType()) ||
                ENTITY_TYPE_CLAIMED_USERS.equals(approverDTO.getApproverType())) {
            if (approverDTO.getApproverName().equals(userId)) {
                isAssignedApprovalTask = true;
            }
        } else {
            List<String> roleIds = getAssignedRoleIds(userId, tenantDomain);
            if (roleIds.contains(approverDTO.getApproverName())) {
                isAssignedApprovalTask = true;
            }
        }
        if (!isAssignedApprovalTask) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_APPROVAL_TASK_IS_NOT_ASSIGNED.
                            getDescription(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_APPROVAL_TASK_IS_NOT_ASSIGNED.getCode());
        }
        if (WorkflowEngineConstants.TaskStatus.BLOCKED.name().equals(approverDTO.getTaskStatus())) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getDescription(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getCode());
        }
        if (WorkflowEngineConstants.TaskStatus.APPROVED.name().equals(approverDTO.getTaskStatus())
                || WorkflowEngineConstants.TaskStatus.REJECTED.name().equals(approverDTO.getTaskStatus())) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_COMPLETED.getDescription(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_COMPLETED.getCode());
        }
    }

    private int getNumberOfApprovalStepsFromWorkflowParameters(List<Parameter> workflowParameterList) {

        int maxStep = 0;

        for (Parameter parameter : workflowParameterList) {
            if (parameter.getParamName().equals(WorkflowEngineConstants.ParameterName.USER_AND_ROLE_STEP)) {
                String value = parameter.getqName();
                if (value != null && !value.isEmpty()) {
                    String[] parts = value.split("-");
                    if (parts.length > 1) {
                        int step = Integer.parseInt(parts[1]);
                        if (step > maxStep) {
                            maxStep = step;
                        }
                    }
                }
            }
        }
        return maxStep;
    }

    private org.wso2.carbon.identity.workflow.mgt.bean.WorkflowRequest getWorkflowRequestBean(String requestId)
            throws WorkflowEngineException {

        try {
            return WorkflowEngineServiceDataHolder.getInstance().getWorkflowManagementService()
                    .getWorkflowRequestBean(requestId);
        } catch (WorkflowException e) {
            throw new WorkflowEngineException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_WORKFLOW_REQUEST.
                            getDescription(),
                    WorkflowEngineConstants.ErrorMessages.ERROR_OCCURRED_WHILE_RETRIEVING_WORKFLOW_REQUEST.getCode());
        }
    }

    private void handleApproval(String approvalTaskId) throws WorkflowEngineException {

        String workflowRequestId = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(approvalTaskId);
        String workflowId = approvalTaskDAO.getWorkflowID(approvalTaskId);

        handleApprovalTaskApproval(approvalTaskId, workflowRequestId, workflowId);

        int stepValue = approvalTaskDAO.getCurrentApprovalStepOfWorkflowRequest(workflowRequestId, workflowId);

        // Audit log for approval action.
        ApprovalTaskAuditLogger.AuditLogBuilder auditBuilder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE)
                .taskId(approvalTaskId)
                .workflowRequestId(workflowRequestId)
                .workflowId(workflowId)
                .newStatus(WorkflowEngineConstants.TaskStatus.APPROVED.toString())
                .stepValue(stepValue);
        auditLogger.printAuditLog(auditBuilder);

        List<Parameter> approvalWorkflowParameterList = getApprovalWorkflowParameters(workflowId);

        /* If the current step value is less than the total number of approval steps defined in the workflow
           parameters, then we need to add more approval tasks for the next step. Otherwise,
           we can complete the workflow request with an approved status. */
        if (stepValue < getNumberOfApprovalStepsFromWorkflowParameters(approvalWorkflowParameterList)) {
            WorkflowRequest workflowRequest = buildWorkflowRequest(workflowRequestId);
            addApprovalTasksForWorkflowRequest(workflowRequest, approvalWorkflowParameterList);
        } else {
            completeWorkflowApproval(workflowRequestId, workflowId);
        }
    }

    private void handleReject(String approvalTaskId) throws WorkflowEngineException {

        String workflowRequestId = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(approvalTaskId);
        String workflowId = approvalTaskDAO.getWorkflowID(approvalTaskId);
        handleApprovalTaskRejection(approvalTaskId, workflowRequestId);

        // Audit log for rejection action.
        ApprovalTaskAuditLogger.AuditLogBuilder auditBuilder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.REJECT)
                .taskId(approvalTaskId)
                .workflowRequestId(workflowRequestId)
                .newStatus(WorkflowEngineConstants.TaskStatus.REJECTED.toString());
        auditLogger.printAuditLog(auditBuilder);

        completeWorkflowReject(workflowRequestId, workflowId);
    }

    private void handleRelease(String taskId) throws WorkflowEngineServerException {

        String readyStatus = WorkflowEngineConstants.TaskStatus.READY.toString();
        String requestID = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(taskId);
        String approverType = approvalTaskDAO.getApproverType(taskId);
        String workflowId = approvalTaskDAO.getWorkflowID(taskId);
        List<String> taskIds = approvalTaskDAO.getApprovalTasksByWorkflowRequestId(requestID, workflowId);

        for (String id : taskIds) {
            approvalTaskDAO.updateApprovalTaskStatus(id, readyStatus);

            if (taskId.equals(id) && ENTITY_TYPE_CLAIMED_USERS.equals(approverType)) {
                approvalTaskDAO.deleteApprovalTasksOfWorkflowRequest(id);
            }
        }
    }

    private void handleClaim(String updatedApprovalTaskId) throws WorkflowEngineException {

        String userId = Utils.resolveUserID(CarbonContext.getThreadLocalCarbonContext().getUserId());
        String reservedStatus = WorkflowEngineConstants.TaskStatus.RESERVED.toString();
        String blockedStatus = WorkflowEngineConstants.TaskStatus.BLOCKED.toString();

        if (blockedStatus.equals(approvalTaskDAO.getApprovalTaskStatus(updatedApprovalTaskId))) {
            throw new WorkflowEngineClientException(
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getDescription(),
                    WorkflowEngineConstants.ErrorMessages.USER_ERROR_TASK_ALREADY_CLAIMED.getCode());
        }

        String workflowRequestID = approvalTaskDAO.getWorkflowRequestIdByApprovalTaskId(updatedApprovalTaskId);
        String approverType = approvalTaskDAO.getApproverType(updatedApprovalTaskId);
        String workflowId = approvalTaskDAO.getWorkflowID(updatedApprovalTaskId);
        List<String> existingApprovalTasks = approvalTaskDAO.getApprovalTasksByWorkflowRequestId(workflowRequestID,
                workflowId);

        for (String existingApprovalTaskId : existingApprovalTasks) {
            if (existingApprovalTaskId.equals(updatedApprovalTaskId)) {
                if (WorkflowEngineConstants.APPROVER_TYPE_USERS.equals(approverType)) {
                    approvalTaskDAO.updateApprovalTaskStatus(updatedApprovalTaskId, reservedStatus);

                    // Audit log for reserve action.
                    ApprovalTaskAuditLogger.AuditLogBuilder auditBuilder = auditLogger.auditBuilder()
                            .operation(ApprovalTaskAuditLogger.Operation.RESERVE)
                            .taskId(updatedApprovalTaskId)
                            .approverId(userId)
                            .workflowRequestId(workflowRequestID)
                            .workflowId(workflowId)
                            .newStatus(reservedStatus);
                    auditLogger.printAuditLog(auditBuilder);
                } else if (WorkflowEngineConstants.APPROVER_TYPE_ROLES.equals(approverType)) {
                    // Create a new task for the user who claimed the task.
                    String newTaskId = UUID.randomUUID().toString();
                    approvalTaskDAO.addApproversOfRequest(newTaskId, workflowRequestID, workflowId,
                            ENTITY_TYPE_CLAIMED_USERS, userId, reservedStatus);
                    // Audit log for reserve action.
                    ApprovalTaskAuditLogger.AuditLogBuilder auditBuilder = auditLogger.auditBuilder()
                            .operation(ApprovalTaskAuditLogger.Operation.RESERVE)
                            .taskId(newTaskId)
                            .approverId(userId)
                            .approverType(ENTITY_TYPE_CLAIMED_USERS)
                            .workflowRequestId(workflowRequestID)
                            .workflowId(workflowId)
                            .newStatus(reservedStatus);
                    auditLogger.printAuditLog(auditBuilder);

                    // Update the status of the existing task to BLOCKED.
                    approvalTaskDAO.updateApprovalTaskStatus(updatedApprovalTaskId, blockedStatus);

                    // Audit log for reserve action.
                    ApprovalTaskAuditLogger.AuditLogBuilder blockedApprovalBuilder = auditLogger.auditBuilder()
                            .operation(ApprovalTaskAuditLogger.Operation.RESERVE)
                            .taskId(newTaskId)
                            .approverId(userId)
                            .workflowRequestId(workflowRequestID)
                            .workflowId(workflowId)
                            .newStatus(blockedStatus);
                    auditLogger.printAuditLog(blockedApprovalBuilder);
                }
            } else {
                // Update the status of all existing tasks to BLOCKED.
                approvalTaskDAO.updateApprovalTaskStatus(existingApprovalTaskId, blockedStatus);
            }
        }
    }

    private void completeWorkflowApproval(String workflowRequestId, String workflowId)
            throws WorkflowEngineServerException {

        WSWorkflowResponse wsWorkflowResponse = new WSWorkflowResponse();
        String relationshipId = workflowRequestDAO.getRelationshipId(workflowRequestId, workflowId);
        wsWorkflowResponse.setUuid(relationshipId);
        wsWorkflowResponse.setStatus(APPROVED);
        wsWorkflowCallBackService.onCallback(wsWorkflowResponse);

        // Trigger initiator notification asynchronously.
        try {
            String notificationChannels = extractWorkFlowInitiatorNotificationChannels(workflowId);
            String userId = Utils.resolveUserID(CarbonContext.getThreadLocalCarbonContext().getUserId());
            if (StringUtils.isNotBlank(notificationChannels)) {
                executeNotificationAsync(userId, workflowId, workflowRequestId, false, APPROVED,
                        notificationChannels);
            }
        } catch (WorkflowEngineException e) {
            log.error("Error while retrieving workflow parameters for initiator notification for workflow: {}",
                    workflowId, e);
        }
    }

    private void completeWorkflowReject(String workflowRequestId, String workflowId) throws WorkflowEngineException {

        WSWorkflowResponse wsWorkflowResponse = new WSWorkflowResponse();
        List<String> workflowRelationshipIds = workflowRequestDAO.getRelationshipIds(workflowRequestId);
        for (String relationshipId : workflowRelationshipIds) {
            wsWorkflowResponse.setUuid(relationshipId);
            wsWorkflowResponse.setStatus(REJECTED);
            wsWorkflowCallBackService.onCallback(wsWorkflowResponse);
        }

        // Trigger initiator notification asynchronously.
        try {
            String notificationChannels = extractWorkFlowInitiatorNotificationChannels(workflowId);
            String userId = Utils.resolveUserID(CarbonContext.getThreadLocalCarbonContext().getUserId());

            if (StringUtils.isNotBlank(notificationChannels)) {
                executeNotificationAsync(userId, workflowId, workflowRequestId, false, REJECTED,
                        notificationChannels);
            }
        } catch (WorkflowEngineException e) {
            log.error("Error while retrieving workflow parameters for initiator notification for workflow: {}",
                    workflowId, e);
        }
    }

    /**
     * Extracts workflow initiator notification channels from workflow parameters.
     *
     * @param workflowId The workflow ID.
     * @return The notification channels configuration string, or null if not found.
     * @throws WorkflowEngineException If parameter retrieval fails.
     */
    private String extractWorkFlowInitiatorNotificationChannels(String workflowId) throws WorkflowEngineException {

        List<Parameter> parameterList = getApprovalWorkflowParameters(workflowId);
        for (Parameter parameter : parameterList) {
            if (parameter.getParamName().equalsIgnoreCase(PARAM_NOTIFICATION)) {
                String qName = parameter.getqName();
                if (StringUtils.isNotBlank(qName) && qName.startsWith(Q_NAME_INITIATOR_CHANNELS_PREFIX)) {
                    return parameter.getParamValue();
                }
            }
        }
        return null;
    }

    private List<Parameter> getApprovalWorkflowParameters(String workflowId) throws WorkflowEngineException {

        try {
            return WorkflowEngineServiceDataHolder.getInstance().getWorkflowManagementService().
                    getWorkflowParameters(workflowId);
        } catch (WorkflowException e) {
            throw new WorkflowEngineException(WorkflowEngineConstants.ErrorMessages
                    .ERROR_OCCURRED_WHILE_RETRIEVING_PARAMETER_LIST.getDescription(), e);
        }
    }

    private List<PropertyDTO> getRequestParameters(WorkflowRequest workflowRequest) throws WorkflowEngineException {

        List<PropertyDTO> workflowRequestProperties = new ArrayList<>();

        for (RequestParameter param : workflowRequest.getRequestParameters()) {
            if (param.getName().equals(WorkflowEngineConstants.ParameterName.CREDENTIAL)) {
                continue;
            }
            Object value = param.getValue();
            if (value != null) {
                String valueString = value.toString().trim();
                String paramString = param.getName().trim();
                if (ROLE_ID_PARAM_NAME.equals(param.getName())) {
                    String tenantDomain = IdentityTenantUtil.getTenantDomain(workflowRequest.getTenantId());
                    try {
                        paramString = ROLE_NAME_PARAM_NAME;
                        RoleBasicInfo roleBasicInfo = WorkflowEngineServiceDataHolder.getInstance()
                                .getRoleManagementService().getRoleBasicInfoById(valueString, tenantDomain);
                        valueString = roleBasicInfo.getName();
                        if (RoleConstants.APPLICATION.equals(roleBasicInfo.getAudience())) {
                            PropertyDTO propertyDTO = new PropertyDTO();
                            propertyDTO.setKey(ROLE_ASSOCIATED_APPLICATION_PARAM_NAME);
                            propertyDTO.setValue(roleBasicInfo.getAudienceName());
                            workflowRequestProperties.add(propertyDTO);
                        }
                    } catch (IdentityRoleManagementException e) {
                        if (StringUtils.equals(ROLE_NOT_FOUND_ERROR_CODE, e.getErrorCode())) {
                            valueString = StringUtils.EMPTY;
                        } else {
                            throw new WorkflowEngineException(e.getMessage(), e);
                        }
                    }
                } else if (USERS_TO_BE_ADDED_PARAM_NAME.equals(paramString)
                        || USERS_TO_BE_DELETED_PARAM_NAME.equals(paramString)) {
                    try {
                        AbstractUserStoreManager userStoreManager =
                                (AbstractUserStoreManager) WorkflowEngineServiceDataHolder.getInstance()
                                        .getRealmService().getTenantUserRealm(workflowRequest.getTenantId())
                                        .getUserStoreManager();
                        if (value instanceof List) {
                            List<String> userNames = userStoreManager.getUserNamesFromUserIDs((List<String>) value);
                            if (CollectionUtils.isNotEmpty(userNames)) {
                                valueString = String.join(COMMA_SEPARATOR, userNames);
                            } else {
                                valueString = StringUtils.EMPTY;
                            }
                        }
                    } catch (UserStoreException e) {
                        throw new WorkflowEngineException(e.getMessage(), e);
                    }
                } else if (CLAIMS_PROPERTY_NAME.equals(paramString)) {
                    if (WorkflowDataType.STRING_STRING_MAP_TYPE.equals(param.getValueType()) &&
                            param.getValue() != null) {
                        Map<String, String> claimsMap = (Map<String, String>) param.getValue();

                        List<LocalClaim> localClaims;
                        try {
                            localClaims = claimMetadataManagementService.getLocalClaims(
                                    CarbonContext.getThreadLocalCarbonContext().getTenantDomain());
                        } catch (ClaimMetadataException e) {
                            log.error("Error while retrieving local claims for tenant: {}",
                                    CarbonContext.getThreadLocalCarbonContext().getTenantDomain(), e);
                            continue;
                        }

                        for (Map.Entry<String, String> entry : claimsMap.entrySet()) {
                            String claimUri = entry.getKey();
                            String claimValue = entry.getValue();
                            String displayName = localClaims.stream()
                                    .filter(localClaim -> localClaim.getClaimURI().equals(claimUri))
                                    .map(localClaim -> localClaim.getClaimProperty(DISPLAY_NAME_PROPERTY))
                                    .findFirst()
                                    .orElse(claimUri);
                            PropertyDTO propertyDTO = new PropertyDTO();
                            propertyDTO.setKey(displayName);
                            propertyDTO.setValue(claimValue);
                            workflowRequestProperties.add(propertyDTO);
                        }
                        // The claims are already added. No need to add the full claim list as another parameter again.
                        continue;
                    }
                } else if (TENANT_DOMAIN_PARAM_NAME.equals(paramString)) {
                    // Skip these parameters as they are not required in the task parameters.
                    continue;
                }
                PropertyDTO propertyDTO = new PropertyDTO();
                propertyDTO.setKey(paramString);
                propertyDTO.setValue(valueString);
                workflowRequestProperties.add(propertyDTO);
            }
        }
        return workflowRequestProperties;
    }

    private List<PropertyDTO> getPropertyDTOs(Map<String, String> props) {

        return props.entrySet().stream().map(p -> getPropertyDTO(p.getKey(), p.getValue()))
                .collect(Collectors.toList());
    }

    private PropertyDTO getPropertyDTO(String key, String value) {

        PropertyDTO prop = new PropertyDTO();
        prop.setKey(key);
        prop.setValue(value);
        return prop;
    }

    private String getWorkflowRequestId(WorkflowRequest request) {

        List<RequestParameter> requestParameter;
        for (int i = 0; i < request.getRequestParameters().size(); i++) {
            requestParameter = request.getRequestParameters();
            if (requestParameter.get(i).getName().equals(WorkflowEngineConstants.ParameterName.REQUEST_ID)) {
                return (String) requestParameter.get(i).getValue();
            }
        }
        return null;
    }

    private String findAssociationNameByWorkflowAndEvent(String workflowID, String eventType) {

        try {
            return WorkflowEngineServiceDataHolder.getInstance().getWorkflowManagementService()
                    .getAssociationsForWorkflow(workflowID).stream()
                    .filter(association -> association.getEventId().equals(eventType))
                    .findFirst()
                    .map(Association::getAssociationName)
                    .orElse(null);
        } catch (WorkflowException e) {
            log.error("Error while retrieving association name for workflow ID: {} and event type: {}", workflowID,
                    eventType, e);
            return null;
        }
    }

    private void handleApprovalTaskApproval(String approvalTaskId, String workflowRequestId, String workflowId)
            throws WorkflowEngineServerException {

        // Update the approval task status to APPROVED / REJECTED and delete other tasks of the same workflow request.
        approvalTaskDAO.updateApprovalTaskStatus(approvalTaskId, ApprovalTaskServiceImpl.APPROVED);
        approvalTaskDAO.deleteApprovalTasksExceptGivenApprovalTaskId(workflowRequestId, workflowId, approvalTaskId);

        /* Update the entity of the approval task to the current user.
           This is to ensure that the task is marked as completed by the user who approved it
           and to maintain the integrity of the task history. */
        try {
            String userId = Utils.resolveUserID(CarbonContext.getThreadLocalCarbonContext().getUserId());
            approvalTaskDAO.updateApprovalTaskEntityDetail(approvalTaskId, ENTITY_TYPE_USERS, userId);
        } catch (WorkflowEngineException e) {
            throw new WorkflowEngineServerException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_RETRIEVING_ASSOCIATED_USER_ID.getDescription(), e);
        }
    }

    private void handleApprovalTaskRejection(String approvalTaskId, String workflowRequestId)
            throws WorkflowEngineServerException {

        // Update the approval task status to REJECTED and delete other tasks of the same workflow request.
        approvalTaskDAO.updateApprovalTaskStatus(approvalTaskId, ApprovalTaskServiceImpl.REJECTED);
        approvalTaskDAO.deleteApprovalTasksExceptGivenApprovalTaskId(workflowRequestId, approvalTaskId);

        /* Update the entity of the approval task to the current user.
           This is to ensure that the task is marked as completed by the user who approved it
           and to maintain the integrity of the task history. */

        try {
            String userId = Utils.resolveUserID(CarbonContext.getThreadLocalCarbonContext().getUserId());
            approvalTaskDAO.updateApprovalTaskEntityDetail(approvalTaskId, ENTITY_TYPE_USERS,
                    userId);
        } catch (WorkflowEngineException e) {
            throw new WorkflowEngineServerException(
                    WorkflowEngineConstants.ErrorMessages.ERROR_RETRIEVING_ASSOCIATED_USER_ID.getDescription(), e);
        }
    }
}
