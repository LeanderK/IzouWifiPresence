package leanderk.izou.wifipresence;

import java.io.IOException;
import java.net.InetAddress;
import java.util.function.BooleanSupplier;

/**
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

    public boolean hostChanged() {
        return hostChanged.getAsBoolean();
    }

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
