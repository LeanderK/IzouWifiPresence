package leanderk.izou.wifipresence;

import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
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
    private final String hostname;
    private TemporalAmount ttl;
    private LocalDateTime lastReached = LocalDateTime.now();

    /**
     * creates a new TrackingObject
     * @param hostChanged whether the InetAddress still belongs to the same host
     * @param removed callback on removed (after TTL)
     * @param inetAddress the InetAddress
     * @param hostname the hostname of the InetAddress
     * @param ttl the time to live after becoming unreachable
     */
    public TrackingObject(BooleanSupplier hostChanged,
                          Consumer<InetAddress> removed,
                          InetAddress inetAddress,
                          String hostname,
                          TemporalAmount ttl) {
        this.hostChanged = hostChanged;
        this.inetAddress = inetAddress;
        this.removed = removed;
        this.hostname = hostname;
        this.ttl = ttl;
    }

    /**
     * returns whether the InetAddress still belongs to the same host
     * @return true if it still belongs to the same host, false if not
     */
    public boolean hostChanged() {
        return hostChanged.getAsBoolean();
    }

    /**
     * returns the InetAddress of the Object we are interested in
     * @return the InetAddress
     */
    public InetAddress getInetAddress() {
        return inetAddress;
    }

    /**
     * returns the Hostname
     * @return a String containing the Hostname
     */
    public String getHostname() {
        return hostname;
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
        return reachable;
    }

    public boolean isOverLimit(LocalDateTime now) {
        return lastReached.plus(ttl).isBefore(now);
    }

    /**
     * runs the callback for removed tracking
     */
    public void runRemovedCallback() {
        removed.accept(inetAddress);
    }

    public void updateLastReached(LocalDateTime lastReached) {
        if (this.lastReached.isBefore(lastReached))
            this.lastReached = lastReached;
    }

    @Override
    public String toString() {
        return "TrackingObject{" +
                "hostname='" + hostname + '\'' +
                ", inetAddress=" + inetAddress +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TrackingObject)) return false;

        TrackingObject that = (TrackingObject) o;

        return hostname.equals(that.hostname);

    }

    @Override
    public int hashCode() {
        return hostname.hashCode();
    }
}
