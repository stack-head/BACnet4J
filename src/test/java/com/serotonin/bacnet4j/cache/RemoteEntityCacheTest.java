package com.serotonin.bacnet4j.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import com.serotonin.bacnet4j.LocalDeviceImpl;
import com.serotonin.bacnet4j.npdu.test.TestNetwork;
import com.serotonin.bacnet4j.npdu.test.TestNetworkMap;
import com.serotonin.bacnet4j.transport.DefaultTransport;

import lohbihler.warp.WarpClock;

public class RemoteEntityCacheTest {
    private final TestNetworkMap map = new TestNetworkMap();

    @Test
    public void test() {
        final WarpClock clock = new WarpClock();
        final LocalDeviceImpl d = new LocalDeviceImpl(0, new DefaultTransport(new TestNetwork(map, 1, 10))).withClock(clock);

        final RemoteEntityCache<String, String> cache = new RemoteEntityCache<>(d);

        cache.putEntity("key1", "value1", RemoteEntityCachePolicy.NEVER_CACHE);
        cache.putEntity("key2", "value2", RemoteEntityCachePolicy.EXPIRE_5_SECONDS);
        cache.putEntity("key3", "value3", RemoteEntityCachePolicy.NEVER_EXPIRE);

        assertNull(cache.getCachedEntity("key1"));
        assertEquals("value2", cache.getCachedEntity("key2"));
        assertEquals("value3", cache.getCachedEntity("key3"));

        // Advance the clock 10 seconds
        clock.plusSeconds(10);

        assertNull(cache.getCachedEntity("key1"));
        assertNull(cache.getCachedEntity("key2"));
        assertEquals("value3", cache.getCachedEntity("key3"));

        // Advance the clock 1 year
        clock.plusYears(1);

        assertNull(cache.getCachedEntity("key1"));
        assertNull(cache.getCachedEntity("key2"));
        assertEquals("value3", cache.getCachedEntity("key3"));
    }
}
