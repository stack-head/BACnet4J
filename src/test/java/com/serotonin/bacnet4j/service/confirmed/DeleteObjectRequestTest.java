package com.serotonin.bacnet4j.service.confirmed;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDeviceImpl;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class DeleteObjectRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final Address addr = TestNetworkUtils.toAddress(2);
    private LocalDeviceImpl localDevice;

    @Before
    public void before() throws Exception {
        localDevice = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
    }

    @After
    public void after() {
        localDevice.terminate();
    }

    @Test // 15.4.1.3.1
    public void errorTypes() {
        // Ask for an object that doesn't exist.
        TestUtils.assertRequestHandleException( //
                () -> new DeleteObjectRequest(new ObjectIdentifier(ObjectType.accessDoor, 0)).handle(localDevice, addr),
                ErrorClass.object, ErrorCode.unknownObject);
    }

    @Test // 15.4.1.3.1
    public void moreErrorTypes() throws BACnetServiceException {
        // Ask for an object that isn't deletable
        final BACnetObject bo = new BACnetObject(localDevice, ObjectType.accessDoor, 0);
        localDevice.addObject(bo);

        TestUtils.assertRequestHandleException( //
                () -> new DeleteObjectRequest(bo.getId()).handle(localDevice, addr), ErrorClass.object,
                ErrorCode.objectDeletionNotPermitted);
    }

    @Test
    public void delete() throws Exception {
        // Ask for an object that isn't deletable
        final BACnetObject bo = new BACnetObject(localDevice, ObjectType.accessDoor, 0);
        localDevice.addObject(bo);
        bo.setDeletable(true);

        new DeleteObjectRequest(bo.getId()).handle(localDevice, addr);
    }
}
