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
package org.trellisldp.ext.elasticsearch;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_QUERY;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.component.http.HttpMethods.DELETE;
import static org.apache.camel.component.http.HttpMethods.GET;
import static org.apache.camel.component.http.HttpMethods.PUT;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;

import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;

public class ElasticsearchRouterTest extends CamelBlueprintTestSupport {

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
        props.setProperty("elasticsearch.url", "https://elasticsearch.example.com/ldp/_doc/");
        props.setProperty("ldpath.service.url", "https://ldpath.example.com");
        props.setProperty("ldpath.program.url", "https://ldpath.example.com/program.ldpath");
        return props;
    }

    @Test
    public void testUpdate() throws Exception {
        AdviceWithRouteBuilder.adviceWith(context, "TrellisLdpathFormatter", builder -> {
            builder.mockEndpointsAndSkip("http*");
        });
        AdviceWithRouteBuilder.adviceWith(context, "TrellisElasticsearchUpdater", builder -> {
            builder.weaveAddLast().to("mock:results");
            builder.mockEndpointsAndSkip("http*");
        });
        context.start();

        getMockEndpoint("mock:http:localhost").expectedMessageCount(2);
        getMockEndpoint("mock:http:localhost").expectedHeaderValuesReceivedInAnyOrder(
                HTTP_METHOD, GET, PUT);

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");
        resultEndpoint.expectedHeaderReceived(HTTP_URI,
                "https://elasticsearch.example.com/ldp/_doc/https%3A%2F%2Fldp.example.com%2Fresource");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, PUT);

        template.sendBody("seda:input", loadResourceAsStream("create.jsonld"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testFetch() throws Exception {
        AdviceWithRouteBuilder.adviceWith(context, "TrellisLdpathFormatter", builder -> {
            builder.mockEndpointsAndSkip("http*");
            builder.mockEndpointsAndSkip("direct:update.elasticsearch");
            builder.weaveAddLast().to("mock:results");
        });
        context.start();

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(HTTP_URI, "https://ldpath.example.com");
        resultEndpoint.expectedHeaderReceived(HTTP_QUERY,
                "url=https://ldp.example.com/resource&program=https://ldpath.example.com/program.ldpath");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, GET);

        template.sendBody("seda:input", loadResourceAsStream("create.jsonld"));

        assertMockEndpointsSatisfied();

    }

    @Test
    public void testDelete() throws Exception {
        AdviceWithRouteBuilder.adviceWith(context, "TrellisElasticsearchDeleter", builder -> {
            builder.weaveAddLast().to("mock:results");
            builder.mockEndpointsAndSkip("http*");
        });
        context.start();

        getMockEndpoint("mock:http:localhost").expectedMessageCount(1);

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(HTTP_URI,
                "https://elasticsearch.example.com/ldp/_doc/https%3A%2F%2Fldp.example.com%2Fresource");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, DELETE);

        template.sendBody("seda:input", loadResourceAsStream("delete.jsonld"));

        assertMockEndpointsSatisfied();
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

