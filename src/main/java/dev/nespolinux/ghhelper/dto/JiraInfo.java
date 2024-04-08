package dev.nespolinux.ghhelper.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.With;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
@AllArgsConstructor
@Builder
public class JiraInfo {
    private String link;
    private String status;
    private String assignee;
    private List<String> fixVersions;
    @With
    private List<String> expectedFixVersions;

    public boolean isFixVersionsMatch() {
        Set<String> exp = new HashSet<>(getExpectedFixVersions());
        Set<String> fix = new HashSet<>(getFixVersions());
        return exp.equals(fix);
    }

    public JiraInfo buildExpectedFixVersions(List<PullRequestListItem> pullRequestListItems) {
        return
                this.withExpectedFixVersions(
                        normalizeExpectedVersions(
                                pullRequestListItems.stream()
                                        .filter(PullRequestListItem::isMerged)
                                        .map(this::toExpectedFixVersion)
                                        .toList()));
    }

    private List<String> normalizeExpectedVersions(List<String> jiraInfo) {
        ArrayList<String> normalized = new ArrayList<>(jiraInfo);
        if (normalized.contains("2.41")) {
            normalized.remove("2.42");
            normalized.remove("2.41.1");
        }
        if (normalized.contains("2.41.0")) {
            normalized.remove("2.42");
            normalized.remove("2.41.1");
        }
        return Collections.emptyList();
    }

    private String toExpectedFixVersion(PullRequestListItem pr) {
        String baseRefName = pr.getBaseRefName();
        LocalDateTime mergedAt = pr.getParsedMergedAt();
        if (mergedAt.isBefore(LocalDateTime.of(2024, 3, 7, 0, 0))) {
            if (baseRefName.equals("master")) {
                return "2.41.0";
            }
            return "???";
        }
        if (mergedAt.isBefore(LocalDateTime.of(2024, 3, 21, 0, 0))) {
            if (baseRefName.equals("master")) {
                return "2.42";
            }
            if (baseRefName.equals("2.41")) {
                return "2.41.0";
            }
            return "???";
        }
        if (baseRefName.equals("master")) {
            return "2.42";
        }
        if (baseRefName.equals("2.41")) {
            return "2.41.1";
        }
        if (baseRefName.equals("patch/2.41.0")) {
            return "2.41.0";
        }
        return "???";
    }
}
