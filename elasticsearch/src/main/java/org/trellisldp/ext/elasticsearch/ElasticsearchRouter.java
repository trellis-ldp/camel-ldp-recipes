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
package org.trellisldp.ext.elasticsearch;

import static java.net.URLEncoder.encode;
import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_QUERY;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.LoggingLevel.INFO;
import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.component.http4.HttpMethods.DELETE;
import static org.apache.camel.component.http4.HttpMethods.GET;
import static org.apache.camel.component.http4.HttpMethods.PUT;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_OBJECT_ID;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_TYPE;

import org.apache.camel.builder.RouteBuilder;
import org.slf4j.Logger;
import org.trellisldp.camel.ActivityStreamProcessor;

/**
 * A routing engine for indexing LDP content in Elasticsearch.
 */
public class ElasticsearchRouter extends RouteBuilder {

    private static final Logger LOGGER = getLogger(ElasticsearchRouter.class);
    private static final String HTTP_ENDPOINT = "http4://localhost?useSystemProperties=true";
    private static final String CAMEL_HTTP_HEADERS = "CamelHttp*";

    @Override
    public void configure() throws Exception {
        from("{{input.stream}}").routeId("TrellisElasticsearchRouter")
            .unmarshal().json(Jackson)
            .process(new ActivityStreamProcessor())
            .filter(and(
                        header(ACTIVITY_STREAM_OBJECT_ID).isNotNull(),
                        simple("'{{ldpath.service.url}}' regex '^https?://.+'"),
                        simple("'{{elasticsearch.url}}' regex '^https?://.+'")))
                .process(e -> e.getIn().setHeader("ElasticSearchId",
                            encode(e.getIn().getHeader(ACTIVITY_STREAM_OBJECT_ID, String.class), "UTF-8")))
                .choice()
                    .when(header(ACTIVITY_STREAM_TYPE).contains("Delete"))
                        .to("direct:delete.elasticsearch")
                    .otherwise()
                        .to("direct:fetch.resource");

        from("direct:delete.elasticsearch").routeId("TrellisElasticsearchDeleter")
            .log(INFO, LOGGER, "Deleting ${headers.ActivityStreamObjectId} from elasticsearch")
            .removeHeaders(CAMEL_HTTP_HEADERS)
            .setHeader(HTTP_URI).simple("{{elasticsearch.url}}${headers.ElasticSearchId}")
            .setHeader(HTTP_METHOD).constant(DELETE)
            .to(HTTP_ENDPOINT);

        from("direct:fetch.resource").routeId("TrellisLdpathFormatter")
            .log(INFO, LOGGER, "Fetching resource via LDPath: ${headers.ActivityStreamObjectId}")
            .removeHeaders(CAMEL_HTTP_HEADERS)
            .setHeader(HTTP_URI).simple("{{ldpath.service.url}}")
            .setHeader(HTTP_METHOD).constant(GET)
            .setHeader(HTTP_QUERY).simple("url=${headers.ActivityStreamObjectId}&program={{ldpath.program.url}}")
            .to(HTTP_ENDPOINT)
            .to("direct:update.elasticsearch");

        from("direct:update.elasticsearch").routeId("TrellisElasticsearchUpdater")
            .removeHeaders(CAMEL_HTTP_HEADERS)
            .setHeader(HTTP_URI).simple("{{elasticsearch.url}}${headers.ElasticSearchId}")
            .setHeader(HTTP_METHOD).constant(PUT)
            .setHeader(CONTENT_TYPE).constant("application/json")
            .to(HTTP_ENDPOINT);
    }
}

