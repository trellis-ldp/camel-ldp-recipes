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

import static org.apache.marmotta.ldpath.model.Constants.NS_LMF_FUNCS;

import com.github.jsonldjava.sesame.SesameJSONLDParserFactory;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Map;

import org.apache.marmotta.ldpath.LDPath;
import org.apache.marmotta.ldpath.api.backend.RDFBackend;
import org.apache.marmotta.ldpath.exception.LDPathParseException;
import org.apache.marmotta.ldpath.model.functions.CountFunction;
import org.apache.marmotta.ldpath.model.functions.FirstFunction;
import org.apache.marmotta.ldpath.model.functions.LastFunction;
import org.apache.marmotta.ldpath.model.functions.SortFunction;
import org.apache.marmotta.ldpath.model.tests.functions.EqualTest;
import org.apache.marmotta.ldpath.model.tests.functions.NotEqualTest;
import org.apache.marmotta.ldpath.parser.Configuration;
import org.apache.marmotta.ldpath.parser.DefaultConfiguration;
import org.openrdf.model.Value;
import org.openrdf.model.impl.URIImpl;
import org.openrdf.query.resultio.BooleanQueryResultParserRegistry;
import org.openrdf.query.resultio.TupleQueryResultParserRegistry;
import org.openrdf.query.resultio.sparqlxml.SPARQLBooleanXMLParserFactory;
import org.openrdf.query.resultio.sparqlxml.SPARQLResultsXMLParserFactory;
import org.openrdf.rio.RDFParserRegistry;
import org.openrdf.rio.n3.N3ParserFactory;
import org.openrdf.rio.ntriples.NTriplesParserFactory;
import org.openrdf.rio.rdfxml.RDFXMLParserFactory;
import org.openrdf.rio.trig.TriGParserFactory;
import org.openrdf.rio.turtle.TurtleParserFactory;

public class LDPathHelper {

    final LDPath<Value> ldpath;

    /**
     * Create an LDPath helper class.
     * @param backend the LDCache backend
     */
    public LDPathHelper(final RDFBackend<Value> backend) {
        RDFParserRegistry.getInstance().add(new RDFXMLParserFactory());
        RDFParserRegistry.getInstance().add(new NTriplesParserFactory());
        RDFParserRegistry.getInstance().add(new TurtleParserFactory());
        RDFParserRegistry.getInstance().add(new N3ParserFactory());
        RDFParserRegistry.getInstance().add(new SesameJSONLDParserFactory());
        RDFParserRegistry.getInstance().add(new TriGParserFactory());
        BooleanQueryResultParserRegistry.getInstance().add(new SPARQLBooleanXMLParserFactory());
        TupleQueryResultParserRegistry.getInstance().add(new SPARQLResultsXMLParserFactory());

        final Configuration<Value> config = new DefaultConfiguration<>();
        config.addFunction(NS_LMF_FUNCS + "count", new CountFunction<>());
        config.addFunction(NS_LMF_FUNCS + "first", new FirstFunction<>());
        config.addFunction(NS_LMF_FUNCS + "last", new LastFunction<>());
        config.addFunction(NS_LMF_FUNCS + "sort", new SortFunction<>());
        config.addTestFunction(NS_LMF_FUNCS + "eq", new EqualTest<>());
        config.addTestFunction(NS_LMF_FUNCS + "ne", new NotEqualTest<>());
        this.ldpath = new LDPath<>(backend, config);
    }

    /**
     * Query the linked data cloud.
     * @param url the starting URL
     * @param program the LDPath program
     * @return a data collection of fields
     * @throws LDPathParseException on program failure
     */
    public Map<String, Collection<?>> programQuery(final String url, final InputStream program)
            throws LDPathParseException {
        return ldpath.programQuery(new URIImpl(url), new InputStreamReader(program));
    }
}

