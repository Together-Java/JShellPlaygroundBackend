package org.togetherjava.jshellapi.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

import org.togetherjava.jshellapi.Config;
import org.togetherjava.jshellapi.dto.ContainerState;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

@Service
public class DockerService implements DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerService.class);
    private static final String WORKER_LABEL = "jshell-api-worker";
    private static final UUID WORKER_UNIQUE_ID = UUID.randomUUID();

    private final DockerClient client;
    private final Config config;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final ConcurrentHashMap<StartupScriptId, String> cachedContainers =
            new ConcurrentHashMap<>();
    private final StartupScriptsService startupScriptsService;

    private final String jshellWrapperBaseImageName;

    public DockerService(Config config, StartupScriptsService startupScriptsService)
            throws InterruptedException {
        this.startupScriptsService = startupScriptsService;
        DefaultDockerClientConfig clientConfig =
                DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient =
                new ApacheDockerHttpClient.Builder().dockerHost(clientConfig.getDockerHost())
                    .sslConfig(clientConfig.getSSLConfig())
                    .responseTimeout(Duration.ofSeconds(config.dockerResponseTimeout()))
                    .connectionTimeout(Duration.ofSeconds(config.dockerConnectionTimeout()))
                    .build();
        this.client = DockerClientImpl.getInstance(clientConfig, httpClient);
        this.config = config;

        this.jshellWrapperBaseImageName =
                config.jshellWrapperImageName().split(Config.JSHELL_WRAPPER_IMAGE_NAME_TAG)[0];

        if (!isImagePresentLocally()) {
            pullImage();
        }
        cleanupLeftovers(WORKER_UNIQUE_ID);
        executor.submit(() -> initializeCachedContainer(StartupScriptId.EMPTY));
    }

    private void cleanupLeftovers(UUID currentId) {
        for (Container container : client.listContainersCmd()
            .withLabelFilter(Set.of(WORKER_LABEL))
            .exec()) {
            String containerHumanName =
                    container.getId() + " " + Arrays.toString(container.getNames());
            LOGGER.info("Found worker container '{}'", containerHumanName);
            if (!container.getLabels().get(WORKER_LABEL).equals(currentId.toString())) {
                LOGGER.info("Killing container '{}'", containerHumanName);
                client.killContainerCmd(container.getId()).exec();
            }
        }
    }

    /**
     * Checks if the Docker image with the given name and tag is present locally.
     *
     * @return true if the image is present, false otherwise.
     */
    private boolean isImagePresentLocally() {
        return client.listImagesCmd()
            .withFilter("reference", List.of(jshellWrapperBaseImageName))
            .exec()
            .stream()
            .flatMap(it -> Arrays.stream(it.getRepoTags()))
            .anyMatch(it -> it.endsWith(Config.JSHELL_WRAPPER_IMAGE_NAME_TAG));
    }

    /**
     * Pulls the Docker image.
     */
    private void pullImage() throws InterruptedException {
        if (!isImagePresentLocally()) {
            client.pullImageCmd(jshellWrapperBaseImageName)
                .withTag("master")
                .exec(new PullImageResultCallback())
                .awaitCompletion(5, TimeUnit.MINUTES);
        }
    }

    /**
     * Creates a Docker container with the given name.
     *
     * @param name The name of the container to create.
     * @return The ID of the created container.
     */
    public String createContainer(String name) {
        HostConfig hostConfig = HostConfig.newHostConfig()
            .withAutoRemove(true)
            .withInit(true)
            .withCapDrop(Capability.ALL)
            .withNetworkMode("none")
            .withPidsLimit(2000L)
            .withReadonlyRootfs(true)
            .withMemory((long) config.dockerMaxRamMegaBytes() * 1024 * 1024)
            .withCpuCount((long) Math.ceil(config.dockerCPUsUsage()))
            .withCpusetCpus(config.dockerCPUSetCPUs());

        return client
            .createContainerCmd(jshellWrapperBaseImageName + Config.JSHELL_WRAPPER_IMAGE_NAME_TAG)
            .withHostConfig(hostConfig)
            .withStdinOpen(true)
            .withAttachStdin(true)
            .withAttachStderr(true)
            .withAttachStdout(true)
            .withEnv("evalTimeoutSeconds=" + config.evalTimeoutSeconds(),
                    "sysOutCharLimit=" + config.sysOutCharLimit())
            .withLabels(Map.of(WORKER_LABEL, WORKER_UNIQUE_ID.toString()))
            .withName(name)
            .exec()
            .getId();
    }

    /**
     * Spawns a new Docker container with specified configurations.
     *
     * @param name Name of the container.
     * @param startupScriptId Script to initialize the container with.
     * @return The ContainerState of the newly created container.
     */
    public ContainerState initializeContainer(String name, StartupScriptId startupScriptId)
            throws IOException {
        if (startupScriptId == null || cachedContainers.isEmpty()
                || !cachedContainers.containsKey(startupScriptId)) {
            String containerId = createContainer(name);
            return setupContainerWithScript(containerId, false, startupScriptId);
        }
        String containerId = cachedContainers.get(startupScriptId);
        executor.submit(() -> initializeCachedContainer(startupScriptId));

        client.renameContainerCmd(containerId).withName(name).exec();
        return setupContainerWithScript(containerId, true, startupScriptId);
    }

    /**
     * Initializes a new cached docker container with specified configurations.
     *
     * @param startupScriptId Script to initialize the container with.
     */
    private void initializeCachedContainer(StartupScriptId startupScriptId) {
        String containerName = cachedContainerName();
        String id = createContainer(containerName);
        startContainer(id);

        try (PipedInputStream containerInput = new PipedInputStream();
                BufferedWriter writer = new BufferedWriter(
                        new OutputStreamWriter(new PipedOutputStream(containerInput)))) {
            InputStream containerOutput = attachToContainer(id, containerInput, true);

            writer.write(Utils.sanitizeStartupScript(startupScriptsService.get(startupScriptId)));
            writer.newLine();
            writer.flush();
            containerOutput.close();

            cachedContainers.put(startupScriptId, id);
        } catch (IOException e) {
            killContainerByName(containerName);
            throw new RuntimeException(e);
        }
    }

    /**
     *
     * @param containerId The id of the container
     * @param isCached Indicator if the container is cached or new
     * @param startupScriptId The startup script id of the session
     * @return ContainerState of the spawned container.
     * @throws IOException if an I/O error occurs
     */
    private ContainerState setupContainerWithScript(String containerId, boolean isCached,
            StartupScriptId startupScriptId) throws IOException {
        if (!isCached) {
            startContainer(containerId);
        }
        PipedInputStream containerInput = new PipedInputStream();
        BufferedWriter writer =
                new BufferedWriter(new OutputStreamWriter(new PipedOutputStream(containerInput)));

        InputStream containerOutput = attachToContainer(containerId, containerInput, false);
        BufferedReader reader = new BufferedReader(new InputStreamReader(containerOutput));

        if (!isCached) {
            writer.write(Utils.sanitizeStartupScript(startupScriptsService.get(startupScriptId)));
            writer.newLine();
            writer.flush();
        }

        return new ContainerState(isCached, containerId, reader, writer);
    }

    /**
     * Creates a new container
     *
     * @param containerId the ID of the container to start
     */
    public void startContainer(String containerId) {
        if (!isContainerRunning(containerId)) {
            client.startContainerCmd(containerId).exec();
        }
    }

    /**
     * Attaches to a running Docker container's input (stdin) and output streams (stdout, stderr).
     * Logs any output from stderr and returns an InputStream to read stdout.
     *
     * @param containerId The ID of the running container to attach to.
     * @param containerInput The input stream (containerInput) to send to the container.
     * @param isCached Indicator if the container is cached to prevent writing to output stream.
     * @return InputStream to read the container's stdout
     * @throws IOException if an I/O error occurs
     */
    public InputStream attachToContainer(String containerId, InputStream containerInput,
            boolean isCached) throws IOException {
        PipedInputStream pipeIn = new PipedInputStream();
        PipedOutputStream pipeOut = new PipedOutputStream(pipeIn);

        client.attachContainerCmd(containerId)
            .withLogs(true)
            .withFollowStream(true)
            .withStdOut(true)
            .withStdErr(true)
            .withStdIn(containerInput)
            .exec(new ResultCallback.Adapter<>() {
                @Override
                public void onNext(Frame object) {
                    try {
                        String payloadString =
                                new String(object.getPayload(), StandardCharsets.UTF_8);
                        if (object.getStreamType() == StreamType.STDOUT) {
                            if (!isCached) {
                                pipeOut.write(object.getPayload()); // Write stdout data to pipeOut
                            }
                        } else {
                            LOGGER.warn("Received STDERR from container {}: {}", containerId,
                                    payloadString);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });

        return pipeIn;
    }

    /**
     * Checks if the Docker container with the given ID is currently running.
     *
     * @param containerId the ID of the container to check
     * @return true if the container is running, false otherwise
     */
    public boolean isContainerRunning(String containerId) {
        InspectContainerResponse containerResponse = client.inspectContainerCmd(containerId).exec();
        return Boolean.TRUE.equals(containerResponse.getState().getRunning());
    }

    private String cachedContainerName() {
        return "cached_session_" + UUID.randomUUID();
    }

    public void killContainerByName(String name) {
        LOGGER.debug("Fetching container to kill {}.", name);
        List<Container> containers = client.listContainersCmd().withNameFilter(Set.of(name)).exec();
        if (containers.size() == 1) {
            LOGGER.debug("Found 1 container for name {}.", name);
        } else {
            LOGGER.error("Expected 1 container but found {} for name {}.", containers.size(), name);
        }
        for (Container container : containers) {
            client.killContainerCmd(container.getId()).exec();
        }
    }

    public boolean isDead(String containerName) {
        return client.listContainersCmd().withNameFilter(Set.of(containerName)).exec().isEmpty();
    }

    @Override
    public void destroy() throws Exception {
        LOGGER.info("destroy() called. Destroying all containers...");
        executor.shutdown();
        cleanupLeftovers(UUID.randomUUID());
        client.close();
    }
}
