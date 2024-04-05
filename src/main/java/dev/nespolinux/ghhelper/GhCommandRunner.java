package dev.nespolinux.ghhelper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.nespolinux.ghhelper.dto.PullRequestListItem;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;

@Service
public class GhCommandRunner {

    private final ObjectMapper objectMapper;

    public GhCommandRunner() {
        this.objectMapper = new ObjectMapper();
        this.objectMapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false);
    }


    public List<PullRequestListItem> getPrList() {
        return Stream.concat(
                        getPrList(false).stream(),
                        getPrList(true).stream())
                .toList();
    }

    @SneakyThrows
    private List<PullRequestListItem> getPrList(boolean merged) {
        List<PullRequestListItem> list = objectMapper.readValue(
                executeCommand(getPrListCommand(merged)),
                new TypeReference<>() {
                });
        return list.stream()
                .map(pr -> pr.withMerged(merged))
                .toList();
    }

    private String getLoggedUser() {
        return executeCommand("gh api user -q \".login\"").replace("\n", "");
    }

    public String getLoggedUserName() {
        return executeCommand("gh api user -q \".name\"").replace("\n", "");
    }

    private String getPrListCommand(boolean merged) {
        String ghUser = getLoggedUser();
        return "gh pr list -R dhis2/dhis2-core " + (merged ? "--state merged " : "") + "--author " + ghUser + " --json id,number,title,baseRefName,createdAt,mergedAt,mergeable,statusCheckRollup";
    }

    @SneakyThrows
    private String executeCommand(String command) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command("sh", "-c", command);
        Process process = processBuilder.start();
        //String processError = IOUtils.toString(process.getErrorStream(), StandardCharsets.UTF_8);
        //if (StringUtils.isNotBlank(processError)) {
        //    throw new RuntimeException(processError);
        //}
        return IOUtils.toString(process.getInputStream(), StandardCharsets.UTF_8);
    }

}
