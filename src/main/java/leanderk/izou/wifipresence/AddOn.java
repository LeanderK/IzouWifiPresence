package leanderk.izou.wifipresence;

import intellimate.izou.activator.Activator;
import intellimate.izou.contentgenerator.ContentGenerator;
import intellimate.izou.events.EventsController;
import intellimate.izou.output.OutputExtension;
import intellimate.izou.output.OutputPlugin;
import ro.fortsoft.pf4j.Extension;

/**
 * @author LeanderK
 * @version 1.0
 */
@Extension
public class AddOn extends intellimate.izou.addon.AddOn{
    public static final String ID = AddOn.class.getCanonicalName();
    public static final String EVENT_ENTERED = "izou.presence.general";
    public static final String EVENT_LEFT = "izou.presence.general.leaving";
    private WifiScanner wifiScanner;

    /**
     * the default constructor for AddOns
     */
    public AddOn() {
        super(ID);
    }

    /**
     * use this method to build your instances etc.
     */
    @Override
    public void prepare() {
        wifiScanner = new WifiScanner(getContext());
    }

    /**
     * use this method to register (if needed) your Activators.
     *
     * @return Array containing Instances of Activators
     */
    @Override
    public Activator[] registerActivator() {
        Activator[] activators = new Activator[1];
        activators[0] = wifiScanner;
        return activators;
    }

    /**
     * use this method to register (if needed) your ContentGenerators.
     *
     * @return Array containing Instances of ContentGenerators
     */
    @Override
    public ContentGenerator[] registerContentGenerator() {
        return new ContentGenerator[0];
    }

    /**
     * use this method to register (if needed) your EventControllers.
     *
     * @return Array containing Instances of EventControllers
     */
    @Override
    public EventsController[] registerEventController() {
        EventsController[] eventsControllers = new EventsController[1];
        eventsControllers[0] = new WifiPresenceEventsController(wifiScanner ,getContext());
        return eventsControllers;
    }

    /**
     * use this method to register (if needed) your OutputPlugins.
     *
     * @return Array containing Instances of OutputPlugins
     */
    @Override
    public OutputPlugin[] registerOutputPlugin() {
        return new OutputPlugin[0];
    }

    /**
     * use this method to register (if needed) your Output.
     *
     * @return Array containing Instances of OutputExtensions
     */
    @Override
    public OutputExtension[] registerOutputExtension() {
        return new OutputExtension[0];
    }
}
