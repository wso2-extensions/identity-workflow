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

package org.wso2.carbon.identity.workflow.engine.dto;

/**
 * Action to perform on the task dto.
 */
public class StateDTO {

    /**
     * Actions to perform on an approval task.
     */
    public enum ActionEnum {
        CLAIM, RELEASE, APPROVE, REJECT,
    };

    private ActionEnum action = null;

    /**
     * getAction to perform on the task.
     **/
    public ActionEnum getAction() {

        return action;
    }

    /**
     * Set Action to perform on the task.
     **/
    public void setAction(ActionEnum action) {

        this.action = action;
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();
        sb.append("class StateDTO {\n");
        sb.append("  action: ").append(action).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
