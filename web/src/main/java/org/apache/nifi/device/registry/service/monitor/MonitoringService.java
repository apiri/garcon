package org.apache.nifi.device.registry.service.monitor;

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
 * Created on 6/7/17.
 */

/**
 * Service for monitoring the operational characteristics of a NiFi Cluster and reacting to certain thresholds being met in a user defined manner. Thresholds
 * are measured by querying the underlying storage service where the NiFi provenance data is stored at user defined intervals. At a later point there will be
 * an option to do this without relying on those underlying structures.
 */
public interface MonitoringService {
}
