/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
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

package org.wso2.carbon.identity.workflow.engine;

import org.testng.Assert;
import org.testng.annotations.Test;
import org.wso2.carbon.identity.workflow.engine.dto.PropertyDTO;
import org.wso2.carbon.identity.workflow.mgt.bean.RequestParameter;
import org.wso2.carbon.identity.workflow.mgt.dto.WorkflowRequest;
import sun.reflect.ReflectionFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests for {@link ApprovalTaskServiceImpl}.
 */
public class ApprovalTaskServiceImplTest {

    @Test
    public void testGroupUserListsAreShownAsUsernames() throws Exception {

        WorkflowRequest request = new WorkflowRequest();
        request.setEventType("UPDATE_GROUPS_OF_USERS");
        List<RequestParameter> parameters = new ArrayList<>();
        parameters.add(parameter("Group Name", "engineering"));
        parameters.add(parameter("Users to be Added", Arrays.asList("alice", "bob")));
        parameters.add(parameter("Users to be Deleted", Collections.emptyList()));
        request.setRequestParameters(parameters);

        List<PropertyDTO> properties = getRequestParameters(request);

        Assert.assertEquals(valueOf(properties, "Users to be Added"), "alice,bob");
        Assert.assertEquals(valueOf(properties, "Users to be Deleted"), "");
    }

    @SuppressWarnings("unchecked")
    private List<PropertyDTO> getRequestParameters(WorkflowRequest request) throws Exception {

        Method method = ApprovalTaskServiceImpl.class.getDeclaredMethod("getRequestParameters",
                WorkflowRequest.class);
        method.setAccessible(true);
        // Skip field initializers: they build carbon services that need a running server.
        Constructor<?> constructor = ReflectionFactory.getReflectionFactory().newConstructorForSerialization(
                ApprovalTaskServiceImpl.class, Object.class.getDeclaredConstructor());
        return (List<PropertyDTO>) method.invoke(constructor.newInstance(), request);
    }

    private static RequestParameter parameter(String name, Object value) {

        RequestParameter parameter = new RequestParameter();
        parameter.setName(name);
        parameter.setValue(value);
        return parameter;
    }

    private static String valueOf(List<PropertyDTO> properties, String key) {

        return properties.stream().filter(property -> key.equals(property.getKey()))
                .map(PropertyDTO::getValue).findFirst().orElse(null);
    }
}
