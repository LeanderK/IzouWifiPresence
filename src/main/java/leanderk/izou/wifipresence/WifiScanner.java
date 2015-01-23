package leanderk.izou.wifipresence;

import intellimate.izou.system.Context;

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
public class WifiScanner {
    private List<DiscoverService> discoverServiceList = Collections.synchronizedList(new ArrayList<>());
    private List<TrackingObject> trackingObjects = Collections.synchronizedList(new ArrayList<>());
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(2);
    private Context context;
    private ScheduledFuture<?> reachabilityFuture;
    private ScheduledFuture<?> checkHostsFuture;

    public WifiScanner(Context context) {
        JmDNSDiscoverService jmDNSDiscoverService = new JmDNSDiscoverService(this, context);
        discoverServiceList.add(jmDNSDiscoverService);
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
                trackingObjects.remove(trackingObject);
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
                trackingObjects.remove(trackingObject);
            } else {
                context.logger.getLogger().error(trackingObject.getInetAddress().getHostAddress()
                        + "has the same host");
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
}
