package com.salesmanager.shop.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.shop.constants.ApplicationConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.salesmanager.shop.constants.ApplicationConstants.RECAPTCHA_PRIVATE_KEY;
import static com.salesmanager.shop.constants.ApplicationConstants.RECAPTCHA_URL;
import static java.lang.Boolean.valueOf;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.impl.client.HttpClientBuilder.create;
import static org.apache.http.util.EntityUtils.toByteArray;

@Component
public class CaptchaRequestUtils {

    @Inject
    private CoreConfiguration configuration; //for reading public and secret key

    private static final String SUCCESS_INDICATOR = "success";

    public boolean checkCaptcha(String gRecaptchaResponse) throws Exception {

        HttpClient client = create().build();

        String url = configuration.getProperty(RECAPTCHA_URL);
        ;

        List<NameValuePair> data = new ArrayList<NameValuePair>();
        data.add(new BasicNameValuePair("secret", configuration.getProperty(RECAPTCHA_PRIVATE_KEY)));
        data.add(new BasicNameValuePair("response", gRecaptchaResponse));

    /* NameValuePair[] data = {
                new NameValuePair("secret", configuration.getProperty(ApplicationConstants.RECAPTCHA_PRIVATE_KEY)),
                new NameValuePair("response", gRecaptchaResponse)
              };*/

        // Create a method instance.
        HttpPost post = new HttpPost(url);
        post.setEntity(new UrlEncodedFormEntity(data, UTF_8));

        boolean checkCaptcha = false;


        try {
            // Execute the method.
            HttpResponse httpResponse = client.execute(post);
            int statusCode = httpResponse.getStatusLine().getStatusCode();

            if (statusCode != SC_OK) {
                throw new Exception("Got an invalid response from reCaptcha " + url + " [" + httpResponse.getStatusLine() + "]");
            }

            // Read the response body.
            HttpEntity entity = httpResponse.getEntity();
            byte[] responseBody = toByteArray(entity);


            // Deal with the response.
            // Use caution: ensure correct character encoding and is not binary data
            //System.out.println(new String(responseBody));

            String json = new String(responseBody);

            Map<String, String> map = new HashMap<String, String>();
            ObjectMapper mapper = new ObjectMapper();

            map = mapper.readValue(json,
                    new TypeReference<HashMap<String, String>>() {
                    });

            String successInd = map.get(SUCCESS_INDICATOR);

            if (isBlank(successInd)) {
                throw new Exception("Unreadable response from reCaptcha " + json);
            }

            Boolean responseBoolean = valueOf(successInd);

            if (responseBoolean) {
                checkCaptcha = true;
            }

            return checkCaptcha;

        } finally {
            // Release the connection.
            post.releaseConnection();
        }
    }


}
