package dev.nespolinux.ghhelper;

import dev.nespolinux.ghhelper.dto.JiraInfo;
import dev.nespolinux.ghhelper.dto.JiraResponse;
import dev.nespolinux.ghhelper.dto.JiraWithPr;
import dev.nespolinux.ghhelper.dto.PullRequest;
import dev.nespolinux.ghhelper.dto.PullRequestWithJiraInfo;
import dev.nespolinux.ghhelper.dto.QueryData;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.nespolinux.ghhelper.dto.PullRequest.toLocalDateTime;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class GhHelperService {

    private final GhCommandRunner ghCommandRunner;
    private final RestTemplate restTemplate;

    @Value("${gh.user}")
    private String ghUser;
    @Value("${dhis2.git.base}")
    private String dhis2GitBase;
    @Value("${days:30}")
    private long days;

    public QueryData getData() {
        return getData(ghUser, dhis2GitBase);
    }

    public QueryData getData(String user) {
        return getData(user, dhis2GitBase);
    }

    public QueryData getData(String user, String baseDir) {
        List<PullRequestWithJiraInfo> prs = enrich(ghCommandRunner.getPrs(user, baseDir));

        List<JiraWithPr> jiraWithPr = prs.stream()
                .flatMap(pr -> pr.getJiraInfos().stream().map(jiraInfo -> Pair.of(pr.getPullRequest(), jiraInfo)))
                .collect(Collectors.groupingBy(Pair::getRight, mapping(Pair::getLeft, toList())))
                .entrySet().stream()
                .map(entry -> JiraWithPr.builder()
                        .jiraInfo(entry.getKey())
                        .prInfo(entry.getValue())
                        .build())
                .sorted(this::compareDates)
                .toList();

        return QueryData.builder()
                .jiraWithPrs(jiraWithPr)
                .build();
    }

    private int compareDates(JiraWithPr jiraWithPr1, JiraWithPr jiraWithPr2) {
        LocalDateTime localDateTime1 = Stream.concat(
                        jiraWithPr1.getPrInfo().stream().map(PullRequest::getParsedMergedAt),
                        jiraWithPr1.getPrInfo().stream().map(PullRequest::getParsedCreatedAt))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);
        LocalDateTime localDateTime2 = Stream.concat(
                        jiraWithPr2.getPrInfo().stream().map(PullRequest::getParsedMergedAt),
                        jiraWithPr2.getPrInfo().stream().map(PullRequest::getParsedCreatedAt))
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(LocalDateTime.MIN);

        return localDateTime2.compareTo(localDateTime1);
    }

    private List<PullRequestWithJiraInfo> enrich(List<PullRequest> prs) {
        return prs.stream()
                .filter(pr -> pr.getTitle().contains("DHIS2-"))
                .filter(this::isRecentEnoughOrOpen)
                .map(pr -> PullRequestWithJiraInfo.builder()
                        .pullRequest(pr.withPrLink(getPrLink(pr))
                                .withParsedDates(pr))
                        .jiraInfos(extractJiraTicket(pr))
                        .build())
                .toList();
    }

    private boolean isRecentEnoughOrOpen(PullRequest pr) {
        String consideredDate = pr.getMergedAt() == null ? pr.getCreatedAt() : pr.getMergedAt();
        return toLocalDateTime(consideredDate).isAfter(LocalDateTime.now().minusDays(days));
    }

    private String getPrLink(PullRequest pr) {
        return "https://github.com/dhis2/dhis2-core/pull/" + pr.getNumber();
    }

    private List<JiraInfo> extractJiraTicket(PullRequest pr) {
        String title = pr.getTitle();
        List<Integer> dhis2Indexes = findDhis2(title);
        if (dhis2Indexes.isEmpty()) {
            return List.of();
        }
        Set<String> ticketNumbers = new HashSet<>();
        List<JiraInfo> ticketLinks = new ArrayList<>();
        for (int index : dhis2Indexes) {
            int start = index + 6;
            int end = start;
            while (end < title.length() && Character.isDigit(title.charAt(end))) {
                end++;
            }
            ticketNumbers.add(title.substring(start, end));
        }
        for (String ticketNumber : ticketNumbers) {
            String link = "https://jira.dhis2.org/browse/DHIS2-" + ticketNumber;
            JiraResponse jiraResponse = getJiraResponse(ticketNumber);
            List<String> fixVersions = getFixVersions(jiraResponse);
            String status = getStatus(jiraResponse);
            String assignee = getAssignee(jiraResponse);
            ticketLinks.add(new JiraInfo(link, status, assignee, fixVersions));
        }
        return ticketLinks;
    }

    private List<String> getFixVersions(JiraResponse jiraResponse) {
        return Optional.ofNullable(jiraResponse)
                .map(JiraResponse::getFields)
                .map(JiraResponse.Fields::getFixVersions)
                .orElse(List.of())
                .stream()
                .map(JiraResponse.NameableObj::getName)
                .toList();
    }

    private String getStatus(JiraResponse jiraResponse) {
        return Optional.ofNullable(jiraResponse)
                .map(JiraResponse::getFields)
                .map(JiraResponse.Fields::getStatus)
                .map(JiraResponse.NameableObj::getName)
                .orElse("-unset-");
    }

    private String getAssignee(JiraResponse jiraResponse) {
        return Optional.ofNullable(jiraResponse)
                .map(JiraResponse::getFields)
                .map(JiraResponse.Fields::getAssignee)
                .map(JiraResponse.DefaultNameableObj::getDisplayName)
                .orElse("-unset-");
    }

    private JiraResponse getJiraResponse(String ticketNumber) {
        String url = "https://jira.dhis2.org/rest/api/2/issue/DHIS2-" + ticketNumber;
        return restTemplate.getForObject(url, JiraResponse.class);
    }

    public List<Integer> findDhis2(String textString) {
        String word = "DHIS2-";
        List<Integer> indexes = new ArrayList<>();
        String lowerCaseTextString = textString.toLowerCase();
        String lowerCaseWord = word.toLowerCase();

        int index = 0;
        while (index != -1) {
            index = lowerCaseTextString.indexOf(lowerCaseWord, index);
            if (index != -1) {
                indexes.add(index);
                index++;
            }
        }
        return indexes;
    }
}
