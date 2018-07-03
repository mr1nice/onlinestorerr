package com.salesmanager.shop.init.data;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.salesmanager.core.business.constants.SystemConstants;
import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.services.merchant.MerchantStoreService;
import com.salesmanager.core.business.services.reference.init.InitializationDatabase;
import com.salesmanager.core.business.services.system.MerchantConfigurationService;
import com.salesmanager.core.business.services.system.SystemConfigurationService;
import com.salesmanager.core.business.services.user.GroupService;
import com.salesmanager.core.business.services.user.PermissionService;
import com.salesmanager.core.business.utils.CoreConfiguration;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.system.MerchantConfig;
import com.salesmanager.core.model.system.SystemConfiguration;
import com.salesmanager.core.model.user.Group;
import com.salesmanager.core.model.user.GroupType;
import com.salesmanager.core.model.user.Permission;
import com.salesmanager.shop.admin.model.permission.Permissions;
import com.salesmanager.shop.admin.model.permission.ShopPermission;
import com.salesmanager.shop.admin.security.WebUserServices;
import com.salesmanager.shop.constants.ApplicationConstants;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.xml.transform.stream.StreamSource;

import static com.salesmanager.core.model.merchant.MerchantStore.DEFAULT_STORE;
import static com.salesmanager.core.model.user.GroupType.ADMIN;
import static java.lang.String.format;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;


@Component
public class InitializationLoader {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(InitializationLoader.class);

	
	@Inject
	private MerchantConfigurationService merchantConfigurationService;

	
	@Inject
	private InitializationDatabase initializationDatabase;
	
	@Inject
	private InitData initData;
	
	@Inject
	private SystemConfigurationService systemConfigurationService;
	
	@Inject
	private WebUserServices userDetailsService;

	@Inject
	protected PermissionService  permissionService;
	
	@Inject
	protected GroupService   groupService;
	
	@Inject
	private CoreConfiguration configuration;
	
	@Inject
	protected MerchantStoreService merchantService;

    @Inject
    private ObjectMapper jacksonObjectMapper;

    @Inject
    private ResourceLoader resourceLoader;
	
	@PostConstruct
	public void init() {
		
		try {
			
			if (initializationDatabase.isEmpty()) {


                Resource permissionXML = resourceLoader.getResource("classpath:/permission/permission.json");

                InputStream xmlSource = permissionXML.getInputStream();

                Permissions permissions = jacksonObjectMapper.readValue(xmlSource, Permissions.class);

                LOGGER.info(format("%s : Shopizer database is empty, populate it....", "sm-shop"));

                initializationDatabase.populate("sm-shop");

                MerchantStore store = merchantService.getByCode(DEFAULT_STORE);

                Map<String, Group> groupMap = new HashMap<String, Group>();
                if (isNotEmpty(permissions.getShopPermission())) {

                    for (ShopPermission shopPermission : permissions.getShopPermission()) {

                        Permission permission = new Permission(shopPermission.getType());

                        for (String groupName : shopPermission.getShopGroup().getName()) {
                            if (groupMap.get(groupName) == null) {
                                Group group = new Group(groupName);
                                group.setGroupType(ADMIN);
                                groupService.create(group);
                                groupMap.put(groupName, group);
                                permission.getGroups().add(group);
                            } else {
                                permission.getGroups().add(groupMap.get(groupName));
                            }
                            permissionService.create(permission);
                        }


                    }
                }

                userDetailsService.createDefaultAdmin();
                MerchantConfig config = new MerchantConfig();
                config.setAllowPurchaseItems(true);
                config.setDisplayAddToCartOnFeaturedItems(true);

                merchantConfigurationService.saveMerchantConfig(config, store);

                loadData();

            }
			
		} catch (Exception e) {
			LOGGER.error("Error in the init method",e);
		}
		

		
	}
	
	private void loadData() throws ServiceException {
		
		String loadTestData = configuration.getProperty(ApplicationConstants.POPULATE_TEST_DATA);
		boolean loadData =  !StringUtils.isBlank(loadTestData) && loadTestData.equals(SystemConstants.CONFIG_VALUE_TRUE);


		if(loadData) {

			SystemConfiguration configuration = systemConfigurationService.getByKey(ApplicationConstants.TEST_DATA_LOADED);

			if(configuration!=null) {
					if(configuration.getKey().equals(ApplicationConstants.TEST_DATA_LOADED)) {
						if(configuration.getValue().equals(SystemConstants.CONFIG_VALUE_TRUE)) {
							return;
						}
					}
			}

			initData.initInitialData();

			configuration = new SystemConfiguration();
			configuration.getAuditSection().setModifiedBy(SystemConstants.SYSTEM_USER);
			configuration.setKey(ApplicationConstants.TEST_DATA_LOADED);
			configuration.setValue(SystemConstants.CONFIG_VALUE_TRUE);
			systemConfigurationService.create(configuration);


		}
	}



}
