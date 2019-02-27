package com.serotonin.bacnet4j.service.unconfirmed;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDeviceImpl;
import com.serotonin.bacnet4j.RemoteDevice;
import com.serotonin.bacnet4j.RemoteObject;
import com.serotonin.bacnet4j.event.DeviceEventAdapter;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.Segmentation;

public class AutoDiscoveryTest {
    /**
     * Test to ensure that when an IHave is received from an unknown device that a properly configured RemoteDevice
     * object gets cached.
     *
     * @throws Exception
     */
    @Test
    public void iHaveToWhoIs() throws Exception {
        final TestNetworkMap map = new TestNetworkMap();
        final LocalDeviceImpl d1 = new LocalDeviceImpl(1, new DefaultTransport(new TestNetwork(map, 1, 0))).initialize();
        final LocalDeviceImpl d2 = new LocalDeviceImpl(2, new DefaultTransport(new TestNetwork(map, 2, 0))).initialize();

        d1.getEventHandler().addListener(new DeviceEventAdapter() {
            @Override
            public void iHaveReceived(final RemoteDevice d, final RemoteObject o) {
                assertEquals(Segmentation.segmentedBoth, d.getDeviceProperty(PropertyIdentifier.segmentationSupported));
            }
        });

        // Send an IHave from d2
        d2.sendGlobalBroadcast(new IHaveRequest(d2.getId(), d2.getId(), d2.get(PropertyIdentifier.objectName)));

        // Wait while d1 receives the IHave, sends a WhoIs to d2, and then receives an IAm from d2 and creates
        // a remote device from the content.
        Thread.sleep(300);

        // Now check that d1 has the correct information on d2.
        final RemoteDevice rd2 = d1.getCachedRemoteDevice(2);
        assertNotNull(rd2);
        // Check a property that is not in the IHave, but is in the IAm
        assertEquals(Segmentation.segmentedBoth, rd2.getDeviceProperty(PropertyIdentifier.segmentationSupported));
    }
}
