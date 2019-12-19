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
package org.trellisldp.ext.websub;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_METHOD;
import static org.apache.camel.Exchange.HTTP_URI;
import static org.apache.camel.builder.PredicateBuilder.and;
import static org.apache.camel.builder.PredicateBuilder.not;
import static org.apache.camel.component.http.HttpMethods.POST;
import static org.apache.camel.model.dataformat.JsonLibrary.Jackson;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_OBJECT_ID;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_TYPE;

import org.apache.camel.builder.RouteBuilder;
import org.trellisldp.camel.ActivityStreamProcessor;

/**
 * A routing engine for WebSub notifications from a producer.
 */
public class WebSubRouter extends RouteBuilder {

    @Override
    public void configure() throws Exception {
        from("{{input.stream}}").routeId("TrellisWebSubRouter")
            .unmarshal().json(Jackson)
            .process(new ActivityStreamProcessor())
            .setHeader(HTTP_URI).simple("{{subscriber.url}}")
            .filter(and(
                        not(header(ACTIVITY_STREAM_TYPE).contains("Delete")),
                        header(ACTIVITY_STREAM_OBJECT_ID).isNotNull(),
                        header(HTTP_URI).regex("^https?://.+")))
                .setHeader(HTTP_METHOD).constant(POST)
                .setHeader(CONTENT_TYPE).constant("application/x-www-form-urlencoded")
                .transform().simple("hub.mode=\"publish\"&hub.url=${header.ActivityStreamObjectId}")
                .to("http://localhost?useSystemProperties=true");
    }
}
