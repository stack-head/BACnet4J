package com.serotonin.bacnet4j.service.confirmed;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDeviceImpl;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.npdu.test.TestNetworkUtils;
import com.serotonin.bacnet4j.obj.AnalogInputObject;
import com.serotonin.bacnet4j.obj.LifeSafetyPointObject;
import com.serotonin.bacnet4j.obj.LifeSafetyZoneObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyMode;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyOperation;
import com.serotonin.bacnet4j.type.enumerated.LifeSafetyState;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.SilencedState;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class LifeSafetyOperationRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final Address addr = TestNetworkUtils.toAddress(2);
    private final LocalDeviceImpl localDevice = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0)));

    private AnalogInputObject ai;
    private LifeSafetyPointObject lsp;
    private LifeSafetyZoneObject lsz;

    @Before
    public void before() throws Exception {
        localDevice.initialize();

        ai = new AnalogInputObject(localDevice, 0, "ai", 5, EngineeringUnits.noUnits, false);
        lsp = new LifeSafetyPointObject(localDevice, 0, "lsp", LifeSafetyState.alarm, LifeSafetyMode.on, false,
                new SequenceOf<>(), LifeSafetyOperation.none, SilencedState.unsilenced);
        lsz = new LifeSafetyZoneObject(localDevice, 0, "lzp", LifeSafetyState.alarm, LifeSafetyMode.on, false,
                new SequenceOf<>(), LifeSafetyOperation.none, SilencedState.unsilenced, new SequenceOf<>());
    }

    @After
    public void after() {
        localDevice.terminate();
    }

    @Test
    public void errorTypes() {
        // Object does not exist.
        TestUtils.assertRequestHandleException(() -> new LifeSafetyOperationRequest(new UnsignedInteger(12),
                new CharacterString("test"), LifeSafetyOperation.none, new ObjectIdentifier(ObjectType.accessDoor, 0))
                        .handle(localDevice, addr),
                ErrorClass.object, ErrorCode.unknownObject);

        // Bad object type
        TestUtils.assertRequestHandleException(
                () -> new LifeSafetyOperationRequest(new UnsignedInteger(12), new CharacterString("test"),
                        LifeSafetyOperation.none, ai.getId()).handle(localDevice, addr),
                ErrorClass.object, ErrorCode.unsupportedObjectType);
    }

    @Test
    public void specificObject() throws Exception {
        new LifeSafetyOperationRequest(new UnsignedInteger(12), new CharacterString("test"),
                LifeSafetyOperation.silence, lsp.getId()).handle(localDevice, addr);

        assertEquals(SilencedState.allSilenced, lsp.get(PropertyIdentifier.silenced));
    }

    @Test
    public void unspecificObject() throws Exception {
        new LifeSafetyOperationRequest(new UnsignedInteger(12), new CharacterString("test"),
                LifeSafetyOperation.silenceVisual, null).handle(localDevice, addr);

        assertEquals(SilencedState.visibleSilenced, lsp.get(PropertyIdentifier.silenced));
        assertEquals(SilencedState.visibleSilenced, lsz.get(PropertyIdentifier.silenced));
    }
}
