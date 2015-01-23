package leanderk.izou.wifipresence;

import java.net.InetAddress;
import java.util.List;

/**
 * A Discover-Service is a Service, who is looking in the network for Devices we should track. It is not the purpose of
 * the DiscoverService to track, only to discover.
 * Use {@link #newInetAddressDiscovered(TrackingObject)} to start tracking it.
 * @author LeanderK
 * @version 1.0
 */
public class DiscoverService {
    private WifiScanner wifiScanner;

    public DiscoverService(WifiScanner wifiScanner) {
        this.wifiScanner = wifiScanner;
    }

    /**
     * call this method if you have found a Device to track
     * @param trackingObject the Device to track
     */
    public void newInetAddressDiscovered(TrackingObject trackingObject) {
        wifiScanner.newInetAddressDiscovered(trackingObject);
    }

    /**
     * returns whether the InetAddress is already beeing tracked.
     * @param inetAddress the inetAddress to track
     * @return true if being tracked, false if not
     */
    public boolean isAlreadyTracking(InetAddress inetAddress) {
        return wifiScanner.isAlreadyTracking(inetAddress);
    }

    /**
     * the hostnames we are searching for
     * @return a list of hostnames
     */
    public List<String> getInterestedHostnames() {
        return wifiScanner.getInterestedHostNames();
    }
}
