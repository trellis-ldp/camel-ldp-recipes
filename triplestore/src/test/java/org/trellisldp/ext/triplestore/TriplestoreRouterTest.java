/*
 * Copyright (c) 2021 Aaron Coburn and individual contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.ext.triplestore;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.component.http.HttpMethods.POST;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_OBJECT_ID;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;

public class TriplestoreRouterTest extends CamelBlueprintTestSupport {

    @EndpointInject(uri = "mock:results")
    protected MockEndpoint resultEndpoint;

    @Produce(uri = "direct:start")
    protected ProducerTemplate template;

    @Override
    public boolean isUseAdviceWith() {
        return true;
    }

    @Override
    public boolean isUseRouteBuilder() {
        return false;
    }

    @Override
    protected String getBlueprintDescriptor() {
        return "/OSGI-INF/blueprint/blueprint.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final Properties props = new Properties();
        props.setProperty("input.stream", "seda:input");
        props.setProperty("triplestore.url", "https://triplestore.example.com");
        return props;
    }

    @Test
    public void testUpdate() throws Exception {
        AdviceWith.adviceWith(context, "TrellisTriplestoreUpdater", builder -> {
            builder.weaveAddLast().to("mock:results");
            builder.mockEndpointsAndSkip("http*");
        });
        context.start();

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        resultEndpoint.expectedHeaderReceived(HTTP_URI, "https://triplestore.example.com");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, POST);

        final Map<String, Object> headers = new HashMap<>();
        headers.put(CONTENT_TYPE, "application/n-triples");
        headers.put(ACTIVITY_STREAM_OBJECT_ID, "https://ldp.example.com/resource");
        final String body = "<http://example.com> a <http://example.com/Type> .";
        template.sendBodyAndHeaders("direct:update.triplestore", body, headers);

        assertMockEndpointsSatisfied();

        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
        assertTrue(result.contains(TriplestoreUtils.encode(
                        "DELETE WHERE { GRAPH <https://ldp.example.com/resource> { ?s ?p ?o } };", "UTF-8")));
        assertTrue(result.contains(TriplestoreUtils.encode(
                        "INSERT DATA { GRAPH <https://ldp.example.com/resource> {" + body + "} };", "UTF-8")));
        assertTrue(result.startsWith("update=DELETE"));
    }

    @Test
    public void testDelete() throws Exception {
        AdviceWith.adviceWith(context, "TrellisTriplestoreDeleter", builder -> {
            builder.weaveAddLast().to("mock:results");
            builder.mockEndpointsAndSkip("http*");
        });
        context.start();

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded; charset=utf-8");
        resultEndpoint.expectedHeaderReceived(HTTP_URI, "https://triplestore.example.com");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, POST);

        template.sendBody("seda:input", loadResourceAsStream("delete.jsonld"));

        assertMockEndpointsSatisfied();

        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
        assertEquals(TriplestoreUtils.sparqlUpdate(
                        "DELETE WHERE { GRAPH <https://www.example.com/resource> { ?s ?p ?o } };"), result);
    }

    @Test
    public void testNotActivityMessage() throws Exception {
        context.start();

        resultEndpoint.expectedMessageCount(0);

        template.sendBody("seda:input", "{\"foo\": \"bar\"}");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testNotJson() throws Exception {
        context.start();

        resultEndpoint.expectedMessageCount(0);

        template.sendBody("seda:input", "foo = bar");

        assertMockEndpointsSatisfied();
    }
}

