package dev.nespolinux.ghhelper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JiraWithPr {
    private String link;
    private List<String> fixVersions;
    private String title;
    private String baseRefName;
    private String number;
    private String mergedAt;
    @With
    private String prLink;
    @With
    private List<JiraInfo> jiraTicket;

    @Data
    @AllArgsConstructor
    public static class JiraInfo {

    }
}
