package com.serotonin.bacnet4j.util;

import static com.serotonin.bacnet4j.TestUtils.assertListEqualsIgnoreOrder;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;

import org.junit.Assert;
import org.junit.Test;

import com.serotonin.bacnet4j.LocalDeviceImpl;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.TestUtils;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;

public class RemoteDeviceDiscovererTest {
    private final TestNetworkMap map = new TestNetworkMap();

    @Test
    public void noCallback() throws Exception {
        final BiPredicate<Integer, RemoteDevice> predicate = (i, d) -> d.getInstanceNumber() == i;

        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 1))).initialize();
        final LocalDeviceImpl d2 = new LocalDeviceImpl(12, new DefaultTransport(new TestNetwork(map, 112, 1))).initialize();
        final LocalDeviceImpl d3 = new LocalDeviceImpl(13, new DefaultTransport(new TestNetwork(map, 113, 1))).initialize();
        final LocalDeviceImpl d4 = new LocalDeviceImpl(14, new DefaultTransport(new TestNetwork(map, 114, 1))).initialize();
        final LocalDeviceImpl d5 = new LocalDeviceImpl(15, new DefaultTransport(new TestNetwork(map, 115, 1))).initialize();
        final LocalDeviceImpl d6 = new LocalDeviceImpl(16, new DefaultTransport(new TestNetwork(map, 116, 1))).initialize();
        final LocalDeviceImpl d7 = new LocalDeviceImpl(17, new DefaultTransport(new TestNetwork(map, 117, 1))).initialize();

        final RemoteDeviceDiscoverer discoverer = new RemoteDeviceDiscoverer(d1);
        discoverer.start();
        Thread.sleep(300);

        assertListEqualsIgnoreOrder(TestUtils.toList(12, 13, 14, 15, 16, 17), discoverer.getRemoteDevices(), predicate);
        assertListEqualsIgnoreOrder(TestUtils.toList(12, 13, 14, 15, 16, 17), discoverer.getLatestRemoteDevices(),
                predicate);

        //
        // Add some more devices
        final LocalDeviceImpl d8 = new LocalDeviceImpl(18, new DefaultTransport(new TestNetwork(map, 118, 1))).initialize();
        d8.sendGlobalBroadcast(d8.getIAm());
        Thread.sleep(300);

        assertListEqualsIgnoreOrder(TestUtils.toList(12, 13, 14, 15, 16, 17, 18), discoverer.getRemoteDevices(),
                predicate);
        assertListEqualsIgnoreOrder(TestUtils.toList(18), discoverer.getLatestRemoteDevices(), predicate);

        //
        // Add some more devices
        d2.sendGlobalBroadcast(d2.getIAm());
        d3.sendGlobalBroadcast(d3.getIAm());
        final LocalDeviceImpl d9 = new LocalDeviceImpl(19, new DefaultTransport(new TestNetwork(map, 119, 1))).initialize();
        d9.sendGlobalBroadcast(d9.getIAm());
        final LocalDeviceImpl d10 = new LocalDeviceImpl(20, new DefaultTransport(new TestNetwork(map, 120, 1))).initialize();
        d10.sendGlobalBroadcast(d10.getIAm());
        Thread.sleep(300);

        assertListEqualsIgnoreOrder(TestUtils.toList(12, 13, 14, 15, 16, 17, 18, 19, 20), discoverer.getRemoteDevices(),
                predicate);
        assertListEqualsIgnoreOrder(TestUtils.toList(19, 20), discoverer.getLatestRemoteDevices(), predicate);

        // Stop and add more devices to make sure they are not discovered.
        discoverer.stop();
        final LocalDeviceImpl d11 = new LocalDeviceImpl(21, new DefaultTransport(new TestNetwork(map, 121, 1))).initialize();
        d11.sendGlobalBroadcast(d11.getIAm());
        final LocalDeviceImpl d12 = new LocalDeviceImpl(22, new DefaultTransport(new TestNetwork(map, 122, 1))).initialize();
        d12.sendGlobalBroadcast(d12.getIAm());
        Thread.sleep(300);

        assertListEqualsIgnoreOrder(TestUtils.toList(12, 13, 14, 15, 16, 17, 18, 19, 20), discoverer.getRemoteDevices(),
                predicate);
        assertListEqualsIgnoreOrder(new ArrayList<Integer>(), discoverer.getLatestRemoteDevices(), predicate);

        // Cleanup
        d1.terminate();
        d2.terminate();
        d3.terminate();
        d4.terminate();
        d5.terminate();
        d6.terminate();
        d7.terminate();
        d8.terminate();
        d9.terminate();
        d10.terminate();
        d11.terminate();
        d12.terminate();
    }

    @Test
    public void withCallback() throws Exception {
        final BiPredicate<RemoteDevice, Integer> predicate = (d, i) -> d.getInstanceNumber() == i;

        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 1))).initialize();
        final LocalDeviceImpl d2 = new LocalDeviceImpl(12, new DefaultTransport(new TestNetwork(map, 112, 1))).initialize();
        final LocalDeviceImpl d3 = new LocalDeviceImpl(13, new DefaultTransport(new TestNetwork(map, 113, 1))).initialize();
        final LocalDeviceImpl d4 = new LocalDeviceImpl(14, new DefaultTransport(new TestNetwork(map, 114, 1))).initialize();
        final LocalDeviceImpl d5 = new LocalDeviceImpl(15, new DefaultTransport(new TestNetwork(map, 115, 1))).initialize();
        final LocalDeviceImpl d6 = new LocalDeviceImpl(16, new DefaultTransport(new TestNetwork(map, 116, 1))).initialize();
        final LocalDeviceImpl d7 = new LocalDeviceImpl(17, new DefaultTransport(new TestNetwork(map, 117, 1))).initialize();

        final List<Integer> expected = TestUtils.toList(12, 13, 14, 15, 16, 17);
        final RemoteDeviceDiscoverer discoverer = new RemoteDeviceDiscoverer(d1, (d) -> {
            final int index = TestUtils.indexOf(expected, d, predicate);
            if (index == -1)
                Assert.fail("RemoteDevice " + d.getInstanceNumber() + " not found in expected list");
            expected.remove(index);
        });

        discoverer.start();
        Thread.sleep(300);

        //
        // Add some more devices
        expected.add(18);
        final LocalDeviceImpl d8 = new LocalDeviceImpl(18, new DefaultTransport(new TestNetwork(map, 118, 1))).initialize();
        d8.sendGlobalBroadcast(d8.getIAm());
        Thread.sleep(300);

        //
        // Send some duplicate IAms
        d2.sendGlobalBroadcast(d2.getIAm());
        d3.sendGlobalBroadcast(d3.getIAm());

        //
        // Add some more devices
        expected.add(19);
        expected.add(20);
        final LocalDeviceImpl d9 = new LocalDeviceImpl(19, new DefaultTransport(new TestNetwork(map, 119, 1))).initialize();
        d9.sendGlobalBroadcast(d9.getIAm());
        final LocalDeviceImpl d10 = new LocalDeviceImpl(20, new DefaultTransport(new TestNetwork(map, 120, 1))).initialize();
        d10.sendGlobalBroadcast(d10.getIAm());
        Thread.sleep(300);

        // Stop and add more devices to make sure they are not discovered.
        discoverer.stop();
        final LocalDeviceImpl d11 = new LocalDeviceImpl(21, new DefaultTransport(new TestNetwork(map, 121, 1))).initialize();
        d11.sendGlobalBroadcast(d11.getIAm());
        final LocalDeviceImpl d12 = new LocalDeviceImpl(22, new DefaultTransport(new TestNetwork(map, 122, 1))).initialize();
        d12.sendGlobalBroadcast(d12.getIAm());
        Thread.sleep(300);

        // Cleanup
        d1.terminate();
        d2.terminate();
        d3.terminate();
        d4.terminate();
        d5.terminate();
        d6.terminate();
        d7.terminate();
        d8.terminate();
        d9.terminate();
        d10.terminate();
        d11.terminate();
        d12.terminate();
    }
}
