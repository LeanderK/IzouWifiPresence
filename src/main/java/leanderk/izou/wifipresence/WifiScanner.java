package leanderk.izou.wifipresence;

import org.xbill.DNS.Lookup;
import org.xbill.DNS.Type;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Enumeration;

/**
 * @author LeanderK
 * @version 1.0
 */
public class WifiScanner {


    /**
     * @see sun.net.spi.nameservice.NameService#lookupAllHostAddr(java.lang.String)
     */
    /*public byte[][] lookupAllHostAddr(String name) throws UnknownHostException {
        log.debug("");

        String ipAddress = NameStore.getInstance().get(name);
        if (!StringUtils.isEmpty(ipAddress)){
            log.debug("\tmatch");
            byte[] ip = Util.textToNumericFormat(ipAddress);
            return new byte[][]{ip};
        } else {
            log.debug("\tmiss");
            return defaultDnsImpl.lookupAllHostAddr(name);
        }
    }*/

    public void scanWifi() {
        try {
            InetAddress localHostLANAddress = getLocalHostLANAddress();
            System.out.printf("a");
            Lookup lookup = new Lookup(ipAddress + "." + dnsblDomain, Type.PTR);
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
        /*
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        DirContext ictx = null;
        try {
            ictx = new InitialDirContext(env);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        String[] dnsServers = null;
        try {
            dnsServers = ((String) ictx.getEnvironment().get("java.naming.provider.url")).split(" ");
        } catch (NamingException e) {
            e.printStackTrace();
        }

        System.out.println("DNS Servers: " + dnsServers );*/

        /*
        int type = Type.A;
        for (int i = 0; i < 1; i++) {
            Lookup l = new Lookup(, type);
            l.run();
        }*/
        /*
            String ipAddress = "104.1.168.192";
            String dnsblDomain = "in-addr.arpa";
            Record[] records;
            Lookup lookup = new Lookup(ipAddress + "." + dnsblDomain, Type.PTR);
            SimpleResolver resolver = new SimpleResolver();
            resolver.setAddress(InetAddress.getByName("192.168.1.1"));
            lookup.setResolver(resolver);
            records = lookup.run();

            if(lookup.getResult() == Lookup.SUCCESSFUL) {
                for (int i = 0; i < records.length; i++) {
                    if(records[i] instanceof PTRRecord) {
                        PTRRecord ptr = (PTRRecord) records[i];
                        System.out.println("DNS Record: " + records[0].rdataToString());
                    }
                }
            } else {
                System.out.println("Failed lookup");
            }

        } catch(Exception e) {
            System.out.println("Exception: " + e);
        }*/
    }

    private ArrayList<String> scanSubNet(String subnet){
        ArrayList<String> hosts = new ArrayList<String>();

        InetAddress inetAddress = null;
        for(int i=1; i<10; i++){
            //Log.d(TAG, "Trying: " + subnet + String.valueOf(i));
            try {
                inetAddress = InetAddress.getByName(subnet + String.valueOf(i));
                if(inetAddress.isReachable(1000)){
                    hosts.add(inetAddress.getHostName());
                    //Log.d(TAG, inetAddress.getHostName());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return hosts;
    }

    /**
     * Returns an <code>InetAddress</code> object encapsulating what is most likely the machine's LAN IP address.
     * <p/>
     * This method is intended for use as a replacement of JDK method <code>InetAddress.getLocalHost</code>, because
     * that method is ambiguous on Linux systems. Linux systems enumerate the loopback network interface the same
     * way as regular LAN network interfaces, but the JDK <code>InetAddress.getLocalHost</code> method does not
     * specify the algorithm used to select the address returned under such circumstances, and will often return the
     * loopback address, which is not valid for network communication. Details
     * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4665037">here</a>.
     * <p/>
     * This method will scan all IP addresses on all network interfaces on the host machine to determine the IP address
     * most likely to be the machine's LAN address. If the machine has multiple IP addresses, this method will prefer
     * a site-local IP address (e.g. 192.168.x.x or 10.10.x.x, usually IPv4) if the machine has one (and will return the
     * first site-local address if the machine has more than one), but if the machine does not hold a site-local
     * address, this method will return simply the first non-loopback address found (IPv4 or IPv6).
     * <p/>
     * If this method cannot find a non-loopback address using this selection algorithm, it will fall back to
     * calling and returning the result of JDK method <code>InetAddress.getLocalHost</code>.
     * <p/>
     *
     * @throws UnknownHostException If the LAN address of the machine cannot be found.
     */
    private static InetAddress getLocalHostLANAddress() throws UnknownHostException {
        try {
            InetAddress candidateAddress = null;
            // Iterate all NICs (network interface cards)...
            for (Enumeration ifaces = NetworkInterface.getNetworkInterfaces(); ifaces.hasMoreElements();) {
                NetworkInterface iface = (NetworkInterface) ifaces.nextElement();
                // Iterate all IP addresses assigned to each card...
                for (Enumeration inetAddrs = iface.getInetAddresses(); inetAddrs.hasMoreElements();) {
                    InetAddress inetAddr = (InetAddress) inetAddrs.nextElement();
                    if (!inetAddr.isLoopbackAddress()) {

                        if (inetAddr.isSiteLocalAddress()) {
                            // Found non-loopback site-local address. Return it immediately...
                            return inetAddr;
                        }
                        else if (candidateAddress == null) {
                            // Found non-loopback address, but not necessarily site-local.
                            // Store it as a candidate to be returned if site-local address is not subsequently found...
                            candidateAddress = inetAddr;
                            // Note that we don't repeatedly assign non-loopback non-site-local addresses as candidates,
                            // only the first. For subsequent iterations, candidate will be non-null.
                        }
                    }
                }
            }
            if (candidateAddress != null) {
                // We did not find a site-local address, but we found some other non-loopback address.
                // Server might have a non-site-local address assigned to its NIC (or it might be running
                // IPv6 which deprecates the "site-local" concept).
                // Return this non-loopback candidate address...
                return candidateAddress;
            }
            // At this point, we did not find a non-loopback address.
            // Fall back to returning whatever InetAddress.getLocalHost() returns...
            InetAddress jdkSuppliedAddress = InetAddress.getLocalHost();
            if (jdkSuppliedAddress == null) {
                throw new UnknownHostException("The JDK InetAddress.getLocalHost() method unexpectedly returned null.");
            }
            return jdkSuppliedAddress;
        }
        catch (Exception e) {
            UnknownHostException unknownHostException = new UnknownHostException("Failed to determine LAN address: " + e);
            unknownHostException.initCause(e);
            throw unknownHostException;
        }
    }

}
