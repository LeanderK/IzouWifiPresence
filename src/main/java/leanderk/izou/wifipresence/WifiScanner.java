package leanderk.izou.wifipresence;

import org.intellimate.izou.sdk.Context;
import org.intellimate.izou.sdk.frameworks.presence.provider.PresenceIndicatorLevel;
import org.intellimate.izou.sdk.frameworks.presence.provider.template.PresenceConstant;

import java.net.InetAddress;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

/**
 * The WifiScanner holds the DiscoverServices, which discover devices, and tracks them.
 * It pings every tracked device every second to check whether it is still there and verifies the hostname every 30
 * seconds to check whether the Address still belongs to the interested HostName.
 * @author LeanderK
 * @version 1.0
 */
public class WifiScanner extends PresenceConstant {
    public static final String ID = WifiScanner.class.getCanonicalName();
    private static final String PROPERTIES_ID = "hostname_";
    private List<DiscoverService> discoverServiceList = Collections.synchronizedList(new ArrayList<>());
    private Set<TrackingObject> trackingObjects = new HashSet<>();
    private ReentrantReadWriteLock lock =  new ReentrantReadWriteLock();
    private Queue<TrackingObject> trackingObjectsToAdd = new ArrayDeque<>();
    private List<String> interestedHostNames = Collections.synchronizedList(new ArrayList<>());
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private ScheduledFuture<?> reachabilityFuture;
    @SuppressWarnings({"FieldCanBeLocal", "unused"})
    private ScheduledFuture<?> checkHostsFuture;

    public WifiScanner(Context context) {
        super(context, ID, false, PresenceIndicatorLevel.WEAK);
        getContext().getPropertiesAssistant().getProperties().stringPropertyNames().stream()
                .filter(key -> key.matches(PROPERTIES_ID + "\\d+"))
                .map(getContext().getPropertiesAssistant()::getProperty)
                .forEach(interestedHostNames::add);
        JmDNSDiscoverService jmDNSDiscoverService = new JmDNSDiscoverService(this, getContext());
        discoverServiceList.add(jmDNSDiscoverService);
        scanWifi();
    }

    /**
     * starts scanning the Wifi
     */
    public void scanWifi() {
        reachabilityFuture =
                scheduledExecutorService.scheduleAtFixedRate((Runnable) this::checkReachability, 0, 1, TimeUnit.SECONDS);
        checkHostsFuture =
                scheduledExecutorService.scheduleAtFixedRate((Runnable) this::checkHostAndDoMaintenance, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * call this class when you have discorvered a new Device to track.
     * <p>
     * from now on the InetAddress will be tracked. It will check if it has not already been tracked.
     * </p>
     * @param trackingObject the new InetAddress.
     */
    public void newInetAddressDiscovered(TrackingObject trackingObject) {
        //debug("New request to track " + trackingObject.toString());
        trackingObjectsToAdd.add(trackingObject);
    }

    /**
     * adds a TrackingObject to TrackingObjects
     * @param trackingObject the new TrackingObject
     */
    public void addToTrackingObjects(TrackingObject trackingObject) {
        lock.readLock().lock();
        try {
            if (!trackingObjects.add(trackingObject)) {
                //debug("tracking " + trackingObject.toString());
                debug("now present");
                setPresence(true);
            } else {
                //debug("already tracking " + trackingObject.toString());
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    /**
     * returns a List of interested HostNames
     * @return a List
     */
    public List<String> getInterestedHostNames() {
        return interestedHostNames;
    }

    /**
     * pings every tracking-Object
     */
    public void checkReachability() {
        trackingObjectsToAdd.forEach(this::addToTrackingObjects);
        trackingObjectsToAdd.clear();
        try {
            lock.readLock().lock();
            LocalDateTime now = LocalDateTime.now();
            try {
                trackingObjects.stream()
                        .filter(TrackingObject::isReachable)
                        .forEach(trackingObject -> trackingObject.setLastReached(now));
            } finally {
                lock.readLock().unlock();
            }
        } catch (Exception e) {
            error("An Error occured", e);
        }
    }

    /**
     * checks if the InetAddress we are pinging are still belonging to the Host we are interested in.
     */
    public void checkHostAndDoMaintenance() {
        try {
            LocalDateTime now = LocalDateTime.now();
            List<TrackingObject> toRemove = trackingObjects.stream()
                    .filter(trackingObject -> trackingObject.hostChanged() || trackingObject.isOverLimit(now))
                    .collect(Collectors.toList());
            if (!toRemove.isEmpty()) {
                lock.writeLock().lock();
                try {
                    trackingObjects.removeAll(toRemove);
                } finally {
                    lock.writeLock().unlock();
                }
                toRemove.forEach(TrackingObject::runRemovedCallback);
                lostPresence();
            }
        } catch (Exception e) {
            error("An error occured", e);
        }
    }

    /**
     * fires an event if no trackingobjects are left
     */
    public void lostPresence() {
        if (trackingObjects.isEmpty()) {
            debug("declaring unreachable");
            setPresence(false);
            discoverServiceList.forEach(Resettable::reset);
        }
    }

    /**
     * checks whether we are already tracking the InetAddress
     * @param inetAddress the InetAddress to check
     * @return true if already tracking, false if not
     */
    public boolean isAlreadyTracking(InetAddress inetAddress) {
        lock.readLock().lock();
        try {
            return trackingObjects.stream()
                    .map(TrackingObject::getInetAddress)
                    .anyMatch(inet -> inet.equals(inetAddress));
        } finally {
            lock.readLock().unlock();
        }
    }
}
