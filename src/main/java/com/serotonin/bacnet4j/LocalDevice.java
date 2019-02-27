package com.serotonin.bacnet4j;

import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.serotonin.bacnet4j.cache.CachePolicies;
import com.serotonin.bacnet4j.event.DeviceEventHandler;
import com.serotonin.bacnet4j.event.ExceptionDispatcher;
import com.serotonin.bacnet4j.event.PrivateTransferHandler;
import com.serotonin.bacnet4j.event.ReinitializeDeviceHandler;
import com.serotonin.bacnet4j.exception.BACnetException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.npdu.Network;
import com.serotonin.bacnet4j.obj.BACnetObject;
import com.serotonin.bacnet4j.obj.DeviceObject;
import com.serotonin.bacnet4j.obj.mixin.CovContext;
import com.serotonin.bacnet4j.persistence.IPersistence;
import com.serotonin.bacnet4j.service.confirmed.ConfirmedRequestService;
import com.serotonin.bacnet4j.service.confirmed.DeviceCommunicationControlRequest.EnableDisable;
import com.serotonin.bacnet4j.service.unconfirmed.IAmRequest;
import com.serotonin.bacnet4j.service.unconfirmed.UnconfirmedRequestService;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.ServicesSupported;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.enumerated.RestartReason;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;
import com.serotonin.bacnet4j.util.RemoteDeviceFinder.RemoteDeviceFuture;

public interface LocalDevice {

    public Clock getClock();

    public DeviceObject getDeviceObject();

    public Network getNetwork();

    public CachePolicies getCachePolicies();

    public Map<ObjectIdentifier, List<CovContext>> getCovContexts();

    public ObjectIdentifier getId();
    
    public int getInstanceNumber();

    public <T extends Encodable> T get(final PropertyIdentifier pid);

    public DeviceEventHandler getEventHandler();

    public ExceptionDispatcher getExceptionDispatcher();

    public int getNextProcessId();

    public PrivateTransferHandler getPrivateTransferHandler(final UnsignedInteger vendorId,
            final UnsignedInteger serviceNumber);

    public ReinitializeDeviceHandler getReinitializeDeviceHandler();

    public LocalDeviceImpl initialize() throws Exception;

    public LocalDeviceImpl initialize(final RestartReason lastRestartReason) throws Exception;

    public void terminate();

    public boolean isInitialized();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Executors
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Schedules the given command for later execution.
     */
    public ScheduledFuture<?> schedule(final Runnable command, final long period, final TimeUnit unit);

    /**
     * Schedules the given command for later execution.
     */
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period,
            final TimeUnit unit);

    /**
     * Schedules the given command for later execution.
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay,
            final TimeUnit unit);

    /**
     * Submits the given task for immediate execution.
     */
    public Future<?> submit(final Runnable task);

    /**
     * Submits the given task for immediate execution.
     */
    public void execute(final Runnable task);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Device configuration.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public String getPassword();

    /**
     * Returns the currently configured timeout in ms within the transport.
     */
    public int getTransportTimeout();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Local object management
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public BACnetObject getObjectRequired(final ObjectIdentifier id) throws BACnetServiceException;

    public List<BACnetObject> getLocalObjects();

    public BACnetObject getObject(final ObjectIdentifier id);

    public BACnetObject getObject(final String name);

    public void addObject(final BACnetObject obj) throws BACnetServiceException;

    public int getNextInstanceObjectNumber(final ObjectType objectType);

    public BACnetObject removeObject(final ObjectIdentifier id) throws BACnetServiceException;
    
    public ServicesSupported getServicesSupported();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Remote device management
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Returns the cached remote device, or null if not found.
     *
     * @param instanceNumber
     * @return the remote device or null if not found.
     */
    public RemoteDevice getCachedRemoteDevice(final int instanceNumber);

    public RemoteDevice getCachedRemoteDevice(final Address address);

    public RemoteDevice removeCachedRemoteDevice(final int instanceNumber);

    /**
     * Returns a future to get the remote device for the given instanceNumber. If a cached instance is found the future
     * will be set immediately. Otherwise, a finder will be used to try to find it. If this is successful the device
     * will be cached.
     *
     * The benefits of this method are:
     * 1) It will cache the remote device if it is found.
     * 2) It returns a cancelable future.
     *
     * If multiple threads are likely to request a remote device reference around the same time, it may be better to
     * use the blocking method below.
     *
     * @param instanceNumber
     * @return the remote device future
     */
    public RemoteDeviceFuture getRemoteDevice(final int instanceNumber);

    /**
     * Returns the remote device for the given instanceNumber using the default timeout. If a cached instance is not
     * found the finder will be used to try and find it. A timeout exception is thrown if it can't be found.
     *
     * @param instanceNumber
     * @return the remote device
     * @throws BACnetException
     *             if anything goes wrong, including timeout.
     */
    public RemoteDevice getRemoteDeviceBlocking(final int instanceNumber) throws BACnetException;

    /**
     * Returns the remote device for the given instanceNumber. If a cached instance is not found the finder will be used
     * to try and find it. A timeout exception is thrown if it can't be found.
     *
     * The benefits of this method are:
     * 1) It will cache the remote device if it is found.
     * 2) Multiple threads that request the same remote device around the same time will be joined on the same request
     *
     * If you require the ability to cancel a request, use the non-blocking method above.
     *
     * @param instanceNumber
     * @return the remote device
     * @throws BACnetException
     *             if anything goes wrong, including timeout.
     */
    public RemoteDevice getRemoteDeviceBlocking(final int instanceNumber, final long timeoutMillis) throws BACnetException;

    /**
     * Updates the remote device with the given number with the given address, but only if the
     * remote device is cached. Otherwise, nothing happens.
     *
     * @param instanceNumber
     * @param address
     * @return
     */
    public void updateRemoteDevice(final int instanceNumber, final Address address);

    /**
     * Clears the cache of remote devices.
     */
    public void clearRemoteDevices();

    public List<RemoteDevice> getRemoteDevices();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Cached property management
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    //
    // Get properties

    public <T extends Encodable> T getCachedRemoteProperty(final int did, final ObjectIdentifier oid, final PropertyIdentifier pid);

    public <T extends Encodable> T getCachedRemoteProperty(final int did, final ObjectIdentifier oid, final PropertyIdentifier pid, final UnsignedInteger pin);

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Message sending
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    public ServiceFuture send(final RemoteDevice d, final ConfirmedRequestService serviceRequest);

    public ServiceFuture send(final Address address, final ConfirmedRequestService serviceRequest);

    public void send(final RemoteDevice d, final ConfirmedRequestService serviceRequest, final ResponseConsumer consumer);

    public void send(final Address address, final ConfirmedRequestService serviceRequest, final ResponseConsumer consumer);

    public void send(final RemoteDevice d, final UnconfirmedRequestService serviceRequest);

    public void send(final Address address, final UnconfirmedRequestService serviceRequest);

    public void sendLocalBroadcast(final UnconfirmedRequestService serviceRequest);

    public void sendGlobalBroadcast(final UnconfirmedRequestService serviceRequest);

    public void setCommunicationControl(final EnableDisable enableDisable, final int minutes);

    public EnableDisable getCommunicationControlState();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Persistence
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public IPersistence getPersistence();

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Convenience methods
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////

    public Address[] getAllLocalAddresses();

    public Address getLoopbackAddress();

    public IAmRequest getIAm();

    public void incrementDatabaseRevision();

    /**
     * Notify the callback that we have the same Device id like an other device.
     *
     * @param from
     */
    public void notifySameDeviceIdCallback(Address from);
    
}
