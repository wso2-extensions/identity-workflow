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
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.IObjectFactory;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.ObjectFactory;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineServerException;
import org.wso2.carbon.identity.workflow.engine.internal.dao.WorkflowEventRequestDAO;
import org.wso2.carbon.identity.workflow.engine.util.TestUtils;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.wso2.carbon.identity.workflow.engine.util.TestUtils.getConnection;
import static org.wso2.carbon.identity.workflow.engine.util.TestUtils.mockDataSource;
import static org.wso2.carbon.identity.workflow.engine.util.TestUtils.spyConnection;

@PrepareForTest({IdentityDatabaseUtil.class})
public class WorkflowEventRequestDAOImplTest extends PowerMockTestCase {

    private static final Log log = LogFactory.getLog(WorkflowEventRequestDAOImplTest.class);
    private WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();

    @BeforeMethod
    public void setUp() throws Exception {

        TestUtils.initiateH2Base();
        mockStatic(IdentityDatabaseUtil.class);
    }

    @AfterMethod
    public void tearDown() throws Exception {

        TestUtils.closeH2Base();
    }

    @DataProvider(name = "testAddApproversOfRequestsData")
    public Object[][] testAddApproversOfRequestsData() {

        return new Object[][]{
                // taskId
                // eventId
                // workflowId
                // approverType
                // approverName
                // taskStatus
                {"task1", "event1", "workflowId1", "role", "role1", "Reserved"},
                {"task2", "event2", "workflowId2", "role", "role2", "Reserved"},
                {"task3", "event3", "workflowId3", "user", "user2", "Ready"}
        };
    }

    @Test(dataProvider = "testAddApproversOfRequestsData")
    public void testAddApproversOfRequest(String taskId, String eventId, String workflowId, String approverType,
                                          String approverName, String taskStatus) {

        {
            DataSource dataSource = mock(DataSource.class);
            TestUtils.mockDataSource(dataSource);

            try (Connection connection = TestUtils.getConnection()) {
                Connection spyConnection = TestUtils.spyConnection(connection);
                when(dataSource.getConnection()).thenReturn(spyConnection);
                try {
                    workflowEventRequestDAO.addApproversOfRequest(taskId, eventId, workflowId, approverType,
                            approverName, taskStatus);
                    String task = workflowEventRequestDAO.getApproversOfRequest(eventId);
                    assertNotNull(task);
                    Assert.assertEquals(workflowEventRequestDAO.getWorkflowID(taskId), workflowId);
                    Assert.assertEquals(workflowEventRequestDAO.getRequestID(taskId), eventId);
                    Assert.assertEquals(workflowEventRequestDAO.getApproversOfRequest(eventId), taskId);
                    Assert.assertEquals(workflowEventRequestDAO.getTaskStatusOfRequest(taskId), taskStatus);
                } catch (WorkflowEngineServerException e) {
                    log.error(String.format("Error while adding task: %s", taskId), e);
                }
            } catch (SQLException e) {
                //Mock behaviour. Hence, ignored.
            } catch (WorkflowEngineServerException e) {
                assertEquals(e.getMessage(),
                        String.format("Error occurred while adding the taskId: %s, for event: %s, for workflow" +
                                        " id: %s, having the approver type: %s, with approver name " +
                                        ": %s, task status: %s",
                                taskId, eventId, workflowId, approverType, approverName, taskStatus));
            }
        }
    }

    @DataProvider(name = "testApprovalOfRequestData")
    public Object[][] testDeleteApprovalOfRequestData() {

        return new Object[][]{
                // taskId
                // eventId
                {"task1", "event1"},
                {"task2", "event2"}
        };
    }

    @Test(dataProvider = "testApprovalOfRequestData")
    public void testDeleteApproversOfRequest(String taskId, String eventId) {

        DataSource dataSource = mock(DataSource.class);
        TestUtils.mockDataSource(dataSource);

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            try {
                workflowEventRequestDAO.deleteApproversOfRequest(taskId);
                String task = workflowEventRequestDAO.getApproversOfRequest(eventId);
                Assert.assertNull(task);
            } catch (WorkflowEngineServerException e) {
                log.error(String.format("Error while adding task: %s", taskId), e);
            }
        } catch (SQLException e) {
            //Mock behaviour. Hence, ignored.
        } catch (WorkflowEngineServerException e) {
            assertEquals(e.getMessage(),
                    String.format("Error occurred while adding the taskId: %s, for event: %s",
                            taskId, eventId));
        }
    }

    @DataProvider(name = "testStatesOfRequestData")
    public Object[][] testCreateStatesOfRequestData() {

        return new Object[][]{
                // eventId
                // workflowId
                // currentStep
                {"event1", "wf1", 1},
                {"event2", "wf2", 2},
                {"event3", "wf3", 0}
        };
    }

    @Test(dataProvider = "testStatesOfRequestData")
    public void testCreateStatesOfRequest(String eventId, String workflowId, int currentStep) {

        DataSource dataSource = mock(DataSource.class);
        mockDataSource(dataSource);

        try (Connection connection = getConnection()) {
            Connection spyConnection = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            workflowEventRequestDAO.createStatesOfRequest(eventId, workflowId, currentStep);
            int step = workflowEventRequestDAO.getStateOfRequest(eventId, workflowId);
            assertEquals(step, currentStep);
        } catch (SQLException | WorkflowEngineServerException e) {
            //Mock behaviour. Hence, ignored.
        }
    }

    @Test(dataProvider = "testStatesOfRequestData")
    public void testUpdateStateOfRequest(String eventId, String workflowId, int currentStep) {

        DataSource dataSource = mock(DataSource.class);
        mockDataSource(dataSource);

        try (Connection connection = getConnection()) {
            Connection spyConnection = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            workflowEventRequestDAO.createStatesOfRequest(eventId, workflowId, currentStep);
            int step = workflowEventRequestDAO.getStateOfRequest(eventId, workflowId);
            step += 1;
            workflowEventRequestDAO.updateStateOfRequest(eventId, workflowId, step);
            int updatedStep = workflowEventRequestDAO.getStateOfRequest(eventId, workflowId);
            assertEquals(step, updatedStep);
        } catch (SQLException | WorkflowEngineServerException e) {
            //Mock behaviour. Hence, ignored.
        }
    }

    @DataProvider(name = "testDeleteStateOfRequestData")
    public Object[][] testDeleteStateOfRequestData() {

        return new Object[][]{
                // eventId
                // workflowId
                {"event1", "wf1"},
        };
    }

    @Test(dataProvider = "testDeleteStateOfRequestData")
    public void testDeleteCurrentStepOfRequest(String eventId, String workflowId) {

        DataSource dataSource = mock(DataSource.class);
        mockDataSource(dataSource);

        try (Connection connection = getConnection()) {
            Connection spyConnection = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            workflowEventRequestDAO.deleteCurrentStepOfRequest(eventId);
            int step = workflowEventRequestDAO.getStateOfRequest(eventId, workflowId);
            assertEquals(step, 0);
        } catch (SQLException | WorkflowEngineServerException e) {
            //Mock behaviour. Hence, ignored.
        }
    }

    @DataProvider(name = "testListApproversData")
    public Object[][] testListApproversData() {

        List<String> approverList = new ArrayList<>();
        approverList.add("Alex");
        approverList.add("Bob");

        List<String> approverList1 = new ArrayList<>();
        approverList1.add("Sam");

        return new Object[][]{
                // eventId
                // expected List
                {"task4", approverList},
                {"task5", approverList1}
        };
    }

    @Test(dataProvider = "testListApproversData")
    public void testListApprovers(String taskId, List<String> expectedList) {

        DataSource dataSource = mock(DataSource.class);
        mockDataSource(dataSource);

        try (Connection connection = getConnection()) {
            Connection spyConnection = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            workflowEventRequestDAO.addApproversOfRequest("task4", "event4", "wf1",
                    "user", "Alex", "Reserved");
            workflowEventRequestDAO.addApproversOfRequest("task4", "event4", "wf1",
                    "user", "Bob", "Reserved");
            workflowEventRequestDAO.addApproversOfRequest("task5", "event5", "wf2",
                    "user", "Sam", "Reserved");

            List<String> approvers = workflowEventRequestDAO.listApprovers(taskId);
            Assert.assertEquals(approvers, expectedList);

        } catch (SQLException | WorkflowEngineServerException e) {
            //Mock behaviour. Hence, ignored.
        }
    }

    @DataProvider(name = "testGetRequestIdFromApproverData")
    public Object[][] testGetRequestIdFromApproverData() {

        return new Object[][]{
                // eventId
                // workflowId
                {"Alex", "event6"},
                {"Bob", "event5"}
        };
    }

    @Test(dataProvider = "testGetRequestIdFromApproverData")
    public void testGetRequestIdFromApprover(String approverName, String eventId) {

        DataSource dataSource = mock(DataSource.class);
        mockDataSource(dataSource);

        try (Connection connection = getConnection()) {
            Connection spyConnection = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            workflowEventRequestDAO.addApproversOfRequest("task6", "event6", "wf6",
                    "user", "Alex", "Reserved");
            Assert.assertFalse(approverName.isEmpty());

        } catch (SQLException | WorkflowEngineServerException e) {
            //Mock behaviour. Hence, ignored.
        }
    }

    @DataProvider(name = "testGetRequestsListData")
    public Object[][] testGetRequestsListData() {

        //List approverList = Arrays.asList(new String[]{"Bob"});
        List<String> requestList = new ArrayList<>();
        requestList.add("request1");
        requestList.add("request2");

        List<String> requestList1 = new ArrayList<>();
        requestList1.add("request3");

        return new Object[][]{
                // eventId
                // expected List
                {"Alex", requestList},
                {"Sam", requestList1}
        };
    }

    @Test(dataProvider = "testGetRequestsListData")
    public void testGetRequestsList(String approverName, List<String> exceptedList) {

        DataSource dataSource = mock(DataSource.class);
        mockDataSource(dataSource);

        try (Connection connection = getConnection()) {
            Connection spyConnection = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            workflowEventRequestDAO.addApproversOfRequest("task7", "request1", "wf1",
                    "user", "Alex", "Reserved");
            workflowEventRequestDAO.addApproversOfRequest("task8", "request2", "wf1",
                    "user", "Alex", "Reserved");
            workflowEventRequestDAO.addApproversOfRequest("task9", "request3", "wf2",
                    "user", "Sam", "Reserved");

            List<String> requests = workflowEventRequestDAO.getRequestsList(approverName);
            Assert.assertEquals(requests, exceptedList);

        } catch (SQLException | WorkflowEngineServerException e) {
            //Mock behaviour. Hence, ignored.
        }
    }

    @DataProvider(name = "testUpdateStatusOfRequestData")
    public Object[][] testUpdateStatusOfRequestData() {

        return new Object[][]{
                // taskId
                // taskStatus
                {"task10", "Completed"},
                {"task11", "Reserved"}
        };
    }

    @Test(dataProvider = "testUpdateStatusOfRequestData")
    public void testUpdateStatusOfRequest(String taskId, String taskStatus) {

        DataSource dataSource = mock(DataSource.class);
        mockDataSource(dataSource);

        try (Connection connection = getConnection()) {
            Connection spyConnection = spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            workflowEventRequestDAO.addApproversOfRequest("task10", "event4", "wf1",
                    "user", "Alex", "Reserved");
            workflowEventRequestDAO.addApproversOfRequest("task11", "event5", "wf2",
                    "role", "Bob", "Ready");
            workflowEventRequestDAO.updateStatusOfRequest(taskId, taskStatus);
            String updatedStatus = workflowEventRequestDAO.getTaskStatusOfRequest(taskId);
            Assert.assertEquals(updatedStatus, taskStatus);

        } catch (SQLException | WorkflowEngineServerException e) {
            //Mock behaviour. Hence, ignored.
        }
    }

    @ObjectFactory
    public IObjectFactory getObjectFactory() {

        return new org.powermock.modules.testng.PowerMockObjectFactory();
    }
}
