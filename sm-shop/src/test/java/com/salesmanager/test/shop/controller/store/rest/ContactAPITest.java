package com.salesmanager.test.shop.controller.store.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.shop.model.shop.ContactForm;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;

import static java.lang.System.out;
import static java.nio.charset.Charset.forName;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.OK;

@Ignore
public class ContactAPITest {
	
	private RestTemplate restTemplate;


	private HttpHeaders getHeader() {
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = new MediaType("application", "json", forName("UTF-8"));
        headers.setContentType(mediaType);
        return headers;
    }

    @Test
    @Ignore
    public void contactUs() throws Exception {
        restTemplate = new RestTemplate();


        ContactForm contact = new ContactForm();
        contact.setComment("A few good words for you!");
        contact.setEmail("me@test.com");
        contact.setName("Johny Depp");
        contact.setSubject("Hello ny friend");

        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = writer.writeValueAsString(contact);

        out.println(json);

        HttpEntity<String> httpEntity = new HttpEntity<String>(json, getHeader());

        ResponseEntity<AjaxResponse> response = restTemplate.exchange("http://localhost:8080/sm-shop/services/public/DEFAULT/contact", POST, httpEntity, AjaxResponse.class);

        if (response.getStatusCode() != OK) {
            throw new Exception();
        } else {
            out.println(response.getBody() + " Success sending contact");
        }
    }
	
		
}
