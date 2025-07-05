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
package org.wso2.carbon.identity.workflow.engine.internal.dao;

import java.sql.Timestamp;

/**
 * WorkflowRequestDAO interface provides methods to manage workflow requests.
 */
public interface WorkflowRequestDAO {

    /**
     * Returns the relationship ID given the request ID.
     *
     * @param requestId the event ID that need to be checked.
     * @return the relationship ID.
     */
    String getRelationshipId(String requestId);

    /**
     * Returns the initiator given the request ID.
     *
     * @param requestId the request ID that need to be checked.
     * @return string initiator.
     */
    String getInitiatedUser(String requestId);

    /**
     * Returns the created time of the request.
     *
     * @param requestId the request ID that need to be checked.
     * @return the created time.
     */
    Timestamp getCreatedAtTimeInMill(String requestId);
}
