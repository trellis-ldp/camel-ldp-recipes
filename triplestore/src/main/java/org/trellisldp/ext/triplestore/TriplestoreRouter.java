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
import static org.apache.camel.component.http.HttpConstants.CONTENT_TYPE_WWW_FORM_URLENCODED;
import static org.apache.camel.component.http.HttpMethods.GET;
import static org.apache.camel.component.http.HttpMethods.POST;
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

    private static final String DELETE_FROM_TRIPLESTORE = "direct:delete.triplestore";
    private static final String FETCH_RESOURCE = "direct:fetch.resource";
    private static final String UPDATE_TRIPLESTORE = "direct:update.triplestore";
    private static final String HTTP_ENDPOINT = "http://localhost?useSystemProperties=true";
    private static final String CONTENT_TYPE_N_TRIPLES = "application/n-triples";
    private static final String CHARSET_UTF_8 = "; charset=utf-8";
    private static final String PREFER = "Prefer";
    private static final String ACCEPT = "Accept";

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
                        .to(DELETE_FROM_TRIPLESTORE)
                    .otherwise()
                        .to(FETCH_RESOURCE);

        from(DELETE_FROM_TRIPLESTORE).routeId("TrellisTriplestoreDeleter")
            .log(INFO, LOGGER, "Deleting ${headers.ActivityStreamObjectId} from triplestore")
            .setHeader(HTTP_URI).simple("{{triplestore.url}}")
            .setHeader(HTTP_METHOD).constant(POST)
            .setHeader(CONTENT_TYPE).constant(CONTENT_TYPE_WWW_FORM_URLENCODED + CHARSET_UTF_8)
            .process(e -> e.getIn().setBody(sparqlUpdate(deleteAll(e))))
            .to(HTTP_ENDPOINT);

        from(FETCH_RESOURCE).routeId("TrellisResourceFetcher")
            .setHeader(HTTP_URI).header(ACTIVITY_STREAM_OBJECT_ID)
            .setHeader(HTTP_METHOD).constant(GET)
            .setHeader(PREFER).constant("{{prefer.header}}")
            .setHeader(ACCEPT).constant(CONTENT_TYPE_N_TRIPLES)
            .to(HTTP_ENDPOINT)
            .to(UPDATE_TRIPLESTORE);

        from(UPDATE_TRIPLESTORE).routeId("TrellisTriplestoreUpdater")
            .filter(header(CONTENT_TYPE).isEqualTo(CONTENT_TYPE_N_TRIPLES))
                .log(INFO, LOGGER, "Updating ${headers.ActivityStreamObjectId} in triplestore")
                .removeHeaders("CamelHttp*")
                .setHeader(HTTP_URI).simple("{{triplestore.url}}")
                .setHeader(HTTP_METHOD).constant(POST)
                .setHeader(CONTENT_TYPE).constant(CONTENT_TYPE_WWW_FORM_URLENCODED + CHARSET_UTF_8)
                .process(e -> e.getIn().setBody(sparqlUpdate(deleteAll(e) +
                                "INSERT DATA { GRAPH <" + graphName(e) + "> {" +
                                e.getIn().getBody(String.class) + "} };")))
                .to(HTTP_ENDPOINT);
    }

    private static String deleteAll(final Exchange exchange) throws NoSuchHeaderException {
        return "DELETE WHERE { GRAPH <" + graphName(exchange) + "> { ?s ?p ?o } };";
    }
}

