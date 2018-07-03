package com.salesmanager.shop.store.api.v0.system;


import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import com.salesmanager.core.business.services.system.ModuleConfigurationService;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.web.bind.annotation.RequestMethod.DELETE;
import static org.springframework.web.bind.annotation.RequestMethod.POST;


@Controller
@RequestMapping("/services")
public class SystemRESTController {


    private static final Logger LOGGER = getLogger(SystemRESTController.class);

    @Inject
    private ModuleConfigurationService moduleConfigurationService;

    /**
     * Creates or updates a configuration module. A JSON has to be created on the client side which represents
     * an object that will create a new module (payment, shipping ...) which can be used and configured from
     * the administration tool. Here is an example of configuration accepted
     * <p>
     * {
     * "module": "PAYMENT",
     * "code": "paypal-express-checkout",
     * "type":"paypal",
     * "version":"104.0",
     * "regions": ["*"],
     * "image":"icon-paypal.png",
     * "configuration":[{"env":"TEST","scheme":"","host":"","port":"","uri":"","config1":"https://www.sandbox.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token="},{"env":"PROD","scheme":"","host":"","port":"","uri":"","config1":"https://www.paypal.com/cgi-bin/webscr?cmd=_express-checkout&token="}]
     * <p>
     * }
     * <p>
     * see : shopizer/sm-core/src/main/resources/reference/integrationmodules.json for more samples
     *
     * @param json
     * @param request
     * @param response
     * @throws Exception
     */
    @RequestMapping(value = "/private/system/module", method = POST, consumes = "application/json")
    @ResponseBody
    public AjaxResponse createOrUpdateModule(@RequestBody final String json, HttpServletRequest request, HttpServletResponse response) throws Exception {


        AjaxResponse resp = new AjaxResponse();

        try {


            LOGGER.debug("Creating or updating an integration module : " + json);

            moduleConfigurationService.createOrUpdateModule(json);

            response.setStatus(200);

            resp.setStatus(200);

        } catch (Exception e) {
            resp.setStatus(500);
            resp.setErrorMessage(e);
            response.sendError(503, "Exception while creating or updating the module " + e.getMessage());
        }

        return resp;

    }

    @RequestMapping(value = "/private/system/optin", method = POST)
    @ResponseBody
    public AjaxResponse createOptin(@RequestBody final String json, HttpServletRequest request, HttpServletResponse response) throws Exception {


        AjaxResponse resp = new AjaxResponse();

        try {
            LOGGER.debug("Creating an optin : " + json);
            //moduleConfigurationService.createOrUpdateModule(json);
            response.setStatus(200);
            resp.setStatus(200);

        } catch (Exception e) {
            resp.setStatus(500);
            resp.setErrorMessage(e);
            response.sendError(503, "Exception while creating optin " + e.getMessage());
        }

        return resp;

    }

    @RequestMapping(value = "/private/system/optin/{code}", method = DELETE)
    @ResponseBody
    public AjaxResponse deleteOptin(@RequestBody final String code, HttpServletRequest request, HttpServletResponse response) throws Exception {


        AjaxResponse resp = new AjaxResponse();

        try {
            LOGGER.debug("Delete optin : " + code);
            //moduleConfigurationService.createOrUpdateModule(json);
            response.setStatus(200);
            resp.setStatus(200);

        } catch (Exception e) {
            resp.setStatus(500);
            resp.setErrorMessage(e);
            response.sendError(503, "Exception while deleting optin " + e.getMessage());
        }

        return resp;

    }

    @RequestMapping(value = "/private/system/optin/{code}/customer", method = POST, consumes = "application/json")
    @ResponseBody
    public AjaxResponse createOptinCustomer(@RequestBody final String code, HttpServletRequest request, HttpServletResponse response) throws Exception {


        AjaxResponse resp = new AjaxResponse();

        try {
            LOGGER.debug("Adding a customer optin : " + code);
            //moduleConfigurationService.createOrUpdateModule(json);
            response.setStatus(200);
            resp.setStatus(200);

        } catch (Exception e) {
            resp.setStatus(500);
            resp.setErrorMessage(e);
            response.sendError(503, "Exception while creating uptin " + e.getMessage());
        }

        return resp;

    }

}
