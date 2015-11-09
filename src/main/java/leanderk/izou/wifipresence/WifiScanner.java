package leanderk.izou.wifipresence;

import org.intellimate.izou.sdk.Context;
import org.intellimate.izou.sdk.frameworks.presence.provider.PresenceIndicatorLevel;
import org.intellimate.izou.sdk.frameworks.presence.provider.template.PresenceConstant;

import java.net.InetAddress;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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
    private List<TrackingObject> trackingObjects = new ArrayList<>();
    private List<TrackingObject> unreachableTrackingObjects = new ArrayList<>();
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
                scheduledExecutorService.scheduleAtFixedRate((Runnable) this::checkReachability, 0, 4, TimeUnit.SECONDS);
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
        debug("New request to track " + trackingObject.toString());
        trackingObjectsToAdd.add(trackingObject);
    }

    /**
     * adds a TrackingObject to TrackingObjects
     * @param trackingObject the new TrackingObject
     */
    public void addToTrackingObjects(TrackingObject trackingObject) {
        if (!trackingObjects.stream()
                .anyMatch(alreadyTracking -> alreadyTracking.getHostname().equals(trackingObject.getHostname())) ||
            !unreachableTrackingObjects.stream()
                        .anyMatch(alreadyTracking -> alreadyTracking.getHostname().equals(trackingObject.getHostname()))) {
            debug("tracking " + trackingObject.toString());
            setPresence(true);
            trackingObjects.add(trackingObject);
        } else {
            debug("already tracking " + trackingObject.toString());
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
        while (!trackingObjectsToAdd.isEmpty()) {
            TrackingObject trackingObject = trackingObjectsToAdd.poll();
            addToTrackingObjects(trackingObject);
        }
        try {
            Iterator<TrackingObject> iterator = trackingObjects.iterator();

            while (iterator.hasNext()) {
                TrackingObject trackingObject = iterator.next();

                //context.logger.getLogger().error("checking reachability for " + trackingObject.getInetAddress().getHostAddress());
                if (!trackingObject.isReachable()) {
                    debug(trackingObject.toString() + " is not reachable");
                    iterator.remove();
                    trackingObject.updateLimit();
                    unreachableTrackingObjects.add(trackingObject);
                } else {
//                    context.logger.getLogger().debug(trackingObject.getInetAddress().getHostAddress() + " is reachable");
                }
            }

            iterator = unreachableTrackingObjects.iterator();

            while (iterator.hasNext()) {
                TrackingObject trackingObject = iterator.next();

                //debug("checking reachability for unreached "
                //        + trackingObject.toString());
                if (!trackingObject.isReachable()) {
                //    error(trackingObject.toString() + " is still  not reachable");
                } else {
                    addToTrackingObjects(trackingObject);
                    iterator.remove();
                }
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

            Iterator<TrackingObject> iterator = trackingObjects.iterator();

            while (iterator.hasNext()) {
                TrackingObject trackingObject = iterator.next();

                //context.logger.getLogger().debug("checking host for " + trackingObject.getInetAddress().getHostAddress());
                if (trackingObject.hostChanged()) {
                    debug(trackingObject.toString() + " has a different host");
                    iterator.remove();
                    trackingObject.runRemovedCallback();
                    declaredUnreachable();
                } else {
//                    context.logger.getLogger().error(trackingObject.getInetAddress().getHostAddress()
//                            + " has the same host");
                }
            }

            iterator = unreachableTrackingObjects.iterator();
            LocalTime time = null;
            while (iterator.hasNext()) {
                TrackingObject trackingObject = iterator.next();
                if (time == null) time = LocalTime.now();

                if (trackingObject.getLimit().isBefore(time)) {
                    debug("unreachable" + trackingObject.toString() + " Time-To-Live timed out");
                    iterator.remove();
                    trackingObject.runRemovedCallback();
                    declaredUnreachable();
                    continue;
                }

                //debug("checking host for unreachable"
                //        + trackingObject.getInetAddress().getHostAddress());
                if (trackingObject.hostChanged()) {
                    debug("unreachable " + trackingObject.toString() + " has a different host");
                    iterator.remove();
                    declaredUnreachable();
                }
            }
        } catch (Exception e) {
            debug("An error occured", e);
        }
    }

    /**
     * fires an event if no trackingobjects are left
     */
    public void declaredUnreachable() {
        if (unreachableTrackingObjects.isEmpty() && trackingObjects.isEmpty()) {
            setPresence(false);
        }
    }

    /**
     * checks whether we are already tracking the InetAddress
     * @param inetAddress the InetAddress to check
     * @return true if already tracking, false if not
     */
    public boolean isAlreadyTracking(InetAddress inetAddress) {
        boolean reachable = trackingObjects.stream()
                .map(TrackingObject::getInetAddress)
                .anyMatch(inet -> inet.equals(inetAddress));

        boolean unreachable = unreachableTrackingObjects.stream()
                .map(TrackingObject::getInetAddress)
                .anyMatch(inet -> inet.equals(inetAddress));

        return reachable || unreachable;
    }
}
