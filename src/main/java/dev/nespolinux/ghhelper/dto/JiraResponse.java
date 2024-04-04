package dev.nespolinux.ghhelper.dto;

import lombok.Data;

import java.util.List;

@Data
public class JiraResponse {
    private Fields fields;

    @Data
    public static class Fields {
        private List<NameableObj> fixVersions;
        private NameableObj status;
        private DefaultNameableObj assignee;
    }

    @Data
    public static class NameableObj {
        private String name;
    }

    @Data
    public static class DefaultNameableObj {
        private String displayName;
    }
}
