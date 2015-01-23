package leanderk.izou.wifipresence;

import java.io.IOException;
import java.net.InetAddress;
import java.util.function.BooleanSupplier;

/**
 * This Object represents an InetAddress we want to track. It also has a method used to check whether the InetAddress
 * still belongs to the Device we want to track.
 * @author LeanderK
 * @version 1.0
 */
public class TrackingObject {
    private BooleanSupplier hostChanged;
    private InetAddress inetAddress;

    public TrackingObject(BooleanSupplier hostChanged, InetAddress inetAddress) {
        this.hostChanged = hostChanged;
        this.inetAddress = inetAddress;
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
     * checks whether the TrackingObject is reachable with a timeout of 2 secs
     * @return true if reachable, false if not
     */
    public boolean isReachable() {
        try {
            return !inetAddress.isReachable(2000);
        } catch (IOException e) {
            return true;
        }
    }
}
