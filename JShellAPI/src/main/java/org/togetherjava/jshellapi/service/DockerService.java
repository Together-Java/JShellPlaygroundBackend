package org.togetherjava.jshellapi.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;

import org.togetherjava.jshellapi.Config;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class DockerService implements DisposableBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerService.class);
    private static final String WORKER_LABEL = "jshell-api-worker";
    private static final UUID WORKER_UNIQUE_ID = UUID.randomUUID();

    private final DockerClient client;

    public DockerService(Config config) {
        DefaultDockerClientConfig clientConfig =
                DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient =
                new ApacheDockerHttpClient.Builder().dockerHost(clientConfig.getDockerHost())
                    .sslConfig(clientConfig.getSSLConfig())
                    .responseTimeout(Duration.ofSeconds(config.dockerResponseTimeout()))
                    .connectionTimeout(Duration.ofSeconds(config.dockerConnectionTimeout()))
                    .build();
        this.client = DockerClientImpl.getInstance(clientConfig, httpClient);

        cleanupLeftovers(WORKER_UNIQUE_ID);
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

    public String spawnContainer(long maxMemoryMegs, long cpus, @Nullable String cpuSetCpus,
            String name, Duration evalTimeout, long sysoutLimit) throws InterruptedException {
        String imageName = "togetherjava.org:5001/togetherjava/jshellwrapper";
        boolean presentLocally = client.listImagesCmd()
            .withFilter("reference", List.of(imageName))
            .exec()
            .stream()
            .flatMap(it -> Arrays.stream(it.getRepoTags()))
            .anyMatch(it -> it.endsWith(":master"));

        if (!presentLocally) {
            client.pullImageCmd(imageName)
                .withTag("master")
                .exec(new PullImageResultCallback())
                .awaitCompletion(5, TimeUnit.MINUTES);
        }

        return client.createContainerCmd(imageName + ":master")
            .withHostConfig(HostConfig.newHostConfig()
                .withAutoRemove(true)
                .withInit(true)
                .withCapDrop(Capability.ALL)
                .withNetworkMode("none")
                .withPidsLimit(2000L)
                .withReadonlyRootfs(true)
                .withMemory(maxMemoryMegs * 1024 * 1024)
                .withCpuCount(cpus)
                .withCpusetCpus(cpuSetCpus))
            .withStdinOpen(true)
            .withAttachStdin(true)
            .withAttachStderr(true)
            .withAttachStdout(true)
            .withEnv("evalTimeoutSeconds=" + evalTimeout.toSeconds(),
                    "sysOutCharLimit=" + sysoutLimit)
            .withLabels(Map.of(WORKER_LABEL, WORKER_UNIQUE_ID.toString()))
            .withName(name)
            .exec()
            .getId();
    }

    public InputStream startAndAttachToContainer(String containerId, InputStream stdin)
            throws IOException {
        PipedInputStream pipeIn = new PipedInputStream();
        PipedOutputStream pipeOut = new PipedOutputStream(pipeIn);

        client.attachContainerCmd(containerId)
            .withLogs(true)
            .withFollowStream(true)
            .withStdOut(true)
            .withStdErr(true)
            .withStdIn(stdin)
            .exec(new ResultCallback.Adapter<>() {
                @Override
                public void onNext(Frame object) {
                    try {
                        String payloadString =
                                new String(object.getPayload(), StandardCharsets.UTF_8);
                        if (object.getStreamType() == StreamType.STDOUT) {
                            pipeOut.write(object.getPayload());
                        } else {
                            LOGGER.warn("Received STDERR from container {}: {}", containerId,
                                    payloadString);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                }
            });

        client.startContainerCmd(containerId).exec();
        return pipeIn;
    }

    public void killContainerByName(String name) {
        LOGGER.debug("Fetching container to kill {}.", name);
        List<Container> containers = client.listContainersCmd().withNameFilter(Set.of(name)).exec();
        LOGGER.debug("Number of containers to kill: {} for name {}.", containers.size(), name);
        if (containers.size() != 1) {
            LOGGER.error("There is more than 1 container for name {}.", name);
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
        cleanupLeftovers(UUID.randomUUID());
        client.close();
    }
}
