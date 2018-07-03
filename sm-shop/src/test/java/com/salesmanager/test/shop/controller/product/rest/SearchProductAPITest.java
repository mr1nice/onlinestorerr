package com.salesmanager.test.shop.controller.product.rest;

import java.nio.charset.Charset;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.salesmanager.shop.model.catalog.SearchProductList;
import com.salesmanager.shop.model.catalog.SearchProductRequest;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;

import static java.lang.System.out;
import static java.nio.charset.Charset.forName;
import static org.springframework.security.crypto.codec.Base64.encode;

public class SearchProductAPITest {
	
	private RestTemplate restTemplate;
	
	public HttpHeaders getHeader() {
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = new MediaType("application", "json", forName("UTF-8"));
        headers.setContentType(mediaType);
        String authorisation = "admin" + ":" + "password";
        byte[] encodedAuthorisation = encode(authorisation.getBytes());
        headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
        return headers;
    }


    @Ignore
    public void testSearch() throws Exception {

        SearchProductRequest searchRequest = new SearchProductRequest();
        searchRequest.setCount(15);
        searchRequest.setStart(0);
        searchRequest.setQuery("test");

        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = writer.writeValueAsString(searchRequest);

        out.println(json);

        HttpEntity<String> entity = new HttpEntity<String>(json, getHeader());

        restTemplate = new RestTemplate();

        ResponseEntity response = restTemplate.postForEntity("http://localhost:8080/api/v1/search", entity, SearchProductList.class);

        SearchProductList search = (SearchProductList) response.getBody();
        out.println("Search count : " + search.getProductCount());

    }

}
