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
package org.trellisldp.ext.osgi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.configureConsole;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.karafDistributionConfiguration;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.keepRuntimeFolder;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.logLevel;
import static org.osgi.framework.Constants.OBJECTCLASS;
import static org.osgi.framework.FrameworkUtil.createFilter;

import java.io.File;

import javax.inject.Inject;

import org.apache.karaf.features.FeaturesService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.ConfigurationManager;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.karaf.options.LogLevelOption.LogLevel;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.component.ComponentException;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Test OSGi provisioning.
 */
@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class OSGiTest {

    @Inject
    protected FeaturesService featuresService;

    @Inject
    protected BundleContext bundleContext;

    @Configuration
    public Option[] config() {
        final ConfigurationManager cm = new ConfigurationManager();
        final String rmiRegistryPort = cm.getProperty("karaf.rmiRegistry.port");
        final String rmiServerPort = cm.getProperty("karaf.rmiServer.port");
        final String sshPort = cm.getProperty("karaf.ssh.port");
        final String jenaVersion = cm.getProperty("jena.version");
        final String camelVersion = cm.getProperty("camel.version");

        return new Option[] {
            karafDistributionConfiguration()
                .frameworkUrl(maven().groupId("org.apache.karaf").artifactId("apache-karaf")
                        .version(cm.getProperty("karaf.version")).type("zip"))
                .unpackDirectory(new File("build", "exam"))
                .useDeployFolder(false),
            logLevel(LogLevel.INFO),
            keepRuntimeFolder(),
            configureConsole().ignoreLocalConsole(),

            features(maven().groupId("org.apache.karaf.features").artifactId("standard")
                        .versionAsInProject().classifier("features").type("xml"), "scr"),
            features(maven().groupId("org.apache.camel.karaf").artifactId("apache-camel")
                        .version(camelVersion).classifier("features").type("xml")),
            features(maven().groupId("org.apache.jena").artifactId("jena-osgi-features")
                        .version(jenaVersion).classifier("features").type("xml")),
            features(maven().groupId("org.trellisldp.ext").artifactId("camel-ldp-karaf")
                        .type("xml").classifier("features").versionAsInProject()),

            editConfigurationFilePut("etc/org.ops4j.pax.url.mvn.cfg", "org.ops4j.pax.url.mvn.repositories",
                    "https://repo1.maven.org/maven2@id=central"),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiRegistryPort", rmiRegistryPort),
            editConfigurationFilePut("etc/org.apache.karaf.management.cfg", "rmiServerPort", rmiServerPort),
            editConfigurationFilePut("etc/org.apache.karaf.shell.cfg", "sshPort", sshPort)
       };
    }

    @Test
    public void testWebSubInstallation() throws Exception {
        assertFalse("camel-ldp-websub already installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-websub")));
        featuresService.installFeature("camel-ldp-websub");
        assertTrue("camel-ldp-websub not installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-websub")));
        featuresService.uninstallFeature("camel-ldp-websub");
        assertFalse("camel-ldp-websub still installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-websub")));
    }

    @Test
    public void testLDPathInstallation() throws Exception {
        assertFalse("camel-ldp-ldpath already installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-ldpath")));
        featuresService.installFeature("camel-ldp-ldpath");
        assertTrue("camel-ldp-ldpath not installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-ldpath")));
        featuresService.uninstallFeature("camel-ldp-ldpath");
        assertFalse("camel-ldp-ldpath still installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-ldpath")));
    }

    @Test
    public void testTriplestoreInstallation() throws Exception {
        assertFalse("camel-ldp-triplestore already installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-triplestore")));
        featuresService.installFeature("camel-ldp-triplestore");
        assertTrue("camel-ldp-triplestore not installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-triplestore")));
        featuresService.uninstallFeature("camel-ldp-triplestore");
        assertFalse("camel-ldp-triplestore still installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-triplestore")));
    }

    @Test
    public void testElasticSearchInstallation() throws Exception {
        assertFalse("camel-ldp-elasticsearch already installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-elasticsearch")));
        featuresService.installFeature("camel-ldp-elasticsearch");
        assertTrue("camel-ldp-elasticsearch not installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-elasticsearch")));
        featuresService.uninstallFeature("camel-ldp-elasticsearch");
        assertFalse("camel-ldp-elasticsearch still installed!",
                featuresService.isInstalled(featuresService.getFeature("camel-ldp-elasticsearch")));
    }

    protected <T> T getOsgiService(final Class<T> type, final String filter, final long timeout)
            throws InvalidSyntaxException, InterruptedException {
        final ServiceTracker<?, T> tracker = new ServiceTracker<>(bundleContext,
                createFilter("(&(" + OBJECTCLASS + "=" + type.getName() + ")" + filter + ")"), null);
        tracker.open(true);
        final T svc = tracker.waitForService(timeout);
        if (svc == null) {
            throw new ComponentException("Gave up waiting for service " + filter);
        }
        return svc;
    }
}
