package leanderk.izou.wifipresence;

import intellimate.izou.events.Event;
import intellimate.izou.events.EventsController;
import intellimate.izou.system.Context;

/**
 * @author LeanderK
 * @version 1.0
 */
public class WifiPresenceEventsController implements EventsController {
    public static final String ID = WifiPresenceEventsController.class.getCanonicalName();
    private WifiScanner wifiScanner;
    private Context context;

    public WifiPresenceEventsController(WifiScanner wifiScanner, Context context) {
        this.wifiScanner = wifiScanner;
        this.context = context;
    }

    /**
     * Controls whether the fired Event should be dispatched to all the listeners
     * <p>
     * This method should execute quickly
     *
     * @param event the ID of the event
     * @return true if events should be dispatched
     */
    @Override
    public boolean controlEventDispatcher(Event event) {
        return wifiScanner.anyPresent();
    }

    /**
     * this method gets called when the task submitted to the ThreadPool crashes
     *
     * @param e the exception catched
     */
    @Override
    public void exceptionThrown(Exception e) {
        context.logger.getLogger().fatal("WifiPresenceEventsController crashed!");
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
