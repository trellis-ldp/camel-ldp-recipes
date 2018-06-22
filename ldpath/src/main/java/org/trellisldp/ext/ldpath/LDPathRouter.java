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
package org.trellisldp.ext.ldpath;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;

import org.apache.camel.builder.RouteBuilder;

/**
 * An HTTP service router for an LDPath service.
 */
public class LDPathRouter extends RouteBuilder {

    private static final String PARSE_ROUTE = "direct:parse";
    private static final String QUERY_ROUTE = "direct:query";

    @Override
    public void configure() throws Exception {
        from("jetty:http://{{endpoint.host}}:{{endpoint.port}}{{endpoint.prefix}}?"
                + "httpMethodRestrict=GET,POST&"
                + "sendServerVersion=false")
            .routeId("TrellisLDPathService")
            .routeDescription("Route LDPath queries to Marmotta's LDPath service")
            .choice()
                .when(not(header("url").regex("^https?://.+")))
                    .setHeader(HTTP_RESPONSE_CODE).constant(400)
                    .setHeader(CONTENT_TYPE).constant("text/plain")
                    .transform(constant("Missing/invalid url parameter"))
                .when(header(HTTP_METHOD).isEqualTo("GET"))
                    .to(QUERY_ROUTE)
                .when(header(HTTP_METHOD).isEqualTo("POST"))
                    .to(PARSE_ROUTE);

        from(QUERY_ROUTE).routeId("TrellisLDPathQuery")
            .choice()
                .when(header("program").regex("^https?://.*"))
                    .removeHeaders("CamelHttp*")
                    .setHeader(HTTP_URI).header("program")
                    .to("http4://localhost?useSystemProperties=true")
                    .to(PARSE_ROUTE)
                .otherwise()
                    .to("{{ldpath.defaultProgram}}")
                    .to(PARSE_ROUTE);

        from(PARSE_ROUTE).routeId("TrellisLDPathParser")
            .to("direct:programQuery")
            .marshal().json(Jackson)
            .removeHeaders("*")
            .setHeader(CONTENT_TYPE).constant("application/json");
    }
}
