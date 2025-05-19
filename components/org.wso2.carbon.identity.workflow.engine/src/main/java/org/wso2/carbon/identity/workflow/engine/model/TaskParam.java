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

package org.wso2.carbon.identity.workflow.engine.model;

/**
 * Task Parameter model, Get task parameters as list.
 */
public class TaskParam {

    private String itemName;
    private String itemValue;

    /**
     * Get the Item name of the task parameters.
     *
     * @return The Item name of the task parameters.
     */
    public String getItemName() {
        return itemName;
    }

    /**
     *  Set the Item name for the task parameters.
     *
     * @param itemName The Item name for the task parameters.
     */
    public void setItemName(String itemName) {
        this.itemName = itemName;
    }

    /**
     * Get the Item value of the task parameters.
     *
     * @return The Item value of the task parameters.
     */
    public String getItemValue() {
        return itemValue;
    }

    /**
     * Set the Item value for the task parameters.
     *
     * @param itemValue The Item value for the task parameters.
     */
    public void setItemValue(String itemValue) {
        this.itemValue = itemValue;
    }
}
