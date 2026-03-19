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
import org.wso2.carbon.identity.workflow.engine.internal.dao.WorkflowRequestDAO;
import org.wso2.carbon.identity.workflow.engine.util.WorkflowEngineConstants;

import java.sql.Timestamp;
import java.util.List;

/**
 * Implementation of {@link WorkflowRequestDAO} to handle workflow request related database operations.
 */
public class WorkflowRequestDAOImpl implements WorkflowRequestDAO {

    private static final Log log = LogFactory.getLog(WorkflowRequestDAOImpl.class.getName());

    @Override
    public String getRelationshipId(String workflowRequestId, String workflowId) throws WorkflowEngineServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            return jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.
                            GET_RELATIONSHIP_ID_BY_REQUEST_ID_AND_WORKFLOW_ID,
                    ((resultSet, i) -> (
                            resultSet.getString(WorkflowEngineConstants.RELATIONSHIP_ID_IN_REQUEST_COLUMN))),
                    preparedStatement -> {
                            preparedStatement.setString(1, workflowRequestId);
                            preparedStatement.setString(2, workflowId);
                    });
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving relationship ID from " +
                    "workflow request Id: %s and workflow Id: %s", workflowRequestId, workflowId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
    }

    @Override
    public List<String> getRelationshipIds(String workflowRequestId) throws WorkflowEngineServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        try {
            return jdbcTemplate.executeQuery(WorkflowEngineConstants.SqlQueries.
                            GET_RELATIONSHIP_IDS_BY_REQUEST_ID,
                    ((resultSet, i) -> (
                            resultSet.getString(WorkflowEngineConstants.RELATIONSHIP_ID_IN_REQUEST_COLUMN))),
                    preparedStatement -> preparedStatement.setString(1,
                            workflowRequestId));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving relationship IDs from " +
                    "workflow request Id: %s", workflowRequestId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
    }

    @Override
    public String getInitiatedUser(String requestId) throws WorkflowEngineServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String createdBy;
        try {
            createdBy = jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.
                            GET_CREATED_USER,
                    ((resultSet, i) -> (
                            resultSet.getString(WorkflowEngineConstants.CREATED_USER_COLUMN))),
                    preparedStatement -> preparedStatement.setString(1, requestId));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving initiator from " +
                    "workflow requestId: %s", requestId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return createdBy;
    }

    @Override
    public Timestamp getCreatedAtTimeInMill(String requestId) throws WorkflowEngineServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        java.sql.Timestamp createdTime;
        try {
            createdTime = jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.
                            GET_CREATED_TIME_IN_MILL,
                    ((resultSet, i) -> (
                            resultSet.getTimestamp(WorkflowEngineConstants.CREATED_AT_IN_MILL_COLUMN))),
                    preparedStatement -> preparedStatement.setString(1, requestId));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving createdAt time from " +
                    "workflow request Id: %s", requestId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return createdTime;
    }
}
