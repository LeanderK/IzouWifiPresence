package leanderk.izou.wifipresence;

import intellimate.izou.activator.Activator;
import intellimate.izou.events.Event;
import intellimate.izou.system.Context;
import intellimate.izou.system.IdentificationManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
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
public class WifiScanner extends Activator{
    public static final String ID = WifiScanner.class.getCanonicalName();
    private static final String PROPERTIES_ID = "hostname_";
    private List<DiscoverService> discoverServiceList = Collections.synchronizedList(new ArrayList<>());
    private List<TrackingObject> trackingObjects = new ArrayList<>();
    private List<TrackingObject> unreachableTrackingObjects = new ArrayList<>();
    private Queue<TrackingObject> trackingObjectsToAdd = new ArrayDeque<>();
    private List<String> interestedHostNames = Collections.synchronizedList(new ArrayList<>());
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
    private Context context;
    private ScheduledFuture<?> reachabilityFuture;
    private ScheduledFuture<?> checkHostsFuture;

    public WifiScanner(Context context) {
        super(context);
        this.context = context;
    }

    /**
     * Starting an Activator causes this method to be called.
     *
     * @throws InterruptedException will be caught by the Activator implementation, doesn't restart the activator
     */
    @Override
    public void activatorStarts() throws InterruptedException {
        getContext().properties.getPropertiesContainer().getProperties().stringPropertyNames().stream()
                .filter(key -> key.matches(PROPERTIES_ID + "\\d+"))
                .map(getContext().properties::getProperties)
                .forEach(interestedHostNames::add);
        JmDNSDiscoverService jmDNSDiscoverService = new JmDNSDiscoverService(this, context);
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
     * This method gets called when the Activator Thread got exceptionThrown.
     * <p>
     * This is an unusual way of ending a thread. The main reason for this should be, that the activator was interrupted
     * by an uncaught exception.
     *
     * @param e if not null, the exception, which caused the termination
     * @return true if the Thread should be restarted
     */
    @Override
    public boolean terminated(Exception e) {
        context.logger.getLogger().fatal("WifiScanner crashed", e);
        return true;
    }

    /**
     * call this class when you have discorvered a new Device to track.
     * <p>
     * from now on the InetAddress will be tracked. It will check if it has not already been tracked.
     * </p>
     * @param trackingObject the new InetAddress.
     */
    public void newInetAddressDiscovered(TrackingObject trackingObject) {
        context.logger.getLogger().debug("New request to track " + trackingObject.getInetAddress().getHostAddress());
        trackingObjectsToAdd.add(trackingObject);
    }

    /**
     * adds a TrackingObject to TrackingObjects
     * @param trackingObject the new TrackingObject
     */
    public void addToTrackingObjects(TrackingObject trackingObject) {
        if (!trackingObjects.stream()
                .anyMatch(alreadyTracking -> alreadyTracking.getInetAddress().equals(trackingObject.getInetAddress()))) {
            context.logger.getLogger().error("tracking " + trackingObject.getInetAddress().getHostAddress());
            if (trackingObjects.isEmpty()) {
                try {
                    IdentificationManager.getInstance().getIdentification(this)
                            .flatMap(id -> Event.createEvent(Event.RESPONSE, id))
                            .orElseThrow(() -> new IllegalStateException("Unable to create Event"))
                            .addDescriptor(AddOn.EVENT_ENTERED)
                            .fire(getCaller(), (event, counter) -> counter <= 3,
                                    event -> getContext().logger.getLogger().error("failed to fire Event"));
                } catch (IllegalStateException e) {
                    getContext().logger.getLogger().error("Unable to create Event");
                }
            }
            trackingObjects.add(trackingObject);
        } else {
            context.logger.getLogger().error("already tracking " + trackingObject.getInetAddress().getHostAddress());
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
            if (!trackingObjects.stream()
                    .anyMatch(alreadyTracking -> alreadyTracking.getInetAddress().equals(trackingObject.getInetAddress()))) {
                addToTrackingObjects(trackingObject);
            } else {
                context.logger.getLogger().debug("already tracking " + trackingObject.getInetAddress().getHostAddress());
            }
        }
        try {
            Iterator<TrackingObject> iterator = trackingObjects.iterator();

            while (iterator.hasNext()) {
                TrackingObject trackingObject = iterator.next();

                //context.logger.getLogger().error("checking reachability for " + trackingObject.getInetAddress().getHostAddress());
                if (!trackingObject.isReachable()) {
                    context.logger.getLogger().error(trackingObject.getInetAddress().getHostAddress() + " is not reachable");
                    iterator.remove();
                    removedFromTrackingObjectsList();
                    trackingObject.updateLimit();
                    unreachableTrackingObjects.add(trackingObject);
                } else {
//                    context.logger.getLogger().debug(trackingObject.getInetAddress().getHostAddress() + " is reachable");
                }
            }

            iterator = unreachableTrackingObjects.iterator();

            while (iterator.hasNext()) {
                TrackingObject trackingObject = iterator.next();

                context.logger.getLogger().error("checking reachability for unreached "
                        + trackingObject.getInetAddress().getHostAddress());
                if (!trackingObject.isReachable()) {
                    context.logger.getLogger().error(trackingObject.getInetAddress().getHostAddress() + " is still  not reachable");
                } else {
                    addToTrackingObjects(trackingObject);
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            context.logger.getLogger().error("An Error occured", e);
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
                    context.logger.getLogger().error(trackingObject.getInetAddress().getHostAddress()
                            + " has a different host");
                    iterator.remove();
                    trackingObject.runRemovedCallback();
                    removedFromTrackingObjectsList();
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
                    context.logger.getLogger().error("unreachable"
                            + trackingObject.getInetAddress().getHostAddress() + " Time-To-Live timed out");
                    iterator.remove();
                    trackingObject.runRemovedCallback();
                }

                context.logger.getLogger().error("checking host for unreachable"
                        + trackingObject.getInetAddress().getHostAddress());
                if (trackingObject.hostChanged()) {
                    context.logger.getLogger().error("unreachable " + trackingObject.getInetAddress().getHostAddress()
                            + " has a different host");
                    iterator.remove();
                }
            }
        } catch (Exception e) {
            context.logger.getLogger().error("An error occured", e);
        }
    }

    /**
     * fires an event if no trackingobjects are left
     */
    public void removedFromTrackingObjectsList() {
        if (trackingObjects.isEmpty()) {
            try {
                IdentificationManager.getInstance().getIdentification(this)
                        .flatMap(id -> Event.createEvent(Event.RESPONSE, id))
                        .orElseThrow(() -> new IllegalStateException("Unable to create Event"))
                        .addDescriptor(AddOn.EVENT_LEFT)
                        .fire(getCaller(),
                                (event, counter) -> counter <= 3,
                                event -> getContext().logger.getLogger().error("failed to fire Event"));
            } catch (IllegalStateException e) {
                getContext().logger.getLogger().error("Unable to create Event");
            }
        }
    }

    /**
     * returns whether any TrackingObjects we are interested in are currently present
     * @return true if present, false if not.
     */
    public boolean anyPresent() {
        return !trackingObjects.isEmpty();
    }

    /**
     * checks whether we are already tracking the InetAddress
     * @param inetAddress the InetAddress to check
     * @return true if already tracking, false if not
     */
    public boolean isAlreadyTracking(InetAddress inetAddress) {
        return trackingObjects.stream()
                .map(TrackingObject::getInetAddress)
                .anyMatch(inet -> inet.equals(inetAddress))

                || unreachableTrackingObjects.stream()
                .map(TrackingObject::getInetAddress)
                .anyMatch(inet -> inet.equals(inetAddress));
    }

    public void develop() {
        InetAddress[] addresses = new InetAddress[0];
        try {
            addresses = InetAddress.getAllByName("www.google.de");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        for (InetAddress address : addresses) {
            try {
                if (address.isReachable(10000))
                {
                    System.out.println("Connected "+ address);
                }
                else
                {
                    System.out.println("Failed "+address);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * An ID must always be unique.
     * A Class like Activator or OutputPlugin can just provide their .class.getCanonicalName()
     * If you have to implement this interface multiple times, just concatenate unique Strings to
     * .class.getCanonicalName()
     *
     * @return A String containing an ID
     */
    @Override
    public String getID() {
        return ID;
    }
}
