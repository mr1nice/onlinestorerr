package com.salesmanager.core.business.services.reference.init;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.business.services.catalog.product.type.ProductTypeService;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.reference.country.CountryService;
import com.salesmanager.core.business.services.reference.currency.CurrencyService;
import com.salesmanager.core.business.services.reference.language.LanguageService;
import com.salesmanager.core.business.services.reference.loader.IntegrationModulesLoader;
import com.salesmanager.core.business.services.reference.loader.ZonesLoader;
import com.salesmanager.core.business.services.reference.zone.ZoneService;
import com.salesmanager.core.business.services.system.ModuleConfigurationService;
import com.salesmanager.core.business.services.tax.TaxClassService;
import com.salesmanager.core.constants.SchemaConstant;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.catalog.product.manufacturer.ManufacturerDescription;
import com.salesmanager.core.model.catalog.product.type.ProductType;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.country.Country;
import com.salesmanager.core.model.reference.country.CountryDescription;
import com.salesmanager.core.model.reference.currency.Currency;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.reference.zone.Zone;
import com.salesmanager.core.model.reference.zone.ZoneDescription;
import com.salesmanager.core.model.system.IntegrationModule;
import com.salesmanager.core.model.tax.taxclass.TaxClass;

import static com.salesmanager.core.model.merchant.MerchantStore.DEFAULT_STORE;
import static com.salesmanager.core.model.tax.taxclass.TaxClass.DEFAULT_TAX_CLASS;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.Currency.getInstance;

@Service("initializationDatabase")
public class InitializationDatabaseImpl implements InitializationDatabase {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(InitializationDatabaseImpl.class);
	

	
	@Inject
	private ZoneService zoneService;
	
	@Inject
	private LanguageService languageService;
	
	@Inject
	private CountryService countryService;
	
	@Inject
	private CurrencyService currencyService;
	
	@Inject
	protected MerchantStoreService merchantService;
		
	@Inject
	protected ProductTypeService productTypeService;
	
	@Inject
	private TaxClassService taxClassService;
	
	@Inject
	private ZonesLoader zonesLoader;
	
	@Inject
	private IntegrationModulesLoader modulesLoader;
	
	@Inject
	private ManufacturerService manufacturerService;
	
	@Inject
	private ModuleConfigurationService moduleConfigurationService;
	

	
	private String name;
	
	public boolean isEmpty() {
		return languageService.count() == 0;
	}
	
	@Transactional
	public void populate(String contextName) throws ServiceException {
		this.name =  contextName;
		
		createLanguages();
		createCountries();
		createZones();
		createCurrencies();
		createSubReferences();
		createModules();
		createMerchant();


	}
	


	private void createCurrencies() throws ServiceException {
		LOGGER.info(String.format("%s : Populating Currencies ", name));

		for (String code : SchemaConstant.CURRENCY_MAP.keySet()) {
  
            try {
				java.util.Currency c = getInstance(code);

				if (c == null) {
					LOGGER.info(format("%s : Populating Currencies : no currency for code : %s", name, code));
				}

				Currency currency = new Currency();
				currency.setName(c.getCurrencyCode());
				currency.setCurrency(c);
				currencyService.create(currency);
			} catch (IllegalArgumentException e) {
            	LOGGER.info(String.format("%s : Populating Currencies : no currency for code : %s", name, code));
            }
        }  
	}

	private void createCountries() throws ServiceException {
		LOGGER.info(String.format("%s : Populating Countries ", name));
		List<Language> languages = languageService.list();
		for(String code : SchemaConstant.COUNTRY_ISO_CODE) {
			Locale locale = SchemaConstant.LOCALES.get(code);
			if (locale != null) {
				Country country = new Country(code);
				countryService.create(country);
				
				for (Language language : languages) {
					String name = locale.getDisplayCountry(new Locale(language.getCode()));
					CountryDescription description = new CountryDescription(language, name);
					countryService.addCountryDescription(country, description);
				}
			}
		}
	}
	
	private void createZones() throws ServiceException {
		LOGGER.info(String.format("%s : Populating Zones ", name));
        try {

    		  Map<String,Zone> zonesMap = new HashMap<String,Zone>();
    		  zonesMap = zonesLoader.loadZones("reference/zoneconfig.json");
              
              for (Map.Entry<String, Zone> entry : zonesMap.entrySet()) {
            	    String key = entry.getKey();
            	    Zone value = entry.getValue();
            	    if(value.getDescriptions()==null) {
            	    	LOGGER.warn("This zone " + key + " has no descriptions");
            	    	continue;
            	    }
            	    
            	    List<ZoneDescription> zoneDescriptions = value.getDescriptions();
            	    value.setDescriptons(null);

            	    zoneService.create(value);
            	    
            	    for(ZoneDescription description : zoneDescriptions) {
            	    	description.setZone(value);
            	    	zoneService.addDescription(value, description);
            	    }
              }

  		} catch (Exception e) {
  		    
  			throw new ServiceException(e);
  		}

	}

	private void createLanguages() throws ServiceException {
		LOGGER.info(String.format("%s : Populating Languages ", name));
		for(String code : SchemaConstant.LANGUAGE_ISO_CODE) {
			Language language = new Language(code);
			languageService.create(language);
		}
	}
	
	private void createMerchant() throws ServiceException {
        LOGGER.info(format("%s : Creating merchant ", name));

        Date date = new Date(currentTimeMillis());

        Language en = languageService.getByCode("en");
        Country ca = countryService.getByCode("CA");
        Currency currency = currencyService.getByCode("CAD");
        Zone qc = zoneService.getByCode("QC");

        List<Language> supportedLanguages = new ArrayList<Language>();
        supportedLanguages.add(en);
        MerchantStore store = new MerchantStore();
        store.setCountry(ca);
        store.setCurrency(currency);
        store.setDefaultLanguage(en);
        store.setInBusinessSince(date);
        store.setZone(qc);
        store.setStorename("Default store");
        store.setStorephone("888-888-8888");
        store.setCode(DEFAULT_STORE);
        store.setStorecity("My city");
        store.setStoreaddress("1234 Street address");
        store.setStorepostalcode("H2H-2H2");
        store.setStoreEmailAddress("john@test.com");
        store.setDomainName("localhost:8080");
        store.setStoreTemplate("generic");
        store.setLanguages(supportedLanguages);

        merchantService.create(store);


        TaxClass taxclass = new TaxClass(DEFAULT_TAX_CLASS);
        taxclass.setMerchantStore(store);

        taxClassService.create(taxclass);
        Manufacturer defaultManufacturer = new Manufacturer();
        defaultManufacturer.setCode("DEFAULT");
        defaultManufacturer.setMerchantStore(store);

        ManufacturerDescription manufacturerDescription = new ManufacturerDescription();
        manufacturerDescription.setLanguage(en);
        manufacturerDescription.setName("DEFAULT");
        manufacturerDescription.setManufacturer(defaultManufacturer);
        manufacturerDescription.setDescription("DEFAULT");
        defaultManufacturer.getDescriptions().add(manufacturerDescription);

        manufacturerService.create(defaultManufacturer);


    }

	private void createModules() throws ServiceException {
		
		try {
			
			List<IntegrationModule> modules = modulesLoader.loadIntegrationModules("reference/integrationmodules.json");
            for (IntegrationModule entry : modules) {
        	    moduleConfigurationService.create(entry);
          }
			
			
		} catch (Exception e) {
			throw new ServiceException(e);
		}
		
		
	}
	
	private void createSubReferences() throws ServiceException {
		
		LOGGER.info(String.format("%s : Loading catalog sub references ", name));
		

		
		ProductType productType = new ProductType();
		productType.setCode(ProductType.GENERAL_TYPE);
		productTypeService.create(productType);


		
		
	}
	

	



}