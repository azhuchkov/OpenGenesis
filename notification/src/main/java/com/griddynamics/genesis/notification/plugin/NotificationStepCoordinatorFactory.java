/*
 * Copyright (c) 2010-2012 Grid Dynamics Consulting Services, Inc, All Rights Reserved
 *   http://www.griddynamics.com
 *
 *   This library is free software; you can redistribute it and/or modify it under the terms of
 *   the GNU Lesser General Public License as published by the Free Software Foundation; either
 *   version 2.1 of the License, or any later version.
 *
 *   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 *   AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 *   IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *   DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 *   FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 *   DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 *   SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 *   CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 *   OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 *   OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 *   Project:     Genesis
 *   Description:  Continuous Delivery Platform
 */
package com.griddynamics.genesis.notification.plugin;

import com.griddynamics.genesis.notification.template.StringTemplateEngine;
import com.griddynamics.genesis.notification.template.TemplateEngine;
import com.griddynamics.genesis.plugin.PluginConfigurationContext;
import com.griddynamics.genesis.plugin.StepExecutionContext;
import com.griddynamics.genesis.plugin.adapter.AbstractPartialStepCoordinatorFactory;
import com.griddynamics.genesis.workflow.*;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.MailSender;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import scala.collection.JavaConversions;

import java.util.Properties;

public class NotificationStepCoordinatorFactory extends AbstractPartialStepCoordinatorFactory {

    private Logger log = LoggerFactory.getLogger(this.getClass());

    public NotificationStepCoordinatorFactory(String pluginId, PluginConfigurationContext pluginConfiguration) {
        super(pluginId, pluginConfiguration);
    }

    @Override
    public boolean isDefinedAt(Step step) {
        return step instanceof NotificationStep;
    }

    @Override
    public StepCoordinator apply(Step step, StepExecutionContext context) {
        return new NotificationStepCoordinator(context, (NotificationStep) step, emailSenderConfiguration(), getTemplateEngine());
    }

    public EmailSenderConfiguration emailSenderConfiguration() {
        java.util.Map<String,String> config = getConfig();
        String senderName = config.get(NotificationPluginConfig.senderName);
        String senderEmail = getRequiredParameter(config, NotificationPluginConfig.senderEmail);
        String smtpHost = getRequiredParameter(config, NotificationPluginConfig.smtpHost);
        Integer smtpPort = getIntParameter(config, NotificationPluginConfig.smtpPort, Short.MAX_VALUE * 2);
        String smtpUsername = config.get(NotificationPluginConfig.smtpUsername);
        String smtpPassword = config.get(NotificationPluginConfig.smtpPassword);
        Boolean useTls = Boolean.parseBoolean(config.get(NotificationPluginConfig.useTls));
        Boolean useSSL = Boolean.parseBoolean(config.get(NotificationPluginConfig.useSSL));
        Integer connectTimeout = getIntParameter(config, NotificationPluginConfig.connectTimeout, Integer.MAX_VALUE);
        Integer smtpTimeout = getIntParameter(config, NotificationPluginConfig.smtpTimeout, Integer.MAX_VALUE);
        return new EmailSenderConfiguration(senderName, senderEmail, smtpHost, smtpPort,
                smtpUsername, smtpPassword, useTls, connectTimeout, smtpTimeout, useSSL);
    }

    private String getRequiredParameter(java.util.Map<String, String> config, String parameterName) {
        String value = config.get(parameterName);
        if (StringUtils.isEmpty(value)) {
            throw new IllegalArgumentException(String.format("'%s' cannot be empty", parameterName));
        }
        return value;
    }

    private static Integer getIntParameter(java.util.Map<String, String> config, String paramName, int maxValue) {
        String paramValue = config.get(paramName);
        if (StringUtils.isNotEmpty(paramValue) && StringUtils.isNumeric(paramValue)) {
            try {
                int i = Integer.parseInt(paramValue);
                if (i > maxValue) {
                    throw new IllegalArgumentException(String.format("%s is too big: %s", paramName, paramValue));
                }
                return i;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(String.format("%s is is too big: %s", paramName, paramValue));
            }
        } else {
            throw new IllegalArgumentException(paramName + String.format("%s is not a number: '%s'", paramName, paramValue));
        }
    }

    private TemplateEngine getTemplateEngine() {
        String templateFolder = getConfig().get(NotificationPluginConfig.templateFolder);
        TemplateEngine templateEngine;
        templateEngine = new StringTemplateEngine(templateFolder);
        return templateEngine;
    }

}
