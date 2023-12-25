package org.togetherjava.jshellapi.service;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.PullImageResultCallback;
import com.github.dockerjava.api.model.*;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.httpclient5.ApacheDockerHttpClient;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class DockerService implements AutoCloseable {

    private static final String WORKER_LABEL = "jshell-api-worker";
    private static final UUID WORKER_UNIQUE_ID = UUID.randomUUID();

    private final DockerClient client;

    public DockerService() {
        DefaultDockerClientConfig clientConfig = DefaultDockerClientConfig.createDefaultConfigBuilder().build();
        ApacheDockerHttpClient httpClient = new ApacheDockerHttpClient.Builder()
                .dockerHost(clientConfig.getDockerHost())
                .sslConfig(clientConfig.getSSLConfig())
                .responseTimeout(Duration.ofSeconds(60))
                .connectionTimeout(Duration.ofSeconds(60))
                .build();
        this.client = DockerClientImpl.getInstance(clientConfig, httpClient);

        cleanupLeftovers();
    }

    private void cleanupLeftovers() {
        for (Container container : client.listContainersCmd().withLabelFilter(Set.of(WORKER_LABEL)).exec()) {
            String containerHumanName = container.getId() + " " + Arrays.toString(container.getNames());
            System.out.println("Found worker container " + containerHumanName);
            if (!container.getLabels().get(WORKER_LABEL).equals(WORKER_UNIQUE_ID.toString())) {
                System.out.println("Killing container " + containerHumanName);
                client.killContainerCmd(container.getId()).exec();
            }
        }
    }

    public String spawnContainer(
            long maxMemoryMegs, long cpus, String name, Duration evalTimeout, long sysoutLimit
    ) throws InterruptedException {
        String imageName = "togetherjava.org:5001/togetherjava/jshellwrapper";
        boolean presentLocally = client.listImagesCmd()
                .withImageNameFilter(imageName)
                .exec()
                .stream()
                .anyMatch(it -> Arrays.asList(it.getRepoTags()).contains("master"));

        if (!presentLocally) {
            client.pullImageCmd(imageName)
                    .withTag("master")
                    .exec(new PullImageResultCallback())
                    .awaitCompletion(5, TimeUnit.MINUTES);
        }

        return client.createContainerCmd(
                        imageName + ":master"
                )
                .withHostConfig(
                        HostConfig.newHostConfig()
                                .withAutoRemove(true)
                                .withInit(true)
                                .withCapDrop(Capability.ALL)
                                .withNetworkMode("none")
                                .withPidsLimit(2000L)
                                .withReadonlyRootfs(true)
                                .withMemory(maxMemoryMegs * 1024 * 1024)
                                .withCpuCount(cpus)
                )
                .withStdinOpen(true)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .withEnv("evalTimeoutSeconds=" + evalTimeout.toSeconds(), "sysOutCharLimit=" + sysoutLimit)
                .withLabels(Map.of(WORKER_LABEL, WORKER_UNIQUE_ID.toString()))
                .withName(name)
                .exec()
                .getId();
    }

    public InputStream startAndAttachToContainer(String containerId, InputStream stdin) throws IOException {
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
                            String payloadString = new String(object.getPayload(), StandardCharsets.UTF_8);
                            if (object.getStreamType() == StreamType.STDOUT) {
                                pipeOut.write(object.getPayload());
                            } else {
                                System.err.println(":( " + payloadString);
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
        for (Container container : client.listContainersCmd().withNameFilter(Set.of(name)).exec()) {
            client.killContainerCmd(container.getId()).exec();
        }
    }

    @Override
    public void close() throws Exception {
        client.close();
    }

    public boolean isDead(String containerName) {
        return client.listContainersCmd().withNameFilter(Set.of(containerName)).exec().isEmpty();
    }
}
