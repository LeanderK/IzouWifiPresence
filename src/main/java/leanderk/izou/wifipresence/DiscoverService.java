package leanderk.izou.wifipresence;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

/**
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
        return new ArrayList<>();
    }
}
