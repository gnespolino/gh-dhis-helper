package dev.nespolinux.ghhelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.nespolinux.ghhelper.dto.PullRequest;
import io.micrometer.common.util.StringUtils;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

@Service
public class GhCommandRunner {

    private final ObjectMapper objectMapper;

    public GhCommandRunner() {
        this.objectMapper = new ObjectMapper();
    }

    public List<PullRequest> getPrs(String user, String baseDir) {
        return Stream.concat(
                        getPrs(user, baseDir, false).stream(),
                        getPrs(user, baseDir, true).stream())
                .toList();
    }

    @SneakyThrows
    private List<PullRequest> getPrs(String ghUser, String dhis2GitBase, boolean merged) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", "cd " + dhis2GitBase + " && gh pr list " + (merged ? "--state merged" : "") + " --author " + ghUser + " --json number,title,baseRefName,createdAt,mergedAt");
        Process process = processBuilder.start();
        String processOutput = IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
        String processError = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
        if (StringUtils.isNotBlank(processError)) {
            return List.of();
        }
        return readJson(processOutput).stream()
                .map(pr -> pr.withMerged(merged))
                .toList();
    }

    @SneakyThrows
    private List<PullRequest> readJson(String processOutput) {
        return objectMapper.readValue(processOutput, new TypeReference<>() {
        });
    }

}
