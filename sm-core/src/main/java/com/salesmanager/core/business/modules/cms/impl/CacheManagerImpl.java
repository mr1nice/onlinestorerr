package com.salesmanager.core.business.modules.cms.impl;

import java.util.List;
import java.util.Properties;

import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.PersistenceConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.configuration.cache.StoreConfiguration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.tree.TreeCache;
import org.infinispan.tree.TreeCacheFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.salesmanager.core.business.modules.cms.impl.VendorCacheManager.getInstance;
import static org.infinispan.eviction.EvictionStrategy.LRU;

public abstract class CacheManagerImpl implements CacheManager {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(CacheManagerImpl.class);

	private static final String LOCATION_PROPERTIES = "location";
	
	@SuppressWarnings("rawtypes")
	private TreeCache treeCache = null;

	@SuppressWarnings("unchecked")
	protected void init(String namedCache, String location) {
		
		
		try {

            VendorCacheManager manager = getInstance();

            if (manager == null) {
                LOGGER.error("CacheManager is null");
                return;
            }
            final PersistenceConfigurationBuilder persistConfig = new ConfigurationBuilder().persistence();
            persistConfig.passivation(false);
            final SingleFileStoreConfigurationBuilder fileStore = new SingleFileStoreConfigurationBuilder(persistConfig).location(location);
            fileStore.invocationBatching().enable();
            fileStore.eviction().maxEntries(15);
            fileStore.eviction().strategy(LRU);
            fileStore.jmxStatistics().disable();
            final Configuration config = persistConfig.addStore(fileStore).build();
            config.compatibility().enabled();
            manager.getManager().defineConfiguration(namedCache, config);

            final Cache<String, String> cache = manager.getManager().getCache(namedCache);


            TreeCacheFactory f = new TreeCacheFactory();

            treeCache = f.createTreeCache(cache);

            cache.start();

            LOGGER.debug("CMS started");


        } catch (Exception e) {
      	LOGGER.error("Error while instantiating CmsImageFileManager",e);
      } finally {
          
      }
		
		
		
		
		
	}
	
	public EmbeddedCacheManager getManager() {
		return VendorCacheManager.getInstance().getManager();
	}

	@SuppressWarnings("rawtypes")
	public TreeCache getTreeCache() {
		return treeCache;
	}

	

}
