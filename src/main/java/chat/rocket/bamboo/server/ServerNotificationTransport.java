package chat.rocket.bamboo.server;

import com.atlassian.bamboo.chains.ChainResultsSummary;
import com.atlassian.bamboo.commit.Commit;
import com.atlassian.bamboo.configuration.AdministrationConfiguration;
import com.atlassian.bamboo.deployments.environments.Environment;
import com.atlassian.bamboo.deployments.projects.DeploymentProject;
import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.deployments.versions.DeploymentVersion;
import com.atlassian.bamboo.deployments.versions.DeploymentVersionStatus;
import com.atlassian.bamboo.notification.Notification;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.project.Project;
import com.atlassian.bamboo.resultsummary.BuildResultsSummary;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.resultsummary.tests.TestCaseResult;
import com.atlassian.bamboo.resultsummary.tests.TestCaseResultError;
import com.atlassian.bamboo.resultsummary.tests.TestResultsSummary;
import com.atlassian.bamboo.resultsummary.vcs.RepositoryChangeset;
import com.atlassian.bamboo.utils.HttpUtils;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.atlassian.spring.container.ContainerManager;

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
    private final String channel;

    private AdministrationConfiguration administrationConfiguration;

    private CloseableHttpClient client;

    @Nullable
    private final ImmutablePlan plan;
    @Nullable
    private final ResultsSummary resultsSummary;
    @Nullable
    private final DeploymentResult deploymentResult;
    @Nullable
    private final DeploymentProject deploymentProject;

    public ServerNotificationTransport(
        String webhookUrl,
        String channel,
        @Nullable ImmutablePlan plan,
        @Nullable ResultsSummary resultsSummary,
        @Nullable DeploymentResult deploymentResult,
        @Nullable DeploymentProject deploymentProject,
        CustomVariableContext customVariableContext)
    {
        this.webhookUrl = customVariableContext.substituteString(webhookUrl);
        this.channel = customVariableContext.substituteString(channel);
        this.plan = plan;
        this.resultsSummary = resultsSummary;
        this.deploymentResult = deploymentResult;
        this.deploymentProject = deploymentProject;
        this.administrationConfiguration = getAdministrationConfiguration();

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
        JSONObject payload = new JSONObject();

        try {
            putConfigDataInPayload(payload);

            putServerDataInPayload(payload);

            putNotificationDataInPayload(payload, notification);

            putPlanDataInPayload(payload);

            putProjectDataInPayload(payload);

            putResultsSummaryDataInPayload(payload);

            putDeploymentResultDataInPayload(payload);
        } catch (JSONException e) {
            log.error("JSON construction error :" + e.getMessage(), e);
        }

        return payload;
    }

    private void putConfigDataInPayload(JSONObject payload) throws JSONException
    {
        JSONObject config = new JSONObject();

        config.put("channel", channel);

        payload.put("config", config);
    }

    private void putServerDataInPayload(JSONObject payload)
            throws JSONException
    {
        JSONObject server = new JSONObject();
        server.put("baseUrl", administrationConfiguration.getBaseUrl());
        server.put("instanceName", administrationConfiguration.getInstanceName());

        payload.put("server", server);
    }

    private void putNotificationDataInPayload(JSONObject payload, Notification notification) throws JSONException
    {
        JSONObject notificationJson = new JSONObject();
        notificationJson.put("type", notification.getClass().getName());
        notificationJson.put("description", notification.getDescription());

        payload.put("notification", notificationJson);
    }

    private void putPlanDataInPayload(JSONObject payload)
            throws JSONException
    {
        if (plan == null) return;

        JSONObject planDetails = new JSONObject();
        planDetails.put("key", plan.getPlanKey());
        planDetails.put("name", plan.getName());
        planDetails.put("description", plan.getDescription());
        planDetails.put("url", administrationConfiguration.getBaseUrl() + "/browse/" + plan.getKey());

        payload.put("plan", planDetails);
    }

    private void putProjectDataInPayload(JSONObject payload)
            throws JSONException
    {
        if (plan == null) {
            putDeploymentProjectDataInPayload(payload);
            return;
        }

        Project project = plan.getProject();

        JSONObject projectDetails = new JSONObject();
        projectDetails.put("key", project.getKey());
        projectDetails.put("name", project.getName());
        projectDetails.put("description", project.getDescription());
        projectDetails.put("url", administrationConfiguration.getBaseUrl() + "/browse/" + project.getKey());

        payload.put("project", projectDetails);
    }

    private void putDeploymentProjectDataInPayload(JSONObject payload)
            throws JSONException
    {
        if (deploymentProject == null) return;

        JSONObject projectDetails = new JSONObject();
        projectDetails.put("key", deploymentProject.getKey().getKey());
        projectDetails.put("name", deploymentProject.getName());
        projectDetails.put("description", deploymentProject.getDescription());
        projectDetails.put("url",
            administrationConfiguration.getBaseUrl() +
            "/deploy/viewDeploymentProjectEnvironments.action?id=" +
            deploymentProject.getId()
        );

        payload.put("project", projectDetails);
    }

    private void putDeploymentResultDataInPayload(JSONObject payload) throws JSONException
    {
        if (deploymentResult == null) return;

        JSONObject deploymentDetails = new JSONObject();
        deploymentDetails.put("id", deploymentResult.getKey().getDeploymentResultId());
        deploymentDetails.put("triggerName", deploymentResult.getTriggerReason().getName());
        deploymentDetails.put("state", deploymentResult.getDeploymentState().toString());
        deploymentDetails.put("url",
            administrationConfiguration.getBaseUrl() +
            "/deploy/viewDeploymentResult.action?deploymentResultId=" +
            deploymentResult.getKey().getDeploymentResultId()
        );

        Environment environment = deploymentResult.getEnvironment();

        JSONObject environmentDetails = new JSONObject();
        environmentDetails.put("id", environment.getId());
        environmentDetails.put("name", environment.getName());
        environmentDetails.put("url",
            administrationConfiguration.getBaseUrl() +
            "/deploy/viewEnvironment.action?id=" +
            environment.getId()
        );

        deploymentDetails.put("environment", environmentDetails);

        DeploymentVersion version = deploymentResult.getDeploymentVersion();

        if (version != null) {
            JSONObject versionDetails = new JSONObject();
            versionDetails.put("id", version.getId());
            versionDetails.put("name", version.getName());
            versionDetails.put("creatorDisplayName", version.getCreatorDisplayName());
            versionDetails.put("url",
                administrationConfiguration.getBaseUrl() +
                "/deploy/viewDeploymentVersion.action?versionId=" +
                version.getId()
            );

            DeploymentVersionStatus versionStatus = version.getVersionStatus();

            if (versionStatus != null) {
                versionDetails.put("status", versionStatus.getDisplayName());
            }

            deploymentDetails.put("version", versionDetails);
        }

        payload.put("deploymentResult", deploymentDetails);
    }

    private void putResultsSummaryDataInPayload(JSONObject payload) throws JSONException
    {
        if (resultsSummary == null) return;

        JSONObject buildDetails = new JSONObject();
        buildDetails.put("key", resultsSummary.getPlanResultKey());
        buildDetails.put("number", resultsSummary.getBuildNumber());
        buildDetails.put("reason", resultsSummary.getReasonSummary());
        buildDetails.put("successful", resultsSummary.isSuccessful());
        buildDetails.put("buildCompletedDate", ZonedDateTime.ofInstant(resultsSummary.getBuildCompletedDate().toInstant(), ZoneId.systemDefault()));
        buildDetails.put("artifact", !resultsSummary.getArtifactLinks().isEmpty());
        buildDetails.put("url", administrationConfiguration.getBaseUrl() + "/browse/" + resultsSummary.getPlanResultKey());

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

        payload.put("build", buildDetails);
    }

    private AdministrationConfiguration getAdministrationConfiguration()
    {
        return (AdministrationConfiguration) ContainerManager.getComponent("administrationConfiguration");
    }
}
