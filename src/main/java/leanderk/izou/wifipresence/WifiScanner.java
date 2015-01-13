package leanderk.izou.wifipresence;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.util.Hashtable;

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
        Hashtable env = new Hashtable();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.dns.DnsContextFactory");
        DirContext ictx = null;
        try {
            ictx = new InitialDirContext(env);
        } catch (NamingException e) {
            e.printStackTrace();
        }
        String dnsServers = null;
        try {
            dnsServers = (String) ictx.getEnvironment().get("java.naming.provider.url");
        } catch (NamingException e) {
            e.printStackTrace();
        }

        System.out.println("DNS Servers: " + dnsServers );

        /*
        int type = Type.A;
        for (int i = 0; i < 1; i++) {
            Lookup l = new Lookup(, type);
            l.run();
        }*/
    }


}
