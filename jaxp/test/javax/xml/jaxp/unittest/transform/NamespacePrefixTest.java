/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package transform;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;
import static jaxp.library.JAXPTestUtilities.runWithAllPerm;

/*
 * @test
 * @bug 8167179
 * @library /javax/xml/jaxp/libs
 * @run testng/othervm -DrunSecMngr=true transform.NamespacePrefixTest
 * @run testng/othervm transform.NamespacePrefixTest
 * @summary This class tests the generation of namespace prefixes
 */
public class NamespacePrefixTest {

    @Test
    public void testReuseTemplates() throws Exception {
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Source xslsrc = new StreamSource(new StringReader(XSL));
        final Templates tmpl = tf.newTemplates(xslsrc);
        for (int i = 0; i < TRANSF_COUNT; i++) {
            checkResult(doTransformation(tmpl.newTransformer()));
        }
    }

    @Test
    public void testReuseTransformer() throws Exception {
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Source xslsrc = new StreamSource(new StringReader(XSL));
        final Transformer t = tf.newTransformer(xslsrc);
        for (int i = 0; i < TRANSF_COUNT; i++) {
            checkResult(doTransformation(t));
        }
    }

    @Test
    public void testConcurrentTransformations() throws Exception {
        final TransformerFactory tf = TransformerFactory.newInstance();
        final Source xslsrc = new StreamSource(new StringReader(XSL));
        final Templates tmpl = tf.newTemplates(xslsrc);
        concurrentTestPassed.set(true);

        // Execute multiple TestWorker tasks
        for (int id = 0; id < THREADS_COUNT; id++) {
            EXECUTOR.execute(new TransformerThread(tmpl.newTransformer(), id));
        }
        // Initiate shutdown of previously submitted task
        runWithAllPerm(EXECUTOR::shutdown);
        // Wait for termination of submitted tasks
        if (!EXECUTOR.awaitTermination(THREADS_COUNT, TimeUnit.SECONDS)) {
            // If not all tasks terminates during the time out force them to shutdown
            runWithAllPerm(EXECUTOR::shutdownNow);
        }
        // Check if all transformation threads generated the correct namespace prefix
        assertTrue(concurrentTestPassed.get());
    }

    // Do one transformation with the provided transformer
    private static String doTransformation(Transformer t) throws Exception {
        StringWriter resWriter = new StringWriter();
        Source xmlSrc = new StreamSource(new StringReader(XML));
        t.transform(xmlSrc, new StreamResult(resWriter));
        return resWriter.toString();
    }

    // Check if the transformation result string contains the
    // element with the exact namespace prefix generated.
    private static void checkResult(String result) {
        // Check prefix of 'Element2' element, it should always be the same
        assertTrue(result.contains(EXPECTED_CONTENT));
    }

    // Check if the transformation result string contains the element with
    // the exact namespace prefix generated by current thread.
    // If the expected prefix is not found and there was no failures observed by
    // other test threads then mark concurrent test as failed.
    private static void checkThreadResult(String result, int id) {
        boolean res = result.contains(EXPECTED_CONTENT);
        System.out.printf("%d: transformation result: %s%n", id, res ? "Pass" : "Fail");
        if (!res) {
            System.out.printf("%d result:%s%n", id, result);
        }
        concurrentTestPassed.compareAndSet(true, res);
    }

    // TransformerThread task that does the transformation similar
    // to testReuseTransformer test method
    private class TransformerThread implements Runnable {

        private final Transformer transformer;
        private final int id;

        TransformerThread(Transformer transformer, int id) {
            this.transformer = transformer;
            this.id = id;
        }

        @Override
        public void run() {
            try {
                System.out.printf("%d: waiting for barrier%n", id);
                //Synchronize startup of all tasks
                BARRIER.await();
                System.out.printf("%d: starting transformation%n", id);
                checkThreadResult(doTransformation(transformer), id);
            } catch (Exception ex) {
                throw new RuntimeException("TransformerThread " + id + " failed", ex);
            }
        }
    }

    // Number of subsequent transformations
    private static final int TRANSF_COUNT = 10;

    // Number of transformer threads running concurently
    private static final int THREADS_COUNT = 10;

    // Variable for storing the concurrent transformation test result. It is
    // updated by transformer threads
    private static final AtomicBoolean concurrentTestPassed = new AtomicBoolean(true);

    // Cyclic barrier for threads startup synchronization
    private static final CyclicBarrier BARRIER = new CyclicBarrier(THREADS_COUNT);

    // Thread pool
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    // XSL that transforms XML and produces unique namespace prefixes for each element
    private final static String XSL = "<xsl:stylesheet version=\"1.0\" xmlns:xsl=\"http://www.w3.org/1999/XSL/Transform\">\n"
            + " <xsl:template match=\"node()|@*\" priority=\"1\">\n"
            + "     <xsl:copy>\n"
            + "       <xsl:apply-templates select=\"node()|@*\"/>\n"
            + "     </xsl:copy>\n"
            + " </xsl:template>\n"
            + " <xsl:template match=\"*\" priority=\"2\">\n"
            + "  <xsl:element name=\"{name()}\" namespace=\"{namespace-uri()}\">\n"
            + "   <xsl:apply-templates select=\"node()|@*\"/>\n"
            + "  </xsl:element>\n"
            + " </xsl:template>\n"
            + "</xsl:stylesheet>";

    // Simple XML content with root and two child elements
    private final static String XML = "<TestRoot xmlns=\"test.xmlns\">\n"
            + "  <Element1 xmlns=\"test.xmlns\">\n"
            + "  </Element1>\n"
            + "  <Element2 xmlns=\"test.xmlns\">\n"
            + "  </Element2>\n"
            + "</TestRoot>";

    // With thread local namespace prefix index each transformation result should
    // be the same and contain the same prefix for Element2
    private final static String EXPECTED_CONTENT = "</ns2:Element2>";

}
