package leanderk.izou.wifipresence;

import org.intellimate.izou.events.EventsControllerModel;
import org.intellimate.izou.sdk.activator.Activator;
import org.intellimate.izou.sdk.contentgenerator.ContentGenerator;
import org.intellimate.izou.sdk.output.OutputExtension;
import org.intellimate.izou.sdk.output.OutputPlugin;
import ro.fortsoft.pf4j.Extension;

/**
 * @author LeanderK
 * @version 1.0
 */
@Extension
public class AddOn extends org.intellimate.izou.sdk.addon.AddOn{
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
        ContentGenerator[] contentGenerators = new ContentGenerator[1];
        contentGenerators[0] = new WifiPresenceContentGenerator(getContext(), wifiScanner);
        return contentGenerators;
    }

    /**
     * use this method to register (if needed) your EventControllers.
     *
     * @return Array containing Instances of EventControllers
     */
    @Override
    public EventsControllerModel[] registerEventController() {
        EventsControllerModel[] eventsControllers = new EventsControllerModel[1];
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
