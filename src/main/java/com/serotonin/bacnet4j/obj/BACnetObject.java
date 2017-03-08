/*
 * ============================================================================
 * GNU General Public License
 * ============================================================================
 *
 * Copyright (C) 2015 Infinite Automation Software. All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * When signing a commercial license with Infinite Automation Software,
 * the following extension to GPL is made. A special exception to the GPL is
 * included to allow you to distribute a combined work that includes BAcnet4J
 * without being obliged to provide the source code for any proprietary components.
 *
 * See www.infiniteautomation.com for commercial license options.
 *
 * @author Matthew Lohbihler
 */
package com.serotonin.bacnet4j.obj;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.serotonin.bacnet4j.LocalDevice;
import com.serotonin.bacnet4j.exception.BACnetRuntimeException;
import com.serotonin.bacnet4j.exception.BACnetServiceException;
import com.serotonin.bacnet4j.obj.mixin.CommandableMixin;
import com.serotonin.bacnet4j.obj.mixin.CovReportingMixin;
import com.serotonin.bacnet4j.obj.mixin.HasStatusFlagsMixin;
import com.serotonin.bacnet4j.obj.mixin.PropertyListMixin;
import com.serotonin.bacnet4j.obj.mixin.intrinsicReporting.IntrinsicReportingMixin;
import com.serotonin.bacnet4j.service.acknowledgement.GetAlarmSummaryAck.AlarmSummary;
import com.serotonin.bacnet4j.service.acknowledgement.GetEnrollmentSummaryAck.EnrollmentSummary;
import com.serotonin.bacnet4j.service.acknowledgement.GetEventInformationAck.EventSummary;
import com.serotonin.bacnet4j.service.confirmed.GetEnrollmentSummaryRequest.AcknowledgmentFilter;
import com.serotonin.bacnet4j.service.confirmed.GetEnrollmentSummaryRequest.EventStateFilter;
import com.serotonin.bacnet4j.service.confirmed.GetEnrollmentSummaryRequest.PriorityFilter;
import com.serotonin.bacnet4j.type.Encodable;
import com.serotonin.bacnet4j.type.constructed.Address;
import com.serotonin.bacnet4j.type.constructed.PropertyReference;
import com.serotonin.bacnet4j.type.constructed.PropertyValue;
import com.serotonin.bacnet4j.type.constructed.RecipientProcess;
import com.serotonin.bacnet4j.type.constructed.SequenceOf;
import com.serotonin.bacnet4j.type.constructed.TimeStamp;
import com.serotonin.bacnet4j.type.constructed.ValueSource;
import com.serotonin.bacnet4j.type.enumerated.ErrorClass;
import com.serotonin.bacnet4j.type.enumerated.ErrorCode;
import com.serotonin.bacnet4j.type.enumerated.EventState;
import com.serotonin.bacnet4j.type.enumerated.EventType;
import com.serotonin.bacnet4j.type.enumerated.ObjectType;
import com.serotonin.bacnet4j.type.enumerated.PropertyIdentifier;
import com.serotonin.bacnet4j.type.primitive.Boolean;
import com.serotonin.bacnet4j.type.primitive.CharacterString;
import com.serotonin.bacnet4j.type.primitive.Date;
import com.serotonin.bacnet4j.type.primitive.Null;
import com.serotonin.bacnet4j.type.primitive.ObjectIdentifier;
import com.serotonin.bacnet4j.type.primitive.Real;
import com.serotonin.bacnet4j.type.primitive.Time;
import com.serotonin.bacnet4j.type.primitive.UnsignedInteger;

/**
 * @author Matthew
 */
public class BACnetObject {
    private final LocalDevice localDevice;
    private final ObjectType objectType;
    protected final Map<PropertyIdentifier, Encodable> properties = new ConcurrentHashMap<>();
    private final List<BACnetObjectListener> listeners = new CopyOnWriteArrayList<>();

    // Mixins
    private final List<AbstractMixin> mixins = new ArrayList<>();
    private CommandableMixin commandableMixin;
    private HasStatusFlagsMixin hasStatusFlagsMixin;
    private final PropertyListMixin propertyListMixin;
    private IntrinsicReportingMixin intrinsicReportingMixin;
    private CovReportingMixin changeOfValueMixin;

    public BACnetObject(final LocalDevice localDevice, final ObjectType type, final int instanceNumber)
            throws BACnetServiceException {
        this(localDevice, type, instanceNumber, null);
    }

    public BACnetObject(final LocalDevice localDevice, final ObjectType type, final int instanceNumber,
            final String name) throws BACnetServiceException {
        this(localDevice, new ObjectIdentifier(type, instanceNumber), name);
    }

    public BACnetObject(final LocalDevice localDevice, final ObjectIdentifier id) throws BACnetServiceException {
        this(localDevice, id, null);
    }

    public BACnetObject(final LocalDevice localDevice, final ObjectIdentifier id, final String name)
            throws BACnetServiceException {
        if (id == null)
            throw new IllegalArgumentException("object id cannot be null");

        this.localDevice = localDevice;
        objectType = id.getObjectType();

        properties.put(PropertyIdentifier.objectIdentifier, id);
        properties.put(PropertyIdentifier.objectName, new CharacterString(name == null ? id.toString() : name));
        properties.put(PropertyIdentifier.objectType, objectType);

        // All objects have a property list.
        propertyListMixin = new PropertyListMixin(this);
        addMixin(propertyListMixin);
        propertyListMixin.update();

        if (!id.getObjectType().equals(ObjectType.device))
            // The device object will add itself to the local device after it initializes.
            localDevice.addObject(this);
    }

    //
    //
    // Convenience methods
    //
    public ObjectIdentifier getId() {
        return get(PropertyIdentifier.objectIdentifier);
    }

    public int getInstanceId() {
        return getId().getInstanceNumber();
    }

    public String getObjectName() {
        final CharacterString name = get(PropertyIdentifier.objectName);
        if (name == null)
            return null;
        return name.getValue();
    }

    LocalDevice getLocalDevice() {
        return localDevice;
    }

    //
    //
    // Object notifications
    //
    /**
     * Called when the object is removed from the device.
     */
    public void removedFromDevice() {
        // no op, override as required
    }

    //
    //
    // Listeners
    //
    public void addListener(final BACnetObjectListener l) {
        listeners.add(l);
    }

    public void removeListener(final BACnetObjectListener l) {
        listeners.remove(l);
    }

    //
    //
    // Mixins
    //
    protected final void addMixin(final AbstractMixin mixin) {
        mixins.add(mixin);

        if (mixin instanceof HasStatusFlagsMixin)
            hasStatusFlagsMixin = (HasStatusFlagsMixin) mixin;
        else if (mixin instanceof CommandableMixin)
            commandableMixin = (CommandableMixin) mixin;
        else if (mixin instanceof IntrinsicReportingMixin)
            intrinsicReportingMixin = (IntrinsicReportingMixin) mixin;
        else if (mixin instanceof CovReportingMixin)
            changeOfValueMixin = (CovReportingMixin) mixin;
    }

    public void setOverridden(final boolean b) {
        if (hasStatusFlagsMixin != null)
            hasStatusFlagsMixin.setOverridden(b);
        if (commandableMixin != null)
            commandableMixin.setOverridden(b);
    }

    public boolean isOverridden() {
        if (hasStatusFlagsMixin != null)
            return hasStatusFlagsMixin.isOverridden();
        if (commandableMixin != null)
            return commandableMixin.isOverridden();
        return false;
    }

    //
    // Commandable
    protected void _supportCommandable(final Encodable relinquishDefault) {
        if (commandableMixin != null)
            commandableMixin.supportCommandable(relinquishDefault);
    }

    public boolean supportsCommandable() {
        if (commandableMixin != null)
            return commandableMixin.supportsCommandable();
        return false;
    }

    protected void _supportValueSource() {
        if (commandableMixin != null)
            commandableMixin.supportValueSource();
    }

    public boolean supportsValueSource() {
        if (commandableMixin != null)
            return commandableMixin.supportsValueSource();
        return false;
    }

    //
    // Intrinsic reporting
    public void acknowledgeAlarm(final UnsignedInteger acknowledgingProcessIdentifier,
            final EventState eventStateAcknowledged, final TimeStamp timeStamp,
            final CharacterString acknowledgmentSource, final TimeStamp timeOfAcknowledgment)
            throws BACnetServiceException {
        if (intrinsicReportingMixin == null)
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.noAlarmConfigured);
        intrinsicReportingMixin.acknowledgeAlarm(acknowledgingProcessIdentifier, eventStateAcknowledged, timeStamp,
                acknowledgmentSource, timeOfAcknowledgment);
    }

    //
    // COVs
    protected void _supportCovReporting(final Real covIncrement) {
        addMixin(new CovReportingMixin(this, covIncrement));
    }

    public AlarmSummary getAlarmSummary() {
        if (intrinsicReportingMixin != null)
            return intrinsicReportingMixin.getAlarmSummary();
        return null;
    }

    public EventSummary getEventSummary() {
        if (intrinsicReportingMixin != null)
            return intrinsicReportingMixin.getEventSummary();
        return null;
    }

    public EnrollmentSummary getEnrollmentSummary(final AcknowledgmentFilter acknowledgmentFilter,
            final RecipientProcess enrollmentFilter, final EventStateFilter eventStateFilter,
            final EventType eventTypeFilter, final PriorityFilter priorityFilter,
            final UnsignedInteger notificationClassFilter) {
        if (intrinsicReportingMixin != null)
            return intrinsicReportingMixin.getEnrollmentSummary(acknowledgmentFilter, enrollmentFilter,
                    eventStateFilter, eventTypeFilter, priorityFilter, notificationClassFilter);
        return null;
    }

    //
    // COV
    public void addCovSubscription(final Address from, final UnsignedInteger subscriberProcessIdentifier,
            final Boolean issueConfirmedNotifications, final UnsignedInteger lifetime,
            final PropertyReference monitoredPropertyIdentifier, final Real covIncrement)
            throws BACnetServiceException {
        if (changeOfValueMixin == null)
            throw new BACnetServiceException(ErrorClass.object, ErrorCode.optionalFunctionalityNotSupported);
        changeOfValueMixin.addCovSubscription(from, subscriberProcessIdentifier, issueConfirmedNotifications, lifetime,
                monitoredPropertyIdentifier, covIncrement);
    }

    public void removeCovSubscription(final Address from, final UnsignedInteger subscriberProcessIdentifier,
            final PropertyReference monitoredPropertyIdentifier) {
        if (changeOfValueMixin != null)
            changeOfValueMixin.removeCovSubscription(from, subscriberProcessIdentifier, monitoredPropertyIdentifier);
    }

    //
    //
    // Get property
    //
    @SuppressWarnings("unchecked")
    public final <T extends Encodable> T getProperty(final PropertyIdentifier pid) throws BACnetServiceException {
        // Check that the requested property is valid for the object. This will throw an exception if the
        // property doesn't belong.
        ObjectProperties.getPropertyTypeDefinitionRequired(objectType, pid);

        // Do some property-specific checking here.
        if (PropertyIdentifier.localTime.equals(pid))
            return (T) new Time();
        if (PropertyIdentifier.localDate.equals(pid))
            return (T) new Date();

        // Give the mixins notice that the property is being read.
        for (final AbstractMixin mixin : mixins)
            mixin.beforeReadProperty(pid);

        return (T) get(pid);
    }

    /**
     * This method should only be used internally. Services should use the getProperty method.
     */
    @SuppressWarnings("unchecked")
    public <T extends Encodable> T get(final PropertyIdentifier pid) {
        return (T) properties.get(pid);
    }

    public final Encodable getPropertyRequired(final PropertyIdentifier pid) throws BACnetServiceException {
        final Encodable p = getProperty(pid);
        if (p == null)
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty);
        return p;
    }

    public final Encodable getProperty(final PropertyIdentifier pid, final UnsignedInteger propertyArrayIndex)
            throws BACnetServiceException {
        final Encodable result = getProperty(pid);
        if (propertyArrayIndex == null)
            return result;

        if (!(result instanceof SequenceOf<?>))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.propertyIsNotAnArray);

        final SequenceOf<?> array = (SequenceOf<?>) result;
        final int index = propertyArrayIndex.intValue();
        if (index == 0)
            return new UnsignedInteger(array.getCount());

        if (index > array.getCount())
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidArrayIndex);

        return array.get(index);
    }

    public final Encodable getPropertyRequired(final PropertyIdentifier pid, final UnsignedInteger propertyArrayIndex)
            throws BACnetServiceException {
        final Encodable p = getProperty(pid, propertyArrayIndex);
        if (p == null)
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.unknownProperty);
        return p;
    }

    //
    //
    // Set property
    //
    public BACnetObject writeProperty(final ValueSource valueSource, final PropertyIdentifier pid,
            final Encodable value) {
        try {
            writeProperty(valueSource, new PropertyValue(pid, value));
        } catch (final BACnetServiceException e) {
            throw new BACnetRuntimeException(e);
        }
        return this;
    }

    public BACnetObject writeProperty(final ValueSource valueSource, final PropertyIdentifier pid, final int indexBase1,
            final Encodable value) {
        try {
            writeProperty(valueSource, new PropertyValue(pid, new UnsignedInteger(indexBase1), value, null));
        } catch (final BACnetServiceException e) {
            throw new BACnetRuntimeException(e);
        }
        return this;
    }

    /**
     * Entry point for writing a property via services. Provides validation and writing using mixins.
     *
     * @param value
     * @throws BACnetServiceException
     */
    public void writeProperty(final ValueSource valueSource, final PropertyValue value) throws BACnetServiceException {
        final PropertyIdentifier pid = value.getPropertyIdentifier();

        if (PropertyIdentifier.objectIdentifier.equals(pid))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        if (PropertyIdentifier.objectType.equals(pid))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);
        if (PropertyIdentifier.priorityArray.equals(pid))
            throw new BACnetServiceException(ErrorClass.property, ErrorCode.writeAccessDenied);

        // Validation - run through the mixins
        boolean handled = false;
        for (final AbstractMixin mixin : mixins) {
            handled = mixin.validateProperty(valueSource, value);
            if (handled)
                break;
        }
        if (!handled) {
            // Default behaviour is to validate against the object property definitions.
            final PropertyTypeDefinition def = ObjectProperties.getPropertyTypeDefinitionRequired(objectType,
                    value.getPropertyIdentifier());
            if (value.getPropertyArrayIndex() == null) {
                // Expecting to write to a non-list property.
                //if (value.getValue() instanceof Null && !def.isOptional())
                //    throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType,
                //            "Null provided, but the value is not optional");

                if (def.isSequenceOf()) {
                    // Replacing an entire array. Validate each element of the given array.
                    @SuppressWarnings("unchecked")
                    final SequenceOf<Encodable> seq = (SequenceOf<Encodable>) value.getValue();
                    for (final Encodable e : seq) {
                        if (e == null || !def.getClazz().isAssignableFrom(e.getClass()))
                            throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType,
                                    "expected " + def.getClazz() + ", received=" + (e == null ? "null" : e.getClass()));
                    }
                } else if (!def.getClazz().isAssignableFrom(value.getValue().getClass()))
                    // Validate the given data type.
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType,
                            "expected " + def.getClazz() + ", received=" + value.getValue().getClass());
            } else {
                // Expecting to write to an array element.
                if (!def.isSequenceOf())
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.propertyIsNotAnArray);
                if (!def.getClazz().isAssignableFrom(value.getValue().getClass()))
                    throw new BACnetServiceException(ErrorClass.property, ErrorCode.invalidDataType);
            }
        }

        // Writing
        handled = false;
        for (final AbstractMixin mixin : mixins) {
            handled = mixin.writeProperty(valueSource, value);
            if (handled)
                break;
        }
        if (!handled) {
            // Default is to just set the property.
            if (value.getPropertyArrayIndex() != null) {
                // Set the value in a list or array.
                final int indexBase1 = value.getPropertyArrayIndex().intValue();
                @SuppressWarnings("unchecked")
                SequenceOf<Encodable> list = (SequenceOf<Encodable>) properties.get(pid);

                if (value.getValue() instanceof Null) {
                    if (list != null) {
                        //Encodable oldValue = list.get(indexBase1);
                        list.remove(indexBase1);
                        //fireSubscriptions(pid, oldValue, null);
                    }
                } else {
                    if (list == null)
                        list = new SequenceOf<>();
                    list.set(indexBase1, value.getValue());
                    writePropertyInternal(pid, list);
                }
            } else
                // Set the value of a property
                writePropertyInternal(pid, value.getValue());
        }
    }

    /**
     * Entry point for changing a property circumventing mixin support. Used primarily for object configuration and
     * property writes from mixins themselves, but can also be used by client code to set object properties. Calls mixin
     * "after write" methods and fires COV subscriptions.
     *
     * @param pid
     * @param value
     * @return
     */
    public BACnetObject writePropertyInternal(final PropertyIdentifier pid, final Encodable value) {
        final Encodable oldValue = properties.get(pid);
        properties.put(pid, value);

        // After writing.
        for (final AbstractMixin mixin : mixins)
            mixin.afterWriteProperty(pid, oldValue, value);

        if (!Objects.equals(value, oldValue)) {
            // Notify listeners
            for (final BACnetObjectListener l : listeners)
                l.propertyChange(pid, oldValue, value);
        }

        // Special handling to update the property list
        if (oldValue == null && !PropertyIdentifier.propertyList.equals(pid))
            propertyListMixin.update();

        return this;
    }

    //
    //
    // Other
    //

    @Override
    public int hashCode() {
        final ObjectIdentifier id = getId();
        final int PRIME = 31;
        int result = 1;
        result = PRIME * result + (id == null ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(final Object obj) {
        final ObjectIdentifier id = getId();
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final BACnetObject other = (BACnetObject) obj;
        if (id == null) {
            if (other.getId() != null)
                return false;
        } else if (!id.equals(other.getId()))
            return false;
        return true;
    }
}
