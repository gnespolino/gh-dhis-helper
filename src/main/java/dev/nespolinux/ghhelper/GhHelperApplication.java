package dev.nespolinux.ghhelper;

import dev.nespolinux.ghhelper.dto.JiraInfo;
import dev.nespolinux.ghhelper.dto.JiraWithPr;
import dev.nespolinux.ghhelper.dto.PullRequestListItem;
import dev.nespolinux.ghhelper.dto.QueryData;
import hu.webarticum.treeprinter.SimpleTreeNode;
import hu.webarticum.treeprinter.printer.listing.ListingTreePrinter;
import hu.webarticum.treeprinter.text.AnsiFormat;
import hu.webarticum.treeprinter.text.ConsoleText;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static hu.webarticum.treeprinter.text.AnsiFormat.BG_BRIGHT_BLUE;
import static hu.webarticum.treeprinter.text.AnsiFormat.BG_BRIGHT_GREEN;
import static hu.webarticum.treeprinter.text.AnsiFormat.BG_BRIGHT_MAGENTA;
import static hu.webarticum.treeprinter.text.AnsiFormat.BG_BRIGHT_RED;
import static hu.webarticum.treeprinter.text.AnsiFormat.BG_BRIGHT_YELLOW;
import static hu.webarticum.treeprinter.text.AnsiFormat.BLACK;
import static hu.webarticum.treeprinter.text.AnsiFormat.BOLD;
import static hu.webarticum.treeprinter.text.AnsiFormat.BRIGHT_GREEN;
import static hu.webarticum.treeprinter.text.AnsiFormat.BRIGHT_MAGENTA;
import static hu.webarticum.treeprinter.text.AnsiFormat.BRIGHT_RED;
import static hu.webarticum.treeprinter.text.AnsiFormat.BRIGHT_WHITE;
import static hu.webarticum.treeprinter.text.AnsiFormat.CYAN;
import static hu.webarticum.treeprinter.text.AnsiFormat.GREEN;
import static hu.webarticum.treeprinter.text.AnsiFormat.RED;

@SpringBootApplication
@RequiredArgsConstructor
public class GhHelperApplication implements CommandLineRunner {

    private final GhHelperService ghHelperService;
    private final GhCommandRunner ghCommandRunner;

    public static void main(String[] args) {
        SpringApplication.run(GhHelperApplication.class, args);
    }

    @Override
    public void run(String... args) {
            printResults(ghHelperService.getData());
    }

    private void printResults(QueryData prs) {
        SimpleTreeNode treeJira = asTreeJira(prs.getJiraWithPrs());
        new ListingTreePrinter((AnsiFormat.BLUE)).print(treeJira);
    }

    private SimpleTreeNode asTreeJira(List<JiraWithPr> jiraWithPrs) {
        SimpleTreeNode root = new SimpleTreeNode("By Jira");
        jiraWithPrs.forEach(jiraWithPr -> {
            SimpleTreeNode jiraNode = getJiraInfoNode(jiraWithPr.getJiraInfo());
            jiraWithPr.getPrInfo().forEach(pr -> {
                SimpleTreeNode prNode = new SimpleTreeNode(getPrNodeText(pr));
                jiraNode.addChild(prNode);
            });
            root.addChild(jiraNode);
        });
        return root;
    }

    private SimpleTreeNode getJiraInfoNode(JiraInfo jiraInfo) {
        return new SimpleTreeNode(getJiraInfoConsoleText(jiraInfo));
    }

    private static AnsiFormat compose(AnsiFormat... formats) {
        AnsiFormat result = AnsiFormat.NONE;
        for (AnsiFormat format : formats) {
            result = result.compose(format);
        }
        return result;
    }

    private ConsoleText getJiraInfoConsoleText(JiraInfo jiraInfo) {
        Map<String, AnsiFormat> statusColors = Map.of(
                "Done", BG_BRIGHT_GREEN,
                "In Progress", BG_BRIGHT_YELLOW,
                "Testing", BG_BRIGHT_BLUE,
                "In Review", BG_BRIGHT_MAGENTA,
                "To Do", BG_BRIGHT_RED
        );

        ConsoleText status = ConsoleText.of(" " + jiraInfo.getStatus() + " ")
                .format(
                        statusColors.getOrDefault(jiraInfo.getStatus(), AnsiFormat.BG_BRIGHT_WHITE)
                                .compose(BLACK)
                                .compose(BOLD));

        ConsoleText assignee = ConsoleText.of(jiraInfo.getAssignee());

        if (jiraInfo.getAssignee().equals(ghCommandRunner.getLoggedUserName())) {
            assignee = assignee.format(AnsiFormat.BRIGHT_YELLOW.compose(BOLD));
        } else {
            assignee = assignee.format(AnsiFormat.YELLOW);
        }

        ConsoleText fixVersionCheckMark;
        if (!jiraInfo.isFixVersionsMatch()) {
            fixVersionCheckMark = ConsoleText.of(" \u2717").format(BRIGHT_RED);
        } else {
            fixVersionCheckMark = ConsoleText.of(" \u2713").format(BRIGHT_GREEN);
        }

        ConsoleText consoleText = ConsoleText.of("[" + jiraInfo.getLink() + "]")
                .concat(" ")
                .concat(status)
                .concat(" ")
                .concat(assignee)
                .concat(" [FIX VERSIONS")
                .concat(fixVersionCheckMark)
                .concat("]:");

        Set<String> allFixVersions = new HashSet<>(jiraInfo.getFixVersions());
        allFixVersions.addAll(jiraInfo.getExpectedFixVersions());

        Set<String> inJiraButNotExpected = jiraInfo.getFixVersions().stream()
                .filter(fixVersion -> !jiraInfo.getExpectedFixVersions().contains(fixVersion))
                .collect(java.util.stream.Collectors.toSet());

        Set<String> expectedButNotInJira = jiraInfo.getExpectedFixVersions().stream()
                .filter(fixVersion -> !jiraInfo.getFixVersions().contains(fixVersion))
                .collect(java.util.stream.Collectors.toSet());

        if (allFixVersions.isEmpty()) {
            consoleText = consoleText.concat(" ").concat(
                    ConsoleText.of("[NONE]").format(compose(BG_BRIGHT_RED, BOLD, BRIGHT_WHITE)));
        } else {
            for (String fixVersion : allFixVersions) {
                consoleText = consoleText.concat(" ");
                if (inJiraButNotExpected.contains(fixVersion)) {
                    consoleText = consoleText.concat(
                            ConsoleText.of("\u2717")
                                    .format(compose(BRIGHT_RED, BG_BRIGHT_BLUE))
                                    .concat(ConsoleText.of(fixVersion)
                                            .format(compose(BG_BRIGHT_BLUE, BLACK, BOLD))));
                } else if (expectedButNotInJira.contains(fixVersion)) {
                    consoleText = consoleText.concat(
                            ConsoleText.of("\u2717 ")
                                    .format(compose(BRIGHT_RED, BG_BRIGHT_YELLOW))
                                    .concat(ConsoleText.of(fixVersion)
                                            .format(compose(BG_BRIGHT_YELLOW, BLACK, BOLD))));
                } else {
                    consoleText = consoleText.concat(
                            ConsoleText.of("\u2713 ")
                                    .format(compose(GREEN, BG_BRIGHT_GREEN))
                                    .concat(ConsoleText.of(fixVersion)
                                            .format(compose(BG_BRIGHT_GREEN, BLACK, BOLD))));
                }
            }
        }

        return consoleText;
    }

    private ConsoleText getPrNodeText(PullRequestListItem pr) {
        ConsoleText date = pr.isMerged() ?
                ConsoleText.of(pr.getMergedAt()).format(GREEN) :
                ConsoleText.of(pr.getCreatedAt()).format(RED);

        ConsoleText statusMark = ConsoleText.of("");
        ConsoleText failingChecks = ConsoleText.of("");
        List<PullRequestListItem.StatusCheckRollup> failures = pr.getFailingStatusChecks();

        if (!failures.isEmpty()) {
            for (PullRequestListItem.StatusCheckRollup failure : failures) {
                statusMark = ConsoleText.of("\u2717").format(BRIGHT_RED);
                failingChecks = failingChecks.concat(
                                ConsoleText.of(failure.getName())
                                        .format(BRIGHT_WHITE
                                                .compose(BG_BRIGHT_RED)))
                        .concat(" ");
            }
        } else {
            statusMark = ConsoleText.of("\u2713").format(BRIGHT_GREEN);
        }

        AnsiFormat titleFormat = AnsiFormat.NONE;
        if (pr.getMergeable().equals("CONFLICTING")) {
            titleFormat = AnsiFormat.BG_BRIGHT_RED;
        }

        if (pr.isMerged()) {
            titleFormat = compose(titleFormat, BRIGHT_MAGENTA);
        } else {
            titleFormat = compose(titleFormat, BRIGHT_GREEN);
        }

        ConsoleText title = ConsoleText.of(pr.getTitleWithEllipsis()).format(titleFormat);

        return statusMark
                .concat("[")
                .concat(ConsoleText.of(pr.getHeadRefName()).format(BRIGHT_WHITE))
                .concat(" ")
                .concat(ConsoleText.of("\u2192").format(AnsiFormat.BRIGHT_BLUE))
                .concat(" ")
                .concat(ConsoleText.of(pr.getBaseRefName()).format(CYAN))
                .concat("] ")
                .concat(title)
                .concat(" " + pr.getPrLink())
                .concat(" ")
                .concat(failingChecks)
                .concat(" @")
                .concat(date);
    }
}
