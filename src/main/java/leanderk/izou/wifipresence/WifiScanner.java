package leanderk.izou.wifipresence;

import intellimate.izou.activator.Activator;
import intellimate.izou.events.Event;
import intellimate.izou.system.Context;
import intellimate.izou.system.IdentificationManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author LeanderK
 * @version 1.0
 */
public class WifiScanner extends Activator{
    public static final String ID = WifiScanner.class.getCanonicalName();
    private List<DiscoverService> discoverServiceList = Collections.synchronizedList(new ArrayList<>());
    private List<TrackingObject> trackingObjects = Collections.synchronizedList(new ArrayList<>());
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
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
        JmDNSDiscoverService jmDNSDiscoverService = new JmDNSDiscoverService(this, context);
        discoverServiceList.add(jmDNSDiscoverService);
        scanWifi();
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
        context.logger.getLogger().error("New request to track " + trackingObject.getInetAddress().getHostAddress());
        if (!trackingObjects.contains(trackingObject)) {
            context.logger.getLogger().error("tracking " + trackingObject.getInetAddress().getHostAddress());
            trackingObjects.add(trackingObject);
            try {
                IdentificationManager.getInstance().getIdentification(this)
                        .flatMap(id -> Event.createEvent(Event.NOTIFICATION, id))
                        .orElseThrow(() -> new IllegalStateException("Unable to create Event"))
                        .addDescriptor(AddOn.EVENT_ENTERED)
                        .fire(getCaller(), (event, counter) -> counter <= 3,
                                event -> getContext().logger.getLogger().error("failed to fire Event"));
            } catch (IllegalStateException e) {
                getContext().logger.getLogger().error("Unable to create Event");
            }
        } else {
            context.logger.getLogger().error("already tracking " + trackingObject.getInetAddress().getHostAddress());
        }
    }

    /**
     * starts scanning the Wifi
     */
    public void scanWifi() {
        reachabilityFuture =
                scheduledExecutorService.scheduleAtFixedRate((Runnable) this::checkReachability, 0, 1, TimeUnit.SECONDS);
        checkHostsFuture =
                scheduledExecutorService.scheduleAtFixedRate((Runnable) this::checkHost, 0, 30, TimeUnit.SECONDS);
    }

    /**
     * pings every tracking-Object
     */
    public void checkReachability() {
        //noinspection Convert2streamapi
        for (TrackingObject trackingObject : trackingObjects) {
            context.logger.getLogger().error("checking reachability for " + trackingObject.getInetAddress().getHostAddress());
            if (!trackingObject.isReachable()) {
                context.logger.getLogger().error(trackingObject.getInetAddress().getHostAddress() + "is not reachable");
                remove(trackingObject);
            } else {
                context.logger.getLogger().error(trackingObject.getInetAddress().getHostAddress() + "is reachable");
            }
        }
    }

    /**
     * checks if the InetAddress we are pinging are still belonging to the Host we are interested in.
     */
    public void checkHost() {
        //noinspection Convert2streamapi
        for(TrackingObject trackingObject : trackingObjects) {
            context.logger.getLogger().error("checking host for " + trackingObject.getInetAddress().getHostAddress());
            if (trackingObject.hostChanged()) {
                context.logger.getLogger().error(trackingObject.getInetAddress().getHostAddress()
                        + "has a different host");
                remove(trackingObject);
            } else {
                context.logger.getLogger().error(trackingObject.getInetAddress().getHostAddress()
                        + "has the same host");
            }
        }
    }

    /**
     * removes a trackingObject
     * @param trackingObject the TreckingObject to remove
     */
    public void remove(TrackingObject trackingObject) {
        trackingObjects.remove(trackingObject);
        if (trackingObjects.isEmpty()) {
            try {
                IdentificationManager.getInstance().getIdentification(this)
                        .flatMap(id -> Event.createEvent(Event.NOTIFICATION, id))
                        .orElseThrow(() -> new IllegalStateException("Unable to create Event"))
                        .addDescriptor(AddOn.EVENT_LEFT)
                        .fire(getCaller(), (event, counter) -> counter <= 3,
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

    public boolean isAlreadyTracking(InetAddress inetAddress) {
        return trackingObjects.stream()
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
