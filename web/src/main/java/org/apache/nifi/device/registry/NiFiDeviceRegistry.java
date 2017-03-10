/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.nifi.device.registry;

import org.apache.nifi.device.registry.cli.DummyCommand;
import org.apache.nifi.device.registry.managed.Site2SiteManagedProxy;
import org.apache.nifi.device.registry.resource.DeviceResource;

import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;

public class NiFiDeviceRegistry
        extends Application<NiFiDeviceRegistryConfiguration> {

    @Override
    public void initialize(Bootstrap<NiFiDeviceRegistryConfiguration> bootstrap) {
        bootstrap.addCommand(new DummyCommand());

        //Creates an Asset bundle to serve up static content. Served from http://localhost:8080/assets/
        bootstrap.addBundle(new AssetsBundle());
    }

    @Override
    public void run(NiFiDeviceRegistryConfiguration configuration, Environment environment) throws Exception {

//        final DBIFactory factory = new DBIFactory();
//        final DBI jdbi = factory.build(environment, configuration.getDataSourceFactory(), "mysql");
//        final DummyDAO dao = jdbi.onDemand(DummyDAO.class);

        //Add managed instances.
        environment.lifecycle().manage(new Site2SiteManagedProxy());

        //Register your Web Resources like below.
        final DeviceResource dummyResource = new DeviceResource(configuration);
        environment.jersey().register(dummyResource);
    }

    public static void main(String[] args) throws Exception {
        new NiFiDeviceRegistry().run(args);
    }
}