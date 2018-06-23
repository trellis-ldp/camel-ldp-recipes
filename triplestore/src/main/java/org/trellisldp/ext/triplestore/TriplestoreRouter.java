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
package org.trellisldp.ext.triplestore;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.component.http4.HttpMethods.GET;
import static org.apache.camel.component.http4.HttpMethods.POST;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_OBJECT_ID;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_TYPE;
import static org.trellisldp.ext.triplestore.TriplestoreUtils.graphName;
import static org.trellisldp.ext.triplestore.TriplestoreUtils.sparqlUpdate;

import org.apache.camel.Exchange;
import org.apache.camel.NoSuchHeaderException;
import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.trellisldp.camel.ActivityStreamProcessor;

/**
 * A routing engine for indexing LDP content in a Triplestore.
 */
public class TriplestoreRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(TriplestoreRouter.class);

    @Override
    public void configure() throws Exception {
        from("{{input.stream}}").routeId("TrellisTriplestoreRouter")
            .unmarshal().json(Jackson)
            .process(new ActivityStreamProcessor())
            .filter(and(
                        header(ACTIVITY_STREAM_OBJECT_ID).isNotNull(),
                        simple("'{{triplestore.url}}' regex '^https?://.+'")))
                .choice()
                    .when(header(ACTIVITY_STREAM_TYPE).contains("Delete"))
                        .to("direct:delete.triplestore")
                    .otherwise()
                        .to("direct:fetch.resource");

        from("direct:delete.triplestore").routeId("TrellisTriplestoreDeleter")
            .log(INFO, LOGGER, "Deleting ${headers.ActivityStreamObjectId} from triplestore")
            .setHeader(HTTP_URI).simple("{{triplestore.url}}")
            .setHeader(HTTP_METHOD).constant(POST)
            .setHeader(CONTENT_TYPE).constant("application/x-www-form-urlencoded; charset=utf-8")
            .process(e -> e.getIn().setBody(sparqlUpdate(deleteAll(e))))
            .to("http4://localhost?useSystemProperties=true");

        from("direct:fetch.resource").routeId("TrellisResourceFetcher")
            .setHeader(HTTP_URI).header(ACTIVITY_STREAM_OBJECT_ID)
            .setHeader(HTTP_METHOD).constant(GET)
            .setHeader("Prefer").constant("{{prefer.header}}")
            .setHeader("Accept").constant("application/n-triples")
            .to("http4://localhost?useSystemProperties=true")
            .to("direct:update.triplestore");

        from("direct:update.triplestore").routeId("TrellisTriplestoreUpdater")
            .filter(header(CONTENT_TYPE).isEqualTo("application/n-triples"))
                .log(INFO, LOGGER, "Updating ${headers.ActivityStreamObjectId} in triplestore")
                .removeHeaders("CamelHttp*")
                .setHeader(HTTP_URI).simple("{{triplestore.url}}")
                .setHeader(HTTP_METHOD).constant(POST)
                .setHeader(CONTENT_TYPE).constant("application/x-www-form-urlencoded; charset=utf-8")
                .process(e -> e.getIn().setBody(sparqlUpdate(deleteAll(e) +
                                "INSERT DATA { GRAPH <" + graphName(e) + "> {" +
                                e.getIn().getBody(String.class) + "} };")))
                .to("http4://localhost?useSystemProperties=true");
    }

    private static String deleteAll(final Exchange exchange) throws NoSuchHeaderException {
        return "DELETE WHERE { GRAPH <" + graphName(exchange) + "> { ?s ?p ?o } };";
    }
}

