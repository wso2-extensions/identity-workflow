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

package org.wso2.carbon.identity.workflow.engine.util;

import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Collections;
import java.util.Map;

/**
 * Unit tests for ApprovalTaskAuditLogger class.
 */
public class ApprovalTaskAuditLoggerTest {

    private ApprovalTaskAuditLogger auditLogger;

    @BeforeMethod
    public void setUp() {

        auditLogger = new ApprovalTaskAuditLogger();
    }

    @Test
    public void testBuildDataMapWithAllFields() {

        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE)
                .taskId("task-123")
                .workflowRequestId("req-456")
                .workflowId("wf-789")
                .approverType("USER")
                .approverId("user-001")
                .existingStatus("PENDING")
                .newStatus("APPROVED")
                .stepValue(1);

        Map<String, Object> dataMap = builder.buildDataMap();
        Assert.assertNotNull(dataMap, "Data map should not be null");
        Assert.assertEquals(dataMap.get("TaskId"), "task-123");
        Assert.assertEquals(dataMap.get("WorkflowRequestId"), "req-456");
        Assert.assertEquals(dataMap.get("WorkflowId"), "wf-789");
        Assert.assertEquals(dataMap.get("ApproverType"), "USER");
        Assert.assertEquals(dataMap.get("ApproverId"), "user-001");
        Assert.assertEquals(dataMap.get("TaskExistingStatus"), "PENDING");
        Assert.assertEquals(dataMap.get("TaskNewStatus"), "APPROVED");
        Assert.assertEquals(dataMap.get("StepValue"), 1);
    }

    @Test
    public void testBuildDataMapWithNullValues() {

        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE)
                .taskId("task-123");

        Map<String, Object> dataMap = builder.buildDataMap();
        Assert.assertNotNull(dataMap, "Data map should not be null");
        Assert.assertEquals(dataMap.get("TaskId"), "task-123");
        Assert.assertFalse(dataMap.containsKey("WorkflowRequestId"),
                "Null workflow request ID should not be in map");
        Assert.assertFalse(dataMap.containsKey("WorkflowId"),
                "Null workflow ID should not be in map");
        Assert.assertFalse(dataMap.containsKey("ApproverType"), "Null approver type should not be in map");
        Assert.assertFalse(dataMap.containsKey("ApproverId"), "Null approver ID should not be in map");
        Assert.assertFalse(dataMap.containsKey("TaskExistingStatus"),
                "Null existing status should not be in map");
        Assert.assertFalse(dataMap.containsKey("TaskNewStatus"), "Null new status should not be in map");
        Assert.assertFalse(dataMap.containsKey("StepValue"), "Null step value should not be in map");
    }

    @Test
    public void testBuildDataMapWithEmptyStrings() {

        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE)
                .taskId("task-123")
                .workflowRequestId("")
                .workflowId("   ")
                .approverType(null);

        Map<String, Object> dataMap = builder.buildDataMap();
        Assert.assertNotNull(dataMap, "Data map should not be null");
        Assert.assertEquals(dataMap.get("TaskId"), "task-123");
        Assert.assertFalse(dataMap.containsKey("WorkflowRequestId"),
                "Empty workflow request ID should not be in map");
        Assert.assertFalse(dataMap.containsKey("WorkflowId"), "Blank workflow ID should not be in map");
        Assert.assertFalse(dataMap.containsKey("ApproverType"), "Null approver type should not be in map");
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Operation is required for audit logging")
    public void testValidationFailsWithNullOperation() {

        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .taskId("task-123");
        auditLogger.printAuditLog(builder);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Task ID is required for audit logging")
    public void testValidationFailsWithNullTaskId() {
        // Setup
        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE);
        auditLogger.printAuditLog(builder);
    }

    @Test(expectedExceptions = IllegalArgumentException.class,
          expectedExceptionsMessageRegExp = "Task ID is required for audit logging")
    public void testValidationFailsWithBlankTaskId() {

        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE)
                .taskId("   ");
        auditLogger.printAuditLog(builder);
    }

    @Test
    public void testPrintAuditLogWithNullBuilder() {

        auditLogger.printAuditLog(null);

        // If we reach here, the test passed (no exception thrown).
        Assert.assertTrue(true, "Method should handle null builder gracefully");
    }

    @Test
    public void testAllOperationTypes() {

        ApprovalTaskAuditLogger.Operation[] operations = {
            ApprovalTaskAuditLogger.Operation.APPROVE,
            ApprovalTaskAuditLogger.Operation.REJECT,
            ApprovalTaskAuditLogger.Operation.RESERVE,
            ApprovalTaskAuditLogger.Operation.RELEASE,
            ApprovalTaskAuditLogger.Operation.COMPLETE
        };

        for (ApprovalTaskAuditLogger.Operation operation : operations) {
            ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                    .operation(operation)
                    .taskId("task-123");

            Map<String, Object> dataMap = builder.buildDataMap();
            Assert.assertNotNull(dataMap, "Data map should not be null for operation: " + operation);
            Assert.assertNotNull(operation.getLogAction(),
                    "Log action should not be null for operation: " + operation);
        }
    }

    @Test
    public void testOperationLogActions() {

        Assert.assertEquals(ApprovalTaskAuditLogger.Operation.APPROVE.getLogAction(), "approve-approval");
        Assert.assertEquals(ApprovalTaskAuditLogger.Operation.REJECT.getLogAction(), "reject-approval");
        Assert.assertEquals(ApprovalTaskAuditLogger.Operation.RESERVE.getLogAction(), "reserve-approval");
        Assert.assertEquals(ApprovalTaskAuditLogger.Operation.RELEASE.getLogAction(), "release-approval");
        Assert.assertEquals(ApprovalTaskAuditLogger.Operation.COMPLETE.getLogAction(), "complete-approval");
    }

    @Test
    public void testTriggerAuditLogWithNullOperation() {

        auditLogger.triggerAuditLog(null, "task-123", Collections.singletonMap("key", "value"));
        Assert.assertTrue(true, "Method should handle null operation gracefully");
    }

    @Test
    public void testTriggerAuditLogWithBlankTargetId() {

        auditLogger.triggerAuditLog(ApprovalTaskAuditLogger.Operation.APPROVE, "  ",
                Collections.singletonMap("key", "value"));
        Assert.assertTrue(true, "Method should handle blank target ID gracefully");
    }

    @Test
    public void testBuilderReusability() {

        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE)
                .taskId("task-123");

        Map<String, Object> dataMap1 = builder.buildDataMap();
        Map<String, Object> dataMap2 = builder.buildDataMap();

        Assert.assertNotNull(dataMap1, "First data map should not be null");
        Assert.assertNotNull(dataMap2, "Second data map should not be null");
        Assert.assertEquals(dataMap1.get("TaskId"), dataMap2.get("TaskId"), "Task IDs should be equal");
    }

    @Test
    public void testBuilderWithStepValueZero() {

        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE)
                .taskId("task-123")
                .stepValue(0);

        Map<String, Object> dataMap = builder.buildDataMap();
        Assert.assertNotNull(dataMap, "Data map should not be null");
        Assert.assertEquals(dataMap.get("StepValue"), 0, "Step value should be 0");
    }

    @Test
    public void testBuilderWithNegativeStepValue() {

        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE)
                .taskId("task-123")
                .stepValue(-1);

        Map<String, Object> dataMap = builder.buildDataMap();
        Assert.assertNotNull(dataMap, "Data map should not be null");
        Assert.assertEquals(dataMap.get("StepValue"), -1, "Step value should be -1");
    }

    @Test
    public void testBuilderWithMinimalRequiredFields() {
        // Setup - only operation and taskId (required fields)
        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.RESERVE)
                .taskId("minimal-task");

        Map<String, Object> dataMap = builder.buildDataMap();
        Assert.assertNotNull(dataMap, "Data map should not be null");
        Assert.assertEquals(dataMap.get("TaskId"), "minimal-task");

        Assert.assertFalse(dataMap.containsKey("WorkflowRequestId"), "Optional field should not be present");
        Assert.assertFalse(dataMap.containsKey("WorkflowId"), "Optional field should not be present");
        Assert.assertFalse(dataMap.containsKey("ApproverType"), "Optional field should not be present");
    }

    @Test
    public void testBuilderFieldOverwriting() {

        ApprovalTaskAuditLogger.AuditLogBuilder builder = auditLogger.auditBuilder()
                .operation(ApprovalTaskAuditLogger.Operation.APPROVE)
                .taskId("original-task")
                .taskId("updated-task")
                .stepValue(1)
                .stepValue(2);


        Map<String, Object> dataMap = builder.buildDataMap();
        Assert.assertNotNull(dataMap, "Data map should not be null");
        Assert.assertEquals(dataMap.get("TaskId"), "updated-task", "Task ID should be updated value");
        Assert.assertEquals(dataMap.get("StepValue"), 2, "Step value should be updated value");
    }
}
