package com.salesmanager.test.shop.controller.system.rest;

import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.*;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.out;
import static java.nio.charset.Charset.forName;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.crypto.codec.Base64.encode;

@Ignore
public class SystemAPITest {
	
	private RestTemplate restTemplate;


	private HttpHeaders getHeader() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(APPLICATION_JSON);
        MediaType mediaType = new MediaType("application", "json", forName("UTF-8"));
        headers.setContentType(mediaType);
        String authorisation = "admin" + ":" + "password";
        byte[] encodedAuthorisation = encode(authorisation.getBytes());
        headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
        return headers;
    }

    //@Test
    @Ignore
    public void createIntegrationModule() throws Exception {
        restTemplate = new RestTemplate();

        String json =

                "{"
                        + "\"module\":\"PAYMENT\","
                        + "\"code\":\"braintree\","
                        + "\"type\":\"creditcard\","
                        + "\"version\":\"1.0\","
                        + "\"regions\":[\"US\",\"CA\",\"GB\",\"AU\",\"FI\",\"DK\",\"IE\",\"NO\",\"SE\",\"AL\",\"AD\",\"AT\",\"BY\",\"BE\",\"BG\",\"HY\",\"CY\",\"CZ\",\"FR\",\"GR\",\"IS\",\"IE\",\"IM\",\"IT\",\"PL\",\"LU\",\"CH\",\"RS\",\"SG\",\"MY\",\"HK\",\"NZ\"],"
                        + "\"image\":\"braintree.jpg\","
                        + "\"configuration\":[{\"env\":\"TEST\",\"scheme\":\"https\",\"host\":\"NOT_REQUIRED\",\"port\":\"NOT_REQUIRED\",\"uri\":\"/\"},{\"env\":\"PROD\",\"scheme\":\"https\",\"host\":\"NOT_REQUIRED\",\"port\":\"NOT_REQUIRED\",\"uri\":\"/\"}]"
                        + "}";


        /**
         "{"
         +	"\"module\":\"SHIPPING\","
         +	"\"code\":\"priceByDistance\","
         +	"\"version\":\"1.0\","
         +	"\"regions\":[\"*\"]"
         +"}";
         **/


        out.println(json);

        List<HttpMessageConverter<?>> messageConverters = new ArrayList<HttpMessageConverter<?>>();
        //messageConverters.add(new FormHttpMessageConverter());
        //messageConverters.add(new StringHttpMessageConverter());
        messageConverters.add(new MappingJackson2HttpMessageConverter());

        restTemplate.setMessageConverters(messageConverters);

        HttpEntity<String> httpEntity = new HttpEntity<String>(json, getHeader());

        ResponseEntity<AjaxResponse> response = restTemplate.exchange("http://localhost:8080/services/private/system/module", POST, httpEntity, AjaxResponse.class);

        if (response.getStatusCode() != OK) {
            throw new Exception();
        } else {
            out.println(response.getBody() + " Success creating module");
        }
    }
	
		
}
