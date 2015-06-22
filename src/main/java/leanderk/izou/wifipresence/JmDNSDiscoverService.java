package leanderk.izou.wifipresence;

import org.intellimate.izou.sdk.Context;

import javax.jmdns.*;
import javax.jmdns.impl.DNSIncoming;
import java.io.IOException;
import java.net.InetAddress;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Uses zeroconf to discover devices
 * @author LeanderK
 * @version 1.0
 */
public class JmDNSDiscoverService extends DiscoverService {
    @SuppressWarnings("FieldCanBeLocal")
    private List<String> COMMON_SERVICES = Arrays.asList("_apple-mobdev2._tcp.local.");
    private HashMap<String, ServiceListener> serviceListeners = new HashMap<>();
    private HashMap<InetAddress, String> devices = new HashMap<>();
    @SuppressWarnings("FieldCanBeLocal")
    private ServiceTypeListener servicesListener;
    private final Context context;
    private JmDNS jmDNS;

    public JmDNSDiscoverService(WifiScanner wifiScanner, Context context) {
        super(wifiScanner);
        this.context = context;
        try {
            jmDNS = JmDNS.create();
        } catch (IOException e) {
            context.getLogger().fatal("unable to creat JmDNS", e);
            return;
        }
        initJmDNS();
        jmDNS.getServiceInfo("", "", true);
    }

    private void initJmDNS() {
        Logger logger = Logger.getLogger(DNSIncoming.class.getName());
        logger.setLevel(Level.SEVERE);
        serviceListeners.entrySet().stream()
                .forEach(entry -> jmDNS.addServiceListener(entry.getKey(), entry.getValue()));
        COMMON_SERVICES.forEach(this::addServiceListener);
        listenForNewServices();
    }

    /**
     * listens for all the available services.
     */
    private void listenForNewServices() {
        servicesListener = new ServiceTypeListener() {
            @Override
            public void serviceTypeAdded(ServiceEvent serviceEvent) {
                addServiceListener(serviceEvent.getType());
            }

            @Override
            public void subTypeForServiceTypeAdded(ServiceEvent serviceEvent) {
                addServiceListener(serviceEvent.getType());
            }
        };
        try {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            jmDNS.addServiceTypeListener(servicesListener);
        } catch (IOException e) {
            context.getLogger().error("unable to add ");
        }
    }

    private void addServiceListener(String type) {
        if (!serviceListeners.containsKey(type)) {
            context.getLogger().debug("Service: " + type + " discovered");
            jmDNS.addServiceListener(type, createNewDeviceListener(type));
        }
    }

    /**
     * creates a new ServiceListener, which should be used to listen to new Devices
     * @return an ServiceListener
     */
    private ServiceListener createNewDeviceListener(ServiceEvent serviceEvent) {
        return createNewDeviceListener(serviceEvent.getType());
    }

    /**
     * creates a new ServiceListener, which should be used to listen to new Devices
     * @return an ServiceListener
     */
    private ServiceListener createNewDeviceListener(String type) {
        ServiceListener listener = new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent serviceEvent) {
                ServiceInfo serviceInfos = jmDNS.getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
                newDeviceFound(serviceInfos);
            }

            @Override
            public void serviceRemoved(ServiceEvent serviceEvent) {
                //not implemented yet
            }

            @Override
            public void serviceResolved(ServiceEvent serviceEvent) {
                //System.out.println("HEY");
                ServiceInfo serviceInfos = jmDNS.getServiceInfo(serviceEvent.getType(), serviceEvent.getName());
                newDeviceFound(serviceInfos);
            }
        };
        serviceListeners.put(type, listener);
        ServiceInfo[] list = jmDNS.list(type);
        return listener;
    }

    /**
     * this method should be called when a new Device may be found
     * @param serviceInfo the ServiceInfo object of the device
     */
    private void newDeviceFound(ServiceInfo serviceInfo) {
        Optional<String> first = getInterestedHostnames().stream()
                    .filter(interested -> serviceInfo.getServer().contains(interested))
                    .findFirst();
        if (first.isPresent()) {
            for (int x = 0; x < 2; x++) {
                InetAddress[] inetAddresses;
                if (x == 0) {
                    inetAddresses = serviceInfo.getInet4Addresses();
                } else {
                    inetAddresses = serviceInfo.getInet6Addresses();
                }
                if (inetAddresses == null)
                    continue;
                for (int i = 0; i < inetAddresses.length; i++) {
                    InetAddress inetAddress = inetAddresses[i];
                    try {
                        if (inetAddress.isReachable(300)) {
                            devices.put(inetAddress, serviceInfo.getType());
                            newInetAddressDiscovered(new TrackingObject(
                                    () -> !checkHost(inetAddress, serviceInfo.getType(), serviceInfo.getName()),
                                    inet -> trackingObjectRemoved(inetAddress),
                                    inetAddress,
                                    first.get(),
                                    LocalTime.of(0, 30)));
                            return;
                        }
                    } catch (IOException e) {
                        context.getLogger().error("An error occurred while trying to reach device," +
                                " unfortunately this is fairly common. InetAddress: " + inetAddress.toString() + " " +
                                +(i + 1) + " from " + inetAddresses.length);
                        //it can cause JmDNS to ignore the device for and hour or longer!
                        //last inetAddress?
                        if (i + 1 == inetAddresses.length) {
                            restartJmDNS();
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * returns false if the InetAddress is not found in the Query for type and name
     * @param address the InetAddress to check
     * @param type the type of the service
     * @param name the name of the device
     * @return true if found, false if not
     */
    private boolean checkHost(InetAddress address, String type, String name) {
        ServiceInfo serviceInfo = jmDNS.getServiceInfo(type, name);
        if (serviceInfo == null) return false;
        for (InetAddress inetAddress : serviceInfo.getInetAddresses()) {
            if (inetAddress.equals(address)) {
                return true;
            }
        }
        return false;
    }

    private void trackingObjectRemoved (InetAddress inetAddress) {
        String type = devices.get(inetAddress);
        //way better! but not working right :/
        /*
        JmDNSImpl jmDNS1 = (JmDNSImpl) jmDNS;
        if (type != null) {
            ServiceListener listener = serviceListeners.get(type);
            jmDNS.removeServiceListener(type, listener);
            jmDNS1.getCache().getDNSEntryList(type).forEach(entry -> jmDNS1.getCache().removeDNSEntry(entry));
            jmDNS1.cleanCache();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            //jmDNS1.renewServiceCollector(new DNSRecordWithType(type));
            jmDNS.addServiceListener(type, createNewDeviceListener(type));
        }*/
        restartJmDNS();
    }

    /**
     * expensive operation! Restarts the whole thing!
     * MUST find a workaround to get JmDNS forget devices.
     */
    private void restartJmDNS() {
        context.getLogger().debug("restarting JmDNS");
        try {
            jmDNS.close();
        } catch (IOException e) {
            context.getLogger().error("Unable to close JmDNS", e);
        }
        try {
            jmDNS = JmDNS.create();
        } catch (IOException e) {
            context.getLogger().fatal("unable to creat JmDNS", e);
            return;
        }
        initJmDNS();
    }
}
