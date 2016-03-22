import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.ConfigurationProperties;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.impl.DefaultServiceLocator;
import org.eclipse.aether.repository.*;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.JavaScopes;
import org.eclipse.aether.util.graph.transformer.ConflictResolver;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;

import static java.util.Collections.unmodifiableMap;

public final class AetherConflictRes {
    public static void main(String[] args) throws DependencyResolutionException {

        final List<Dependency> deps = new ArrayList<>();
        deps.add(new Dependency (
            new DefaultArtifact("ch.qos.logback", "logback-classic", null, "jar", "1.1.6"),
            JavaScopes.COMPILE,
            false
        ));
        deps.add(new Dependency (
            new DefaultArtifact("org.slf4j", "slf4j-simple", null, "jar", "1.7.19"),
            JavaScopes.COMPILE,
            false
        ));

        System.err.println("\nResolving: " + deps);
        final RepositorySystem system = newRepositorySystem();
        final RepositorySystemSession session = newRepositorySession(system, new LocalRepository(getLocalRepoHome()));
        initRepos(session);
        final CollectRequest req = new CollectRequest().setRepositories(repos).setDependencies(deps);
        final DependencyRequest dependencyRequest = new DependencyRequest(req, null);
        final DependencyResult result = system.resolveDependencies(session, dependencyRequest);
        System.err.println("\nResult: " + result + "\n");
    }

    private static File getLocalRepoHome() {
        return Paths.get(System.getProperty("user.home"), ".m2").resolve("repository").toFile();
    }

    private static List<RemoteRepository> repos = new ArrayList<>();
    private static final Map<String, String> WELL_KNOWN_REPOS = unmodifiableMap(new HashMap<String, String>() {
        {
            put("central", "https://repo1.maven.org/maven2/");
            put("central-http", "http://repo1.maven.org/maven2/");
            put("jcenter", "https://jcenter.bintray.com/");
            put("jcenter-http", "http://jcenter.bintray.com/");
            put("local", "file:" + getLocalRepoHome());
        }
    });
    private static final RepositoryPolicy p =
        new RepositoryPolicy(true, RepositoryPolicy.UPDATE_POLICY_NEVER, RepositoryPolicy.CHECKSUM_POLICY_WARN);
    private static void initRepos(RepositorySystemSession session) {
        for (final String r : WELL_KNOWN_REPOS.values()) {
            final ProxySelector selector = session.getProxySelector();
            RemoteRepository repo = createRepo(r, p, p);
            final Proxy proxy = selector.getProxy(repo);
            if (proxy != null)
                repo = new RemoteRepository.Builder(repo).setProxy(proxy).build();
            repos.add(repo);
        }
    }

    private static RemoteRepository createRepo(String repo, RepositoryPolicy releasePolicy, RepositoryPolicy snapshotPolicy) {
        if (repo.startsWith("file:")) {
            releasePolicy = new RepositoryPolicy(releasePolicy.isEnabled(), releasePolicy.getUpdatePolicy(), RepositoryPolicy.CHECKSUM_POLICY_IGNORE);
            snapshotPolicy = releasePolicy;
        }
        return new RemoteRepository.Builder(repo, "default", repo).setReleasePolicy(releasePolicy).setSnapshotPolicy(snapshotPolicy).build();
    }

    private static RepositorySystem newRepositorySystem() {
        /*
         * We're using DefaultServiceLocator rather than Guice/Sisu because it's more lightweight.
         * This method pulls together the necessary Aether components and plugins.
         */
        final DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();

        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);

        return locator.getService(RepositorySystem.class);
    }

    private static RepositorySystemSession newRepositorySession(RepositorySystem system, LocalRepository localRepo) {
        final DefaultRepositorySystemSession s = MavenRepositorySystemUtils.newSession();

        s.setConfigProperty(ConfigurationProperties.CONNECT_TIMEOUT, 60000);
        s.setConfigProperty(ConfigurationProperties.REQUEST_TIMEOUT, 60000);
        s.setConfigProperty(ConflictResolver.CONFIG_PROP_VERBOSE, true);

        s.setOffline(false);
        s.setUpdatePolicy(RepositoryPolicy.UPDATE_POLICY_ALWAYS);

        s.setLocalRepositoryManager(system.newLocalRepositoryManager(s, localRepo));

        s.setDependencyGraphTransformer(newConflictResolver());

        return s;
    }

    private static ConflictResolver newConflictResolver() {
        return new ConflictResolver (
            new org.eclipse.aether.util.graph.transformer.NearestVersionSelector(),
            new org.eclipse.aether.util.graph.transformer.JavaScopeSelector(),
            new org.eclipse.aether.util.graph.transformer.SimpleOptionalitySelector(),
            new org.eclipse.aether.util.graph.transformer.JavaScopeDeriver()
        );
    }

    private AetherConflictRes() {}
}
