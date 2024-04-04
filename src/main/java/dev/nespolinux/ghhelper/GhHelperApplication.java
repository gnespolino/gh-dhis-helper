package dev.nespolinux.ghhelper;

import dev.nespolinux.ghhelper.dto.JiraInfo;
import dev.nespolinux.ghhelper.dto.JiraWithPr;
import dev.nespolinux.ghhelper.dto.PullRequest;
import dev.nespolinux.ghhelper.dto.QueryData;
import hu.webarticum.treeprinter.SimpleTreeNode;
import hu.webarticum.treeprinter.printer.listing.ListingTreePrinter;
import hu.webarticum.treeprinter.text.AnsiFormat;
import hu.webarticum.treeprinter.text.ConsoleText;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.List;
import java.util.Map;

@SpringBootApplication
@RequiredArgsConstructor
public class GhHelperApplication implements CommandLineRunner {

    private final MyService myService;

    public static void main(String[] args) {
        SpringApplication.run(GhHelperApplication.class, args);
    }

    @Override
    public void run(String... args) {
        if (args.length == 0) {
            printResults(myService.getData());
            return;
        }
        if (args.length == 1) {
            printResults(myService.getData(args[0]));
            return;
        }
        if (args.length == 2) {
            printResults(myService.getData(args[0], args[1]));
        }
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

    private ConsoleText getJiraInfoConsoleText(JiraInfo jiraInfo) {
        Map<String, AnsiFormat> statusColors = Map.of(
                "Done", AnsiFormat.BG_BRIGHT_GREEN,
                "In Progress", AnsiFormat.BG_BRIGHT_YELLOW,
                "Testing", AnsiFormat.BG_BRIGHT_BLUE,
                "In Review", AnsiFormat.BG_BRIGHT_MAGENTA,
                "To Do", AnsiFormat.BG_BRIGHT_RED
        );

        ConsoleText status = ConsoleText.of(jiraInfo.getStatus())
                .format(
                        statusColors.getOrDefault(jiraInfo.getStatus(), AnsiFormat.BG_BRIGHT_WHITE)
                                .compose(AnsiFormat.BLACK)
                                .compose(AnsiFormat.BOLD));

        ConsoleText assignee = ConsoleText.of(jiraInfo.getAssignee())
                .format(AnsiFormat.BLINKING.compose(AnsiFormat.BRIGHT_YELLOW).compose(AnsiFormat.BOLD));

        ConsoleText consoleText = ConsoleText.of("[" + jiraInfo.getLink() + "]")
                .concat(" ")
                .concat(status)
                .concat(" ")
                .concat(assignee)
                .concat(" ");

        for (String fixVersion : jiraInfo.getFixVersions()) {
            consoleText = consoleText
                    .concat(" ")
                    .concat(ConsoleText.of(fixVersion).format(AnsiFormat.BG_BRIGHT_BLUE.compose(AnsiFormat.BLACK).compose(AnsiFormat.BOLD)));
        }

        return consoleText;
    }

    private ConsoleText getPrNodeText(PullRequest pr) {
        ConsoleText date = pr.isMerged() ?
                ConsoleText.of(pr.getMergedAt()).format(AnsiFormat.GREEN) :
                ConsoleText.of(pr.getCreatedAt()).format(AnsiFormat.RED);

        ConsoleText title = pr.isMerged() ?
                ConsoleText.of(pr.getTitleWithEllipsis()).format(AnsiFormat.BRIGHT_MAGENTA) :
                ConsoleText.of(pr.getTitleWithEllipsis()).format(AnsiFormat.BRIGHT_GREEN);

        return ConsoleText.of("[")
                .concat(ConsoleText.of(pr.getBaseRefName()).format(AnsiFormat.CYAN))
                .concat("] ")
                .concat(title)
                .concat(" " + pr.getPrLink())
                .concat(" @")
                .concat(date);
    }
}
