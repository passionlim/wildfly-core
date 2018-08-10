/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2018, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.wildfly.core.test.standalone.mgmt.api.core;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.test.integration.management.api.ReadConfigAsFeaturesTestBase;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.wildfly.core.testrunner.ManagementClient;
import org.wildfly.core.testrunner.ServerController;
import org.wildfly.core.testrunner.UnsuccessfulOperationException;
import org.wildfly.core.testrunner.WildflyTestRunner;

import javax.inject.Inject;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.*;

/**
 * Tests operation {@code read-config-as-features} in standalone mode.
 *
 * @author <a href="mailto:rjanik@redhat.com">Richard Janík</a>
 */
@RunWith(WildflyTestRunner.class)
public class ReadConfigAsFeaturesStandaloneTestCase extends ReadConfigAsFeaturesTestBase {

    private File defaultConfig;
    private ModelNode defaultConfigAsFeatures;

    @Inject
    private ManagementClient managementClient;

    @Inject
    private static ServerController serverController;

    @Test
    public void writeParameterTest() throws UnsuccessfulOperationException {
        ModelNode writeParameterOpertaion = Util.getWriteAttributeOperation(
                PathAddress.pathAddress(SUBSYSTEM, "request-controller"),
                "max-requests", 10);

        ModelNode expectedConfigAsFeatures = defaultConfigAsFeatures.clone();
        ModelNode requestControllerSubsystem = getFeatureNodeChild(expectedConfigAsFeatures.get(0), "subsystem.request-controller");
        requestControllerSubsystem.get(PARAMS).set(new ModelNode()).get("max-requests").set(10);

        doTest(Collections.singletonList(writeParameterOpertaion), expectedConfigAsFeatures);
    }

    @Test
    public void undefineParameterTest() throws UnsuccessfulOperationException {
        ModelNode undefineParameterOperation = Util.getUndefineAttributeOperation(
                PathAddress.pathAddress(SUBSYSTEM, "security-manager").append("deployment-permissions", "default"),
                "maximum-permissions");

        ModelNode expectedConfigAsFeatures = defaultConfigAsFeatures.clone();
        ModelNode securityManagerSubsystem = getFeatureNodeChild(expectedConfigAsFeatures.get(0), "subsystem.security-manager");
        securityManagerSubsystem.get(CHILDREN).get(0).remove(PARAMS);

        doTest(Collections.singletonList(undefineParameterOperation), expectedConfigAsFeatures);
    }

    @Test
    public void addChildrenTest() throws UnsuccessfulOperationException {
        ModelNode addChildrenOperation = Util.createAddOperation(
                PathAddress.pathAddress(SUBSYSTEM, "jmx").append("configuration", "audit-log"));
        addChildrenOperation.get("enabled").set(true);
        addChildrenOperation.get("log-boot").set(true);
        addChildrenOperation.get("log-read-only").set(false);

        ModelNode expectedConfigAsFeatures = defaultConfigAsFeatures.clone();
        ModelNode jmxSubsystem = getFeatureNodeChild(expectedConfigAsFeatures.get(0), "subsystem.jmx");
        ModelNode auditLog = new ModelNode();
        auditLog.get(SPEC).set("subsystem.jmx.configuration.audit-log");
        auditLog.get(ID).set(new ModelNode()).get("configuration").set("audit-log");
        ModelNode params = new ModelNode();
        params.get("log-read-only").set(false);
        params.get("log-boot").set(true);
        params.get("enabled").set(true);
        auditLog.get(PARAMS).set(params);
        jmxSubsystem.get(CHILDREN).insert(auditLog, 0);

        doTest(Collections.singletonList(addChildrenOperation), expectedConfigAsFeatures);
    }

    @Test
    public void removeChildrenTest() throws UnsuccessfulOperationException {
        ModelNode removeChildrenOperation = Util.createRemoveOperation(
                PathAddress.pathAddress(SUBSYSTEM, "elytron").append(HTTP_AUTHENTICATION_FACTORY, "management-http-authentication"));

        ModelNode expectedConfigAsFeatures = defaultConfigAsFeatures.clone();
        ModelNode elytronSubsystem = getFeatureNodeChild(expectedConfigAsFeatures.get(0), "subsystem.elytron");
        int httpAuthenticationFactoryIndex = getFeatureNodeChildIndex(elytronSubsystem, "subsystem.elytron.http-authentication-factory");
        elytronSubsystem.get(CHILDREN).remove(httpAuthenticationFactoryIndex);

        doTest(Collections.singletonList(removeChildrenOperation), expectedConfigAsFeatures);
    }

    @Test
    public void removeSubsystemTest() throws UnsuccessfulOperationException {
        ModelNode removeSubsystemOperation = Util.createRemoveOperation(PathAddress.pathAddress(SUBSYSTEM, "discovery"));

        ModelNode expectedConfigAsFeatures = defaultConfigAsFeatures.clone();
        int discoverySubsystemIndex = getFeatureNodeChildIndex(expectedConfigAsFeatures.get(0), "subsystem.discovery");
        expectedConfigAsFeatures.get(0).get(CHILDREN).remove(discoverySubsystemIndex);

        doTest(Collections.singletonList(removeSubsystemOperation), expectedConfigAsFeatures);
    }

    @Test
    public void coreManagementTest() throws UnsuccessfulOperationException {
        ModelNode removeSecurityRealm = Util.createRemoveOperation(
                PathAddress.pathAddress(CORE_SERVICE, MANAGEMENT).append(SECURITY_REALM, "ApplicationRealm"));
        ModelNode addCustomSecurityRealm = Util.createAddOperation(
                PathAddress.pathAddress(CORE_SERVICE, MANAGEMENT).append(SECURITY_REALM, "CustomRealm"));
        addCustomSecurityRealm.get("map-groups-to-roles").set(false);
        ModelNode configureCustomSecurityRealm = Util.createAddOperation(
                PathAddress.pathAddress(CORE_SERVICE, MANAGEMENT).append(SECURITY_REALM, "CustomRealm").append("authentication", "local"));
        configureCustomSecurityRealm.get("default-user").set("john");
        configureCustomSecurityRealm.get("allowed-users").set("john");
        configureCustomSecurityRealm.get("skip-group-loading").set(true);

        ModelNode expectedConfigAsFeatures = defaultConfigAsFeatures.clone();
        ModelNode managementCoreService = getFeatureNodeChild(expectedConfigAsFeatures.get(0), "core-service.management");

        // remove ApplicationRealm
        ModelNode applicationSecurityRealmId = new ModelNode();
        applicationSecurityRealmId.get(SECURITY_REALM).set("ApplicationRealm");
        int applicationSecurityRealmIndex = getFeatureNodeChildIndex(managementCoreService, "core-service.management.security-realm", applicationSecurityRealmId);
        managementCoreService.get(CHILDREN).remove(applicationSecurityRealmIndex);

        // create model nodes for the new CustomRealm
        ModelNode customRealmId = new ModelNode();
        customRealmId.get(SECURITY_REALM).set("CustomRealm");
        ModelNode customRealmParams = new ModelNode();
        customRealmParams.get("map-groups-to-roles").set(false);

        // create the authentication model node for the new CustomRealm
        ModelNode customRealmAuthentication = new ModelNode();
        customRealmAuthentication.get(SPEC).set("core-service.management.security-realm.authentication.local");
        ModelNode authenticationId = new ModelNode();
        authenticationId.get(AUTHENTICATION).set(LOCAL);
        customRealmAuthentication.get(ID).set(authenticationId);
        ModelNode authenticationParams = new ModelNode();
        authenticationParams.get("default-user").set("john");
        authenticationParams.get("allowed-users").set("john");
        authenticationParams.get("skip-group-loading").set(true);
        customRealmAuthentication.get(PARAMS).set(authenticationParams);

        // set up the CustomRealm model node
        ModelNode customRealm = new ModelNode();
        customRealm.get(SPEC).set("core-service.management.security-realm");
        customRealm.get(ID).set(customRealmId);
        customRealm.get(PARAMS).set(customRealmParams);
        customRealm.get(CHILDREN).add(customRealmAuthentication);

        // append the CustomRealm model node to the expected model
        managementCoreService.get(CHILDREN).insert(customRealm, 1);

        doTest(Arrays.asList(removeSecurityRealm, addCustomSecurityRealm, configureCustomSecurityRealm), expectedConfigAsFeatures);
    }

    @Test
    public void interfaceTest() throws UnsuccessfulOperationException {
        ModelNode modifyManagementInterface = Util.getWriteAttributeOperation(PathAddress.pathAddress(INTERFACE, "management"), INET_ADDRESS, "10.10.10.10");
        ModelNode createCustomInterface = Util.createAddOperation(PathAddress.pathAddress(INTERFACE, "custom"));
        createCustomInterface.get(ANY_ADDRESS).set(true);

        ModelNode expectedConfigAsFeatures = defaultConfigAsFeatures.clone();

        // modify managemnet interface
        ModelNode managementInterfaceId = new ModelNode();
        managementInterfaceId.get(INTERFACE).set("management");
        ModelNode managementInterface = getFeatureNodeChild(expectedConfigAsFeatures.get(0), INTERFACE, managementInterfaceId);
        managementInterface.get(PARAMS).get(INET_ADDRESS).set("10.10.10.10");

        // add the custom interface
        ModelNode customInterfaceParams = new ModelNode();
        ModelNode customInterfaceId = new ModelNode();
        customInterfaceParams.get(ANY_ADDRESS).set(true);
        customInterfaceId.get(INTERFACE).set("custom");
        ModelNode customInterface = new ModelNode();
        customInterface.get(SPEC).set(INTERFACE);
        customInterface.get(ID).set(customInterfaceId);
        customInterface.get(PARAMS).set(customInterfaceParams);
        int managementInterfaceIndex = getFeatureNodeChildIndex(expectedConfigAsFeatures.get(0), INTERFACE, managementInterfaceId);
        expectedConfigAsFeatures.get(0).get(CHILDREN).insert(customInterface, managementInterfaceIndex - 1); // insert right before management interface, list order matters

        doTest(Arrays.asList(modifyManagementInterface, createCustomInterface), expectedConfigAsFeatures);

        ModelNode removeCustomInterface = Util.createRemoveOperation(PathAddress.pathAddress(INTERFACE, "custom"));
        expectedConfigAsFeatures.get(0).get(CHILDREN).remove(managementInterfaceIndex - 1);

        doTest(Collections.singletonList(removeCustomInterface), expectedConfigAsFeatures);
    }

    @Test
    public void socketBindingGroupTest() throws UnsuccessfulOperationException {
        ModelNode modifyPortOffsetOperation = Util.getWriteAttributeOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, "standard-sockets"), PORT_OFFSET, 100);
        ModelNode addCustomSocketBindingOperation = Util.createAddOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, "standard-sockets").append(SOCKET_BINDING, "custom"));
        addCustomSocketBindingOperation.get(INTERFACE).set("public");
        addCustomSocketBindingOperation.get(MULTICAST_ADDRESS).set("230.0.0.10");
        ModelNode removeCustomSocketBindingOperation = Util.createRemoveOperation(PathAddress.pathAddress(SOCKET_BINDING_GROUP, "standard-sockets").append(SOCKET_BINDING, "custom"));

        ModelNode expectedConfigAsFeatures = defaultConfigAsFeatures.clone();

        // modify the port offset
        ModelNode standardSocketsId = new ModelNode();
        standardSocketsId.get(SOCKET_BINDING_GROUP).set("standard-sockets");
        ModelNode standardSocketBindingGroup = getFeatureNodeChild(expectedConfigAsFeatures.get(0), SOCKET_BINDING_GROUP, standardSocketsId);
        standardSocketBindingGroup.get(PARAMS).get(PORT_OFFSET).set(100);

        // add custom socket-binding
        ModelNode customSocketBinding = new ModelNode();
        ModelNode customSocketBindingId = new ModelNode();
        ModelNode customSocketBindingParams = new ModelNode();
        customSocketBindingId.get(SOCKET_BINDING).set("custom");
        customSocketBindingParams.get(INTERFACE).set("public");
        customSocketBindingParams.get(MULTICAST_ADDRESS).set("230.0.0.10");
        customSocketBinding.get(SPEC).set("socket-binding-group.socket-binding");
        customSocketBinding.get(ID).set(customSocketBindingId);
        customSocketBinding.get(PARAMS).set(customSocketBindingParams);
        standardSocketBindingGroup.get(CHILDREN).insert(customSocketBinding, 0);

        doTest(Arrays.asList(modifyPortOffsetOperation, addCustomSocketBindingOperation), expectedConfigAsFeatures);

        // remove the custom socket binding
        standardSocketBindingGroup.get(CHILDREN).remove(0);

        doTest(Collections.singletonList(removeCustomSocketBindingOperation), expectedConfigAsFeatures);
    }

    private void doTest(List<ModelNode> operations, ModelNode expectedConfigAsFeatures) throws UnsuccessfulOperationException {
        for (ModelNode operation : operations) {
            managementClient.executeForResult(operation);
        }
        if (!expectedConfigAsFeatures.equals(getConfigAsFeatures())) {
            // Assert.assertEquals() barfs the whole models to the console, we don't want that
            System.out.println("Actual:\n" + getConfigAsFeatures().toJSONString(false) + "\nExpected:\n" + expectedConfigAsFeatures.toJSONString(false));
            Assert.fail("There are differences between the expected and the actual model, see the test output for details");
        }
    }

    @Override
    protected void saveDefaultConfig() throws UnsuccessfulOperationException {
        if (defaultConfig == null) {
            ModelNode result = managementClient.executeForResult(
                    Util.createEmptyOperation(TAKE_SNAPSHOT_OPERATION, PathAddress.EMPTY_ADDRESS));
            defaultConfig = Paths.get(result.asString()).toFile();
        }
    }

    @Override
    protected void saveDefaultResult() throws UnsuccessfulOperationException {
        if (defaultConfigAsFeatures == null) {
            defaultConfigAsFeatures = getConfigAsFeatures();
        }
    }

    @Override
    protected void restoreDefaultConfig() {
        serverController.reload(defaultConfig.getName());
    }

    private ModelNode getConfigAsFeatures() throws UnsuccessfulOperationException {
        return managementClient.executeForResult(
                Util.createEmptyOperation(READ_CONFIG_AS_FEATURES_OPERATION, PathAddress.EMPTY_ADDRESS));
    }
}