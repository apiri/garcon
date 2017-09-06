package org.apache.nifi.device.registry.resource.c2.dao;

import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.apache.nifi.device.registry.resource.c2.core.config.C2DeviceFlowFileConfig;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;

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
 * Created on 7/12/17.
 */

public class C2ServerFlowFileConfigDAO {

    private C2ServerService c2ServerSvc = new C2ServerService();

    public C2DeviceFlowFileConfig getDeviceFlowFileConfiguration() {
        try {
            String newConfig = c2ServerSvc.getConfig();
            if (StringUtils.isNotBlank(newConfig)) {
                C2DeviceFlowFileConfig ffCfg = new C2DeviceFlowFileConfig();
                ffCfg.setConfigDescription("MiNiFi C+++ Config");
                ffCfg.setCreatedTimestamp(new Timestamp(System.currentTimeMillis()));
                ffCfg.setUpdatedTimestamp(new Timestamp(System.currentTimeMillis()));
                String newFlow = "Flow Controller:\n" +
                        "  name: minificpp\n" +
                        "  id: 471deef6-2a6e-4a7d-912a-81cc17e3a205\n" +
                        "  comment: ''\n" +
                        "Processors:\n" +
                        "- id: 92eb3097-7439-318a-0000-000000000000\n" +
                        "  name: GetTCP\n" +
                        "  class: org.apache.nifi.processors.gettcp.GetTCP\n" +
                        "  max concurrent tasks: 1\n" +
                        "  scheduling strategy: TIMER_DRIVEN\n" +
                        "  scheduling period: 0 sec\n" +
                        "  penalization period: 30 sec\n" +
                        "  yield period: 1 sec\n" +
                        "  run duration nanos: 0\n" +
                        "  auto-terminated relationships list: []\n" +
                        "  Properties:\n" +
                        "    connection-attempt-timeout: '3'\n" +
                        "    end-of-message-byte: '0'\n" +
                        "    endpoint-list: localhost:50001\n" +
                        "    receive-buffer-size: 16MB\n" +
                        "    reconnect-interval: 5 sec\n" +
                        "Connections:\n" +
                        "- id: d5a90f06-fccc-3744-0000-000000000000\n" +
                        "  name: GetTCP/Partial/52fbbd82-015e-1000-97aa-99a063ad0dcb\n" +
                        "  source id: 92eb3097-7439-318a-0000-000000000000\n" +
                        "  source relationship name: Partial\n" +
                        "  destination id: 52fbbd82-015e-1000-97aa-99a063ad0dcb\n" +
                        "  max work queue size: 10000\n" +
                        "  max work queue data size: 1 GB\n" +
                        "  flowfile expiration: 0 sec\n" +
                        "  queue prioritizer class: ''\n" +
                        "- id: d5a90f06-fccc-3744-0000-000000000001\n" +
                        "  name: GetTCP/Success/52fbbd82-015e-1000-97aa-99a063ad0dcb\n" +
                        "  source id: 92eb3097-7439-318a-0000-000000000000\n" +
                        "  source relationship name: Success\n" +
                        "  destination id: 52fbbd82-015e-1000-97aa-99a063ad0dcb\n" +
                        "  max work queue size: 10000\n" +
                        "  max work queue data size: 1 GB\n" +
                        "  flowfile expiration: 0 sec\n" +
                        "  queue prioritizer class: ''\n" +
                        "Remote Processing Groups:\n" +
                        "- id: f9e2fc1e-6b1a-38f3-0000-000000000000\n" +
                        "  name: http://nifi:8080/nifi\n" +
                        "  url: http://nifi:8080/nifi\n" +
                        "  comment: ''\n" +
                        "  timeout: 30 sec\n" +
                        "  yield period: 10 sec\n" +
                        "  Input Ports:\n" +
                        "  - id: e6b42975-015d-1000-b3c3-675e4664a6f4\n" +
                        "    name: minificpp\n" +
                        "    comment: ''\n" +
                        "    max concurrent tasks: 1\n" +
                        "    use compression: false\n" +
                        "  - id: 52fbbd82-015e-1000-97aa-99a063ad0dcb\n" +
                        "    name: minificpp-2\n" +
                        "    comment: ''\n" +
                        "    max concurrent tasks: 1\n" +
                        "    use compression: false\n" +
                        "  Output Ports: []\n";
                ffCfg.setConfigFile(newFlow.getBytes(StandardCharsets.UTF_8));
                return ffCfg;
            }
        } catch (IOException ioe) {
            System.err.println("Unable to get device flow file configuration due to: " + ioe.getStackTrace());
        }

        return null;
    }

    private class C2ServerService {
        final CloseableHttpClient c2Http = HttpClients.createDefault();

        public HttpClient getC2Http() {
            return c2Http;
        }

        public String getConfig() throws IOException {
            HttpGet flowGet = new HttpGet("http://c2server:10080/c2/config?class=minificpp");
            String myFlow = "";
            try (CloseableHttpResponse flowGetResponse = c2Http.execute(flowGet);) {
                HttpEntity flowEntity = flowGetResponse.getEntity();
                myFlow = EntityUtils.toString(flowEntity);
                System.out.println("Received flow from c2server of " + myFlow);
                EntityUtils.consume(flowEntity);
            }
            return myFlow;
        }
    }
}
