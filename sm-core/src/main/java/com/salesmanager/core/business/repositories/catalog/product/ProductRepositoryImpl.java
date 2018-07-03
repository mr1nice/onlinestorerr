package com.salesmanager.core.business.repositories.catalog.product;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.persistence.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.salesmanager.core.business.constants.Constants;
import com.salesmanager.core.model.catalog.product.Product;
import com.salesmanager.core.model.catalog.product.ProductCriteria;
import com.salesmanager.core.model.catalog.product.ProductList;
import com.salesmanager.core.model.catalog.product.attribute.AttributeCriteria;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.core.model.tax.taxclass.TaxClass;

import static com.salesmanager.core.business.constants.Constants.ALL_REGIONS;
import static org.apache.commons.collections4.CollectionUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;


public class ProductRepositoryImpl implements ProductRepositoryCustom {

	
    @PersistenceContext
    private EntityManager em;
    
	@Override
	public Product getById(Long productId) {
		
		try {


            StringBuilder qs = new StringBuilder();
            qs.append("select distinct p from Product as p ");
            qs.append("join fetch p.availabilities pa ");
            qs.append("join fetch p.merchantStore merch ");
            qs.append("join fetch p.descriptions pd ");

            qs.append("left join fetch p.categories categs ");
            qs.append("left join fetch categs.descriptions categsd ");

            qs.append("left join fetch pa.prices pap ");
            qs.append("left join fetch pap.descriptions papd ");
            qs.append("left join fetch p.images images ");
            qs.append("left join fetch p.attributes pattr ");
            qs.append("left join fetch pattr.productOption po ");
            qs.append("left join fetch po.descriptions pod ");
            qs.append("left join fetch pattr.productOptionValue pov ");
            qs.append("left join fetch pov.descriptions povd ");
            qs.append("left join fetch p.relationships pr ");
            qs.append("left join fetch p.manufacturer manuf ");
            qs.append("left join fetch manuf.descriptions manufd ");
            qs.append("left join fetch p.type type ");
            qs.append("left join fetch p.taxClass tx ");
            qs.append("left join fetch p.owner owner ");

            qs.append("where p.id=:pid");


            String hql = qs.toString();
            Query q = this.em.createQuery(hql);

            q.setParameter("pid", productId);


            Product p = (Product) q.getSingleResult();


            return p;

        } catch(javax.persistence.NoResultException ers) {
			return null;
		}
		
	}
    
	
	@Override
	public Product getByCode(String productCode, Language language) {
		
		try {


            StringBuilder qs = new StringBuilder();
            qs.append("select distinct p from Product as p ");
            qs.append("join fetch p.availabilities pa ");
            qs.append("join fetch p.descriptions pd ");
            qs.append("join fetch p.merchantStore pm ");
            qs.append("left join fetch pa.prices pap ");
            qs.append("left join fetch pap.descriptions papd ");

            qs.append("left join fetch p.categories categs ");
            qs.append("left join fetch categs.descriptions categsd ");
            qs.append("left join fetch p.images images ");
            qs.append("left join fetch p.attributes pattr ");
            qs.append("left join fetch pattr.productOption po ");
            qs.append("left join fetch po.descriptions pod ");
            qs.append("left join fetch pattr.productOptionValue pov ");
            qs.append("left join fetch pov.descriptions povd ");
            qs.append("left join fetch p.relationships pr ");
            qs.append("left join fetch p.manufacturer manuf ");
            qs.append("left join fetch manuf.descriptions manufd ");
            qs.append("left join fetch p.type type ");
            qs.append("left join fetch p.taxClass tx ");
            qs.append("left join fetch p.owner owner ");

            qs.append("where p.sku=:code ");
            qs.append("and pd.language.id=:lang and papd.language.id=:lang");

            String hql = qs.toString();
            Query q = this.em.createQuery(hql);

            q.setParameter("code", productCode);
            q.setParameter("lang", language.getId());

            Product p = (Product) q.getSingleResult();


            return p;

        } catch(javax.persistence.NoResultException ers) {
			return null;
		}
		
	}
	
    public Product getByFriendlyUrl(MerchantStore store,String seUrl, Locale locale) {


        List regionList = new ArrayList();
        regionList.add("*");
        regionList.add(locale.getCountry());


        StringBuilder qs = new StringBuilder();
        qs.append("select distinct p from Product as p ");
        qs.append("join fetch p.availabilities pa ");
        qs.append("join fetch p.descriptions pd ");
        qs.append("join fetch p.merchantStore pm ");
        qs.append("left join fetch pa.prices pap ");
        qs.append("left join fetch pap.descriptions papd ");

        qs.append("left join fetch p.categories categs ");
        qs.append("left join fetch categs.descriptions categsd ");
        qs.append("left join fetch p.images images ");
        qs.append("left join fetch p.attributes pattr ");
        qs.append("left join fetch pattr.productOption po ");
        qs.append("left join fetch po.descriptions pod ");
        qs.append("left join fetch pattr.productOptionValue pov ");
        qs.append("left join fetch pov.descriptions povd ");
        qs.append("left join fetch p.relationships pr ");
        qs.append("left join fetch p.manufacturer manuf ");
        qs.append("left join fetch manuf.descriptions manufd ");
        qs.append("left join fetch p.type type ");
        qs.append("left join fetch p.taxClass tx ");
        qs.append("left join fetch p.owner owner ");

        qs.append("where pa.region in (:lid) ");
        qs.append("and pd.seUrl=:seUrl ");
        qs.append("and p.available=true and p.dateAvailable<=:dt ");
        qs.append("order by pattr.productOptionSortOrder ");


        String hql = qs.toString();
        Query q = this.em.createQuery(hql);


        q.setParameter("lid", regionList);
        q.setParameter("dt", new Date());
        q.setParameter("seUrl", seUrl);

        Product p = null;

        try {
            p = (Product) q.getSingleResult();
        } catch (NoResultException ignore) {

        }


        return p;

    }

    @Override
	public List<Product> getProductsForLocale(MerchantStore store, Set<Long> categoryIds, Language language, Locale locale) {
		
		ProductList products = this.getProductsListForLocale(store, categoryIds, language, locale, 0, -1);
		
		return products.getProducts();
	}
    
	@Override
	public Product getProductForLocale(long productId, Language language, Locale locale) {


        List regionList = new ArrayList();
        regionList.add("*");
        regionList.add(locale.getCountry());


        StringBuilder qs = new StringBuilder();
        qs.append("select distinct p from Product as p ");
        qs.append("join fetch p.availabilities pa ");
        qs.append("join fetch p.descriptions pd ");
        qs.append("join fetch p.merchantStore pm ");
        qs.append("left join fetch pa.prices pap ");
        qs.append("left join fetch pap.descriptions papd ");
        qs.append("left join fetch p.images images ");
        qs.append("left join fetch p.attributes pattr ");
        qs.append("left join fetch pattr.productOption po ");
        qs.append("left join fetch po.descriptions pod ");
        qs.append("left join fetch pattr.productOptionValue pov ");
        qs.append("left join fetch pov.descriptions povd ");
        qs.append("left join fetch p.relationships pr ");
        qs.append("left join fetch p.manufacturer manuf ");
        qs.append("left join fetch manuf.descriptions manufd ");
        qs.append("left join fetch p.type type ");
        qs.append("left join fetch p.taxClass tx ");
        qs.append("left join fetch p.owner owner ");

        qs.append("where p.id=:pid and pa.region in (:lid) ");
        qs.append("and pd.language.id=:lang and papd.language.id=:lang ");
        qs.append("and p.available=true and p.dateAvailable<=:dt ");

        String hql = qs.toString();
        Query q = this.em.createQuery(hql);

        q.setParameter("pid", productId);
        q.setParameter("lid", regionList);
        q.setParameter("dt", new Date());
        q.setParameter("lang", language.getId());

        @SuppressWarnings("unchecked")
        List<Product> results = q.getResultList();
        if (results.isEmpty()) return null;
        else if (results.size() == 1) return (Product) results.get(0);
        throw new NonUniqueResultException();


    }
	
	@SuppressWarnings("rawtypes")
	@Override
	public List<Product> getProductsListByCategories(Set categoryIds) {


        StringBuilder qs = new StringBuilder();
        qs.append("select distinct p from Product as p ");
        qs.append("join fetch p.merchantStore merch ");
        qs.append("join fetch p.availabilities pa ");
        qs.append("left join fetch pa.prices pap ");

        qs.append("join fetch p.descriptions pd ");
        qs.append("join fetch p.categories categs ");


        qs.append("left join fetch pap.descriptions papd ");
        qs.append("left join fetch p.images images ");
        qs.append("left join fetch p.attributes pattr ");
        qs.append("left join fetch pattr.productOption po ");
        qs.append("left join fetch po.descriptions pod ");
        qs.append("left join fetch pattr.productOptionValue pov ");
        qs.append("left join fetch pov.descriptions povd ");
        qs.append("left join fetch p.manufacturer manuf ");
        qs.append("left join fetch p.type type ");
        qs.append("left join fetch p.taxClass tx ");
        qs.append("left join fetch p.owner owner ");
        qs.append("where categs.id in (:cid)");


        String hql = qs.toString();
        Query q = this.em.createQuery(hql);

        q.setParameter("cid", categoryIds);


        @SuppressWarnings("unchecked")
        List<Product> products = q.getResultList();


        return products;


    }
	
	@Override
	public List<Product> getProductsListByCategories(Set<Long> categoryIds, Language language) {


        StringBuilder qs = new StringBuilder();
        qs.append("select distinct p from Product as p ");
        qs.append("join fetch p.merchantStore merch ");
        qs.append("join fetch p.availabilities pa ");
        qs.append("left join fetch pa.prices pap ");

        qs.append("join fetch p.descriptions pd ");
        qs.append("join fetch p.categories categs ");


        qs.append("left join fetch pap.descriptions papd ");
        qs.append("left join fetch p.images images ");
        qs.append("left join fetch p.attributes pattr ");
        qs.append("left join fetch pattr.productOption po ");
        qs.append("left join fetch po.descriptions pod ");
        qs.append("left join fetch pattr.productOptionValue pov ");
        qs.append("left join fetch pov.descriptions povd ");
        qs.append("left join fetch p.manufacturer manuf ");
        qs.append("left join fetch manuf.descriptions manufd ");
        qs.append("left join fetch p.type type ");
        qs.append("left join fetch p.taxClass tx ");
        qs.append("left join fetch p.owner owner ");
        qs.append("where categs.id in (:cid) ");
        qs.append("and pd.language.id=:lang and papd.language.id=:lang ");
        qs.append("and p.available=true and p.dateAvailable<=:dt ");


        String hql = qs.toString();
        Query q = this.em.createQuery(hql);

        q.setParameter("cid", categoryIds);
        q.setParameter("lang", language.getId());
        q.setParameter("dt", new Date());


        @SuppressWarnings("unchecked")
        List<Product> products = q.getResultList();


        return products;


    }

    @SuppressWarnings({"rawtypes", "unchecked", "unused"})
    private ProductList getProductsListForLocale(MerchantStore store, Set categoryIds, Language language, Locale locale, int first, int max) {


        List regionList = new ArrayList();
        regionList.add(ALL_REGIONS);
        if (locale != null) {
            regionList.add(locale.getCountry());
        }

        ProductList productList = new ProductList();


        Query countQ = this.em.createQuery(
                "select count(p) from Product as p INNER JOIN p.availabilities pa INNER JOIN p.categories categs where p.merchantStore.id=:mId and categs.id in (:cid) and pa.region in (:lid) and p.available=1 and p.dateAvailable<=:dt");

        countQ.setParameter("cid", categoryIds);
        countQ.setParameter("lid", regionList);
        countQ.setParameter("dt", new Date());
        countQ.setParameter("mId", store.getId());

        Number count = (Number) countQ.getSingleResult();


        productList.setTotalCount(count.intValue());

        if (count.intValue() == 0)
            return productList;


        StringBuilder qs = new StringBuilder();
        qs.append("select p from Product as p ");
        qs.append("join fetch p.merchantStore merch ");
        qs.append("join fetch p.availabilities pa ");
        qs.append("left join fetch pa.prices pap ");

        qs.append("join fetch p.descriptions pd ");
        qs.append("join fetch p.categories categs ");


        //not necessary
        //qs.append("join fetch pap.descriptions papd ");


        //images
        qs.append("left join fetch p.images images ");

        //options (do not need attributes for listings)
        //qs.append("left join fetch p.attributes pattr ");
        //qs.append("left join fetch pattr.productOption po ");
        //qs.append("left join fetch po.descriptions pod ");
        //qs.append("left join fetch pattr.productOptionValue pov ");
        //qs.append("left join fetch pov.descriptions povd ");

        //other lefts
        qs.append("left join fetch p.manufacturer manuf ");
        qs.append("left join fetch manuf.descriptions manufd ");
        qs.append("left join fetch p.type type ");
        qs.append("left join fetch p.taxClass tx ");

        //RENTAL
        qs.append("left join fetch p.owner owner ");

        //qs.append("where pa.region in (:lid) ");
        qs.append("where p.merchantStore.id=mId and categs.id in (:cid) and pa.region in (:lid) ");
        //qs.append("and p.available=true and p.dateAvailable<=:dt and pd.language.id=:lang and manufd.language.id=:lang");
        qs.append("and p.available=true and p.dateAvailable<=:dt and pd.language.id=:lang");
        qs.append(" order by p.sortOrder asc");


        String hql = qs.toString();
        Query q = this.em.createQuery(hql);

        q.setParameter("cid", categoryIds);
        q.setParameter("lid", regionList);
        q.setParameter("dt", new Date());
        q.setParameter("lang", language.getId());
        q.setParameter("mId", store.getId());


        q.setFirstResult(first);
        if (max > 0) {
            int maxCount = first + max;

            if (maxCount < count.intValue()) {
                q.setMaxResults(maxCount);
            } else {
                q.setMaxResults(count.intValue());
            }
        }

        List<Product> products = q.getResultList();
        productList.setProducts(products);

        return productList;


    }

    @Override
    public ProductList listByStore(MerchantStore store, Language language, ProductCriteria criteria) {

        ProductList productList = new ProductList();


        StringBuilder countBuilderSelect = new StringBuilder();
        countBuilderSelect.append("select count(distinct p) from Product as p");

        StringBuilder countBuilderWhere = new StringBuilder();
        countBuilderWhere.append(" where p.merchantStore.id=:mId");

        if (!isEmpty(criteria.getProductIds())) {
            countBuilderWhere.append(" and p.id in (:pId)");
        }

        countBuilderSelect.append(" inner join p.descriptions pd");
        countBuilderWhere.append(" and pd.language.id=:lang");

        if (!isBlank(criteria.getProductName())) {
            countBuilderWhere.append(" and lower(pd.name) like:nm");
        }


        if (!isEmpty(criteria.getCategoryIds())) {
            countBuilderSelect.append(" INNER JOIN p.categories categs");
            countBuilderWhere.append(" and categs.id in (:cid)");
        }

        if (criteria.getManufacturerId() != null) {
            countBuilderSelect.append(" INNER JOIN p.manufacturer manuf");
            countBuilderWhere.append(" and manuf.id = :manufid");
        }

        if (!isBlank(criteria.getCode())) {
            countBuilderWhere.append(" and lower(p.sku) like :sku");
        }

        //RENTAL
        if (!isBlank(criteria.getStatus())) {
            countBuilderWhere.append(" and p.rentalStatus = :status");
        }

        if (criteria.getOwnerId() != null) {
            countBuilderSelect.append(" INNER JOIN p.owner owner");
            countBuilderWhere.append(" and owner.id = :ownerid");
        }

        if (!isEmpty(criteria.getAttributeCriteria())) {

            countBuilderSelect.append(" INNER JOIN p.attributes pattr");
            countBuilderSelect.append(" INNER JOIN pattr.productOption po");
            countBuilderSelect.append(" INNER JOIN pattr.productOptionValue pov ");
            countBuilderSelect.append(" INNER JOIN pov.descriptions povd ");
            int count = 0;
            for (AttributeCriteria attributeCriteria : criteria.getAttributeCriteria()) {
                if (count == 0) {
                    countBuilderWhere.append(" and po.code =:").append(attributeCriteria.getAttributeCode());
                    countBuilderWhere.append(" and povd.description like :").append("val").append(count).append(attributeCriteria.getAttributeCode());
                }
                count++;
            }
            countBuilderWhere.append(" and povd.language.id=:lang");

        }


        if (criteria.getAvailable() != null) {
            if (criteria.getAvailable().booleanValue()) {
                countBuilderWhere.append(" and p.available=true and p.dateAvailable<=:dt");
            } else {
                countBuilderWhere.append(" and p.available=false or p.dateAvailable>:dt");
            }
        }

        Query countQ = this.em.createQuery(
                countBuilderSelect.toString() + countBuilderWhere.toString());

        countQ.setParameter("mId", store.getId());


        if (!isEmpty(criteria.getCategoryIds())) {
            countQ.setParameter("cid", criteria.getCategoryIds());
        }


        if (criteria.getAvailable() != null) {
            countQ.setParameter("dt", new Date());
        }

        if (!isBlank(criteria.getCode())) {
            countQ.setParameter("sku", new StringBuilder().append("%").append(criteria.getCode().toLowerCase()).append("%").toString());
        }

        if (criteria.getManufacturerId() != null) {
            countQ.setParameter("manufid", criteria.getManufacturerId());
        }

        if (!isEmpty(criteria.getAttributeCriteria())) {
            int count = 0;
            for (AttributeCriteria attributeCriteria : criteria.getAttributeCriteria()) {
                countQ.setParameter(attributeCriteria.getAttributeCode(), attributeCriteria.getAttributeCode());
                countQ.setParameter("val" + count + attributeCriteria.getAttributeCode(), "%" + attributeCriteria.getAttributeValue() + "%");
                count++;
            }
        }

        countQ.setParameter("lang", language.getId());

        if (!isBlank(criteria.getProductName())) {
            countQ.setParameter("nm", new StringBuilder().append("%").append(criteria.getProductName().toLowerCase()).append("%").toString());
        }

        if (!isEmpty(criteria.getProductIds())) {
            countQ.setParameter("pId", criteria.getProductIds());
        }

        //RENTAL
        if (!isBlank(criteria.getStatus())) {
            countQ.setParameter("status", criteria.getStatus());
        }

        if (criteria.getOwnerId() != null) {
            countQ.setParameter("ownerid", criteria.getOwnerId());
        }

        Number count = (Number) countQ.getSingleResult();

        productList.setTotalCount(count.intValue());

        if (count.intValue() == 0)
            return productList;


        StringBuilder qs = new StringBuilder();
        qs.append("select distinct p from Product as p ");
        qs.append("join fetch p.merchantStore merch ");
        qs.append("join fetch p.availabilities pa ");
        qs.append("left join fetch pa.prices pap ");

        qs.append("join fetch p.descriptions pd ");
        qs.append("left join fetch p.categories categs ");
        qs.append("left join fetch categs.descriptions cd ");


        //images
        qs.append("left join fetch p.images images ");


        //other lefts
        qs.append("left join fetch p.manufacturer manuf ");
        qs.append("left join fetch manuf.descriptions manufd ");
        qs.append("left join fetch p.type type ");
        qs.append("left join fetch p.taxClass tx ");

        //RENTAL
        qs.append("left join fetch p.owner owner ");


        //attributes
        if (!isEmpty(criteria.getAttributeCriteria())) {
            qs.append(" inner join p.attributes pattr");
            qs.append(" inner join pattr.productOption po");
            qs.append(" inner join po.descriptions pod");
            qs.append(" inner join pattr.productOptionValue pov ");
            qs.append(" inner join pov.descriptions povd");
        } else {
            qs.append(" left join fetch p.attributes pattr");
            qs.append(" left join fetch pattr.productOption po");
            qs.append(" left join fetch po.descriptions pod");
            qs.append(" left join fetch pattr.productOptionValue pov");
            qs.append(" left join fetch pov.descriptions povd");
        }

        qs.append(" left join fetch p.relationships pr");


        qs.append(" where merch.id=:mId");
        qs.append(" and pd.language.id=:lang");

        if (!isEmpty(criteria.getProductIds())) {
            qs.append(" and p.id in (:pId)");
        }

        if (!isEmpty(criteria.getCategoryIds())) {
            qs.append(" and categs.id in (:cid)");
        }

        if (criteria.getManufacturerId() != null) {
            qs.append(" and manuf.id = :manufid");
        }


        if (criteria.getAvailable() != null) {
            if (criteria.getAvailable().booleanValue()) {
                qs.append(" and p.available=true and p.dateAvailable<=:dt");
            } else {
                qs.append(" and p.available=false and p.dateAvailable>:dt");
            }
        }

        if (!isBlank(criteria.getProductName())) {
            qs.append(" and lower(pd.name) like :nm");
        }

        if (!isBlank(criteria.getCode())) {
            qs.append(" and lower(p.sku) like :sku");
        }

        //RENTAL
        if (!isBlank(criteria.getStatus())) {
            qs.append(" and p.rentalStatus = :status");
        }

        if (criteria.getOwnerId() != null) {
            qs.append(" and owner.id = :ownerid");
        }

        if (!isEmpty(criteria.getAttributeCriteria())) {
            int cnt = 0;
            for (AttributeCriteria attributeCriteria : criteria.getAttributeCriteria()) {
                qs.append(" and po.code =:").append(attributeCriteria.getAttributeCode());
                qs.append(" and povd.description like :").append("val").append(cnt).append(attributeCriteria.getAttributeCode());
                cnt++;
            }
            qs.append(" and povd.language.id=:lang");

        }
        qs.append(" order by p.sortOrder asc");


        String hql = qs.toString();
        Query q = this.em.createQuery(hql);


        q.setParameter("lang", language.getId());
        q.setParameter("mId", store.getId());

        if (!isEmpty(criteria.getCategoryIds())) {
            q.setParameter("cid", criteria.getCategoryIds());
        }

        if (!isEmpty(criteria.getProductIds())) {
            q.setParameter("pId", criteria.getProductIds());
        }

        if (criteria.getAvailable() != null) {
            q.setParameter("dt", new Date());
        }

        if (criteria.getManufacturerId() != null) {
            q.setParameter("manufid", criteria.getManufacturerId());
        }

        if (!isBlank(criteria.getCode())) {
            q.setParameter("sku", new StringBuilder().append("%").append(criteria.getCode().toLowerCase()).append("%").toString());
        }

        if (!isEmpty(criteria.getAttributeCriteria())) {
            int cnt = 0;
            for (AttributeCriteria attributeCriteria : criteria.getAttributeCriteria()) {
                q.setParameter(attributeCriteria.getAttributeCode(), attributeCriteria.getAttributeCode());
                q.setParameter("val" + cnt + attributeCriteria.getAttributeCode(), "%" + attributeCriteria.getAttributeValue() + "%");
                cnt++;
            }
        }

        //RENTAL
        if (!isBlank(criteria.getStatus())) {
            q.setParameter("status", criteria.getStatus());
        }

        if (criteria.getOwnerId() != null) {
            q.setParameter("ownerid", criteria.getOwnerId());
        }

        if (!isBlank(criteria.getProductName())) {
            q.setParameter("nm", new StringBuilder().append("%").append(criteria.getProductName().toLowerCase()).append("%").toString());
        }

        if (criteria.getMaxCount() > 0) {


            q.setFirstResult(criteria.getStartIndex());
            if (criteria.getMaxCount() < count.intValue()) {
                q.setMaxResults(criteria.getMaxCount());
            } else {
                q.setMaxResults(count.intValue());
            }
        }

        @SuppressWarnings("unchecked")
        List<Product> products = q.getResultList();
        productList.setProducts(products);

        return productList;


    }

	@Override
	public List<Product> listByStore(MerchantStore store) {


        StringBuilder qs = new StringBuilder();
        qs.append("select p from Product as p ");
        qs.append("join fetch p.merchantStore merch ");
        qs.append("join fetch p.availabilities pa ");
        qs.append("left join fetch pa.prices pap ");

        qs.append("join fetch p.descriptions pd ");
        qs.append("left join fetch p.categories categs ");


        qs.append("left join fetch pap.descriptions papd ");
        qs.append("left join fetch p.images images ");
        qs.append("left join fetch p.attributes pattr ");
        qs.append("left join fetch pattr.productOption po ");
        qs.append("left join fetch po.descriptions pod ");
        qs.append("left join fetch pattr.productOptionValue pov ");
        qs.append("left join fetch pov.descriptions povd ");
        qs.append("left join fetch p.manufacturer manuf ");
        qs.append("left join fetch manuf.descriptions manufd ");
        qs.append("left join fetch p.type type ");
        qs.append("left join fetch p.taxClass tx ");
        qs.append("left join fetch p.owner owner ");
        qs.append("where merch.id=:mid");


        String hql = qs.toString();
        Query q = this.em.createQuery(hql);

        q.setParameter("mid", store.getId());


        @SuppressWarnings("unchecked")
        List<Product> products = q.getResultList();


        return products;


    }
	
	
	@Override
	public List<Product> listByTaxClass(TaxClass taxClass) {


        StringBuilder qs = new StringBuilder();
        qs.append("select p from Product as p ");
        qs.append("join fetch p.merchantStore merch ");
        qs.append("join fetch p.availabilities pa ");
        qs.append("left join fetch pa.prices pap ");

        qs.append("join fetch p.descriptions pd ");
        qs.append("join fetch p.categories categs ");


        qs.append("left join fetch pap.descriptions papd ");
        qs.append("left join fetch p.images images ");
        qs.append("left join fetch p.attributes pattr ");
        qs.append("left join fetch pattr.productOption po ");
        qs.append("left join fetch po.descriptions pod ");
        qs.append("left join fetch pattr.productOptionValue pov ");
        qs.append("left join fetch pov.descriptions povd ");
        qs.append("left join fetch p.manufacturer manuf ");
        qs.append("left join fetch manuf.descriptions manufd ");
        qs.append("left join fetch p.type type ");
        qs.append("left join fetch p.taxClass tx ");
        qs.append("left join fetch p.owner owner ");
        qs.append("where tx.id=:tid");


        String hql = qs.toString();
        Query q = this.em.createQuery(hql);

        q.setParameter("tid", taxClass.getId());


        @SuppressWarnings("unchecked")
        List<Product> products = q.getResultList();


        return products;


    }
	
}
