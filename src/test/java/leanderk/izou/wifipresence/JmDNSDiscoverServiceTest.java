package leanderk.izou.wifipresence;

import org.junit.Test;

public class JmDNSDiscoverServiceTest {

    @Test
    public void testListenForNewServices() throws Exception {
        JmDNSDiscoverService jmDNSDiscoverService = new JmDNSDiscoverService(new WifiScanner(), null);
        Thread.sleep(1000000);
    }
}