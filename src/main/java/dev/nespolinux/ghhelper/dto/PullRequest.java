package dev.nespolinux.ghhelper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullRequest {
    private String title;
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

    public static LocalDateTime toLocalDateTime(String mergedAt) {
        String pattern = "yyyy-MM-dd'T'HH:mm:ss'Z'";
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(pattern, Locale.US);
        return LocalDateTime.parse(mergedAt, dateTimeFormatter);
    }

    public String getTitleWithEllipsis() {
        return title.length() > 100 ? title.substring(0, 97) + "..." : title;
    }

    public PullRequest withParsedDates(PullRequest pr) {
        PullRequest newPr = this;
        if (this.createdAt != null) {
            newPr = newPr.withParsedCreatedAt(toLocalDateTime(this.createdAt));
        }
        if (this.mergedAt != null) {
            newPr = newPr.withParsedMergedAt(toLocalDateTime(this.mergedAt));
        }
        return newPr;
    }
}
