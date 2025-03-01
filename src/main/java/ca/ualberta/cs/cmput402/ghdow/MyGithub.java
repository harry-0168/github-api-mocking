package ca.ualberta.cs.cmput402.ghdow;

import java.io.IOException;

import org.kohsuke.github.*;

import java.util.*;

public class MyGithub {
    protected GitHub gitHub;
    protected GHPerson myself;
    protected Map<String, GHRepository> myRepos;
    private List<GHCommit> myCommits;
    public MyGithub(String token) throws IOException {
        gitHub = new GitHubBuilder().withOAuthToken(token).build();
    }

    private GHPerson getMyself() throws IOException {
        if (myself == null) {
            myself = gitHub.getMyself();
        }
        return myself;
    }

    public String getGithubName() throws IOException {
        return gitHub.getMyself().getLogin();
    }

    public List<GHRepository> getRepos() throws IOException {
        final int MAX_ATTEMPTS = 3;
        int attempts = 0;
        IOException lastException = null;

        while (attempts < MAX_ATTEMPTS) {
            try {
                // Fetch repositories from GitHub API
                if (myRepos == null) {
                    myRepos = getMyself().getRepositories();
                }
                return new ArrayList<>(myRepos.values());
            } catch (IOException e) {
                lastException = e;
                attempts++;
                if (attempts < MAX_ATTEMPTS) {
                    // Optional: Add a delay (e.g., Thread.sleep(1000)) for real-world scenarios
                }
            }
        }
        throw new IOException("Failed to fetch repositories after " + MAX_ATTEMPTS + " attempts", lastException);
    }

    static private int argMax(int[] days) {
        int max = Integer.MIN_VALUE;
        int arg = -1;
        for (int i = 0; i < days.length; i++) {
            if (days[i] > max) {
                max = days[i];
                arg = i;
            }
        }
        return arg;
    }

    static private String intToDay(int day) {
        return switch (day) {
            case Calendar.SUNDAY -> "Sunday";
            case Calendar.MONDAY -> "Monday";
            case Calendar.TUESDAY -> "Tuesday";
            case Calendar.WEDNESDAY -> "Wednesday";
            case Calendar.THURSDAY -> "Thursday";
            case Calendar.FRIDAY -> "Friday";
            case Calendar.SATURDAY -> "Saturday";
            default -> throw new IllegalArgumentException("Not a day: " + day);
        };
    }

    static private String intToMonth(int month) {
        return switch (month) {
            case Calendar.JANUARY -> "January";
            case Calendar.FEBRUARY -> "February";
            case Calendar.MARCH -> "March";
            case Calendar.APRIL -> "April";
            case Calendar.MAY -> "May";
            case Calendar.JUNE -> "June";
            case Calendar.JULY -> "July";
            case Calendar.AUGUST -> "August";
            case Calendar.SEPTEMBER -> "September";
            case Calendar.OCTOBER -> "October";
            case Calendar.NOVEMBER -> "November";
            case Calendar.DECEMBER -> "December";
            default -> throw new IllegalArgumentException("Not a month: " + month);
        };
    }

    public String getMostPopularDay() throws IOException {
        final int SIZE = 8;
        int[] days = new int[SIZE];
        Calendar cal = Calendar.getInstance();
        for (GHCommit commit: getCommits()) {
            Date date = commit.getCommitDate();
            cal.setTime(date);
            int day = cal.get(Calendar.DAY_OF_WEEK);
            days[day] += 1;
        }
        return intToDay(argMax(days));
    }

    public String getMostPopularMonth() throws IOException {
        final int SIZE = 12;
        int[] months = new int[SIZE];
        Calendar cal = Calendar.getInstance();
        for (GHCommit commit: getCommits()) {
            Date date = commit.getCommitDate();
            cal.setTime(date);
            int month = cal.get(Calendar.MONTH);
            months[month] += 1;
        }
        return intToMonth(argMax(months));
    }

    // 3. Average time between commits (in days)
    public double getAverageTimeBetweenCommits() throws IOException {
        List<Date> commitDates = new ArrayList<>();
        for (GHCommit commit : getCommits()) {
            commitDates.add(commit.getCommitDate());
        }
        if (commitDates.size() < 2) return 0;

        Collections.sort(commitDates);
        long totalDiffMillis = 0;
        for (int i = 1; i < commitDates.size(); i++) {
            totalDiffMillis += commitDates.get(i).getTime() - commitDates.get(i - 1).getTime();
        }
        double avgMillis = (double) totalDiffMillis / (commitDates.size() - 1);
        return avgMillis / (1000 * 60 * 60 * 24); // return average in days
    }

    // 4. Average number of open issues across repositories
    public double getAverageOpenIssues() {
        try {
            int totalOpenIssues = 0;
            int repoCount = 0;
            for (GHRepository repo : getRepos()) { // Calls the retry-enabled method
                totalOpenIssues += repo.getOpenIssueCount();
                repoCount++;
            }
            return repoCount == 0 ? 0 : (double) totalOpenIssues / repoCount;
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage()); // Gracefully report the error
            return 0.0; // Return a default value
        }
    }



    // 5. Average duration (in days) that pull requests stay open
    public double getAveragePullRequestDuration() throws IOException {
        long totalDurationMillis = 0;
        int prCount = 0;
        for (GHRepository repo : getRepos()) {
            // Get pull requests (using closed state) from the repository
            for (GHPullRequest pr : repo.getPullRequests(GHIssueState.CLOSED)) {
                Date created = pr.getCreatedAt();
                Date closed = pr.getClosedAt();
                if (created != null && closed != null) {
                    totalDurationMillis += closed.getTime() - created.getTime();
                    prCount++;
                }
            }
        }
        return prCount == 0 ? 0 : totalDurationMillis / (prCount * 1000.0 * 60 * 60 * 24);
    }

    // 6. Average number of collaborators per repository
    public double getAverageCollaborators() throws IOException {
        int totalCollaborators = 0;
        int repoCount = 0;
        for (GHRepository repo : getRepos()) {
            totalCollaborators += repo.getCollaborators().size();
            repoCount++;
        }
        return repoCount == 0 ? 0 : (double) totalCollaborators / repoCount;
    }


    protected Iterable<? extends GHCommit> getCommits() throws IOException {
        if (myCommits == null) {
            myCommits = new ArrayList<>();
            int count = 0;
            for (GHRepository repo: getRepos()) {
                System.out.println("Loading commits: repo " + repo.getName());
                try {
                    for (GHCommit commit : repo.queryCommits().author(getGithubName()).list()) {
                        myCommits.add(commit);
                        count++;
                        if (count % 100 == 0) {
                            System.out.println("Loading commits: " + count);
                        }
                    }
                } catch (GHException e) {
                    if (!e.getCause().getMessage().contains("Repository is empty")) {
                        throw e;
                    }
                }
            }
        }
        return myCommits;
    }

    public ArrayList<Date> getIssueCreateDates() throws IOException {
        ArrayList<Date> result = new ArrayList<>();
        for (GHRepository repo: getRepos()) {
            List<GHIssue> issues = repo.getIssues(GHIssueState.CLOSED);
            for (GHIssue issue: issues)
                result.add((new GHIssueWrapper(issue)).getCreatedAt());
            }
        return result;
    }
}

