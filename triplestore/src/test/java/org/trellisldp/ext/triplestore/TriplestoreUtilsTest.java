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

import static org.junit.Assert.assertEquals;

import java.io.UncheckedIOException;

import org.junit.Test;

public class TriplestoreUtilsTest {

    @Test
    public void testEncode() {
        assertEquals("", TriplestoreUtils.encode(null, "foo"));
    }

    @Test(expected = UncheckedIOException.class)
    public void testNonexistentEncoding() {
        TriplestoreUtils.encode("a value", "non-existent-encoding");
    }

    @Test
    public void testEncodeRealValue() {
        assertEquals("a+value+with+spaces", TriplestoreUtils.encode("a value with spaces", "UTF-8"));
    }
}
