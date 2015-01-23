package leanderk.izou.wifipresence;

import intellimate.izou.system.Context;

import javax.jmdns.*;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;

/**
 * @author LeanderK
 * @version 1.0
 */
public class JmDNSDiscoverService extends DiscoverService{
    private HashMap<String, ServiceListener> deviceListeners = new HashMap<>();
    private ServiceTypeListener serviceListener;
    private Context context;
    private JmmDNS jmmDNS;


    public JmDNSDiscoverService(WifiScanner wifiScanner, Context context) {
        super(wifiScanner);
        this.context = context;
        jmmDNS = JmmDNS.Factory.getInstance();
        listenForNewServices();
    }

    public void listenForNewServices() {
        serviceListener = new ServiceTypeListener() {
            @Override
            public void serviceTypeAdded(ServiceEvent serviceEvent) {
                jmmDNS.addServiceListener(serviceEvent.getType(), createNewDeviceListener());
            }

            @Override
            public void subTypeForServiceTypeAdded(ServiceEvent serviceEvent) {
                jmmDNS.addServiceListener(serviceEvent.getType(), createNewDeviceListener());
            }
        };
        try {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            jmmDNS.addServiceTypeListener(serviceListener);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * method used to listen for new Services
     * @param serviceEvent the Service discovered
     */
    private void newServiceDiscovered(ServiceEvent serviceEvent) {
        ServiceListener serviceListener = createNewDeviceListener();
        deviceListeners.put(serviceEvent.getType(), serviceListener);
    }

    /**
     * creates a new ServiceListener, which should be used to listen to new Devices
     * @return an ServiceListener
     */
    private ServiceListener createNewDeviceListener() {
        return new ServiceListener() {
            @Override
            public void serviceAdded(ServiceEvent serviceEvent) {
                ServiceInfo[] serviceInfos = jmmDNS.getServiceInfos(serviceEvent.getType(), serviceEvent.getName());
                newDeviceFound(serviceInfos);
            }

            @Override
            public void serviceRemoved(ServiceEvent serviceEvent) {
                //not implemented yet
            }

            @Override
            public void serviceResolved(ServiceEvent serviceEvent) {
                //nothing to do....
            }
        };
    }

    private void newDeviceFound(ServiceInfo[] serviceInfos) {
        for (int i = 0; i < serviceInfos.length; i++) {
            ServiceInfo serviceInfo = serviceInfos[i];
            if (getInterestedHostnames().contains(serviceInfo.getServer())) {
                for (InetAddress inetAddress : serviceInfo.getInetAddresses()) {
                    try {
                        if (inetAddress.isReachable(300)) {
                            newInetAddressDiscovered(new TrackingObject(() ->
                                    checkHost(inetAddress, serviceInfo.getType(), serviceInfo.getName()), inetAddress));
                            break;
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
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
        ServiceInfo[] serviceInfos = jmmDNS.getServiceInfos(type, name);
        for (int i = 0; i < serviceInfos.length; i++) {
            ServiceInfo serviceInfo = serviceInfos[i];
            for (InetAddress inetAddress : serviceInfo.getInetAddresses()) {
                if (inetAddress.equals(address)) {
                    return true;
                }
            }
        }
        return false;
    }
}
