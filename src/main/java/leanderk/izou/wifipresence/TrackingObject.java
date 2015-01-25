package leanderk.izou.wifipresence;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * This Object represents an InetAddress we want to track. It also has a method used to check whether the InetAddress
 * still belongs to the Device we want to track.
 * @author LeanderK
 * @version 1.0
 */
public class TrackingObject {
    private BooleanSupplier hostChanged;
    private Consumer<InetAddress> removed;
    private InetAddress inetAddress;
    private LocalTime ttl;
    private LocalTime limit;
    private int unreachableCount = 0;
    private int unreachableLimit = 5;
    private int hostChangedCount = 0;
    private int hostChangedLimit = 4;

    public TrackingObject(BooleanSupplier hostChanged, Consumer<InetAddress> removed, InetAddress inetAddress) {
        this(hostChanged, removed, inetAddress, null);
    }

    /**
     * creates a new TrackingObject
     * @param hostChanged whether the InetAddress still belongs to the same host
     * @param removed callback on removed (after TTL)
     * @param inetAddress the InetAddress
     * @param ttl the time to live after becoming unreachable
     */
    public TrackingObject(BooleanSupplier hostChanged,
                          Consumer<InetAddress> removed,
                          InetAddress inetAddress,
                          LocalTime ttl) {
        this.hostChanged = hostChanged;
        this.inetAddress = inetAddress;
        this.removed = removed;
        this.ttl = ttl;
    }

    /**
     * returns whether the InetAddress still belongs to the same host
     * @return true if it still belongs to the same host, false if not
     */
    public boolean hostChanged() {
        if (hostChanged.getAsBoolean()) {
            hostChangedCount++;
            return hostChangedCount >= hostChangedLimit;
        } else {
            if (hostChangedCount != 0)
                hostChangedCount = 0;
            return false;
        }
    }

    /**
     * returns the InetAddress of the Object we are interested in
     * @return the InetAddress
     */
    public InetAddress getInetAddress() {
        return inetAddress;
    }

    /**
     * checks whether the TrackingObject is reachable with a timeout of 2 secs
     * @return true if reachable, false if not
     */
    public boolean isReachable() {
        boolean reachable = false;
        try {
           reachable =  inetAddress.isReachable(400);
        } catch (IOException ignored) { }
        if (reachable) {
            if (unreachableCount != 0)
                unreachableCount = 0;
            return true;
        } else {
            unreachableCount++;
            return unreachableCount <= unreachableLimit;
        }
    }

    /**
     * runs the callback for removed tracking
     */
    public void runRemovedCallback() {
        removed.accept(inetAddress);
    }

    /**
     * when we should end tracking the IP
     * @return LocalTime
     */
    public LocalTime getLimit() {
        return limit;
    }

    public void updateLimit() {
        limit = LocalTime.now();
        if (ttl != null)
            limit = limit.plusHours(ttl.getHour()).plusMinutes(ttl.getMinute());
    }
}
