package ca.ualberta.cs.cmput402.ghdow;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class Main {
    static String getOAuthToken() throws IOException {
        Path tokenFile = Paths.get(
                System.getProperty("user.home"),
                "githubOAuthToken.txt"
        );
        String token = Files.readString(tokenFile);
        return token.strip();
    }
    public static void main(String[] args) throws IOException {
        String token = getOAuthToken();
        MyGithub my = new MyGithub(token);
        System.out.println("Logged in as " + my.getGithubName());
        System.out.println("Most often commits on: " + my.getMostPopularDay());
        System.out.println("Most popular month: " + my.getMostPopularMonth());
        System.out.println("Avg time between commits: " + my.getAverageTimeBetweenCommits());
        System.out.println("Avg open Issues: " + my.getAverageOpenIssues());
        System.out.println("Avg Pr duration: " + my.getAveragePullRequestDuration());
        System.out.println("Avg Collaborators: " + my.getAverageCollaborators());

    }
}