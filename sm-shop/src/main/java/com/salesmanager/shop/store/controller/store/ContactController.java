package com.salesmanager.shop.store.controller.store;

import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.core.model.content.Content;
import com.salesmanager.core.model.content.ContentDescription;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.ApplicationConstants;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.shop.ContactForm;
import com.salesmanager.shop.model.shop.PageInformation;
import com.salesmanager.shop.store.controller.AbstractController;
import com.salesmanager.shop.store.controller.ControllerConstants;
import com.salesmanager.shop.utils.CaptchaRequestUtils;
import com.salesmanager.shop.utils.EmailTemplatesUtils;
import com.salesmanager.shop.utils.LabelUtils;
import com.salesmanager.shop.utils.LocaleUtils;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Locale;

import static com.salesmanager.core.business.utils.ajax.AjaxResponse.RESPONSE_STATUS_FAIURE;
import static com.salesmanager.core.business.utils.ajax.AjaxResponse.RESPONSE_STATUS_SUCCESS;
import static com.salesmanager.shop.constants.ApplicationConstants.RECAPTCHA_PUBLIC_KEY;
import static com.salesmanager.shop.constants.Constants.*;
import static com.salesmanager.shop.store.controller.ControllerConstants.Tiles;
import static com.salesmanager.shop.store.controller.ControllerConstants.Tiles.Content.contactus;
import static com.salesmanager.shop.utils.LocaleUtils.getLocale;
import static org.apache.commons.lang.StringUtils.isBlank;

@Controller
public class ContactController extends AbstractController {
	
	private static final Logger LOGGER = LoggerFactory
			.getLogger(ContactController.class);
	
	@Inject
	private ContentService contentService;
	
	@Inject
	private CoreConfiguration coreConfiguration;
	
	@Inject
	private LabelUtils messages;
	
	@Inject
	private EmailTemplatesUtils emailTemplatesUtils;
	
	@Inject
	private CaptchaRequestUtils captchaRequestUtils;
	
	private final static String CONTACT_LINK = "CONTACT";
	
	
	@RequestMapping("/shop/store/contactus.html")
	public String displayContact(Model model, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {

        MerchantStore store = (MerchantStore) request.getAttribute(MERCHANT_STORE);

        request.setAttribute(LINK_CODE, CONTACT_LINK);

        Language language = (Language) request.getAttribute("LANGUAGE");

        ContactForm contact = new ContactForm();
        model.addAttribute("contact", contact);

        model.addAttribute("recapatcha_public_key", coreConfiguration.getProperty(RECAPTCHA_PUBLIC_KEY));

        Content content = contentService.getByCode(CONTENT_CONTACT_US, store, language);
        ContentDescription contentDescription = null;
        if (content != null && content.isVisible()) {
            contentDescription = content.getDescription();
        }

        if (contentDescription != null) {
            PageInformation pageInformation = new PageInformation();
            pageInformation.setPageDescription(contentDescription.getMetatagDescription());
            pageInformation.setPageKeywords(contentDescription.getMetatagKeywords());
            pageInformation.setPageTitle(contentDescription.getTitle());
            pageInformation.setPageUrl(contentDescription.getName());

            request.setAttribute(REQUEST_PAGE_INFORMATION, pageInformation);

            model.addAttribute("content", contentDescription);

        }
        StringBuilder template = new StringBuilder().append(contactus).append(".").append(store.getStoreTemplate());
        return template.toString();


    }

	
	@RequestMapping(value={"/shop/store/{storeCode}/contact"}, method=RequestMethod.POST)
	public @ResponseBody String sendEmail(@ModelAttribute(value="contact") ContactForm contact, BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response, Locale locale) throws Exception {

		AjaxResponse ajaxResponse = new AjaxResponse();
		
		MerchantStore store = (MerchantStore)request.getAttribute(Constants.MERCHANT_STORE);

		try {

            if (!isBlank(request.getParameter("g-recaptcha-response"))) {
                boolean validateCaptcha = captchaRequestUtils.checkCaptcha(request.getParameter("g-recaptcha-response"));

                if (!validateCaptcha) {
                    LOGGER.debug("Captcha response does not matched");
                    FieldError error = new FieldError("captchaChallengeField", "captchaChallengeField", messages.getMessage("validaion.recaptcha.not.matched", locale));
                    bindingResult.addError(error);
                }
            }


            if (bindingResult.hasErrors()) {
                LOGGER.debug("found {} validation error while validating in customer registration ",
                        bindingResult.getErrorCount());
                ajaxResponse.setErrorString(bindingResult.getAllErrors().get(0).getDefaultMessage());
                ajaxResponse.setStatus(RESPONSE_STATUS_FAIURE);
                return ajaxResponse.toJSONString();

            }

            emailTemplatesUtils.sendContactEmail(contact, store, getLocale(store.getDefaultLanguage()), request.getContextPath());

            ajaxResponse.setStatus(RESPONSE_STATUS_SUCCESS);
        } catch(Exception e) {
			LOGGER.error("An error occured while trying to send an email",e);
			ajaxResponse.setStatus(AjaxResponse.RESPONSE_STATUS_FAIURE);
		}
		
		return ajaxResponse.toJSONString();
		
		
	}
	
	
}
