package org.apache.nifi.processors.rt;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.device.registry.api.device.NiFiDevice;
import org.apache.nifi.processor.util.StandardValidators;
import org.apache.nifi.reporting.AbstractReportingTask;
import org.apache.nifi.reporting.ReportingContext;
import org.apache.nifi.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * <p>
 * Created on 3/13/17.
 */

@Tags({"reporting", "device registry", "IoT"})
@CapabilityDescription("In large IoT based deployments reports back information about this instance to give a NiFi operator a single view of all instances running")
public class DeviceRegistryReportingTask
        extends AbstractReportingTask {

    private static final Logger logger = LoggerFactory.getLogger(DeviceRegistryReportingTask.class);
    private final ObjectMapper mapper = new ObjectMapper();

    private static final PropertyDescriptor DEVICE_REGISTRY_HOST = new PropertyDescriptor.Builder()
            .name("Host")
            .description("NiFi Device Registry service that the metrics will be transported to")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("localhost")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    private static final PropertyDescriptor DEVICE_REGISTRY_PORT = new PropertyDescriptor.Builder()
            .name("Port")
            .description("Port the target NiFi Device Registry is running on")
            .required(true)
            .expressionLanguageSupported(true)
            .defaultValue("8888")
            .addValidator(StandardValidators.NON_EMPTY_VALIDATOR)
            .build();

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        final List<PropertyDescriptor> properties = new ArrayList<>();
        properties.add(DEVICE_REGISTRY_HOST);
        properties.add(DEVICE_REGISTRY_PORT);
        return properties;
    }

    public void onTrigger(ReportingContext reportingContext) {
        logger.info("Running DeviceRegistryReportingTask");

        String host = reportingContext.getProperty(DEVICE_REGISTRY_HOST).evaluateAttributeExpressions().getValue();
        String port = reportingContext.getProperty(DEVICE_REGISTRY_PORT).evaluateAttributeExpressions().getValue();

        //Build NiFiDevice object for payload.
        NiFiDevice device = new NiFiDevice();
        device = populateNetworkingInfo(device);
        logger.info("Populating memory info");
        device = populateMemoryInfo(device);
        logger.info("Populating disk info");
        device = populateDiskSpaceInfo(reportingContext, device);

        report(host, port, device);
    }

    protected boolean report(String host, String port, NiFiDevice device) {

        try {
            String url = "http://" + host + ":" + port + "/api/v1/device/device";

            logger.info("Attempting to report information to {}", url);

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost postRequest = new HttpPost(url);

            StringEntity input = new StringEntity(this.mapper.writeValueAsString(device));
            input.setContentType("application/json");
            postRequest.setEntity(input);

            HttpResponse response = httpClient.execute(postRequest);

            BufferedReader br = new BufferedReader(
                    new InputStreamReader((response.getEntity().getContent())));

            String output;
            while ((output = br.readLine()) != null) {
                logger.info("NiFi Device Registry Response: {}", output);
            }

        } catch (Exception ex) {
            ex.printStackTrace();
            logger.error("Error POSTing JSON to NiFi Device Registry", ex);
            return false;
        }

        return true;
    }


    protected NiFiDevice populateNetworkingInfo(NiFiDevice device) {

        InetAddress ip;
        try {

            ip = InetAddress.getLocalHost();

            NetworkInterface network = NetworkInterface.getByInetAddress(ip);

            //Check this isn't null
            if (network == null) {
                //Hail mary to try and get eth0
                getLogger().warn("Hardcoded getting network interface by ETH0 which could be the incorrect interface but others were null");
                network = NetworkInterface.getByName("eth0");
            }

            byte[] mac = network.getHardwareAddress();

            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
            }

            //Set the values to the device object.
            device.setDeviceId(sb.toString());
            device.setIp(ip.getHostAddress());

            String hostname = InetAddress.getLocalHost().getHostName();
            logger.error("First attempt at getting hostname: " + hostname);
            if (!StringUtils.isEmpty(hostname)) {
                device.setHostname(hostname);
            } else {
                //Try the linux approach ... could fail if hostname(1) system command is not available.
                try {
                    Process process = Runtime.getRuntime().exec("hostname");
                    InputStream is = process.getInputStream();

                    StringWriter writer = new StringWriter();
                    IOUtils.copy(is, writer, "UTF-8");
                    hostname = writer.toString();
                    if (StringUtils.isEmpty(hostname)) {
                        device.setHostname("UNKNOWN");
                    } else {
                        device.setHostname(hostname);
                    }

                } catch (Exception ex) {
                    ex.printStackTrace();
                    logger.error("Error attempting to resolve hostname", ex);
                }
            }

        } catch (UnknownHostException e) {
            e.printStackTrace();
            logger.error("Unknown host exception getting hostname", e);
        } catch (SocketException e){
            e.printStackTrace();
            logger.error("socket exception getting hostname", e);
        }

        return device;
    }

    protected NiFiDevice populateMemoryInfo(NiFiDevice device) {
        device.setAvailableProcessors(Runtime.getRuntime().availableProcessors());
        device.setTotalSystemMemory(Runtime.getRuntime().totalMemory());
        device.setConsumedMemory(Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory());
        device.setAvailableSystemMemory(Runtime.getRuntime().freeMemory());
        return device;
    }

    protected NiFiDevice populateDiskSpaceInfo(ReportingContext reportingContext, NiFiDevice device) {

        try {
            //NiFiProperties properties = NiFiPropertiesLoader.loadDefaultWithKeyFromBootstrap();
            //device.setNiFiProperties(properties);

//            Path nifiFlowConfFile = properties.getFlowConfigurationFile();
//
//            Path templatePath = properties.getTemplateDirectory();
//            logger.info("TemplatePath: " + templatePath.toString());
//            String templateFilePath = templatePath.toString() + File.separator + "flow.xml.gz";
//            logger.info("TemplateFilePath: " + templateFilePath);

//            try {
//                FileInputStream fis = new FileInputStream(properties.getFlowConfigurationFile());
//                String md5 = org.apache.commons.codec.digest.DigestUtils.md5Hex(fis);
//                fis.close();
//                device.setTemplateMD5(md5);
//            } catch (Exception ex) {
//                ex.printStackTrace();
//                logger.error("Error creating MD5 Hash for NiFi Template", ex);
//            }

            //Set the disk report
            //device.setRootDiskReport(createDiskReportForPath(Paths.get("/")));
            //device.setDbDiskReport(createDiskReportForPath(properties.getDatabaseRepositoryPath()));
            //device.setFlowfileRepoDiskReport(createDiskReportForPath(properties.getFlowFileRepositoryPath()));

            //Map<String, Path> contentPaths = properties.getContentRepositoryPaths();
//            Map<String, DiskReport> contentDiskReports = new HashMap<>();
//            Iterator<String> itr = contentPaths.keySet().iterator();
//
//            while (itr.hasNext()) {
//                String key = itr.next();
//                Path path = contentPaths.get(key);
//                contentDiskReports.put(key, createDiskReportForPath(path));
//            }
//            device.setContentRepoDiskReport(contentDiskReports);
//
//            Map<String, Path> provPaths = properties.getProvenanceRepositoryPaths();
//            Map<String, DiskReport> provDiskReports = new HashMap<>();
//            itr = provDiskReports.keySet().iterator();
//
//            while (itr.hasNext()) {
//                String key = itr.next();
//                Path path = provPaths.get(key);
//                provDiskReports.put(key, createDiskReportForPath(path));
//            }
//            device.setProvRepoDiskReport(provDiskReports);


        } catch (Exception e) {
            e.printStackTrace();
            logger.error("Error encountered in populateDiskInfo function", e);
        }

        return device;
    }

//    private DiskReport createDiskReportForPath(Path path) {
//        DiskReport report = new DiskReport();
//
//        File f = path.toFile();
//        if (f.exists()) {
//            report.setAvailableBytes(f.getFreeSpace());
//            report.setPath(path.toString());
//            report.setTotalBytes(f.getTotalSpace());
//            report.setUsedBytes(f.getUsableSpace());
//        } else {
//            //File doesn't exist so null out all information and flag that.
//        }
//
//        return report;
//    }
}
