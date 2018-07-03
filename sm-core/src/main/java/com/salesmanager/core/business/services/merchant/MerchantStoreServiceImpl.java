package com.salesmanager.core.business.services.merchant;

import java.util.List;

import javax.inject.Inject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.salesmanager.core.business.exception.ServiceException;
import com.salesmanager.core.business.repositories.merchant.MerchantRepository;
import com.salesmanager.core.business.services.catalog.product.manufacturer.ManufacturerService;
import com.salesmanager.core.business.services.catalog.product.type.ProductTypeService;
import com.salesmanager.core.business.services.common.generic.SalesManagerEntityServiceImpl;
import com.salesmanager.core.business.services.tax.TaxClassService;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.catalog.product.manufacturer.Manufacturer;
import com.salesmanager.core.model.customer.Customer;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.order.Order;
import com.salesmanager.core.model.system.MerchantConfiguration;
import com.salesmanager.core.model.tax.taxclass.TaxClass;
import com.salesmanager.core.model.user.User;

@Service("merchantService")
public class MerchantStoreServiceImpl extends SalesManagerEntityServiceImpl<Integer, MerchantStore>
        implements MerchantStoreService {


    @Inject
    protected ProductTypeService productTypeService;

    @Inject
    private TaxClassService taxClassService;

    @Inject
    private ManufacturerService manufacturerService;

    private MerchantRepository merchantRepository;

    @Inject
    public MerchantStoreServiceImpl(MerchantRepository merchantRepository) {
        super(merchantRepository);
        this.merchantRepository = merchantRepository;
    }


    public MerchantStore getMerchantStore(String merchantStoreCode) throws ServiceException {
        return merchantRepository.findByCode(merchantStoreCode);
    }

    @Override
    public void saveOrUpdate(MerchantStore store) throws ServiceException {

        super.save(store);

    }


    @Override
    public MerchantStore getByCode(String code) throws ServiceException {

        return merchantRepository.findByCode(code);
    }

}
