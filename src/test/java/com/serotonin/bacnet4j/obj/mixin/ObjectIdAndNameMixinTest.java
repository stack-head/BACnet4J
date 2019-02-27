package com.serotonin.bacnet4j.obj.mixin;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDeviceImpl;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.GroupObject;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;

public class ObjectIdAndNameMixinTest {
    private final TestNetworkMap map = new TestNetworkMap();

    @Test
    public void uniqueDeviceName() throws Exception {
        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        d1.getDeviceObject().writeProperty(null, PropertyIdentifier.objectName,
                new CharacterString("Unique device name"));

        final LocalDeviceImpl d2 = new LocalDeviceImpl(2, new DefaultTransport(new TestNetwork(map, 2, 0))).initialize();
        d2.getRemoteDeviceBlocking(1);

        TestUtils.assertBACnetServiceException(() -> {
            d2.getDeviceObject().writeProperty(null,
                    new PropertyValue(PropertyIdentifier.objectName, new CharacterString("Unique device name")));
        }, ErrorClass.property, ErrorCode.duplicateName);
    }

    @Test
    public void changeOidObjectType() throws Exception {
        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        TestUtils.assertBACnetServiceException(() -> {
            d1.getDeviceObject().writeProperty(null,
                    new PropertyValue(PropertyIdentifier.objectIdentifier, new ObjectIdentifier(ObjectType.group, 0)));
        }, ErrorClass.property, ErrorCode.invalidValueInThisState);
    }

    @Test
    public void changeObjectType() throws Exception {
        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        TestUtils.assertBACnetServiceException(() -> {
            d1.getDeviceObject().writeProperty(null,
                    new PropertyValue(PropertyIdentifier.objectType, ObjectType.group));
        }, ErrorClass.property, ErrorCode.writeAccessDenied);
    }

    @Test
    public void changeInstanceNumber() throws Exception {
        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final GroupObject go = new GroupObject(d1, 0, "go", new SequenceOf<>());
        go.writeProperty(null,
                new PropertyValue(PropertyIdentifier.objectIdentifier, new ObjectIdentifier(ObjectType.group, 1)));
    }

    @Test
    public void uniqueInstanceNumber() throws Exception {
        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final GroupObject go0 = new GroupObject(d1, 0, "go0", new SequenceOf<>());
        final GroupObject go1 = new GroupObject(d1, 1, "go1", new SequenceOf<>());
        TestUtils.assertBACnetServiceException(() -> {
            go0.writeProperty(null, new PropertyValue(PropertyIdentifier.objectIdentifier, go1.getId()));
        }, ErrorClass.property, ErrorCode.duplicateObjectId);
    }

    @Test
    public void changeName() throws Exception {
        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final GroupObject go0 = new GroupObject(d1, 0, "go0", new SequenceOf<>());
        new GroupObject(d1, 1, "go1", new SequenceOf<>());
        go0.writeProperty(null, new PropertyValue(PropertyIdentifier.objectName, new CharacterString("that")));
    }

    @Test
    public void uniqueName() throws Exception {
        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final GroupObject go0 = new GroupObject(d1, 0, "go0", new SequenceOf<>());
        new GroupObject(d1, 1, "go1", new SequenceOf<>());
        TestUtils.assertBACnetServiceException(() -> {
            go0.writeProperty(null, new PropertyValue(PropertyIdentifier.objectName, new CharacterString("go1")));
        }, ErrorClass.property, ErrorCode.duplicateName);
    }
}
