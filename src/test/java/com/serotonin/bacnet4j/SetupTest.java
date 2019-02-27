package com.serotonin.bacnet4j;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;

import lohbihler.warp.WarpClock;

public class SetupTest {
    static final Logger LOG = LoggerFactory.getLogger(SetupTest.class);

    private final int timeout = 200;

    @Test
    public void setup() throws Exception {
        final int count = 20;
        final TestNetworkMap map = new TestNetworkMap();
        final WarpClock clock = new WarpClock();

        final List<LocalDeviceImpl> lds = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            lds.add(new LocalDeviceImpl(i, new DefaultTransport(new TestNetwork(map, i, 0).withTimeout(timeout)))
                    .withClock(clock));
        }

        for (int i = 0; i < count; i++) {
            lds.get(i).initialize();
        }

        for (int i = 0; i < count; i++) {
            final LocalDeviceImpl d = lds.get(i);
            for (int j = 0; j < count; j++) {
                if (i != j) {
                    if ((i + j) % 2 == 0) {
                        d.getRemoteDevice(j).get();
                    } else {
                        d.getRemoteDeviceBlocking(j);
                    }
                }
            }
        }

        for (int i = 0; i < count; i++) {
            lds.get(i).terminate();
        }
    }
}
