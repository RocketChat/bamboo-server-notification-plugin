package chat.rocket.bamboo.server;

import com.atlassian.bamboo.deployments.results.DeploymentResult;
import com.atlassian.bamboo.notification.NotificationRecipient;
import com.atlassian.bamboo.notification.NotificationTransport;
import com.atlassian.bamboo.notification.recipients.AbstractNotificationRecipient;
import com.atlassian.bamboo.deployments.notification.DeploymentResultAwareNotificationRecipient;
import com.atlassian.bamboo.plan.Plan;
import com.atlassian.bamboo.plan.cache.ImmutablePlan;
import com.atlassian.bamboo.plugin.descriptor.NotificationRecipientModuleDescriptor;
import com.atlassian.bamboo.resultsummary.ResultsSummary;
import com.atlassian.bamboo.template.TemplateRenderer;
import com.atlassian.bamboo.variable.CustomVariableContext;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;
import java.util.Map;

public class ServerNotificationRecipient extends AbstractNotificationRecipient implements DeploymentResultAwareNotificationRecipient,
                                                                                           NotificationRecipient.RequiresPlan,
                                                                                           NotificationRecipient.RequiresResultSummary

{
    private static final Logger log = Logger.getLogger(ServerNotificationRecipient.class);

    private static String WEBHOOK_URL = "chat.rocket.bamboo.server.webhookUrl";
    private static String CHANNEL = "chat.rocket.bamboo.server.channel";

    private String webhookUrl = null;
    private String channel = null;

    private TemplateRenderer templateRenderer;

    private ImmutablePlan plan;
    private ResultsSummary resultsSummary;
    private DeploymentResult deploymentResult;
    private CustomVariableContext customVariableContext;

    @Override
    public void populate(@NotNull Map<String, String[]> params)
    {
        if (params.containsKey(WEBHOOK_URL)) {
            int i = params.get(WEBHOOK_URL).length - 1;
            this.webhookUrl = params.get(WEBHOOK_URL)[i];
        }

        if (params.containsKey(CHANNEL)) {
            int i = params.get(CHANNEL).length - 1;
            this.channel = params.get(CHANNEL)[i];
        }
    }

    @Override
    public void init(@Nullable String configurationData)
    {
        if (StringUtils.isNotBlank(configurationData)) {
            try {
                JSONObject config = new JSONObject(configurationData);

                webhookUrl = config.getString(WEBHOOK_URL);
                channel = config.getString(CHANNEL);
            } catch (Exception e) {
                log.error("Error initiating config: " + e.getMessage());
            }
        }
    }

    @NotNull
    @Override
    public String getRecipientConfig()
    {
        JSONObject config = new JSONObject();

        try {
            config.put(WEBHOOK_URL, webhookUrl);
            config.put(CHANNEL, channel);
        } catch (JSONException e) {
            log.error("Error getting recipient config: " + e.getMessage());
        }

        System.out.println("METHOD getRecipientConfig: " + config.toString());

        return config.toString();
    }

    @NotNull
    @Override
    public String getEditHtml()
    {
        String editTemplateLocation = ((NotificationRecipientModuleDescriptor)getModuleDescriptor()).getEditTemplate();
        return templateRenderer.render(editTemplateLocation, populateContext());
    }

    private Map<String, Object> populateContext()
    {
        Map<String, Object> context = Maps.newHashMap();

        context.put("webhookInputName", WEBHOOK_URL);
        context.put("channelInputName", CHANNEL);

        if (webhookUrl != null) {
            context.put("webhookUrl", webhookUrl);
        }

        if (channel != null) {
            context.put("channel", channel);
        }

        return context;
    }

    @NotNull
    @Override
    public String getViewHtml()
    {
        String editTemplateLocation = ((NotificationRecipientModuleDescriptor)getModuleDescriptor()).getViewTemplate();
        return templateRenderer.render(editTemplateLocation, populateContext());
    }



    @NotNull
    public List<NotificationTransport> getTransports()
    {
        List<NotificationTransport> list = Lists.newArrayList();
        list.add(new ServerNotificationTransport(webhookUrl, plan, resultsSummary, deploymentResult, customVariableContext));
        return list;
    }

    public void setPlan(@Nullable final Plan plan)
    {
        this.plan = plan;
    }

    public void setPlan(@Nullable final ImmutablePlan plan)
    {
        this.plan = plan;
    }

    public void setDeploymentResult(@Nullable final DeploymentResult deploymentResult)
    {
        this.deploymentResult = deploymentResult;
    }

    public void setResultsSummary(@Nullable final ResultsSummary resultsSummary)
    {
        this.resultsSummary = resultsSummary;
    }

    //-----------------------------------Dependencies
    public void setTemplateRenderer(TemplateRenderer templateRenderer)
    {
        this.templateRenderer = templateRenderer;
    }

    public void setCustomVariableContext(CustomVariableContext customVariableContext)
    {
        this.customVariableContext = customVariableContext;
    }
}
