package com.serotonin.bacnet4j.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDeviceImpl;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.BinaryValueObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.DeviceStatus;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.OctetString;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class PropertyUtilsTest {
    private final TestNetworkMap map = new TestNetworkMap();

    LocalDeviceImpl d1;
    LocalDeviceImpl d2;
    LocalDeviceImpl d3;
    LocalDeviceImpl d4;
    LocalDeviceImpl d5;
    LocalDeviceImpl d6;

    @Before
    public void before() throws Exception {
        // Create the local devices
        d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 20))).initialize();
        d2 = new LocalDeviceImpl(2, new DefaultTransport(new TestNetwork(map, 2, 30))).initialize();
        d3 = new LocalDeviceImpl(3, new DefaultTransport(new TestNetwork(map, 3, 40))).initialize();
        d4 = new LocalDeviceImpl(4, new DefaultTransport(new TestNetwork(map, 4, 50))).initialize();
        d5 = new LocalDeviceImpl(5, new DefaultTransport(new TestNetwork(map, 5, 60))).initialize();
        d6 = new LocalDeviceImpl(6, new DefaultTransport(new TestNetwork(map, 6, 70))).initialize();

        // Set up objects
        addObjects(d2);
        addObjects(d3);
        addObjects(d4);
        addObjects(d5);
        addObjects(d6);
    }

    private static void addObjects(final LocalDeviceImpl d) throws Exception {
        final int id = d.getInstanceNumber();
        d.writePropertyInternal(PropertyIdentifier.objectName, str("d" + id));
        new BinaryValueObject(d, 0, "ai0", BinaryPV.active, false)
                .writePropertyInternal(PropertyIdentifier.inactiveText, str("inactiveText"))
                .writePropertyInternal(PropertyIdentifier.activeText, str("activeText"));
        new BinaryValueObject(d, 1, "ai1", BinaryPV.inactive, false)
                .writePropertyInternal(PropertyIdentifier.inactiveText, str("inactiveText"))
                .writePropertyInternal(PropertyIdentifier.activeText, str("activeText"));
        new BinaryValueObject(d, 2, "ai2", BinaryPV.active, false)
                .writePropertyInternal(PropertyIdentifier.inactiveText, str("inactiveText"))
                .writePropertyInternal(PropertyIdentifier.activeText, str("activeText"));
    }

    private static CharacterString str(final String s) {
        return new CharacterString(s);
    }

    private static ObjectIdentifier oid(final ObjectType objectType, final int instanceNumber) {
        return new ObjectIdentifier(objectType, instanceNumber);
    }

    private static UnsignedInteger uint(final int value) {
        return new UnsignedInteger(value);
    }

    @After
    public void after() {
        // Shut down
        d1.terminate();
        d2.terminate();
        d3.terminate();
        d4.terminate();
        d5.terminate();
        d6.terminate();
    }

    /**
     * The happy path: everything works.
     */
    @Test
    public void happy() {
        final DeviceObjectPropertyValues callbackValues = new DeviceObjectPropertyValues();
        final ReadListener callback = (progress, deviceId, oid, pid, pin, value) -> {
            callbackValues.add(deviceId, oid, pid, pin, value);
            //            System.out.println("progress=" + progress + ", did=" + deviceId + ", oid=" + oid + ", pid=" + pid + ", pin="
            //                    + pin + ", value=" + value);
            return false;
        };

        // The values specified here...
        final DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences() //
                .add(2, ObjectType.device, 2, PropertyIdentifier.objectName) //
                .addIndex(2, ObjectType.device, 2, PropertyIdentifier.objectList, 2) //
                .add(2, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(2, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(2, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(3, ObjectType.device, 3, PropertyIdentifier.objectName) //
                .addIndex(3, ObjectType.device, 3, PropertyIdentifier.objectList, 2) //
                .add(3, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(3, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(3, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(4, ObjectType.device, 4, PropertyIdentifier.objectName) //
                .addIndex(4, ObjectType.device, 4, PropertyIdentifier.objectList, 2) //
                .addIndex(4, ObjectType.device, 4, PropertyIdentifier.objectName, 2) //
                .add(4, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(4, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(4, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText);

        // ... must also be specified here.
        final DeviceObjectPropertyValues expectedValues = new DeviceObjectPropertyValues() //
                .add(2, ObjectType.device, 2, PropertyIdentifier.objectName, null, str("d2")) //
                .add(2, ObjectType.device, 2, PropertyIdentifier.objectList, 2,
                        new ObjectIdentifier(ObjectType.binaryValue, 0)) //
                .add(2, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(2, ObjectType.binaryValue, 0, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(2, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(2, ObjectType.binaryValue, 1, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(2, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(2, ObjectType.binaryValue, 2, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(3, ObjectType.device, 3, PropertyIdentifier.objectName, null, str("d3")) //
                .add(3, ObjectType.device, 3, PropertyIdentifier.objectList, 2,
                        new ObjectIdentifier(ObjectType.binaryValue, 0)) //
                .add(3, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(3, ObjectType.binaryValue, 0, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(3, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(3, ObjectType.binaryValue, 1, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(3, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(3, ObjectType.binaryValue, 2, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(4, ObjectType.device, 4, PropertyIdentifier.objectName, null, str("d4")) //
                .add(4, ObjectType.device, 4, PropertyIdentifier.objectList, 2,
                        new ObjectIdentifier(ObjectType.binaryValue, 0)) //
                .add(4, ObjectType.device, 4, PropertyIdentifier.objectName, 2,
                        new ErrorClassAndCode(ErrorClass.property, ErrorCode.propertyIsNotAnArray)) //
                .add(4, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(4, ObjectType.binaryValue, 0, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(4, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(4, ObjectType.binaryValue, 1, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(4, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(4, ObjectType.binaryValue, 2, PropertyIdentifier.activeText, null, str("activeText"));

        final DeviceObjectPropertyValues actualValues = PropertyUtils.readProperties(d1, refs, callback);

        assertEquals(expectedValues.size(), callbackValues.size());
        assertEquals(expectedValues, callbackValues);

        assertEquals(expectedValues.size(), actualValues.size());
        assertEquals(expectedValues, actualValues);

        // Check cached values.
        assertNull(d1.getCachedRemoteProperty(2, oid(ObjectType.device, 2), PropertyIdentifier.objectList, uint(1)));
        assertNull(d1.getCachedRemoteProperty(2, oid(ObjectType.device, 2), PropertyIdentifier.objectList, uint(2)));
        assertEquals(str("inactiveText"),
                d1.getCachedRemoteProperty(3, oid(ObjectType.binaryValue, 1), PropertyIdentifier.inactiveText));
        assertNull(d1.getCachedRemoteProperty(4, oid(ObjectType.device, 4), PropertyIdentifier.objectName));
        assertEquals(str("activeText"),
                d1.getCachedRemoteProperty(4, oid(ObjectType.binaryValue, 2), PropertyIdentifier.activeText));
        assertNull(d1.getCachedRemoteProperty(5, oid(ObjectType.binaryValue, 2), PropertyIdentifier.activeText));

        assertNotNull(d1.getCachedRemoteDevice(2));
        assertNotNull(d1.getCachedRemoteDevice(3));
        assertNotNull(d1.getCachedRemoteDevice(4));
        assertNull(d1.getCachedRemoteDevice(5));
        assertNull(d1.getCachedRemoteDevice(6));

        //
        // Now, request a few more properties, some that we already have, some that will be new requests.
        callbackValues.clear();

        // The values specified here...
        refs.clear();
        refs //
                .add(4, ObjectType.device, 4, PropertyIdentifier.objectName) //
                .addIndex(4, ObjectType.device, 4, PropertyIdentifier.objectList, 2) //
                .add(4, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(4, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(4, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText)
                .add(5, ObjectType.device, 5, PropertyIdentifier.objectName) //
                .addIndex(5, ObjectType.device, 5, PropertyIdentifier.objectList, 3) //
                .add(5, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(5, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(5, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText)
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName, PropertyIdentifier.objectList) //
                .add(6, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(6, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText) //
                .add(6, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, PropertyIdentifier.activeText);

        // ... must also be specified here.
        expectedValues.clear();
        expectedValues //
                .add(4, ObjectType.device, 4, PropertyIdentifier.objectName, null, str("d4")) //
                .add(4, ObjectType.device, 4, PropertyIdentifier.objectList, 2, //
                        new ObjectIdentifier(ObjectType.binaryValue, 0)) //
                .add(4, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(4, ObjectType.binaryValue, 0, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(4, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(4, ObjectType.binaryValue, 1, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(4, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(4, ObjectType.binaryValue, 2, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(5, ObjectType.device, 5, PropertyIdentifier.objectName, null, str("d5")) //
                .add(5, ObjectType.device, 5, PropertyIdentifier.objectList, 3, //
                        new ObjectIdentifier(ObjectType.binaryValue, 1)) //
                .add(5, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(5, ObjectType.binaryValue, 0, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(5, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(5, ObjectType.binaryValue, 1, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(5, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(5, ObjectType.binaryValue, 2, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName, null, str("d6")) //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectList, null,
                        new SequenceOf<>( //
                                new ObjectIdentifier(ObjectType.device, 6), //
                                new ObjectIdentifier(ObjectType.binaryValue, 0), //
                                new ObjectIdentifier(ObjectType.binaryValue, 1), //
                                new ObjectIdentifier(ObjectType.binaryValue, 2))) //
                .add(6, ObjectType.binaryValue, 0, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(6, ObjectType.binaryValue, 0, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(6, ObjectType.binaryValue, 1, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(6, ObjectType.binaryValue, 1, PropertyIdentifier.activeText, null, str("activeText")) //
                .add(6, ObjectType.binaryValue, 2, PropertyIdentifier.inactiveText, null, str("inactiveText")) //
                .add(6, ObjectType.binaryValue, 2, PropertyIdentifier.activeText, null, str("activeText"));

        final DeviceObjectPropertyValues actualValues2 = PropertyUtils.readProperties(d1, refs, callback);

        assertEquals(expectedValues.size(), callbackValues.size());
        assertEquals(expectedValues, callbackValues);

        assertEquals(expectedValues.size(), actualValues2.size());
        assertEquals(expectedValues, actualValues2);

        // Check cached values.
        assertEquals(oid(ObjectType.device, 6),
                d1.getCachedRemoteProperty(6, oid(ObjectType.device, 6), PropertyIdentifier.objectList, uint(1)));
        assertEquals(str("activeText"),
                d1.getCachedRemoteProperty(5, oid(ObjectType.binaryValue, 2), PropertyIdentifier.activeText));

        assertNotNull(d1.getCachedRemoteDevice(2));
        assertNotNull(d1.getCachedRemoteDevice(3));
        assertNotNull(d1.getCachedRemoteDevice(4));
        assertNotNull(d1.getCachedRemoteDevice(5));
        assertNotNull(d1.getCachedRemoteDevice(6));
    }

    /**
     * Requests a property from a device that doesn't exist.
     */
    @Test
    public void timeout() {
        final DeviceObjectPropertyValues callbackValues = new DeviceObjectPropertyValues();
        final ReadListener callback = (progress, deviceId, oid, pid, pin, value) -> {
            callbackValues.add(deviceId, oid, pid, pin, value);
            //            System.out.println("progress=" + progress + ", did=" + deviceId + ", oid=" + oid + ", pid=" + pid + ", pin="
            //                    + pin + ", value=" + value);
            return false;
        };

        // The values specified here...
        final DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences() //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName) //
                .add(7, ObjectType.device, 7, PropertyIdentifier.objectName);

        // ... must also be specified here.
        final DeviceObjectPropertyValues expectedValues = new DeviceObjectPropertyValues() //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName, null, str("d6")) //
                .add(7, ObjectType.device, 7, PropertyIdentifier.objectName, null,
                        new ErrorClassAndCode(ErrorClass.device, ErrorCode.timeout));

        final DeviceObjectPropertyValues actualValues = PropertyUtils.readProperties(d1, refs, callback, 1200);

        assertEquals(expectedValues.size(), callbackValues.size());
        assertEquals(expectedValues, callbackValues);

        assertEquals(expectedValues.size(), actualValues.size());
        assertEquals(expectedValues, actualValues);
    }

    /**
     * A device changes its address, and sends an IAm which announces the change.
     */
    @Test
    public void addressChangeWithIAm() throws Exception {
        // The values specified here...
        final DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences() //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName);

        // ... must also be specified here.
        final DeviceObjectPropertyValues expectedValues = new DeviceObjectPropertyValues() //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName, null, str("d6"));

        final DeviceObjectPropertyValues actualValues = PropertyUtils.readProperties(d1, refs, null);

        assertEquals(expectedValues, actualValues);
        assertEquals(str("d6"),
                d1.getCachedRemoteProperty(6, oid(ObjectType.device, 6), PropertyIdentifier.objectName));
        assertEquals(new OctetString(new byte[] { 6 }), d1.getCachedRemoteDevice(6).getAddress().getMacAddress());

        // Change the network address of the device.
        d6.terminate();
        d6 = new LocalDeviceImpl(6, new DefaultTransport(new TestNetwork(map, 16, 70))).initialize();
        d6.sendGlobalBroadcast(d6.getIAm());

        // Give time for the IAm to be processed.
        Thread.sleep(300);

        assertEquals(new OctetString(new byte[] { 16 }), d1.getCachedRemoteDevice(6).getAddress().getMacAddress());
    }

    /**
     * A device changes its address, but doesn't send an IAm. Check that we can adapt.
     */
    @Test
    public void addressChange() throws Exception {
        final DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences() //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName);

        final DeviceObjectPropertyValues expectedValues = new DeviceObjectPropertyValues() //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName, null, str("d6"));

        DeviceObjectPropertyValues actualValues = PropertyUtils.readProperties(d1, refs, null, 3000);

        assertEquals(expectedValues, actualValues);
        assertEquals(str("d6"),
                d1.getCachedRemoteProperty(6, oid(ObjectType.device, 6), PropertyIdentifier.objectName));
        assertEquals(new OctetString(new byte[] { 6 }), d1.getCachedRemoteDevice(6).getAddress().getMacAddress());

        // Change the network address of the device.
        d6.terminate();
        d6 = new LocalDeviceImpl(6, new DefaultTransport(new TestNetwork(map, 16, 70))).initialize();
        d6.writePropertyInternal(PropertyIdentifier.objectName, str("d6"));

        // Try getting a different property from the device.
        refs.clear();
        refs //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName)
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectList);

        expectedValues.clear();
        expectedValues //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectName, null, str("d6")) //
                .add(6, ObjectType.device, 6, PropertyIdentifier.objectList, null,
                        new SequenceOf<>(new ObjectIdentifier(ObjectType.device, 6)));

        actualValues = PropertyUtils.readProperties(d1, refs, null, 3000);

        assertEquals(expectedValues, actualValues);
        assertEquals(str("d6"),
                d1.getCachedRemoteProperty(6, oid(ObjectType.device, 6), PropertyIdentifier.objectName));
        assertEquals(new OctetString(new byte[] { 16 }), d1.getCachedRemoteDevice(6).getAddress().getMacAddress());
    }

    @Test
    public void deviceProperties() {
        final DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences() //
                .add(2, ObjectType.device, 2, PropertyIdentifier.systemStatus) //
                .add(2, ObjectType.device, 2, PropertyIdentifier.maxApduLengthAccepted) //
                .add(2, ObjectType.device, 2, PropertyIdentifier.vendorName);

        final DeviceObjectPropertyValues expectedValues = new DeviceObjectPropertyValues() //
                .add(2, ObjectType.device, 2, PropertyIdentifier.systemStatus, null, DeviceStatus.operational) //
                .add(2, ObjectType.device, 2, PropertyIdentifier.maxApduLengthAccepted, null, new UnsignedInteger(1476)) //
                .add(2, ObjectType.device, 2, PropertyIdentifier.vendorName, null,
                        str("Infinite Automation Systems, Inc."));

        final DeviceObjectPropertyValues actualValues = PropertyUtils.readProperties(d1, refs, null, 3000);

        assertEquals(expectedValues, actualValues);
    }

    @Test
    public void unknownObjectInReadMultiple() {
        final DeviceObjectPropertyReferences refs = new DeviceObjectPropertyReferences() //
                .add(2, ObjectType.device, 2, PropertyIdentifier.systemStatus) //
                .add(2, ObjectType.device, 2, PropertyIdentifier.maxApduLengthAccepted) //
                .add(2, ObjectType.analogInput, 2, PropertyIdentifier.vendorName);

        final DeviceObjectPropertyValues expectedValues = new DeviceObjectPropertyValues() //
                .add(2, ObjectType.device, 2, PropertyIdentifier.systemStatus, null, DeviceStatus.operational) //
                .add(2, ObjectType.device, 2, PropertyIdentifier.maxApduLengthAccepted, null, new UnsignedInteger(1476)) //
                .add(2, ObjectType.analogInput, 2, PropertyIdentifier.vendorName, null,
                        new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject));

        final DeviceObjectPropertyValues actualValues = PropertyUtils.readProperties(d1, refs, null, 3000);

        assertEquals(expectedValues, actualValues);
    }
}
