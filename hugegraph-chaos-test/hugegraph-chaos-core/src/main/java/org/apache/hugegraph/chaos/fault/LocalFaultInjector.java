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

import org.apache.hugegraph.chaos.model.Step;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalFaultInjector implements FaultInjector {

    private static final Logger LOG = LogManager.getLogger(LocalFaultInjector.class);
    private final Map<String, Process> activeProcesses = new ConcurrentHashMap<>();

    @Override
    public void inject(Step.StepAction action) throws Exception {
        String type = action.getType();
        switch (type) {
            case "process_kill":
                injectProcessKill(action);
                break;
            case "network_delay":
                injectNetworkDelay(action);
                break;
            case "network_loss":
                injectNetworkLoss(action);
                break;
            case "cpu_stress":
                injectCpuStress(action);
                break;
            case "memory_stress":
                injectMemoryStress(action);
                break;
            default:
                throw new UnsupportedOperationException("Unknown fault type: " + type);
        }
    }

    @Override
    public void recover(Step.StepAction action) throws Exception {
        String type = action.getType();
        switch (type) {
            case "process_kill":
                recoverProcessKill(action);
                break;
            case "network_delay":
            case "network_loss":
                recoverNetwork(action);
                break;
            case "cpu_stress":
            case "memory_stress":
                recoverStress(action);
                break;
            default:
                LOG.warn("No recovery for fault type: {}", type);
        }
    }

    @Override
    public void healAll() {
        LOG.info("Healing all active faults");
        for (Map.Entry<String, Process> entry : new ArrayList<>(activeProcesses.entrySet())) {
            try {
                entry.getValue().destroyForcibly();
                activeProcesses.remove(entry.getKey());
            } catch (Exception e) {
                LOG.error("Failed to heal fault: {}", entry.getKey(), e);
            }
        }
        try {
            executeCommand("tc qdisc del dev lo root 2>/dev/null || true");
        } catch (Exception e) {
            LOG.warn("Failed to clean up network rules", e);
        }
    }

    private void injectProcessKill(Step.StepAction action) {
        String target = action.getTarget();
        LOG.info("Killing process: {}", target);
        try {
            ProcessHandle.allProcesses()
                         .filter(p -> p.info().commandLine().orElse("").contains(target))
                         .forEach(p -> {
                             LOG.info("Destroying process: {}", p.pid());
                             p.destroyForcibly();
                         });
        } catch (Exception e) {
            LOG.error("Failed to kill process: {}", target, e);
        }
    }

    private void recoverProcessKill(Step.StepAction action) {
        LOG.info("Process recovery for: {} (auto-restart expected)", action.getTarget());
    }

    private void injectNetworkDelay(Step.StepAction action) throws Exception {
        Object latencyObj = action.getParams().get("latency");
        Object jitterObj = action.getParams().get("jitter");
        int latency = parseMillis(latencyObj);
        int jitter = jitterObj != null ? parseMillis(jitterObj) : 0;
        LOG.info("Injecting network delay: {}ms +/- {}ms", latency, jitter);
        ensureTcQdiscClean();
        String cmd = String.format("tc qdisc add dev lo root netem delay %dms %dms",
                                   latency, jitter);
        executeCommand(cmd);
    }

    private void injectNetworkLoss(Step.StepAction action) throws Exception {
        Object lossObj = action.getParams().get("loss");
        double loss = lossObj instanceof Number ? ((Number) lossObj).doubleValue() : 10.0;
        LOG.info("Injecting network loss: {}%", loss);
        ensureTcQdiscClean();
        String cmd = String.format("tc qdisc add dev lo root netem loss %.1f%%", loss);
        executeCommand(cmd);
    }

    private void ensureTcQdiscClean() {
        try {
            executeCommand("tc qdisc del dev lo root 2>/dev/null || true");
        } catch (Exception e) {
            LOG.debug("No existing tc qdisc to clean on lo");
        }
    }

    private void recoverNetwork(Step.StepAction action) throws Exception {
        LOG.info("Recovering network rules");
        executeCommand("tc qdisc del dev lo root");
    }

    private void injectCpuStress(Step.StepAction action) throws Exception {
        Object coresObj = action.getParams().get("cores");
        int cores = coresObj instanceof Number ? ((Number) coresObj).intValue() : 1;
        long durationSec = action.getDuration() != null ? action.getDuration().getSeconds() : 60;
        LOG.info("Injecting CPU stress: {} cores for {}s", cores, durationSec);
        String cmd = String.format("stress-ng --cpu %d --timeout %ds",
                                   cores, durationSec);
        Process process = executeCommandAsync(cmd);
        activeProcesses.put(action.getTarget() + "-cpu", process);
    }

    private void injectMemoryStress(Step.StepAction action) throws Exception {
        Object percentObj = action.getParams().get("percent");
        int percent = percentObj instanceof Number ? ((Number) percentObj).intValue() : 50;
        long durationSec = action.getDuration() != null ? action.getDuration().getSeconds() : 60;
        LOG.info("Injecting memory stress: {}%% for {}s", percent, durationSec);
        String cmd = String.format("stress-ng --vm 1 --vm-bytes %d%% --timeout %ds",
                                   percent, durationSec);
        Process process = executeCommandAsync(cmd);
        activeProcesses.put(action.getTarget() + "-memory", process);
    }

    private void recoverStress(Step.StepAction action) {
        String cpuKey = action.getTarget() + "-cpu";
        String memKey = action.getTarget() + "-memory";
        destroyProcess(cpuKey);
        destroyProcess(memKey);
    }

    private void destroyProcess(String key) {
        Process process = activeProcesses.remove(key);
        if (process != null) {
            process.destroyForcibly();
        }
    }

    private void executeCommand(String command) throws Exception {
        LOG.debug("Executing: {}", command);
        Process process = Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            String error = readStream(process.getErrorStream());
            throw new RuntimeException("Command failed with exit code " + exitCode + ": " + error);
        }
    }

    private Process executeCommandAsync(String command) throws Exception {
        LOG.debug("Executing async: {}", command);
        return Runtime.getRuntime().exec(new String[]{"sh", "-c", command});
    }

    private String readStream(java.io.InputStream stream) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        return sb.toString();
    }

    private int parseMillis(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            String str = (String) value;
            if (str.endsWith("ms")) {
                return Integer.parseInt(str.substring(0, str.length() - 2));
            }
            return Integer.parseInt(str);
        }
        return 100;
    }
}
