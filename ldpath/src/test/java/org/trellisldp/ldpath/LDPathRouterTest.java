/*
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
package org.trellisldp.ldpath;

import static java.util.Collections.emptyList;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.camel.EndpointInject;
import org.apache.camel.Produce;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.blueprint.CamelBlueprintTestSupport;
import org.junit.Test;

public class LDPathRouterTest extends CamelBlueprintTestSupport {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final int TEST_PORT = AvailablePortFinder.getNextAvailable();

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
        return "/OSGI-INF/blueprint/blueprint-test.xml";
    }

    @Override
    protected Properties useOverridePropertiesWithPropertiesComponent() {
        final Properties props = new Properties();
        props.setProperty("endpoint.port", Integer.toString(TEST_PORT));
        return props;
    }

    @Test
    public void testQuery() throws Exception {
        context.getRouteDefinition("TrellisLDPathQuery").adviceWith(context, new AdviceWithRouteBuilder() {
            @Override
            public void configure() throws Exception {
                weaveAddLast().to("mock:results");
            }
        });
        context.start();

        resultEndpoint.expectedMessageCount(1);
        resultEndpoint.expectedHeaderReceived(CONTENT_TYPE, "application/json");

        template.sendBodyAndHeader("direct:query", null, "url", "http://www.trellisldp.org/ns/trellis#");

        assertMockEndpointsSatisfied();

        final String result = resultEndpoint.getExchanges().get(0).getIn().getBody(String.class);

        final Map<String, List<String>> data = MAPPER.readValue(result,
                new TypeReference<Map<String, List<String>>>(){});

        assertFalse(data.isEmpty());
        assertTrue(data.getOrDefault("label", emptyList()).contains("Trellis Linked Data Server Vocabulary"));
        assertTrue(data.getOrDefault("type", emptyList()).contains("http://www.w3.org/2002/07/owl#Ontology"));
        assertTrue(data.getOrDefault("id", emptyList()).contains("http://www.trellisldp.org/ns/trellis#"));
    }
}

