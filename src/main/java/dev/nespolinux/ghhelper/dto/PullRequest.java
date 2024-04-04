package dev.nespolinux.ghhelper.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PullRequest {
    private String title;
    private String baseRefName;
    private String number;
    private String mergedAt;
    @With
    private String prLink;

}
