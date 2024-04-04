package dev.nespolinux.ghhelper.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QueryData {
    private List<JiraWithPr> jiraWithPrs;
}
