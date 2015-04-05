package leanderk.izou.wifipresence;

import org.intellimate.izou.events.EventModel;
import org.intellimate.izou.sdk.Context;
import org.intellimate.izou.sdk.events.EventsController;

/**
 * @author LeanderK
 * @version 1.0
 */
public class WifiPresenceEventsController extends EventsController {
    public static final String ID = WifiPresenceEventsController.class.getCanonicalName();
    private WifiScanner wifiScanner;

    public WifiPresenceEventsController(WifiScanner wifiScanner, Context context) {
        super(context, ID);
        this.wifiScanner = wifiScanner;
    }

    @Override
    public boolean controlEvents(EventModel eventModel) {
        return wifiScanner.anyPresent();
    }
}
