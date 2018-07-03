package com.salesmanager.shop.admin.controller.configurations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.salesmanager.core.business.modules.email.EmailConfig;
import com.salesmanager.core.business.services.system.EmailService;
import com.salesmanager.core.business.services.system.MerchantConfigurationService;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.system.MerchantConfiguration;
import com.salesmanager.core.model.system.MerchantConfigurationType;
import com.salesmanager.shop.admin.controller.ControllerConstants;
import com.salesmanager.shop.admin.model.web.ConfigListWrapper;
import com.salesmanager.shop.admin.model.web.Menu;
import com.salesmanager.shop.constants.Constants;

import static com.salesmanager.core.model.system.MerchantConfigurationType.CONFIG;
import static com.salesmanager.shop.admin.controller.ControllerConstants.Tiles;
import static com.salesmanager.shop.admin.controller.ControllerConstants.Tiles.Configuration;
import static com.salesmanager.shop.admin.controller.ControllerConstants.Tiles.Configuration.accounts;
import static com.salesmanager.shop.admin.controller.ControllerConstants.Tiles.Configuration.email;
import static com.salesmanager.shop.constants.Constants.*;
import static java.lang.Boolean.parseBoolean;


@Controller
public class ConfigurationController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationController.class);
	
	@Inject
	private MerchantConfigurationService merchantConfigurationService;
	
	@Inject
	private EmailService emailService;

	@Inject
	Environment env;
	

	@PreAuthorize("hasRole('AUTH')")
	@RequestMapping(value="/admin/configuration/accounts.html", method=RequestMethod.GET)
	public String displayAccountsConfguration(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        setConfigurationMenu(model, request);
        List<MerchantConfiguration> configs = new ArrayList<MerchantConfiguration>();
        MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);
        MerchantConfiguration merchantFBConfiguration = merchantConfigurationService.getMerchantConfiguration(KEY_FACEBOOK_PAGE_URL, store);
        if (null == merchantFBConfiguration) {
            merchantFBConfiguration = new MerchantConfiguration();
            merchantFBConfiguration.setKey(KEY_FACEBOOK_PAGE_URL);
            merchantFBConfiguration.setMerchantConfigurationType(CONFIG);
        }
        configs.add(merchantFBConfiguration);

        MerchantConfiguration merchantGoogleAnalyticsConfiguration = merchantConfigurationService.getMerchantConfiguration(KEY_GOOGLE_ANALYTICS_URL, store);
        if (null == merchantGoogleAnalyticsConfiguration) {
            merchantGoogleAnalyticsConfiguration = new MerchantConfiguration();
            merchantGoogleAnalyticsConfiguration.setKey(KEY_GOOGLE_ANALYTICS_URL);
            merchantGoogleAnalyticsConfiguration.setMerchantConfigurationType(CONFIG);
        }
        configs.add(merchantGoogleAnalyticsConfiguration);

        MerchantConfiguration merchantInstagramConfiguration = merchantConfigurationService.getMerchantConfiguration(KEY_INSTAGRAM_URL, store);
        if (null == merchantInstagramConfiguration) {
            merchantInstagramConfiguration = new MerchantConfiguration();
            merchantInstagramConfiguration.setKey(KEY_INSTAGRAM_URL);
            merchantInstagramConfiguration.setMerchantConfigurationType(CONFIG);
        }
        configs.add(merchantInstagramConfiguration);

        MerchantConfiguration merchantPinterestConfiguration = merchantConfigurationService.getMerchantConfiguration(KEY_PINTEREST_PAGE_URL, store);
        if (null == merchantPinterestConfiguration) {
            merchantPinterestConfiguration = new MerchantConfiguration();
            merchantPinterestConfiguration.setKey(KEY_PINTEREST_PAGE_URL);
            merchantPinterestConfiguration.setMerchantConfigurationType(CONFIG);
        }
        configs.add(merchantPinterestConfiguration);

        MerchantConfiguration twitterConfiguration = merchantConfigurationService.getMerchantConfiguration(KEY_TWITTER_HANDLE, store);
        if (null == twitterConfiguration) {
            twitterConfiguration = new MerchantConfiguration();
            twitterConfiguration.setKey(KEY_TWITTER_HANDLE);
            twitterConfiguration.setMerchantConfigurationType(CONFIG);
        }
        configs.add(twitterConfiguration);

        ConfigListWrapper configWrapper = new ConfigListWrapper();
        configWrapper.setMerchantConfigs(configs);
        model.addAttribute("configuration", configWrapper);

        return accounts;
    }
	
	@PreAuthorize("hasRole('AUTH')")
	@RequestMapping(value="/admin/configuration/saveConfiguration.html", method=RequestMethod.POST)
	public String saveConfigurations(@ModelAttribute("configuration") ConfigListWrapper configWrapper, BindingResult result, Model model, HttpServletRequest request, Locale locale) throws Exception
	{
		setConfigurationMenu(model, request);
		List<MerchantConfiguration> configs = configWrapper.getMerchantConfigs();
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
		for(MerchantConfiguration mConfigs : configs)
		{
			mConfigs.setMerchantStore(store);
			if(!StringUtils.isBlank(mConfigs.getValue())) {
				mConfigs.setMerchantConfigurationType(MerchantConfigurationType.CONFIG);
				merchantConfigurationService.saveOrUpdate(mConfigs);
			} else {
                MerchantConfiguration config = merchantConfigurationService.getMerchantConfiguration(mConfigs.getKey(), store);
                if (config != null) {
                    merchantConfigurationService.delete(config);
                }
            }
		}	
		model.addAttribute("success","success");
		model.addAttribute("configuration",configWrapper);
		return com.salesmanager.shop.admin.controller.ControllerConstants.Tiles.Configuration.accounts;
		
	}
	
	@PreAuthorize("hasRole('AUTH')")
	@RequestMapping(value="/admin/configuration/email.html", method=RequestMethod.GET)
	public String displayEmailSettings(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		setEmailConfigurationMenu(model, request);
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
		EmailConfig emailConfig = emailService.getEmailConfiguration(store);
		if(emailConfig == null) {
            emailConfig = new EmailConfig();
            emailConfig.setProtocol(env.getProperty("mailSender.protocol"));
            emailConfig.setHost(env.getProperty("mailSender.host"));
            emailConfig.setPort(env.getProperty("mailSender.port}"));
            emailConfig.setUsername(env.getProperty("mailSender.username"));
            emailConfig.setPassword(env.getProperty("mailSender.password"));
            emailConfig.setSmtpAuth(parseBoolean(env.getProperty("mailSender.mail.smtp.auth")));
            emailConfig.setStarttls(parseBoolean(env.getProperty("mail.smtp.starttls.enable")));
        }
		
		model.addAttribute("configuration", emailConfig);
		return ControllerConstants.Tiles.Configuration.email;
	}
	
	@PreAuthorize("hasRole('AUTH')")
	@RequestMapping(value="/admin/configuration/saveEmailConfiguration.html", method=RequestMethod.POST)
	public String saveEmailSettings(@ModelAttribute("configuration") EmailConfig config, BindingResult result, Model model, HttpServletRequest request, Locale locale) throws Exception {
        setEmailConfigurationMenu(model, request);
        MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);
        EmailConfig emailConfig = emailService.getEmailConfiguration(store);
        if (emailConfig == null) {
            emailConfig = new EmailConfig();
        }
        emailConfig.setProtocol(config.getProtocol());
        emailConfig.setHost(config.getHost());
        emailConfig.setPort(config.getPort());
        emailConfig.setUsername(config.getUsername());
        emailConfig.setPassword(config.getPassword());
        emailConfig.setSmtpAuth(config.isSmtpAuth());
        emailConfig.setStarttls(config.isStarttls());

        emailService.saveEmailConfiguration(emailConfig, store);

        model.addAttribute("configuration", emailConfig);
        model.addAttribute("success", "success");
        return email;
    }
	
	private void setConfigurationMenu(Model model, HttpServletRequest request) throws Exception {
		
		Map<String,String> activeMenus = new HashMap<String,String>();
		activeMenus.put("configuration", "configuration");
		activeMenus.put("accounts-conf", "accounts-conf");
		
		@SuppressWarnings("unchecked")
		Map<String, Menu> menus = (Map<String, Menu>)request.getAttribute("MENUMAP");
		
		Menu currentMenu = (Menu)menus.get("configuration");
		model.addAttribute("currentMenu",currentMenu);
		model.addAttribute("activeMenus",activeMenus);
	}
	
	private void setEmailConfigurationMenu(Model model, HttpServletRequest request) throws Exception {
		
		Map<String,String> activeMenus = new HashMap<String,String>();
		activeMenus.put("configuration", "configuration");
		activeMenus.put("email-conf", "email-conf");
		
		@SuppressWarnings("unchecked")
		Map<String, Menu> menus = (Map<String, Menu>)request.getAttribute("MENUMAP");
		
		Menu currentMenu = (Menu)menus.get("configuration");
		model.addAttribute("currentMenu",currentMenu);
		model.addAttribute("activeMenus",activeMenus);
	}
}
