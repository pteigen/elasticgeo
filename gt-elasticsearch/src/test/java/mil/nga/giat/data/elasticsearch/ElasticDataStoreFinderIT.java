/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 * 
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package mil.nga.giat.data.elasticsearch;

import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.common.collect.ImmutableMap;
import org.apache.http.HttpHost;
import org.elasticsearch.client.Node;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.factory.FactoryCreator;
import org.geotools.factory.FactoryRegistry;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

import static org.junit.Assert.*;

public class ElasticDataStoreFinderIT extends ElasticTestSupport {

    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(ElasticDataStoreFinderIT.class);

    private DataStore source;

    @Test
    public void testFactoryDefaults() throws IOException {
        Map<String,Serializable> params = createConnectionParams();
        ElasticDataStoreFactory factory = new ElasticDataStoreFactory();
        dataStore = (ElasticDataStore) factory.createDataStore(params);

        ElasticDataStoreFactory fac = new ElasticDataStoreFactory();
        assertEquals(fac.getDisplayName(), ElasticDataStoreFactory.DISPLAY_NAME);
        assertEquals(fac.getDescription(), ElasticDataStoreFactory.DESCRIPTION);
        assertArrayEquals(fac.getParametersInfo(), ElasticDataStoreFactory.PARAMS);
        assertNull(fac.getImplementationHints());
        assertNull(fac.createNewDataStore(null));
    }

    @Test
    public void testFactory() {
        assertTrue(new ElasticDataStoreFactory().isAvailable());
        scanForPlugins();

        Map<String,Serializable> map = new HashMap<>();
        map.put(ElasticDataStoreFactory.HOSTNAME.key, "localhost");
        map.put(ElasticDataStoreFactory.HOSTPORT.key, PORT);
        map.put(ElasticDataStoreFactory.INDEX_NAME.key, "sample");

        Iterator<DataStoreFactorySpi> ps = getAvailableDataSources();
        ElasticDataStoreFactory fac;
        while (ps.hasNext()) {
            fac = (ElasticDataStoreFactory) ps.next();

            try {
                if (fac.canProcess(map)) {
                    source = fac.createDataStore(map);
                }
            } catch (Throwable t) {
                LOGGER.log(Level.WARNING, "Could not acquire " + fac.getDescription() + ":" + t, t);
            }
        }

        assertNotNull(source);
        assertTrue(source instanceof ElasticDataStore);
    }

    @Test
    public void testFactoryWithMissingRequired() {
        assertTrue(new ElasticDataStoreFactory().isAvailable());
        scanForPlugins();

        Iterator<DataStoreFactorySpi> ps = getAvailableDataSources();
        ElasticDataStoreFactory fac;
        while (ps.hasNext()) {
            fac = (ElasticDataStoreFactory) ps.next();
            assertTrue(!fac.canProcess(ImmutableMap.of(ElasticDataStoreFactory.HOSTNAME.key, "localhost",
                    ElasticDataStoreFactory.HOSTPORT.key, PORT)));
            assertTrue(!fac.canProcess(ImmutableMap.of(ElasticDataStoreFactory.HOSTNAME.key, "localhost",
                    ElasticDataStoreFactory.INDEX_NAME.key, "test")));
            assertTrue(!fac.canProcess(ImmutableMap.of(ElasticDataStoreFactory.HOSTNAME.key, "localhost")));
            assertTrue(!fac.canProcess(ImmutableMap.of(ElasticDataStoreFactory.HOSTPORT.key, PORT,
                    ElasticDataStoreFactory.INDEX_NAME.key, "test")));
            assertTrue(!fac.canProcess(ImmutableMap.of(ElasticDataStoreFactory.HOSTPORT.key, PORT)));
            assertTrue(!fac.canProcess(ImmutableMap.of(ElasticDataStoreFactory.INDEX_NAME.key, "test")));
        }

        assertNull(source);
    }

    @Test
    public void testCreateRestClient() throws IOException {
        assertEquals(ImmutableList.of(new HttpHost("localhost", PORT, "http")), getHosts("localhost"));
        assertEquals(ImmutableList.of(new HttpHost("localhost.localdomain", PORT, "http")),
                getHosts("localhost.localdomain"));

        assertEquals(ImmutableList.of(new HttpHost("localhost", 9201, "http")),
                getHosts("localhost:9201"));
        assertEquals(ImmutableList.of(new HttpHost("localhost.localdomain", 9201, "http")),
                getHosts("localhost.localdomain:9201"));

        assertEquals(ImmutableList.of(new HttpHost("localhost", PORT, "http")),
                getHosts("http://localhost"));
        assertEquals(ImmutableList.of(new HttpHost("localhost", 9200, "http")),
                getHosts("http://localhost:9200"));
        assertEquals(ImmutableList.of(new HttpHost("localhost", 9201, "http")),
                getHosts("http://localhost:9201"));

        assertEquals(ImmutableList.of(new HttpHost("localhost", PORT, "https")),
                getHosts("https://localhost"));
        assertEquals(ImmutableList.of(new HttpHost("localhost", 9200, "https")),
                getHosts("https://localhost:9200"));
        assertEquals(ImmutableList.of(new HttpHost("localhost", 9201, "https")),
                getHosts("https://localhost:9201"));

        assertEquals(ImmutableList.of(
                new HttpHost("somehost.somedomain", PORT, "http"),
                new HttpHost("anotherhost.somedomain", PORT, "http")),
                getHosts("somehost.somedomain:9200,anotherhost.somedomain:9200"));
        assertEquals(ImmutableList.of(
                new HttpHost("somehost.somedomain", PORT, "https"),
                new HttpHost("anotherhost.somedomain", PORT, "https")),
                getHosts("https://somehost.somedomain:9200,https://anotherhost.somedomain:9200"));
        assertEquals(ImmutableList.of(
                new HttpHost("somehost.somedomain", PORT, "https"),
                new HttpHost("anotherhost.somedomain", PORT, "https")),
                getHosts("https://somehost.somedomain:9200, https://anotherhost.somedomain:9200"));
        assertEquals(ImmutableList.of(
                new HttpHost("somehost.somedomain", PORT, "https"),
                new HttpHost("anotherhost.somedomain", PORT, "http")),
                getHosts("https://somehost.somedomain:9200,anotherhost.somedomain:9200"));
    }

    private List<HttpHost> getHosts(String hosts) throws IOException {
        Map<String,Serializable> params = createConnectionParams();
        params.put(ElasticDataStoreFactory.HOSTNAME.key, hosts);
        ElasticDataStoreFactory factory = new ElasticDataStoreFactory();
        return factory.createRestClient(params).getNodes().stream().map(Node::getHost).collect(Collectors.toList());
    }

    private FactoryRegistry getServiceRegistry() {
        return new FactoryCreator(
                Arrays.asList(new Class<?>[] { DataStoreFactorySpi.class }));
    }

    private void scanForPlugins() {
        getServiceRegistry().scanForPlugins();
    }

    private Iterator<DataStoreFactorySpi> getAvailableDataSources() {
        Set<DataStoreFactorySpi> availableDS = new HashSet<>();
        Stream<DataStoreFactorySpi> it = getServiceRegistry().getFactories(DataStoreFactorySpi.class, null, null);
        it.forEach(ds -> {
            if (ds instanceof ElasticDataStoreFactory) {
                final ElasticDataStoreFactory dsFactory = (ElasticDataStoreFactory) ds;
                if (dsFactory.isAvailable()) {
                    availableDS.add(dsFactory);
                }
            }
        });
        return availableDS.iterator();
    }

}
