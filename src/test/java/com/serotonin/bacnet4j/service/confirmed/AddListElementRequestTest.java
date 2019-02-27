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
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.AddressBinding;
import com.serotonin.bacnet4j.type.constructed.BACnetArray;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.VtClass;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class AddListElementRequestTest {
    private final TestNetworkMap map = new TestNetworkMap();
    private final Address addr = TestNetworkUtils.toAddress(2);
    private LocalDeviceImpl localDevice;

    @Before
    public void before() throws Exception {
        localDevice = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0)));
        localDevice.getDeviceObject().writePropertyInternal(PropertyIdentifier.vtClassesSupported, new SequenceOf<>());
        // Create an array of lists.
        localDevice.writePropertyInternal(PropertyIdentifier.forId(5555),
                new BACnetArray<>( //
                        new SequenceOf<>(new Real(0), new Real(1), new Real(2)), //
                        new SequenceOf<>(new Real(3), new Real(4)), //
                        new SequenceOf<>(new Real(5), new Real(6), new Real(7), new Real(8)), //
                        new SequenceOf<>(), //
                        new Real(9)));
        localDevice.initialize();
    }

    @After
    public void after() {
        localDevice.terminate();
    }

    @Test // 15.1.1.3.1
    public void errorTypes() {
        // Ask for an object that doesn't exist.
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.accessDoor, 0), //
                        PropertyIdentifier.absenteeLimit, //
                        null, //
                        new SequenceOf<>() //
                ).handle(localDevice, addr), ErrorClass.object, ErrorCode.unknownObject);

        // Ask for a property that isn't in the object.
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.absenteeLimit, //
                        null, //
                        new SequenceOf<>() //
                ).handle(localDevice, addr), ErrorClass.property, ErrorCode.unknownProperty);

        // Ask for a property that isn't in the object.
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.forId(5556), //
                        null, //
                        new SequenceOf<>() //
                ).handle(localDevice, addr), ErrorClass.property, ErrorCode.unknownProperty);

        // Specify a pin but for a property that isn't an array.
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.vtClassesSupported, //
                        new UnsignedInteger(1), //
                        new SequenceOf<>(new ObjectIdentifier(ObjectType.device, 2)) //
                ).handle(localDevice, addr), ErrorClass.property, ErrorCode.propertyIsNotAnArray);

        // Specify a bad pin for an array property.
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.forId(5555), //
                        UnsignedInteger.ZERO, //
                        new SequenceOf<>(new ObjectIdentifier(ObjectType.device, 2)) //
                ).handle(localDevice, addr), ErrorClass.property, ErrorCode.invalidArrayIndex);

        // Specify a bad pin for an array property.
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.forId(5555), //
                        new UnsignedInteger(6), //
                        new SequenceOf<>(new ObjectIdentifier(ObjectType.device, 2)) //
                ).handle(localDevice, addr), ErrorClass.property, ErrorCode.invalidArrayIndex);

        // Specify a pin for an array property, where the element is not a list..
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.forId(5555), //
                        new UnsignedInteger(5), //
                        new SequenceOf<>(new ObjectIdentifier(ObjectType.device, 2)) //
                ).handle(localDevice, addr), ErrorClass.services, ErrorCode.propertyIsNotAList);

        // Specify a property that is an array, not a list.
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.forId(5555), //
                        null, //
                        new SequenceOf<>() //
                ).handle(localDevice, addr), ErrorClass.services, ErrorCode.propertyIsNotAList);

        // Specify a property that is not a list.
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.objectName, //
                        null, //
                        new SequenceOf<>() //
                ).handle(localDevice, addr), ErrorClass.services, ErrorCode.propertyIsNotAList);

        // Provide an element to add that is not right for the property.
        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.vtClassesSupported, //
                        null, //
                        new SequenceOf<>(new ObjectIdentifier(ObjectType.device, 2)) //
                ).handle(localDevice, addr), ErrorClass.property, ErrorCode.invalidDataType);

        TestUtils.assertRequestHandleException( //
                () -> new AddListElementRequest( //
                        new ObjectIdentifier(ObjectType.device, 1), //
                        PropertyIdentifier.forId(5555), //
                        new UnsignedInteger(4), //
                        new SequenceOf<>(new Real(0), CharacterString.EMPTY) //
                ).handle(localDevice, addr), ErrorClass.property, ErrorCode.invalidDataType);
    }

    @Test
    public void list() throws Exception {
        // Add a few elements.
        new AddListElementRequest( //
                new ObjectIdentifier(ObjectType.device, 1), //
                PropertyIdentifier.vtClassesSupported, //
                null, //
                new SequenceOf<>(VtClass.defaultTerminal, VtClass.ansi_x3_64, VtClass.dec_vt52)) //
                        .handle(localDevice, addr);

        SequenceOf<AddressBinding> vcss = localDevice.get(PropertyIdentifier.vtClassesSupported);
        assertEquals(new SequenceOf<>(VtClass.defaultTerminal, VtClass.ansi_x3_64, VtClass.dec_vt52), vcss);

        // Add a few more, including a couple one that are already there.
        new AddListElementRequest( //
                new ObjectIdentifier(ObjectType.device, 1), //
                PropertyIdentifier.vtClassesSupported, //
                null, //
                new SequenceOf<>(VtClass.defaultTerminal, VtClass.ansi_x3_64, VtClass.dec_vt100, VtClass.dec_vt220))
                        .handle(localDevice, addr);

        vcss = localDevice.get(PropertyIdentifier.vtClassesSupported);
        assertEquals(new SequenceOf<>(VtClass.defaultTerminal, VtClass.ansi_x3_64, VtClass.dec_vt52, VtClass.dec_vt100,
                VtClass.dec_vt220), vcss);

        //        public static final VtClass defaultTerminal = new VtClass(0);
        //        public static final VtClass ansi_x3_64 = new VtClass(1);
        //        public static final VtClass dec_vt52 = new VtClass(2);
        //        public static final VtClass dec_vt100 = new VtClass(3);
        //        public static final VtClass dec_vt220 = new VtClass(4);
        //        public static final VtClass hp_700_94 = new VtClass(5);
        //        public static final VtClass ibm_3130 = new VtClass(6);

    }

    @Test
    public void arrayOfList() throws Exception {
        // Replace all of the elements
        new AddListElementRequest( //
                new ObjectIdentifier(ObjectType.device, 1), //
                PropertyIdentifier.forId(5555), //
                new UnsignedInteger(3), //
                new SequenceOf<>(new Real(10), new Real(11)) //
        ).handle(localDevice, addr);

        SequenceOf<?> aol = localDevice.get(PropertyIdentifier.forId(5555));
        assertEquals(new BACnetArray<>( //
                new SequenceOf<>(new Real(0), new Real(1), new Real(2)), //
                new SequenceOf<>(new Real(3), new Real(4)), //
                new SequenceOf<>(new Real(5), new Real(6), new Real(7), new Real(8), new Real(10), new Real(11)), //
                new SequenceOf<>(), //
                new Real(9)), aol);

        // Only replace the second element
        new AddListElementRequest( //
                new ObjectIdentifier(ObjectType.device, 1), //
                PropertyIdentifier.forId(5555), //
                new UnsignedInteger(4), //
                new SequenceOf<>(new CharacterString("a"), new CharacterString("b")) //
        ).handle(localDevice, addr);

        aol = localDevice.get(PropertyIdentifier.forId(5555));
        assertEquals(new BACnetArray<>( //
                new SequenceOf<>(new Real(0), new Real(1), new Real(2)), //
                new SequenceOf<>(new Real(3), new Real(4)), //
                new SequenceOf<>(new Real(5), new Real(6), new Real(7), new Real(8), new Real(10), new Real(11)), //
                new SequenceOf<>(new CharacterString("a"), new CharacterString("b")), //
                new Real(9)), aol);
    }
}
