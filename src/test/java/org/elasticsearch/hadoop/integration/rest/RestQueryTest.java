/*
 * Copyright 2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.elasticsearch.hadoop.integration.rest;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

import org.elasticsearch.hadoop.cfg.ConfigurationOptions;
import org.elasticsearch.hadoop.cfg.Settings;
import org.elasticsearch.hadoop.rest.BufferedRestClient;
import org.elasticsearch.hadoop.rest.QueryBuilder;
import org.elasticsearch.hadoop.rest.ScrollQuery;
import org.elasticsearch.hadoop.rest.dto.Node;
import org.elasticsearch.hadoop.rest.dto.Shard;
import org.elasticsearch.hadoop.rest.dto.mapping.Field;
import org.elasticsearch.hadoop.serialization.JdkValueReader;
import org.elasticsearch.hadoop.serialization.JdkValueWriter;
import org.elasticsearch.hadoop.serialization.ScrollReader;
import org.elasticsearch.hadoop.util.TestSettings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 */
public class RestQueryTest {

    private BufferedRestClient client;
    private Settings settings;

    @Before
    public void start() throws IOException {
        settings = new TestSettings("rest/savebulk");
        //testSettings.setPort(9200)
        settings.setProperty(ConfigurationOptions.ES_SERIALIZATION_WRITER_CLASS, JdkValueWriter.class.getName());
        settings.setProperty(ConfigurationOptions.ES_SERIALIZATION_WRITER_CLASS, JdkValueWriter.class.getName());
        client = new BufferedRestClient(settings);
        client.waitForYellow();
    }

    @After
    public void stop() throws Exception {
        client.close();
    }

    @Test
    public void testShardInfo() throws Exception {
        Map<Shard, Node> shards = client.getTargetShards();
        System.out.println(shards);
        assertNotNull(shards);
    }

    @Test
    public void testQueryBuilder() throws Exception {
        Settings sets = settings.copy();
        sets.setProperty(ConfigurationOptions.ES_QUERY, "?q=me*");
        QueryBuilder qb = QueryBuilder.query(sets);
        Field mapping = client.getMapping();
        ScrollReader reader = new ScrollReader(new JdkValueReader(), mapping);

        int count = 0;
        for (ScrollQuery query = qb.build(client, reader); query.hasNext();) {
            Object[] next = query.next();
            assertNotNull(next);
            count++;
        }

        assertTrue(count > 0);
    }

    @Test
    public void testQueryShards() throws Exception {
        Map<Shard, Node> targetShards = client.getTargetShards();

        Field mapping = client.getMapping();
        ScrollReader reader = new ScrollReader(new JdkValueReader(), mapping);

        Settings sets = settings.copy();
        sets.setProperty(ConfigurationOptions.ES_QUERY, "?q=me*");

        String nodeId = targetShards.values().iterator().next().getId();
        ScrollQuery query = QueryBuilder.query(sets)
                .shard("0")
                .onlyNode(nodeId)
                .build(client, reader);

        int count = 0;
        for (; query.hasNext();) {
            Object[] next = query.next();
            System.out.println(Arrays.toString(next));
            assertNotNull(next);
            count++;
        }

        assertTrue(count > 0);
    }
}