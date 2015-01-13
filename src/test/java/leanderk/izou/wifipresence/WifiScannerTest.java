package leanderk.izou.wifipresence;

import org.junit.Test;

public class WifiScannerTest {
    @Test
    public void testScanWifi() {
        WifiScanner wifiScanner = new WifiScanner();
        wifiScanner.scanWifi();
    }
}