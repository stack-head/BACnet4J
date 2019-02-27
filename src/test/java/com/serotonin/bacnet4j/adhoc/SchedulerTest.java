package com.serotonin.bacnet4j.adhoc;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.serotonin.bacnet4j.LocalDeviceImpl;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.npdu.ip.IpNetwork;
import com.serotonin.bacnet4j.npdu.ip.IpNetworkBuilder;
import com.serotonin.bacnet4j.service.acknowledgement.ReadPropertyMultipleAck;
import com.serotonin.bacnet4j.service.confirmed.ReadPropertyMultipleRequest;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.constructed.ReadAccessResult;
import com.serotonin.bacnet4j.type.constructed.ReadAccessSpecification;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.util.RequestUtils;

public class SchedulerTest {
    static LocalDeviceImpl localDevice;
    static RemoteDevice d;

    public static void main(final String[] args) throws Exception {
        final IpNetwork network = new IpNetworkBuilder().withBroadcast("192.168.255.255", 16).build();
        final DefaultTransport transport = new DefaultTransport(network);
        localDevice = new LocalDeviceImpl(123, transport);
        //        localDevice.initialize();

        try {
            d = localDevice.getRemoteDeviceBlocking(3214302);
            find(10L, TimeUnit.SECONDS, true);
        } finally {
            localDevice.terminate();
        }
    }

    static void find(final Long period, final TimeUnit unit, final Boolean isTest) {
        try {
            final SequenceOf<ObjectIdentifier> objList = RequestUtils.getObjectList(localDevice, d);
            //            final List<ObjectIdentifier> logOids = getLogOids(objList);
            final List<ObjectIdentifier> logOids = objList.getValues();
            final TimerTask task = new TimerTask() {
                @Override
                public void run() {
                    for (final ObjectIdentifier i : logOids) {
                        try {
                            final List<ReadAccessSpecification> specs = new ArrayList<>();
                            specs.add(new ReadAccessSpecification(i, PropertyIdentifier.presentValue));
                            specs.add(new ReadAccessSpecification(i, PropertyIdentifier.units));
                            specs.add(new ReadAccessSpecification(i, PropertyIdentifier.objectName));
                            specs.add(new ReadAccessSpecification(i, PropertyIdentifier.description));
                            specs.add(new ReadAccessSpecification(i, PropertyIdentifier.objectType));
                            final ReadPropertyMultipleRequest multipleRequest = new ReadPropertyMultipleRequest(
                                    new SequenceOf<>(specs));
                            localDevice.send(d, multipleRequest);
                            final ReadPropertyMultipleAck send = localDevice.send(d, multipleRequest).get();
                            final SequenceOf<ReadAccessResult> readAccessResults = send.getListOfReadAccessResults();
                            System.out.print(i.getInstanceNumber() + " " + i.getObjectType() + ", ");
                            for (final ReadAccessResult result : readAccessResults) {
                                for (final ReadAccessResult.Result r : result.getListOfResults()) {
                                    System.out.print(r.getReadResult() + ", ");
                                }
                            }
                        } catch (final Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            };
            final ScheduledExecutorService scheduledPool = Executors.newSingleThreadScheduledExecutor();
            scheduledPool.scheduleWithFixedDelay(task, 0, period, unit);
        } catch (final Exception e) {
            e.printStackTrace();
        }
    }
}
