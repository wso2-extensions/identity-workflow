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
 * The model class for set up the page pagination.
 */
public class PagePagination {

    private int localPageSize;
    private boolean localPageSizeTracker = false;
    private int localPageNumber;
    private boolean localPageNumberTracker = false;

    /**
     * Maximum number of results expected.
     */
    public int getPageSize() {

        return this.localPageSize;
    }

    /**
     * Set maximum number of results expected.
     */
    public void setPageSize(int param) {

        this.localPageSizeTracker = param != Integer.MIN_VALUE;
        this.localPageSize = param;
    }

    /**
     * Start index of the search.
     */
    public int getPageNumber() {

        return this.localPageNumber;
    }

    /**
     * Set start index of the search.
     */
    public void setPageNumber(int param) {

        this.localPageNumberTracker = param != Integer.MIN_VALUE;
        this.localPageNumber = param;
    }
}
