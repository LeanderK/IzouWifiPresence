package leanderk.izou.wifipresence;

import org.intellimate.izou.events.EventModel;
import org.intellimate.izou.resource.ResourceModel;
import org.intellimate.izou.sdk.Context;
import org.intellimate.izou.sdk.contentgenerator.ContentGenerator;
import org.intellimate.izou.sdk.contentgenerator.EventListener;
import org.intellimate.izou.sdk.events.Event;
import org.intellimate.izou.sdk.resource.Resource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author LeanderK
 * @version 1.0
 */
public class WifiPresenceContentGenerator extends ContentGenerator {
    public static final String ID = WifiPresenceContentGenerator.class.getCanonicalName();
    public static final List<String> RESOURCE_IDS = Arrays.asList("izou.presence.general", "izou.presence");
    private WifiScanner wifiScanner;

    public WifiPresenceContentGenerator(Context context, WifiScanner wifiScanner) {
        super(ID, context);
        this.wifiScanner = wifiScanner;
    }

    @Override
    public List<? extends EventListener> getTriggeredEvents() {
        Optional<EventListener> event_response = EventListener.createEventListener(
                Event.RESPONSE,
                "Signals that an event expects an response from other addons",
                "event_response",
                this);
        return optionalToList(event_response);
    }

    @Override
    public List<? extends Resource> getTriggeredResources() {
        return RESOURCE_IDS.stream()
                .map(this::createResource)
                .flatMap(opt -> optionalToList(opt).stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<? extends Resource> triggered(List<? extends ResourceModel> list, Optional<EventModel> optional) {
        return list.stream()
                .map(model -> createResource(model.getResourceID(), wifiScanner.anyPresent()))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
}
