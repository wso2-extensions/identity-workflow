package org.wso2.carbon.identity.workflow.engine;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.testng.PowerMockTestCase;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.core.util.IdentityDatabaseUtil;
import org.wso2.carbon.identity.core.util.IdentityUtil;
import org.wso2.carbon.identity.workflow.engine.exception.WorkflowEngineServerException;
import org.wso2.carbon.identity.workflow.engine.internal.dao.WorkflowEventRequestDAO;
import org.wso2.carbon.identity.workflow.engine.internal.dao.impl.WorkflowEventRequestDAOImpl;
import org.wso2.carbon.identity.workflow.engine.util.TestUtils;

import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.testng.Assert.assertEquals;

@PrepareForTest({IdentityDatabaseUtil.class, IdentityUtil.class, DefaultWorkflowEventRequestServiceTest.class})
public class DefaultWorkflowEventRequestServiceTest extends PowerMockTestCase {

    DefaultWorkflowEventRequest defaultWorkflowEventRequest = new DefaultWorkflowEventRequestService();
    WorkflowEventRequestDAO workflowEventRequestDAO = new WorkflowEventRequestDAOImpl();
    private static final Log log = LogFactory.getLog(DefaultWorkflowEventRequestServiceTest.class);

    @BeforeMethod
    public void setUp() throws Exception {

        TestUtils.initiateH2Base();
        DataSource dataSource = mock(DataSource.class);
        TestUtils.mockDataSource(dataSource);
        mockStatic(IdentityUtil.class);

        try (Connection connection = TestUtils.getConnection()) {
            Connection spyConnection = TestUtils.spyConnection(connection);
            when(dataSource.getConnection()).thenReturn(spyConnection);
            try {
                workflowEventRequestDAO
                        .addApproversOfRequest("task1", "event1", "wf1", "role",
                                "Manager", "Reserved");
                workflowEventRequestDAO
                        .addApproversOfRequest("task2", "event2", "wf2", "role",
                                "Supervisor", null);
                workflowEventRequestDAO
                        .addApproversOfRequest("task3", "event3", "wf3", "user",
                                "Senior manager", "Reserved");
            } catch (WorkflowEngineServerException e) {
                log.error("Error while adding a task", e);
            }
        }
    }

    @AfterMethod
    public void tearDown() throws Exception {

        TestUtils.closeH2Base();
    }

    @DataProvider(name = "testGetApproversOfRequestsData")
    public Object[][] testGetApproversOfRequestsData() {

        return new Object[][]{
                // eventId
                // taskId
                {"event4", "task4"},
                {"event5", "task5"},
                {"event6", "task6"}
        };
    }

    @Test(dataProvider = "testGetApproversOfRequestsData")
    public void testGetApprovalOfRequest(String eventId, String expected) {

        DataSource dataSource = mock(DataSource.class);
        TestUtils.mockDataSource(dataSource);
        try {
            try (Connection connection = TestUtils.getConnection()) {
                Connection spyConnection = TestUtils.spyConnection(connection);
                when(dataSource.getConnection()).thenReturn(spyConnection);
                try {
                    workflowEventRequestDAO
                            .addApproversOfRequest("task4", "event4", "wf4", "user",
                                    "Bob", "Reserved");
                    workflowEventRequestDAO
                            .addApproversOfRequest("task5", "event5", "wf5", "role",
                                    "Supervisor", "Ready");
                    workflowEventRequestDAO
                            .addApproversOfRequest("task6", "event6", "wf6", "user",
                                    "Sam", "Reserved");
                    String task = workflowEventRequestDAO.getApproversOfRequest(eventId);
                    assertEquals(task, expected);
                } catch (WorkflowEngineServerException e) {
                    log.error(String.format("Error while selecting a task: %s", eventId), e);
                }
            }
        } catch (SQLException e) {
            log.error("SQL Exception", e);
        }
    }

    @DataProvider(name = "testDeleteApprovalOfRequestData")
    public Object[][] testDeleteApprovalOfRequestData() {

        return new Object[][]{
                // taskId
                // eventId
                {"task1", "event1"},
                {"task2", "event2"},
                {"task3", "event3"}
        };
    }

    @Test(dataProvider = "testDeleteApprovalOfRequestData")
    public void testDeleteApprovalOfRequest(String taskId, String eventId) {

        DataSource dataSource = mock(DataSource.class);
        TestUtils.mockDataSource(dataSource);
        try {
            try (Connection connection = TestUtils.getConnection()) {
                Connection spyConnection = TestUtils.spyConnection(connection);
                when(dataSource.getConnection()).thenReturn(spyConnection);
                try {
                    defaultWorkflowEventRequest.deleteApprovalOfRequest(taskId);
                    String task = defaultWorkflowEventRequest.getApprovalOfRequest(eventId);
                    Assert.assertNull(task);
                } catch (WorkflowEngineServerException e) {
                    log.error(String.format("Error while selecting a task: %s", taskId), e);
                }
            }
        } catch (SQLException e) {
            log.error("SQL Exception", e);
        }
    }

    @DataProvider(name = "testStatesOfRequest")
    public Object[][] testCreateStatesOfRequestData() {

        return new Object[][]{
                // eventId
                // workflowId
                // currentStep
                {"event1", "wf1", 1},
                {"event1", "wf1", 2},
                {"event2", "wf2", 2},
                {"event3", "wf3", 1}
        };
    }

    @Test(dataProvider = "testStatesOfRequest")
    public void testStatesOfRequest(String eventId, String workflowId, int currentStep) {

        DataSource dataSource = mock(DataSource.class);
        TestUtils.mockDataSource(dataSource);

        try {
            try (Connection connection = TestUtils.getConnection()) {
                Connection spyConnection = TestUtils.spyConnection(connection);
                when(dataSource.getConnection()).thenReturn(spyConnection);
                try {
                    defaultWorkflowEventRequest.createStatesOfRequest(eventId, workflowId, currentStep);
                    int step = defaultWorkflowEventRequest.getStateOfRequest(eventId, workflowId);
                    assertEquals(step, currentStep);
                } catch (WorkflowEngineServerException e) {
                    log.error(String.format("Error while selecting current step: %s", eventId), e);
                }
            }
        } catch (SQLException e) {
            log.error("SQL Exception", e);
        }
    }

    @Test(dataProvider = "testStatesOfRequest")
    public void testUpdateStateOfRequest(String eventId, String workflowId, int currentStep) {

        DataSource dataSource = mock(DataSource.class);
        TestUtils.mockDataSource(dataSource);
        try {
            try (Connection connection = TestUtils.getConnection()) {
                Connection spyConnection = TestUtils.spyConnection(connection);
                when(dataSource.getConnection()).thenReturn(spyConnection);
                try {
                    defaultWorkflowEventRequest.createStatesOfRequest(eventId, workflowId, currentStep);
                    int step = defaultWorkflowEventRequest.getStateOfRequest(eventId, workflowId);
                    assertEquals(step, currentStep);
                } catch (WorkflowEngineServerException e) {
                    log.error(String.format("Error while selecting current step: %s", eventId), e);
                }
            }
        } catch (SQLException e) {
            log.error("SQL Exception", e);
        }
    }

    @DataProvider(name = "testDeleteStateOfRequestData")
    public Object[][] testDeleteStateOfRequestData() {

        return new Object[][]{
                // eventId
                // workflowId
                {"event1", "wf1"},
                {"event2", "wf2"},
                {"event3", "wf3"}
        };
    }

    @Test(dataProvider = "testDeleteStateOfRequestData")
    public void testDeleteStateOfRequest(String eventId, String workflowId) {

        DataSource dataSource = mock(DataSource.class);
        TestUtils.mockDataSource(dataSource);
        try {
            try (Connection connection = TestUtils.getConnection()) {
                Connection spyConnection = TestUtils.spyConnection(connection);
                when(dataSource.getConnection()).thenReturn(spyConnection);
                try {
                    defaultWorkflowEventRequest.deleteStateOfRequest(eventId);
                    int step = defaultWorkflowEventRequest.getStateOfRequest(eventId, workflowId);

                    assertEquals(step, 0);
                } catch (WorkflowEngineServerException e) {
                    log.error(String.format("Error while selecting current step: %s", eventId), e);
                }
            }
        } catch (SQLException e) {
            log.error("SQL Exception", e);
        }
    }
}
