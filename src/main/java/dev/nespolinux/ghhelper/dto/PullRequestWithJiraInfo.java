package dev.nespolinux.ghhelper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PullRequestWithJiraInfo {
    @With
    private PullRequestListItem pullRequestListItem;
    @With
    private List<JiraInfo> jiraInfos;

}
