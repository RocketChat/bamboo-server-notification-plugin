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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class ServerNotificationRecipient extends AbstractNotificationRecipient implements DeploymentResultAwareNotificationRecipient,
                                                                                           NotificationRecipient.RequiresPlan,
                                                                                           NotificationRecipient.RequiresResultSummary

{
    private static String WEBHOOK_URL = "chat.rocket.bamboo.server.webhookUrl";

    private String webhookUrl = null;

    private TemplateRenderer templateRenderer;

    private ImmutablePlan plan;
    private ResultsSummary resultsSummary;
    private DeploymentResult deploymentResult;
    private CustomVariableContext customVariableContext;

    @Override
    public void populate(@NotNull Map<String, String[]> params)
    {
        for (String next : params.keySet()) {
            System.out.println("next = " + next);
        }

        if (params.containsKey(WEBHOOK_URL)) {
            int i = params.get(WEBHOOK_URL).length - 1;
            this.webhookUrl = params.get(WEBHOOK_URL)[i];
        }
    }

    @Override
    public void init(@Nullable String configurationData)
    {
        if (StringUtils.isNotBlank(configurationData)) {
            String delimiter = "\\|";

            String[] configValues = configurationData.split(delimiter);

            if (configValues.length > 0) {
                webhookUrl = configValues[0];
            }
        }
    }

    @NotNull
    @Override
    public String getRecipientConfig()
    {
        StringBuilder recipientConfig = new StringBuilder();

        if (StringUtils.isNotBlank(webhookUrl)) {
            recipientConfig.append(webhookUrl);
        }

        return recipientConfig.toString();
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

        context.put("inputName", WEBHOOK_URL);

        if (webhookUrl != null) {
            context.put("webhookUrl", webhookUrl);
        }

        System.out.println("populateContext = " + context.toString());

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
