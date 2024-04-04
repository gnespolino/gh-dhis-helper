package dev.nespolinux.ghhelper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JiraWithPr {
    private JiraInfo jiraInfo;
    private List<PullRequest> prInfo;
}
