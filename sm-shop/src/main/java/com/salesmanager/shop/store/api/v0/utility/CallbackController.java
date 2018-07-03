package com.salesmanager.shop.store.api.v0.utility;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.springframework.web.bind.annotation.RequestMethod.GET;


@Controller
@RequestMapping("/services")
public class CallbackController {

    private static final String VERIFY_MESSENGER_WEBHOOK = "VERIFY_MESSENGER_WEBHOOK";

    @RequestMapping(value = "/public/callBack", method = GET)
    public void callBack(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String verificationToken = request.getParameter("hub.verify_token");

        if (!isBlank(verificationToken)) {

            if (verificationToken.equals(VERIFY_MESSENGER_WEBHOOK)) {
                String replyToken = request.getParameter("hub.challenge");

                response.setStatus(SC_OK);
                response.getWriter().write(replyToken);
                response.getWriter().flush();
                response.getWriter().close();
            }

        }
        return;
    }

    public String verifyCallBack() {
        return null;
    }

}
