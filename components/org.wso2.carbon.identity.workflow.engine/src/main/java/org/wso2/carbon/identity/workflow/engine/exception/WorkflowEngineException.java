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

package org.wso2.carbon.identity.workflow.engine.exception;

/**
 * Base exception for handling workflow engine exceptions.
 */
public class WorkflowEngineException extends Exception {

    private String errorCode;

    public WorkflowEngineException(String message, String errorCode) {

        super(message);
        this.errorCode = errorCode;
    }

    public WorkflowEngineException(String message, Throwable cause) {

        super(message, cause);
    }

    /**
     * Returns the error code.
     *
     * @return Error code.
     */
    public String getErrorCode() {

        return errorCode;
    }
}
