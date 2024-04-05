package dev.nespolinux.ghhelper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class JiraWithPr {
    private JiraInfo jiraInfo;
    private List<PullRequestListItem> prInfo;
}
