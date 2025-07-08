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

/**
 * Implementation of {@link WorkflowRequestDAO} to handle workflow request related database operations.
 */
public class WorkflowRequestDAOImpl implements WorkflowRequestDAO {

    private static final Log log = LogFactory.getLog(WorkflowRequestDAOImpl.class.getName());

    @Override
    public String getRelationshipId(String requestId) throws WorkflowEngineServerException {

        JdbcTemplate jdbcTemplate = JdbcUtils.getNewTemplate();
        String taskStatus;
        try {
            taskStatus = jdbcTemplate.fetchSingleRecord(WorkflowEngineConstants.SqlQueries.
                            GET_REQUEST_ID_OF_RELATIONSHIP,
                    ((resultSet, i) -> (
                            resultSet.getString(WorkflowEngineConstants.RELATIONSHIP_ID_IN_REQUEST_COLUMN))),
                    preparedStatement -> preparedStatement.setString(1, requestId));
        } catch (DataAccessException e) {
            String errorMessage = String.format("Error occurred while retrieving relationship ID from" +
                    "event Id: %s", requestId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return taskStatus;
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
            String errorMessage = String.format("Error occurred while retrieving initiator from" +
                    "requestId: %s", requestId);
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
            String errorMessage = String.format("Error occurred while retrieving createdAt time from" +
                    "request Id: %s", requestId);
            if (log.isDebugEnabled()) {
                log.debug(errorMessage, e);
            }
            throw new WorkflowEngineServerException(errorMessage, e);
        }
        return createdTime;
    }
}
