package dev.nespolinux.ghhelper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class JiraInfo {
    private String link;
    private String status;
    private String assignee;
    private List<String> fixVersions;
}
