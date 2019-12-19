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

import static org.apache.camel.support.ExchangeHelper.getMandatoryHeader;
import static org.trellisldp.camel.ActivityStreamProcessor.ACTIVITY_STREAM_OBJECT_ID;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.apache.camel.Exchange;
import org.apache.camel.NoSuchHeaderException;
import org.apache.jena.util.URIref;

final class TriplestoreUtils {

    public static String encode(final String input, final String encoding) {
        if (input != null) {
            try {
                return URLEncoder.encode(input, encoding);
            } catch (final UnsupportedEncodingException ex) {
                throw new UncheckedIOException("Invalid encoding: " + encoding, ex);
            }
        }
        return "";
    }

    public static String sparqlUpdate(final String command) {
        return "update=" + encode(command, "UTF-8");
    }

    public static String graphName(final Exchange exchange) throws NoSuchHeaderException {
        return URIref.encode(getMandatoryHeader(exchange, ACTIVITY_STREAM_OBJECT_ID, String.class));
    }

    private TriplestoreUtils() {
        // prevent instantiation
    }
}
