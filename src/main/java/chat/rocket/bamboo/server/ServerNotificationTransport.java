package chat.rocket.bamboo.server;

import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.resultsummary.BuildResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.tests.TestCaseResult;
import com.atlassian.bamboo.resultsummary.tests.TestCaseResultError;
import com.atlassian.bamboo.resultsummary.tests.TestResultsSummary;
import com.atlassian.bamboo.resultsummary.vcs.RepositoryChangeset;
import com.atlassian.bamboo.utils.HttpUtils;
import com.atlassian.bamboo.variable.CustomVariableContext;
import org.apache.http.HttpHost;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;

import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class ServerNotificationTransport implements NotificationTransport
{
    private static final Logger log = Logger.getLogger(ServerNotificationTransport.class);

    private final String webhookUrl;

    private CloseableHttpClient client;

    @Nullable
    private final ImmutablePlan plan;
    @Nullable
    private final ResultsSummary resultsSummary;
    @Nullable
    private final DeploymentResult deploymentResult;

    public ServerNotificationTransport(String webhookUrl,
                                       @Nullable ImmutablePlan plan,
                                       @Nullable ResultsSummary resultsSummary,
                                       @Nullable DeploymentResult deploymentResult,
                                       CustomVariableContext customVariableContext)
    {
        this.webhookUrl = customVariableContext.substituteString(webhookUrl);
        this.plan = plan;
        this.resultsSummary = resultsSummary;
        this.deploymentResult = deploymentResult;

        URI uri;

        try {
            uri = new URI(webhookUrl);
        } catch (URISyntaxException e) {
            log.error("Unable to set up proxy settings, invalid URI encountered: " + e);
            return;
        }

        HttpUtils.EndpointSpec proxyForScheme = HttpUtils.getProxyForScheme(uri.getScheme());

        if (proxyForScheme != null) {
            HttpHost proxy = new HttpHost(proxyForScheme.host, proxyForScheme.port);
            DefaultProxyRoutePlanner routePlanner = new DefaultProxyRoutePlanner(proxy);
            this.client = HttpClients.custom().setRoutePlanner(routePlanner).build();
        } else {
            this.client = HttpClients.createDefault();
        }
    }

    public void sendNotification(@NotNull Notification notification)
    {
        try {
            HttpPost method = setupPostMethod();
            JSONObject jsonObject = createJSONObject(notification);

            method.setEntity(new StringEntity(jsonObject.toString()));

            log.debug(method.getURI().toString());
            log.debug(method.getEntity().toString());
            client.execute(method);
        } catch(URISyntaxException e) {
            log.error("Error parsing webhook url: " + e.getMessage(), e);
        } catch (UnsupportedEncodingException e) {
            log.error("Unsupported Encoding Exception :" + e.getMessage(), e);
        } catch (IOException e) {
            log.error("Error executing http request: " + e.getMessage(), e);
        }
    }

    private HttpPost setupPostMethod() throws URISyntaxException
    {
        HttpPost post = new HttpPost((new URI(webhookUrl)));
        post.setHeader("Content-Type", "application/json");
        return post;
    }

    private JSONObject createJSONObject(Notification notification)
    {
        JSONObject jsonObject = new JSONObject();

        try {

            jsonObject.put("notificationType", notification.getDescription());

            if (plan != null) {
                JSONObject planDetails = new JSONObject();
                planDetails.put("key", plan.getPlanKey());


                jsonObject.put("plan", planDetails);
            }

            if (resultsSummary != null) {
                JSONObject buildDetails = new JSONObject();
                buildDetails.put("number", resultsSummary.getBuildNumber());
                buildDetails.put("reason", resultsSummary.getShortReasonSummary());
                buildDetails.put("successful", resultsSummary.isSuccessful());
                buildDetails.put("buildCompletedDate", ZonedDateTime.ofInstant(resultsSummary.getBuildCompletedDate().toInstant(), ZoneId.systemDefault()));
                buildDetails.put("artifact", !resultsSummary.getArtifactLinks().isEmpty());

                TestResultsSummary testResultsSummary = resultsSummary.getTestResultsSummary();
                JSONObject testResultOverview = new JSONObject();
                testResultOverview.put("description", testResultsSummary.getTestSummaryDescription());
                testResultOverview.put("totalCount", testResultsSummary.getTotalTestCaseCount());
                testResultOverview.put("failedCount", testResultsSummary.getFailedTestCaseCount());
                testResultOverview.put("existingFailedCount", testResultsSummary.getExistingFailedTestCount());
                testResultOverview.put("fixedCount", testResultsSummary.getFixedTestCaseCount());
                testResultOverview.put("newFailedCount", testResultsSummary.getNewFailedTestCaseCount());
                testResultOverview.put("ignoredCount", testResultsSummary.getIgnoredTestCaseCount());
                testResultOverview.put("quarantineCount", testResultsSummary.getQuarantinedTestCaseCount());
                testResultOverview.put("skippedCount", testResultsSummary.getSkippedTestCaseCount());
                testResultOverview.put("successfulCount", testResultsSummary.getSuccessfulTestCaseCount());
                testResultOverview.put("duration", testResultsSummary.getTotalTestDuration());

                buildDetails.put("testSummary", testResultOverview);

                JSONArray vcsDetails = new JSONArray();
                for (RepositoryChangeset changeset : resultsSummary.getRepositoryChangesets()) {
                    JSONObject changesetDetails = new JSONObject();
                    changesetDetails.put("id", changeset.getChangesetId());
                    changesetDetails.put("repositoryName", changeset.getRepositoryData().getName());

                    JSONArray commits = new JSONArray();
                    for (Commit commit: changeset.getCommits()) {
                        JSONObject commitDetails = new JSONObject();
                        commitDetails.put("id", commit.getChangeSetId());
                        commitDetails.put("comment", commit.getComment());

                        commits.put(commitDetails);
                    }

                    changesetDetails.put("commits", commits);

                    vcsDetails.put(changesetDetails);
                }

                buildDetails.put("vcs", vcsDetails);

                if (resultsSummary instanceof ChainResultsSummary) {
                    ChainResultsSummary chainResultsSummary = (ChainResultsSummary) resultsSummary;

                    JSONArray failedJobs = new JSONArray();
                    for (BuildResultsSummary failedJob : chainResultsSummary.getFailedJobResults()) {
                        JSONObject failedJobDetails = new JSONObject();

                        failedJobDetails.put("id", failedJob.getId());

                        JSONArray testDetails = new JSONArray();

                        for (TestCaseResult testCaseResult : failedJob.getFilteredTestResults().getAllFailedTestList()) {
                            JSONObject testCaseDetails = new JSONObject();
                            testCaseDetails.put("name", testCaseResult.getName());
                            testCaseDetails.put("methodName", testCaseResult.getMethodName());
                            testCaseDetails.put("className", testCaseResult.getTestCase().getTestClass().getName());

                            JSONArray testCaseErrorDetails = new JSONArray();
                            for(TestCaseResultError testCaseResultError : testCaseResult.getErrors()) {
                                testCaseErrorDetails.put(testCaseResultError.getContent());
                            }

                            testCaseDetails.put("errors", testCaseErrorDetails);

                            testDetails.put(testCaseDetails);
                        }

                        failedJobDetails.put("failedTests", testDetails);

                        failedJobs.put(failedJobDetails);
                    }

                    buildDetails.put("failedJobs", failedJobs);
                }

                jsonObject.put("build", buildDetails);
            }

        } catch (JSONException e) {
            log.error("JSON construction error :" + e.getMessage(), e);
        }

        return  jsonObject;
    }
}
