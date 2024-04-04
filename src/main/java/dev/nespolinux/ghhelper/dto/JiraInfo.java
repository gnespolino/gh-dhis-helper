package dev.nespolinux.ghhelper;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class JiraInfo {
    private String link;
    private List<String> fixVersions;
}
