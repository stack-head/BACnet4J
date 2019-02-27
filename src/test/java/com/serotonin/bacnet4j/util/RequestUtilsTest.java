package com.serotonin.bacnet4j.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.serotonin.bacnet4j.LocalDeviceImpl;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.ServiceFuture;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.apdu.Abort;
import com.serotonin.bacnet4j.cache.CachePolicies;
import com.serotonin.bacnet4j.exception.AbortAPDUException;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.ServiceTooBigException;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.obj.AnalogInputObject;
import com.serotonin.bacnet4j.service.acknowledgement.AcknowledgementService;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyMultipleRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.ObjectPropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult.Result;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.AbortReason;
import com.serotonin.bacnet4j.type.enumerated.BinaryPV;
import com.serotonin.bacnet4j.type.enumerated.EngineeringUnits;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;
import com.serotonin.bacnet4j.type.error.ErrorClassAndCode;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

public class RequestUtilsTest {
    private final TestNetworkMap map = new TestNetworkMap();

    /**
     * Test that occasionally throws ServiceTooBig exception so that RequestUtils is forced to repartition the request
     * on the fly.
     *
     * @throws BACnetException
     */
    @Test
    public void repartition() throws BACnetException {
        final ServicesSupported servicesSupported = new ServicesSupported();
        servicesSupported.setAll(true);

        final CachePolicies policies = new CachePolicies();

        final LocalDeviceImpl d = Mockito.mock(LocalDeviceImpl.class);
        when(d.getCachePolicies()).thenReturn(policies);

        final RemoteDevice rd = new RemoteDevice(d, 123);
        rd.setDeviceProperty(PropertyIdentifier.protocolServicesSupported, servicesSupported);
        rd.setDeviceProperty(PropertyIdentifier.segmentationSupported, Segmentation.segmentedBoth);
        rd.setMaxReadMultipleReferences(200);

        final AtomicInteger exceptionCount = new AtomicInteger();
        when(d.send(any(RemoteDevice.class), any(ReadPropertyMultipleRequest.class)))
                .thenAnswer(new Answer<ServiceFuture>() {
                    private int threshold = 50;

                    @Override
                    public ServiceFuture answer(final InvocationOnMock invocation) throws Throwable {
                        final ReadPropertyMultipleRequest req = (ReadPropertyMultipleRequest) invocation.getArgument(1);
                        if (req.getNumberOfProperties() < threshold) {
                            final SequenceOf<ReadAccessResult> accessResults = new SequenceOf<>();
                            for (final ReadAccessSpecification spec : req.getListOfReadAccessSpecs()) {
                                final SequenceOf<Result> results = new SequenceOf<>();
                                for (final PropertyReference ref : spec.getListOfPropertyReferences()) {
                                    results.add(new Result(ref.getPropertyIdentifier(), ref.getPropertyArrayIndex(),
                                            BinaryPV.active));
                                }
                                accessResults.add(new ReadAccessResult(spec.getObjectIdentifier(), results));
                            }

                            if (threshold == 100)
                                threshold = 50;
                            else
                                threshold--;

                            final ReadPropertyMultipleAck ack = new ReadPropertyMultipleAck(accessResults);
                            return new ServiceFuture() {
                                @SuppressWarnings("unchecked")
                                @Override
                                public <T extends AcknowledgementService> T get() throws BACnetException {
                                    return (T) ack;
                                }
                            };
                        }

                        exceptionCount.incrementAndGet();

                        final int mod = exceptionCount.get() % 3;
                        if (mod == 1)
                            throw new ServiceTooBigException("much too big");
                        else if (mod == 2)
                            throw new AbortAPDUException(
                                    new Abort(false, (byte) 0, AbortReason.bufferOverflow.intValue()));
                        throw new AbortAPDUException(
                                new Abort(false, (byte) 0, AbortReason.segmentationNotSupported.intValue()));
                    }
                });

        // Add quite a lot of references to the request.
        final PropertyReferences refs = new PropertyReferences();
        final ObjectIdentifier oid = new ObjectIdentifier(ObjectType.analogInput, 0);
        for (int i = 0; i < 1000; i++) {
            refs.add(oid, PropertyIdentifier.forId(i));
        }

        final PropertyValues pvs = RequestUtils.readProperties(d, rd, refs, null);
        assertEquals(1000, pvs.size());
        assertEquals(9, exceptionCount.get());

        for (int i = 0; i < 1000; i++) {
            assertEquals(BinaryPV.active, pvs.getNoErrorCheck(oid, PropertyIdentifier.forId(i)));
        }
    }

    /**
     * Send a request from d1 to d2 for three properties. One exists, the other doesn't, and the third is for an
     * object that doesn't exist, so the RPM will return an error. RequestUtils should then send the requests one
     * at a time to get the proper results.
     *
     * @throws Exception
     */
    @Test
    public void sendOneAtATimeOnError() throws Exception {
        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final LocalDeviceImpl d2 = new LocalDeviceImpl(2, new DefaultTransport(new TestNetwork(map, 2, 0))).initialize();
        final AnalogInputObject ai = new AnalogInputObject(d2, 0, "ai", 0, EngineeringUnits.noUnits, false);
        final RemoteDevice rd2 = d1.getRemoteDeviceBlocking(2);

        final List<Map<String, Object>> listenerUpdates = new ArrayList<>();
        final ReadListener listener = new ReadListener() {
            @Override
            public boolean progress(final double progress, final int deviceId, final ObjectIdentifier oid,
                    final PropertyIdentifier pid, final UnsignedInteger pin, final Encodable value) {
                final Map<String, Object> updates = new HashMap<>();
                updates.put("progress", progress);
                updates.put("deviceId", deviceId);
                updates.put("oid", oid);
                updates.put("pid", pid);
                updates.put("pin", pin);
                updates.put("value", value);
                listenerUpdates.add(updates);
                return false;
            }
        };

        final List<Pair<ObjectPropertyReference, Encodable>> results = RequestUtils.readProperties(d1, rd2,
                TestUtils.toList( //
                        new ObjectPropertyReference(ai.getId(), PropertyIdentifier.presentValue),
                        new ObjectPropertyReference(ai.getId(), PropertyIdentifier.logDeviceObjectProperty),
                        new ObjectPropertyReference(new ObjectIdentifier(ObjectType.analogOutput, 0),
                                PropertyIdentifier.presentValue)),
                listener);

        // Verify listener updates.
        assertEquals(3, listenerUpdates.size());

        assertEquals(0.33, (Double) listenerUpdates.get(0).get("progress"), 0.01);
        assertEquals(2, listenerUpdates.get(0).get("deviceId"));
        assertEquals(ai.getId(), listenerUpdates.get(0).get("oid"));
        assertEquals(PropertyIdentifier.presentValue, listenerUpdates.get(0).get("pid"));
        assertEquals(null, listenerUpdates.get(0).get("pin"));
        assertEquals(new Real(0), listenerUpdates.get(0).get("value"));

        assertEquals(0.66, (Double) listenerUpdates.get(1).get("progress"), 0.01);
        assertEquals(2, listenerUpdates.get(1).get("deviceId"));
        assertEquals(ai.getId(), listenerUpdates.get(1).get("oid"));
        assertEquals(PropertyIdentifier.logDeviceObjectProperty, listenerUpdates.get(1).get("pid"));
        assertEquals(null, listenerUpdates.get(1).get("pin"));
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty),
                listenerUpdates.get(1).get("value"));

        assertEquals(1, (Double) listenerUpdates.get(2).get("progress"), 0.01);
        assertEquals(2, listenerUpdates.get(2).get("deviceId"));
        assertEquals(new ObjectIdentifier(ObjectType.analogOutput, 0), listenerUpdates.get(2).get("oid"));
        assertEquals(PropertyIdentifier.presentValue, listenerUpdates.get(2).get("pid"));
        assertEquals(null, listenerUpdates.get(2).get("pin"));
        assertEquals(new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject),
                listenerUpdates.get(2).get("value"));

        // Verify the results.
        assertEquals(3, results.size());

        assertEquals(ai.getId(), results.get(0).getLeft().getObjectIdentifier());
        assertEquals(PropertyIdentifier.presentValue, results.get(0).getLeft().getPropertyIdentifier());
        assertEquals(new Real(0), results.get(0).getRight());

        assertEquals(ai.getId(), results.get(1).getLeft().getObjectIdentifier());
        assertEquals(PropertyIdentifier.logDeviceObjectProperty, results.get(1).getLeft().getPropertyIdentifier());
        assertEquals(new ErrorClassAndCode(ErrorClass.property, ErrorCode.unknownProperty), results.get(1).getRight());

        assertEquals(new ObjectIdentifier(ObjectType.analogOutput, 0), results.get(2).getLeft().getObjectIdentifier());
        assertEquals(PropertyIdentifier.presentValue, results.get(2).getLeft().getPropertyIdentifier());
        assertEquals(new ErrorClassAndCode(ErrorClass.object, ErrorCode.unknownObject), results.get(2).getRight());
    }
}
