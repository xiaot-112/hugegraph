/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.hugegraph.chaos.fault;

import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.dsl.ExecWatch;
import org.apache.hugegraph.chaos.model.ChaosConfig;
import org.apache.hugegraph.chaos.model.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

public class K8sFaultInjector implements FaultInjector {

    private static final Logger LOG = LogManager.getLogger(K8sFaultInjector.class);
    private final KubernetesClient k8sClient;
    private final String namespace;
    private final List<String> activeFaults = new ArrayList<>();
    private final Map<String, ExecWatch> activeExecWatches = new ConcurrentHashMap<>();

    public K8sFaultInjector(ChaosConfig.K8sTarget config) {
        String kubeconfig = config.getKubeconfig();
        if (kubeconfig != null && !kubeconfig.isEmpty()) {
            this.k8sClient = new KubernetesClientBuilder()
                    .withConfig(Config.fromKubeconfig(kubeconfig))
                    .build();
        } else {
            this.k8sClient = new KubernetesClientBuilder().build();
        }
        this.namespace = config.getNamespace() != null ? config.getNamespace() : "default";
    }

    @Override
    public void inject(Step.StepAction action) throws Exception {
        String type = action.getType();
        String target = action.getTarget();
        switch (type) {
            case "pod_kill":
                injectPodKill(target);
                break;
            case "network_delay":
                injectNetworkDelay(target, action);
                break;
            case "cpu_stress":
                injectCpuStress(target, action);
                break;
            case "memory_stress":
                injectMemoryStress(target, action);
                break;
            default:
                throw new UnsupportedOperationException("Unknown K8s fault type: " + type);
        }
        activeFaults.add(target + ":" + type);
    }

    @Override
    public void recover(Step.StepAction action) throws Exception {
        String type = action.getType();
        String target = action.getTarget();
        switch (type) {
            case "pod_kill":
                recoverPodKill(target);
                break;
            case "network_delay":
                recoverNetworkDelay(target);
                break;
            case "cpu_stress":
            case "memory_stress":
                recoverStress(target);
                break;
            default:
                LOG.warn("No recovery for K8s fault type: {}", type);
        }
        activeFaults.remove(target + ":" + type);
    }

    @Override
    public void healAll() {
        LOG.info("Healing all active K8s faults");
        for (String fault : new ArrayList<>(activeFaults)) {
            String[] parts = fault.split(":");
            if (parts.length == 2) {
                try {
                    Step.StepAction action = new Step.StepAction();
                    action.setType(parts[1]);
                    action.setTarget(parts[0]);
                    recover(action);
                } catch (Exception e) {
                    LOG.error("Failed to heal fault: {}", fault, e);
                }
            }
        }
    }

    private void injectPodKill(String podName) {
        LOG.info("Deleting pod: {}/{}", namespace, podName);
        try {
            k8sClient.pods().inNamespace(namespace).withName(podName).delete();
        } catch (Exception e) {
            LOG.warn("Failed to delete pod {}/{}, test will continue: {}",
                     namespace, podName, e.getMessage());
        }
    }

    private void recoverPodKill(String podName) {
        LOG.info("Pod {} will be recreated by K8s automatically", podName);
    }

    private void injectNetworkDelay(String podName, Step.StepAction action) {
        Object latencyObj = action.getParams().get("latency");
        int latency = latencyObj instanceof Number ? ((Number) latencyObj).intValue() : 100;
        LOG.info("Injecting network delay on pod {}/{}: {}ms", namespace, podName, latency);
        String cmd = String.format("tc qdisc add dev eth0 root netem delay %dms", latency);
        execInPod(podName, cmd);
    }

    private void recoverNetworkDelay(String podName) {
        LOG.info("Recovering network delay on pod {}/{}", namespace, podName);
        String cmd = "tc qdisc del dev eth0 root";
        execInPod(podName, cmd);
    }

    private void injectCpuStress(String podName, Step.StepAction action) {
        Object coresObj = action.getParams().get("cores");
        int cores = coresObj instanceof Number ? ((Number) coresObj).intValue() : 1;
        LOG.info("Injecting CPU stress on pod {}/{}: {} cores (background)",
                namespace, podName, cores);
        String cmd = String.format("stress-ng --cpu %d", cores);
        String key = podName + ":cpu";
        ExecWatch watch = execInPodAsync(podName, cmd);
        activeExecWatches.put(key, watch);
    }

    private void injectMemoryStress(String podName, Step.StepAction action) {
        Object percentObj = action.getParams().get("percent");
        int percent = percentObj instanceof Number ? ((Number) percentObj).intValue() : 50;
        LOG.info("Injecting memory stress on pod {}/{}: {}%% (background)",
                namespace, podName, percent);
        String cmd = String.format("stress-ng --vm 1 --vm-bytes %d%%", percent);
        String key = podName + ":memory";
        ExecWatch watch = execInPodAsync(podName, cmd);
        activeExecWatches.put(key, watch);
    }

    private void recoverStress(String podName) {
        LOG.info("Stopping stress on pod {}/{}", namespace, podName);
        String cpuKey = podName + ":cpu";
        String memKey = podName + ":memory";
        closeExecWatch(cpuKey);
        closeExecWatch(memKey);
        execInPod(podName, "pkill stress-ng || true");
    }

    private void closeExecWatch(String key) {
        ExecWatch watch = activeExecWatches.remove(key);
        if (watch != null) {
            try {
                watch.close();
            } catch (Exception e) {
                LOG.warn("Failed to close exec watch for {}", key, e);
            }
        }
    }

    private ExecWatch execInPodAsync(String podName, String command) {
        LOG.debug("Executing async in pod {}/{}: {}", namespace, podName, command);
        try {
            return k8sClient.pods().inNamespace(namespace).withName(podName)
                    .redirectingOutput()
                    .redirectingError()
                    .usingListener(new io.fabric8.kubernetes.client.dsl.ExecListener() {
                        @Override
                        public void onOpen() {
                            LOG.debug("Exec stream opened for pod {}/{}", namespace, podName);
                        }

                        @Override
                        public void onFailure(Throwable t, io.fabric8.kubernetes.client.dsl.ExecListener.Response response) {
                            LOG.warn("Exec stream failed for pod {}/{}: {}",
                                     namespace, podName, t.getMessage());
                        }

                        @Override
                        public void onClose(int code, String reason) {
                            LOG.debug("Exec stream closed for pod {}/{}: code={}, reason={}",
                                      namespace, podName, code, reason);
                        }
                    })
                    .exec("sh", "-c", command);
        } catch (Exception e) {
            LOG.error("Failed to start async exec in pod {}/{}", namespace, podName, e);
            return null;
        }
    }

    private void execInPod(String podName, String command) {
        LOG.debug("Executing in pod {}/{}: {}", namespace, podName, command);
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ByteArrayOutputStream err = new ByteArrayOutputStream();
            k8sClient.pods().inNamespace(namespace).withName(podName)
                    .writingOutput(out)
                    .writingError(err)
                    .withTTY()
                    .exec("sh", "-c", command);
            String output = out.toString();
            String error = err.toString();
            if (!output.isEmpty()) {
                LOG.debug("Pod exec output: {}", output);
            }
            if (!error.isEmpty()) {
                LOG.warn("Pod exec error: {}", error);
            }
        } catch (Exception e) {
            LOG.error("Failed to execute in pod {}/{}", namespace, podName, e);
        }
    }
}
