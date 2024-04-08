package dev.nespolinux.ghhelper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullRequestListItem {
    private String id;
    private String title;
    private String headRefName;
    private String baseRefName;
    private String number;
    private String createdAt;
    @With
    private LocalDateTime parsedCreatedAt;
    private String mergedAt;
    @With
    private LocalDateTime parsedMergedAt;
    @With
    private boolean merged;
    @With
    private String prLink;
    private String mergeable;
    private List<StatusCheckRollup> statusCheckRollup;

    public List<StatusCheckRollup> getFailingStatusChecks() {
        return Optional.ofNullable(statusCheckRollup)
                .orElse(Collections.emptyList())
                .stream()
                .filter(statusCheck -> "FAILURE".equalsIgnoreCase(statusCheck.getConclusion()))
                .toList();
    }

    public PullRequestListItem withParsedDates(PullRequestListItem pr) {
        PullRequestListItem newPr = this;
        if (this.createdAt != null) {
            newPr = newPr.withParsedCreatedAt(toLocalDateTime(this.createdAt));
        }
        if (this.mergedAt != null) {
            newPr = newPr.withParsedMergedAt(toLocalDateTime(this.mergedAt));
        }
        return newPr;
    }

    public static LocalDateTime toLocalDateTime(String mergedAt) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern, Locale.US);
        return LocalDateTime.parse(mergedAt, dateTimeFormatter);
    }

    public String getTitleWithEllipsis() {
        return title.length() > 100 ? title.substring(0, 97) + "..." : title;
    }

    @Data
    public static class StatusCheckRollup {
        private String __typename;
        private String completedAt;
        private String conclusion;
        private String name;
        private String status;
    }
}
