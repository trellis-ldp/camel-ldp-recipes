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
package org.trellisldp.ext.websub;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.component.http.HttpMethods.POST;
import static org.apache.camel.util.ObjectHelper.loadResourceAsStream;
import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;

public class WebSubRouterTest extends CamelBlueprintTestSupport {

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
        props.setProperty("subscriber.url", "https://hub.example.com");
        return props;
    }

    @Test
    public void testUpdate() throws Exception {
        AdviceWith.adviceWith(context, "TrellisWebSubRouter", builder -> {
            builder.weaveAddLast().to("mock:results");
            builder.mockEndpointsAndSkip("http*");
        });
        context.start();

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(HTTP_URI, "https://hub.example.com");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, POST);

        template.sendBody("seda:input", loadResourceAsStream("update.jsonld"));

        assertMockEndpointsSatisfied();

        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
        assertEquals("hub.mode=\"publish\"&hub.url=http://www.example.com/resource", result);
    }

    @Test
    public void testCreate() throws Exception {
        AdviceWith.adviceWith(context, "TrellisWebSubRouter", builder -> {
            builder.weaveAddLast().to("mock:results");
            builder.mockEndpointsAndSkip("http*");
        });
        context.start();

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/x-www-form-urlencoded");
        resultEndpoint.expectedHeaderReceived(HTTP_URI, "https://hub.example.com");
        resultEndpoint.expectedHeaderReceived(HTTP_METHOD, POST);

        template.sendBody("seda:input", loadResourceAsStream("create.jsonld"));

        assertMockEndpointsSatisfied();

        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);
        assertEquals("hub.mode=\"publish\"&hub.url=http://www.example.com/a290e05a-c09e-4f10-bd0b-3dbcba425e77",
                result);
    }

    @Test
    public void testDelete() throws Exception {
        AdviceWith.adviceWith(context, "TrellisWebSubRouter", builder -> {
            builder.weaveAddLast().to("mock:results");
            builder.mockEndpointsAndSkip("http*");
        });
        context.start();

        resultEndpoint.expectedMessageCount(0);

        template.sendBody("seda:input", loadResourceAsStream("delete.jsonld"));

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testNotActivityMessage() throws Exception {
        AdviceWith.adviceWith(context, "TrellisWebSubRouter", builder -> {
            builder.weaveAddLast().to("mock:results");
            builder.mockEndpointsAndSkip("http*");
        });
        context.start();

        resultEndpoint.expectedMessageCount(0);

        template.sendBody("seda:input", "{\"foo\": \"bar\"}");

        assertMockEndpointsSatisfied();
    }

    @Test
    public void testNotJson() throws Exception {
        AdviceWith.adviceWith(context, "TrellisWebSubRouter", builder -> {
            builder.weaveAddLast().to("mock:results");
            builder.mockEndpointsAndSkip("http*");
        });
        context.start();

        resultEndpoint.expectedMessageCount(0);

        template.sendBody("seda:input", "foo = bar");

        assertMockEndpointsSatisfied();
    }
}

