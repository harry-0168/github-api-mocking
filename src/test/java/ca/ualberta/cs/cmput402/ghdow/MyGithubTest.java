package ca.ualberta.cs.cmput402.ghdow;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.runner.RunWith;
import org.kohsuke.github.*;
import org.mockito.MockedConstruction;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class) class MyGithubTest {

    private MyGithub myGithub;
    private MyGithub spyGithub;
    private GHRepository mockRepo1;
    private GHRepository mockRepo2;
    private GHCommit commitFriday1;
    private GHCommit commitFriday2;
    private GHCommit commitMonday;
    private GHPullRequest mockPR1;
    private GHPullRequest mockPR2;
    private GHMyself mockMyself;
    private GitHub mockGitHub;

    @BeforeEach
    void setUp() throws IOException {
        // Create a real instance and wrap it with a spy
        myGithub = new MyGithub("fake_token");
        spyGithub = spy(myGithub);

        // Set up mocks for GitHub and GHMyself
        mockGitHub = mock(GitHub.class);
        mockMyself = mock(GHMyself.class);
        spyGithub.gitHub = mockGitHub;
        spyGithub.myself = mockMyself;
        when(mockGitHub.getMyself()).thenReturn(mockMyself);
        when(mockMyself.getLogin()).thenReturn("testuser");

        // Set up mock repositories
        mockRepo1 = mock(GHRepository.class);
        mockRepo2 = mock(GHRepository.class);
        Map<String, GHRepository> repoMap = new HashMap<>();
        repoMap.put("repo1", mockRepo1);
        repoMap.put("repo2", mockRepo2);
        spyGithub.myRepos = repoMap;
        when(mockRepo1.getName()).thenReturn("repo1");
        when(mockRepo2.getName()).thenReturn("repo2");

        // For getAverageCollaborators(), getCollaborators() returns a GHPersonSet.
        @SuppressWarnings("unchecked")
        GHPersonSet<GHUser> mockCollaborators1 = mock(GHPersonSet.class);
        when(mockCollaborators1.size()).thenReturn(2);
        when(mockRepo1.getCollaborators()).thenReturn(mockCollaborators1);

        @SuppressWarnings("unchecked")
        GHPersonSet<GHUser> mockCollaborators2 = mock(GHPersonSet.class);
        when(mockCollaborators2.size()).thenReturn(3);
        when(mockRepo2.getCollaborators()).thenReturn(mockCollaborators2);
    }

    @Test
    void testGetMostPopularDay() throws IOException {
        // Create three commits: two on Friday and one on Monday.
        commitFriday1 = mock(GHCommit.class);
        commitFriday2 = mock(GHCommit.class);
        commitMonday = mock(GHCommit.class);

        Calendar cal = Calendar.getInstance();
        // Set commitFriday1 and commitFriday2 to a Friday.
        cal.set(2024, Calendar.MARCH, 15, 10, 0, 0); // March 15, 2024 (Friday)
        Date fridayDate = cal.getTime();
        // Set commitMonday to a Monday.
        cal.set(2024, Calendar.MARCH, 18, 10, 0, 0); // March 18, 2024 (Monday)
        Date mondayDate = cal.getTime();

        when(commitFriday1.getCommitDate()).thenReturn(fridayDate);
        when(commitFriday2.getCommitDate()).thenReturn(fridayDate);
        when(commitMonday.getCommitDate()).thenReturn(mondayDate);

        List<GHCommit> fakeCommits = Arrays.asList(commitFriday1, commitFriday2, commitMonday);
        doReturn(fakeCommits).when(spyGithub).getCommits();

        // Expect "Friday" because there are 2 commits on Friday.
        assertEquals("Friday", spyGithub.getMostPopularDay());
    }

    @Test
    void testGetMostPopularMonth() throws IOException {
        // Create commits in different months.
        GHCommit commitMarch = mock(GHCommit.class);
        GHCommit commitApril = mock(GHCommit.class);
        GHCommit commitMarch2 = mock(GHCommit.class);

        Calendar cal = Calendar.getInstance();
        // Two commits in March.
        cal.set(2024, Calendar.MARCH, 10, 10, 0, 0);
        Date marchDate = cal.getTime();
        // One commit in April.
        cal.set(2024, Calendar.APRIL, 5, 10, 0, 0);
        Date aprilDate = cal.getTime();

        when(commitMarch.getCommitDate()).thenReturn(marchDate);
        when(commitMarch2.getCommitDate()).thenReturn(marchDate);
        when(commitApril.getCommitDate()).thenReturn(aprilDate);

        List<GHCommit> fakeCommits = Arrays.asList(commitMarch, commitMarch2, commitApril);
        doReturn(fakeCommits).when(spyGithub).getCommits();

        // Expect "March" because it has 2 commits.
        assertEquals("March", spyGithub.getMostPopularMonth());
    }

    @Test
    void testGetAverageTimeBetweenCommits() throws IOException {
        // Create three commits with known time intervals.
        GHCommit commit1 = mock(GHCommit.class);
        GHCommit commit2 = mock(GHCommit.class);
        GHCommit commit3 = mock(GHCommit.class);

        Calendar cal = Calendar.getInstance();
        // commit1: March 10, 2024 10:00:00.
        cal.set(2024, Calendar.MARCH, 10, 10, 0, 0);
        Date date1 = cal.getTime();
        // commit2: March 11, 2024 10:00:00 (1 day later).
        cal.set(2024, Calendar.MARCH, 11, 10, 0, 0);
        Date date2 = cal.getTime();
        // commit3: March 13, 2024 10:00:00 (2 days after commit2).
        cal.set(2024, Calendar.MARCH, 13, 10, 0, 0);
        Date date3 = cal.getTime();

        when(commit1.getCommitDate()).thenReturn(date1);
        when(commit2.getCommitDate()).thenReturn(date2);
        when(commit3.getCommitDate()).thenReturn(date3);

        List<GHCommit> fakeCommits = Arrays.asList(commit1, commit2, commit3);
        doReturn(fakeCommits).when(spyGithub).getCommits();

        // Intervals: 1 day and 2 days. Average = (1 + 2) / 2 = 1.5 days.
        assertEquals(1.5, spyGithub.getAverageTimeBetweenCommits(), 0.01);
    }

    @Test
    void testGetAverageOpenIssues() throws IOException {
        // Set repository to report 10 open issues.
        when(mockRepo1.getOpenIssueCount()).thenReturn(10);
        when(mockRepo2.getOpenIssueCount()).thenReturn(5);

        // getAverageOpenIssues() iterates over our single repository.
        double avgIssues = spyGithub.getAverageOpenIssues();
        assertEquals(7.5, avgIssues, 0.01);
    }


    @Test
    void testGetAverageCollaborators() throws IOException {
        // getAverageCollaborators() iterates over our two repositories.
        double avgCollaborators = spyGithub.getAverageCollaborators();
        assertEquals(2.5, avgCollaborators, 0.01);
    }

    @Test
    void getIssueCreateDates() throws IOException {
        // We don't have a login token for github :(
        String token = "I am a fake token";
        MyGithub my = new MyGithub(token);
        assertNotNull(my);

        // We made this field protected instead of private so we can inject our mock
        // directly
        my.gitHub = mock(GitHub.class);

        // Set up a fake repository
        String fakeRepoName = "fakeRepo";
        GHRepository fakeRepo = mock(GHRepository.class);

        // Put our fake repository in a list of fake repositories
        // We made this field protected instead of private so we can inject our mock
        // directly, but we could have mocked GHMyself/GHPerson instead
        my.myRepos = new HashMap<>();
        my.myRepos.put(fakeRepoName, fakeRepo);

        // Generate some mock issues with mock dates for our mock repository
        final int DATES = 30;

        ArrayList<GHIssue> mockIssues = new ArrayList<>();
        ArrayList<Date> expectedDates = new ArrayList<>();
        HashMap<String, Date> issueToDate = new HashMap<>();
        Calendar calendar = Calendar.getInstance();
        for (int i = 1; i < DATES+1; i++) {
            calendar.set(100, Calendar.JANUARY, i, 1, 1, 1);
            Date issueDate = calendar.getTime();

            // Give this mock GHIssue a unique Mockito "name"
            // This has nothing to do with github, you can
            // give any mockito object a name
            String issueMockName = String.format("getIssueCreateDates issue #%d", i);
            GHIssue issue = mock(GHIssue.class, issueMockName);

            expectedDates.add(issueDate);
            mockIssues.add(issue);

            // Note that we DO NOT try to
            // when(issue.getCreatedAt())
            // because that's what causes the Mockito/github-api bug ...
            // instead we'll just save what we would have wanted to do
            // in a hashmap and then apply it later to GHIssueWrapper
            // which does not have the bug because it doesn't use
            // github-api 's WithBridgeMethods

            issueToDate.put(issueMockName, issueDate);
        }

        // Supply the mock repo with a list of mock issues to return
        when(fakeRepo.getIssues(GHIssueState.CLOSED)).thenReturn(mockIssues);

        List<Date> actualDates;

        // Inside the try block, Mockito will intercept GHIssueWrapper's constructor
        // and have it construct mock GHIssueWrappers instead
        // We have to use a try-with-resources, or it will get stuck like this and probably
        // ruin our other tests.
        try (MockedConstruction<GHIssueWrapper> ignored = mockConstruction(
                GHIssueWrapper.class,
                (mock, context) -> {
                    // Figure out which GHIssue the mock GHIssueWrapper 's
                    // constructor was called with
                    GHIssue issue = (GHIssue) context.arguments().get(0);
                    assertNotNull(issue);

                    // Ask mockito what name we gave the mock issue
                    String issueName = mockingDetails(issue)
                            .getMockCreationSettings()
                            .getMockName()
                            .toString();

                    // Make sure GHIssueWrapper was constructed with one of our mock
                    // GHIssue objects
                    assertTrue(issueToDate.containsKey(issueName));

                    // Get the date associated with the mock GHIssue object
                    // This is where we work around the Mockito/github-api bug!
                    Date date = issueToDate.get(issueName);
                    assertNotNull(date);
                    // Apply the date to the mock GHIssueWrapper
                    when(mock.getCreatedAt()).thenReturn(date);
                }
        )) {
            // This is the only line actually inside the try block
            actualDates = my.getIssueCreateDates();
        }

        // Check that we got our fake dates out
        assertEquals(expectedDates.size(), DATES);
        assertEquals(actualDates.size(), DATES);

        for (int i = 1; i < DATES; i++) {
            assertEquals(expectedDates.get(i), actualDates.get(i));
            System.out.println(expectedDates.get(i));
        }
    }
}

@RunWith(MockitoJUnitRunner.class) class RobustnessTest {

    private MyGithub myGithub;
    private MyGithub spyGithub;
    private GHRepository mockRepo1;
    private GHRepository mockRepo2;

    private GHMyself mockMyself;
    private GitHub mockGitHub;

    @BeforeEach
    void setUp() throws IOException {
        // Create a real instance and wrap it with a spy
        myGithub = new MyGithub("fake_token");
        spyGithub = spy(myGithub);

        // Set up mocks for GitHub and GHMyself
        mockGitHub = mock(GitHub.class);
        mockMyself = mock(GHMyself.class);
        spyGithub.gitHub = mockGitHub;
        spyGithub.myself = mockMyself;
        when(mockGitHub.getMyself()).thenReturn(mockMyself);
        when(mockMyself.getLogin()).thenReturn("testuser");

        // set up mock repositories
        mockRepo1 = mock(GHRepository.class);
        mockRepo2 = mock(GHRepository.class);
    }

    @Test
    void testGetAverageOpenIssues_RetriesThreeTimesOnFailure() throws IOException {
        // Simulate 3 API failures
        when(mockMyself.getRepositories())
                .thenThrow(new IOException("API error")) // Attempt 1: Fail
                .thenThrow(new IOException("API error")) // Attempt 2: Fail
                .thenThrow(new IOException("API error")); // Attempt 3: Fail

        double result = spyGithub.getAverageOpenIssues();

        // Verify the default value is returned after 3 failures
        assertEquals(0.0, result, 0.01);
        // Verify the API was called exactly 3 times
        verify(mockMyself, times(3)).getRepositories();
    }

    @Test
    void testGetAverageOpenIssues_SucceedsAfterRetry() throws IOException {
        // Simulate 2 failures, then success on the third attempt
        when(mockMyself.getRepositories())
                .thenThrow(new IOException("API error")) // Attempt 1: Fail
                .thenThrow(new IOException("API error")) // Attempt 2: Fail
                .thenReturn(Map.of("repo1", mockRepo1, "repo2", mockRepo2)); // Attempt 3: Succeed

        when(mockRepo1.getOpenIssueCount()).thenReturn(10);
        when(mockRepo2.getOpenIssueCount()).thenReturn(5);

        double result = spyGithub.getAverageOpenIssues();

        // Verify the correct result after retries
        assertEquals(7.5, result, 0.01);
        // Verify the API was called 3 times
        verify(mockMyself, times(3)).getRepositories();
    }
}

