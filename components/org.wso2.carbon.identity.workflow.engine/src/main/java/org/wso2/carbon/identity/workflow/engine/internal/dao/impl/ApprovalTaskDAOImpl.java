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

package org.wso2.carbon.identity.workflow.engine.internal.dao.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.wso2.carbon.database.utils.jdbc.JdbcTemplate;
import org.wso2.carbon.database.utils.jdbc.exceptions.DataAccessException;
import org.wso2.carbon.identity.core.util.JdbcUtils;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineServerException;
import org.wso2.carbon.identity.workflow.engine.internal.dao.ApprovalTaskDAO;
import org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

/**
 * Workflow Event Request DAO implementation.
 */
public class ApprovalTaskDAOImpl implements ApprovalTaskDAO {

    private static final Log log = LogFactory.getLog(ApprovalTaskDAOImpl.class.getName());

    /**
     * {@inheritDoc}
     */
    @Override
    public void addApproversOfRequest(String taskId, String eventId, String workflowId, String approverType,
                                      String approverName, String taskStatus) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(WorkflowEngineConstants.SqlQueries.ADD_APPROVAL_LIST_RELATED_TO_USER,
                    preparedStatement -> {
                        preparedStatement.setString(1, taskId);
                        preparedStatement.setString(2, eventId);
                        preparedStatement.setString(3, workflowId);
                        preparedStatement.setString(4, approverType);
                        preparedStatement.setString(5, approverName);
                        preparedStatement.setString(6, taskStatus);
                    });
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while adding request details" +
                    "in eventId: %s  & workflowId: %s", eventId, workflowId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public String getApproversOfRequest(String eventId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String taskIdExists;
        try {
            taskIdExists = jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.
                            GET_TASK_ID_RELATED_TO_USER,
                    ((resultSet, i) -> (
                            resultSet.getString(WorkflowEngineConstants.TASK_ID_COLUMN))),
                    preparedStatement -> preparedStatement.setString(1, eventId));
            if (taskIdExists == null) {
                return null;
            }
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving taskId from" +
                    "requestId: %s", eventId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return taskIdExists;
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void deleteApproversOfRequest(String taskId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(WorkflowEngineConstants.SqlQueries.DELETE_APPROVAL_LIST_RELATED_TO_USER,
                    preparedStatement -> preparedStatement.setString(1, taskId));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error while deleting the approver details from taskId:%s", taskId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void addApprovalTaskStep(String eventId, String workflowId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(WorkflowEngineConstants.SqlQueries.ADD_CURRENT_STEP_FOR_EVENT,
                    preparedStatement -> {
                        preparedStatement.setString(1, eventId);
                        preparedStatement.setString(2, workflowId);
                    });
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while adding request approval steps" +
                    "in event Id: %s & workflowId: %s", eventId, workflowId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
    }

    @Override
    public int getCurrentApprovalStepOfWorkflowRequest(String requestId, String workflowId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String stepExists;
        try {
            stepExists = jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.GET_CURRENT_STEP,
                    ((resultSet, i) -> (
                            Integer.toString(resultSet.getInt(WorkflowEngineConstants.CURRENT_STEP_COLUMN)))),
                    preparedStatement -> {
                        preparedStatement.setString(1, requestId);
                        preparedStatement.setString(2, workflowId);
                    });
            if (stepExists == null) {
                return 0;
            }
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving currentStep from" +
                    "event Id: %s & workflowId: %s", requestId, workflowId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return Integer.parseInt(stepExists);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public void updateStateOfRequest(String eventId, String workflowId, int currentStep) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(WorkflowEngineConstants.SqlQueries.UPDATE_STATE_OF_REQUEST,
                    (preparedStatement -> {
                        setPreparedStatementForStateOfRequest(currentStep, eventId, workflowId, preparedStatement);
                        preparedStatement.setInt(1, currentStep);
                        preparedStatement.setString(2, eventId);
                        preparedStatement.setString(3, workflowId);
                    }));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while updating state from" +
                    "eventId: %s", eventId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
    }

    private void setPreparedStatementForStateOfRequest(int currentStep, String eventId, String workflowId,
                                                       PreparedStatement preparedStatement) throws SQLException {

        preparedStatement.setInt(1, currentStep);
        preparedStatement.setString(2, eventId);
        preparedStatement.setString(3, workflowId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> listApprovers(String taskId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<String> requestsList;
        try {
            requestsList = jdbcTemplate.executeQuery(WorkflowEngineConstants.SqlQueries.
                            GET_APPROVER_NAME_RELATED_TO_CURRENT_TASK_ID, (resultSet, rowNumber) ->
                            resultSet.getString(WorkflowEngineConstants.APPROVER_NAME_COLUMN),
                    preparedStatement -> preparedStatement.setString(1, taskId));
        } catch (DataAccessException e) {
            String errorMessage = "Error occurred while retrieving approvers";
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return requestsList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApproverType(String taskId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String approverType;
        try {
            approverType = jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.
                            GET_APPROVER_TYPE_RELATED_TO_CURRENT_TASK_ID,
                    ((resultSet, i) -> (
                            resultSet.getString(WorkflowEngineConstants.APPROVER_TYPE_COLUMN))),
                    preparedStatement -> preparedStatement.setString(1, taskId));
        } catch (DataAccessException e) {
            String errorMessage = "Error occurred while retrieving approver type";
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return approverType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getWorkflowRequestIdByTaskId(String taskId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String requestId;
        try {
            requestId = jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.
                            GET_APPROVAL_TASK_BY_TASK_ID,
                    ((resultSet, i) -> (
                            resultSet.getString(WorkflowEngineConstants.EVENT_ID))),
                    preparedStatement -> preparedStatement.setString(1, taskId));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving request ID from" +
                    "taskID: %s", taskId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return requestId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getRequestsList(String approverName) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<String> requestIdList;
        try {
            requestIdList = jdbcTemplate.executeQuery(WorkflowEngineConstants.SqlQueries.
                            GET_REQUEST_ID_FROM_APPROVER, (resultSet, rowNumber) ->
                            resultSet.getString(WorkflowEngineConstants.EVENT_ID),
                    preparedStatement -> preparedStatement.setString(1, approverName));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving request id from" +
                    "approver name: %s", approverName);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return requestIdList;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getRequestsList(String approverType, String approverName) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<String> requestIdList;
        try {
            requestIdList = jdbcTemplate.executeQuery(WorkflowEngineConstants.SqlQueries.
                            GET_REQUEST_ID_FROM_APPROVER_AND_TYPE, (resultSet, rowNumber) ->
                            resultSet.getString(WorkflowEngineConstants.EVENT_ID),
                    preparedStatement -> {
                        preparedStatement.setString(1, approverName);
                        preparedStatement.setString(2, approverType);
                    });
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving request id from" +
                    "approver name: %s" + " of approver type: %s", approverName, approverType);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return requestIdList;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getTaskIDList(String approverType, String approverName) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<String> requestIdList;
        try {
            requestIdList = jdbcTemplate.executeQuery(WorkflowEngineConstants.SqlQueries.
                            GET_TASK_ID_FROM_APPROVER_AND_TYPE, (resultSet, rowNumber) ->
                            resultSet.getString(WorkflowEngineConstants.TASK_ID_COLUMN),
                    preparedStatement -> {
                        preparedStatement.setString(1, approverName);
                        preparedStatement.setString(2, approverType);
                    });
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving task id from" +
                    "approver name: %s" + " of approver type: %s", approverName, approverType);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return requestIdList;
    }

    /**
     * {@inheritDoc}
     */
    public List<String> getTaskIDListByStatus(String approverType, String approverName, String status) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<String> requestIdList;
        try {
            requestIdList = jdbcTemplate.executeQuery(WorkflowEngineConstants.SqlQueries.
                            GET_TASK_ID_FROM_APPROVER_AND_TYPE_BY_STATUS, (resultSet, rowNumber) ->
                            resultSet.getString(WorkflowEngineConstants.TASK_ID_COLUMN),
                    preparedStatement -> {
                        preparedStatement.setString(1, approverName);
                        preparedStatement.setString(2, approverType);
                        preparedStatement.setString(3, status);
                    });
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving task id from" +
                    "approver name: %s" + " of approver type: %s", approverName, approverType);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return requestIdList;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getApprovalTaskStatus(String taskId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String taskStatus;
        try {
            taskStatus = jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.
                            GET_TASK_STATUS,
                    ((resultSet, i) -> (
                            resultSet.getString(WorkflowEngineConstants.TASK_STATUS_COLUMN))),
                    preparedStatement -> preparedStatement.setString(1, taskId));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving task status from" +
                    "task Id: %s", taskId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return taskStatus;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateApprovalTaskStatus(String taskId, String taskStatus) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            jdbcTemplate.executeUpdate(WorkflowEngineConstants.SqlQueries.UPDATE_TASK_STATUS,
                    (preparedStatement -> {
                        setPreparedStatementForStatusOfRequest(taskStatus, taskId, preparedStatement);
                        preparedStatement.setString(1, taskStatus);
                        preparedStatement.setString(2, taskId);
                    }));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while updating status from" +
                    "taskID: %s", taskId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getTaskIds(String eventId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        List<String> taskIdList;
        try {
            taskIdList = jdbcTemplate.executeQuery(WorkflowEngineConstants.SqlQueries.
                            GET_TASK_ID_FROM_REQUEST, (resultSet, rowNumber) ->
                            resultSet.getString(WorkflowEngineConstants.TASK_ID_COLUMN),
                    preparedStatement -> preparedStatement.setString(1, eventId));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving tasks from" +
                    "request id : %s", eventId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return taskIdList;
    }

    private void setPreparedStatementForStatusOfRequest(String taskStatus, String taskId,
                                                        PreparedStatement preparedStatement) throws SQLException {

        preparedStatement.setString(1, taskStatus);
        preparedStatement.setString(2, taskId);
    }

    /**
     *{@inheritDoc}
     */
    @Override
    public String getWorkflowID(String taskId) {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String taskStatus;
        try {
            taskStatus = jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.
                            GET_WORKFLOW_ID,
                    ((resultSet, i) -> (
                            resultSet.getString(WorkflowEngineConstants.WORKFLOW_ID))),
                    preparedStatement -> preparedStatement.setString(1, taskId));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving workflow from" +
                    "task Id: %s", taskId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return taskStatus;
    }
}
