package org.apache.nifi.processors.rt;

import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.device.registry.api.device.NiFiDevice;
import org.junit.Assert;
import org.junit.Test;

/**
 * Created by apiri on 26Jun2017.
 */
public class DeviceRegistryReportingTaskTest {

    private class TestableDeviceRegistryReportingTask extends DeviceRegistryReportingTask {
        private static final String host = "localhost";
        private static final String PORT = "8888";

        public  String getHost() {
            return host;
        }

        public  String getPort() {
            return PORT;
        }
    }

    @Test
    public void testPopulateNetworkingInfo() {
        DeviceRegistryReportingTask rt = new TestableDeviceRegistryReportingTask();
        NiFiDevice testDevice = new NiFiDevice();

        testDevice = rt.populateNetworkingInfo(testDevice);
        Assert.assertNotNull(testDevice);
        Assert.assertTrue(StringUtils.isNotBlank(testDevice.getHostname()));
    }

    @Test
    public void testPopulateDiskInformation() {
        DeviceRegistryReportingTask rt = new TestableDeviceRegistryReportingTask();
        NiFiDevice testDevice = new NiFiDevice();

        testDevice = rt.populateDiskSpaceInfo(null, testDevice);
        Assert.assertNotNull(testDevice);
    }

    @Test
    public void testPopulateMemoryInfo() {
        DeviceRegistryReportingTask rt = new TestableDeviceRegistryReportingTask();
        NiFiDevice testDevice = new NiFiDevice();

        testDevice = rt.populateMemoryInfo( testDevice);
        Assert.assertNotNull(testDevice);
        System.out.println(testDevice.getAvailableProcessors());
        System.out.println(testDevice.getTotalSystemMemory());
        System.out.println(testDevice.getConsumedMemory());
        System.out.println(testDevice.getAvailableSystemMemory());
    }

    @Test
    public void testReport() {
        TestableDeviceRegistryReportingTask rt = new TestableDeviceRegistryReportingTask();
        NiFiDevice testDevice = new NiFiDevice();

        testDevice = rt.populateMemoryInfo( testDevice);
        testDevice = rt.populateDiskSpaceInfo(null, testDevice);
        testDevice = rt.populateNetworkingInfo(testDevice);

        rt.report(rt.getHost(), rt.getPort(), testDevice);
    }
}