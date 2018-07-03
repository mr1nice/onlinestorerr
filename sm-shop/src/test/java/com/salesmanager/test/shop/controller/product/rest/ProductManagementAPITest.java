package com.salesmanager.test.shop.controller.product.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.salesmanager.core.model.catalog.product.attribute.ProductOptionType;
import com.salesmanager.shop.model.catalog.category.Category;
import com.salesmanager.shop.model.catalog.manufacturer.Manufacturer;
import com.salesmanager.shop.model.catalog.manufacturer.ManufacturerDescription;
import com.salesmanager.shop.model.catalog.manufacturer.PersistableManufacturer;
import com.salesmanager.shop.model.catalog.product.*;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOption;
import com.salesmanager.shop.model.catalog.product.attribute.PersistableProductOptionValue;
import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionDescription;
import com.salesmanager.shop.model.catalog.product.attribute.ProductOptionValueDescription;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.*;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.salesmanager.core.model.catalog.product.attribute.ProductOptionType.Select;
import static java.lang.System.out;
import static java.nio.charset.Charset.forName;
import static org.springframework.security.crypto.codec.Base64.encode;


@Ignore
public class ProductManagementAPITest {
	
	private RestTemplate restTemplate;
	
	private Long testCategoryID;
	
	private Long testProductID;

	
	public HttpHeaders getHeader() {
        HttpHeaders headers = new HttpHeaders();
        MediaType mediaType = new MediaType("application", "json", forName("UTF-8"));
        headers.setContentType(mediaType);
        String authorisation = "admin" + ":" + "password";
        byte[] encodedAuthorisation = encode(authorisation.getBytes());
        headers.add("Authorization", "Basic " + new String(encodedAuthorisation));
        return headers;
    }

    @Test
    @Ignore
    public void createManufacturer() throws Exception {

        ManufacturerDescription description = new ManufacturerDescription();
        description.setLanguage("en");
        description.setName("Tag Heuer");
        description.setFriendlyUrl("tag-watches");
        description.setTitle("Tag Heuer");

        List<ManufacturerDescription> descriptions = new ArrayList<ManufacturerDescription>();
        descriptions.add(description);

        PersistableManufacturer manufacturer = new PersistableManufacturer();
        manufacturer.setOrder(1);
        manufacturer.setDescriptions(descriptions);


        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = writer.writeValueAsString(manufacturer);

        out.println(json);
		
/*		{
			  "descriptions" : [ {
			    "name" : "Tag Heuer",
			    "description" : null,
			    "friendlyUrl" : "tag-watches",
			    "keyWords" : null,
			    "highlights" : null,
			    "metaDescription" : null,
			    "title" : "Tag Heuer",
			    "language" : "en",
			    "id" : 0
			  } ],
			  "order" : 1,
			  "id" : 0
			}*/

        restTemplate = new RestTemplate();


        HttpEntity<String> entity = new HttpEntity<String>(json, getHeader());

        ResponseEntity response = restTemplate.postForEntity("http://localhost:8080/sm-shop/services/private/DEFAULT/manufacturer", entity, PersistableManufacturer.class);

        PersistableManufacturer manuf = (PersistableManufacturer) response.getBody();
        out.println("New Manufacturer ID : " + manuf.getId());


    }

    @Test
    @Ignore
    public void createProductReview() throws Exception {

        //requires an existing product
        //requires an existing customer
        PersistableProductReview review = new PersistableProductReview();
        review.setCustomerId(1L);
        review.setProductId(1L);
        review.setLanguage("en");
        review.setRating(2D);//rating is on 5
        review.setDescription("Not as good as expected. From what i understood that was supposed to be premium quality but unfortunately i had to return the item after one week... Verry disapointed !");
        review.setDate("2013-06-06");

        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = writer.writeValueAsString(review);

        out.println(json);

        /**
         * {
         "description" : "Excellent product !",
         "productId" : 1,
         "rating" : 4.5,
         "customerId" : 1,
         "date" : "2013-06-06",
         "language" : "en"
         }
         */


        restTemplate = new RestTemplate();


        HttpEntity<String> entity = new HttpEntity<String>(json, getHeader());

        ResponseEntity response = restTemplate.postForEntity("http://localhost:8080/sm-shop/services/private/DEFAULT/product/review", entity, PersistableProductReview.class);

        PersistableProductReview rev = (PersistableProductReview) response.getBody();
        out.println("New ProductReview ID : " + rev.getId());


    }

    @Test
    @Ignore
    public void createOptionValue() throws Exception {

        ProductOptionValueDescription description = new ProductOptionValueDescription();
        description.setLanguage("en");
        description.setName("Red");

        List<ProductOptionValueDescription> descriptions = new ArrayList<ProductOptionValueDescription>();
        descriptions.add(description);

        PersistableProductOptionValue optionValue = new PersistableProductOptionValue();
        optionValue.setOrder(1);
        optionValue.setCode("colorred");
        optionValue.setDescriptions(descriptions);


        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = writer.writeValueAsString(optionValue);

        out.println(json);

        /**
         * {
         "descriptions" : [ {
         "name" : "Red",
         "description" : null,
         "friendlyUrl" : null,
         "keyWords" : null,
         "highlights" : null,
         "metaDescription" : null,
         "title" : null,
         "language" : "en",
         "id" : 0
         } ],
         "order" : 1,
         "code" : "color-red",
         "id" : 0
         }
         */

        restTemplate = new RestTemplate();

        HttpEntity<String> entity = new HttpEntity<String>(json, getHeader());

        ResponseEntity response = restTemplate.postForEntity("http://localhost:8080/sm-shop/services/private/DEFAULT/product/optionValue", entity, PersistableProductOptionValue.class);

        PersistableProductOptionValue opt = (PersistableProductOptionValue) response.getBody();
        out.println("New optionValue ID : " + opt.getId());

    }

    @Test
    @Ignore
    public void createOption() throws Exception {

        ProductOptionDescription description = new ProductOptionDescription();
        description.setLanguage("en");
        description.setName("Color");

        List<ProductOptionDescription> descriptions = new ArrayList<ProductOptionDescription>();
        descriptions.add(description);

        PersistableProductOption option = new PersistableProductOption();
        option.setOrder(1);
        option.setCode("color");
        option.setType(Select.name());
        option.setDescriptions(descriptions);


        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = writer.writeValueAsString(option);

        out.println(json);

        /**
         * {
         "descriptions" : [ {
         "name" : "Color",
         "description" : null,
         "friendlyUrl" : null,
         "keyWords" : null,
         "highlights" : null,
         "metaDescription" : null,
         "title" : null,
         "language" : "en",
         "id" : 0
         } ],
         "type" : SELECT,
         "order" : 1,
         "code" : "color",
         "id" : 0
         }
         */

        restTemplate = new RestTemplate();

        HttpEntity<String> entity = new HttpEntity<String>(json, getHeader());

        ResponseEntity response = restTemplate.postForEntity("http://localhost:8080/sm-shop/services/private/DEFAULT/product/option", entity, PersistableProductOption.class);

        PersistableProductOption opt = (PersistableProductOption) response.getBody();
        out.println("New option ID : " + opt.getId());

    }
		
	
	@Test
	@Ignore
	public void getProducts() throws Exception {
		restTemplate = new RestTemplate();
		
		HttpEntity<String> httpEntity = new HttpEntity<String>(getHeader());
		
		ResponseEntity<ReadableProduct[]> response = restTemplate.exchange("http://localhost:8080/sm-shop/services/rest/products/DEFAULT/en/"+testCategoryID, HttpMethod.GET, httpEntity, ReadableProduct[].class);
		
		if(response.getStatusCode() != HttpStatus.OK){
			throw new Exception();
		}else{
			System.out.println(response.getBody().length + " Product records found.");
		}
	}
	
	@Test
	@Ignore
	public void putProduct() throws Exception {
        restTemplate = new RestTemplate();

    }
	
	@Test
	@Ignore
	public void postProduct() throws Exception {
        restTemplate = new RestTemplate();

        PersistableProduct product = new PersistableProduct();

        String code = "abcdef";


        String categoryCode = "ROOT";


        Category category = new Category();
        category.setCode(categoryCode);
        List<Category> categories = new ArrayList<Category>();
        categories.add(category);


        String manufacturer = "temple";
        Manufacturer collection = new Manufacturer();
        collection.setCode(manufacturer);

        product.setSku(code);
        //product.setCategories(categories); //no category assigned for now

        product.setSortOrder(0);
        product.setAvailable(true);
        product.setProductVirtual(false);
        product.setQuantityOrderMinimum(1);
        product.setProductShipeable(true);
        String image = "/Users/carlsamson/Documents/csti/IMG_4626.jpg";

        File imgPath = new File(image);


        product.setProductHeight(new BigDecimal(20));
        product.setProductLength(new BigDecimal(20));
        product.setProductWeight(new BigDecimal(20));
        product.setProductWidth(new BigDecimal(20));
        product.setQuantity(5);
        product.setQuantityOrderMaximum(2);


        PersistableProductPrice productPrice = new PersistableProductPrice();
        productPrice.setDefaultPrice(true);

        productPrice.setOriginalPrice(new BigDecimal(250));
        productPrice.setDiscountedPrice(new BigDecimal(125));

        List<PersistableProductPrice> productPriceList = new ArrayList<PersistableProductPrice>();
        productPriceList.add(productPrice);

        product.setProductPrices(productPriceList);

        List<ProductDescription> descriptions = new ArrayList<ProductDescription>();
        ProductDescription description = new ProductDescription();
        description.setLanguage("en");
        description.setTitle("Buddha Head");
        description.setName("Buddha Head");
        description.setDescription("Buddha Head");
        description.setFriendlyUrl("buddha-head");

        descriptions.add(description);
        description = new ProductDescription();
        description.setLanguage("fr");
        description.setTitle("Tête de Buddha");
        description.setName("Tête de Buddha");
        description.setDescription(description.getName());
        description.setFriendlyUrl("tete-de-buddha");
        //

        descriptions.add(description);

        product.setDescriptions(descriptions);
        RentalOwner owner = new RentalOwner();
        owner.setId(1L);
        product.setOwner(owner);


        ObjectWriter writer = new ObjectMapper().writer().withDefaultPrettyPrinter();
        String json = writer.writeValueAsString(product);

        out.println(json);


        HttpEntity<String> entity = new HttpEntity<String>(json, getHeader());
        ResponseEntity response = restTemplate.postForEntity("http://localhost:8080/api/v1/product", entity, PersistableProduct.class);

        PersistableProduct prod = (PersistableProduct) response.getBody();


        out.println("---------------------");


    }
	
	@Test
	@Ignore
	public void deleteProduct() throws Exception {
		restTemplate = new RestTemplate();
		
		HttpEntity<String> httpEntity = new HttpEntity<String>(getHeader());
		
		restTemplate.exchange("http://localhost:8080/sm-shop/services/rest/products/DEFAULT/en/"+testCategoryID+"/"+testProductID, HttpMethod.DELETE, httpEntity, ReadableProduct.class);
		System.out.println("Product "+testProductID+" Deleted.");
	}

    public byte[] extractBytes(File imgPath) throws Exception {

        FileInputStream fis = new FileInputStream(imgPath);

        BufferedInputStream inputStream = new BufferedInputStream(fis);
        byte[] fileBytes = new byte[(int) imgPath.length()];
        inputStream.read(fileBytes);
        inputStream.close();

        return fileBytes;


    }
	
}
