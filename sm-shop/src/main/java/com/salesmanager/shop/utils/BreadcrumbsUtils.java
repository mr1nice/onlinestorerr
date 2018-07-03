package com.salesmanager.shop.utils;

import com.salesmanager.core.business.services.catalog.category.CategoryService;
import com.salesmanager.core.model.catalog.category.Category;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.model.reference.language.Language;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.model.catalog.category.ReadableCategory;
import com.salesmanager.shop.model.catalog.product.ReadableProduct;
import com.salesmanager.shop.model.shop.Breadcrumb;
import com.salesmanager.shop.model.shop.BreadcrumbItem;
import com.salesmanager.shop.model.shop.BreadcrumbItemType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.salesmanager.shop.constants.Constants.HOME_MENU_KEY;
import static com.salesmanager.shop.constants.Constants.SHOP_URI;
import static com.salesmanager.shop.model.shop.BreadcrumbItemType.*;
import static com.salesmanager.shop.utils.LocaleUtils.getLocale;
import static java.lang.Long.parseLong;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.StringUtils.isBlank;


@Component
public class BreadcrumbsUtils {
	
	@Inject
	private LabelUtils messages;
	
	@Inject
	private CategoryService categoryService;
	
	@Inject
	private FilePathUtils filePathUtils;
	
	
	public Breadcrumb buildCategoryBreadcrumb(ReadableCategory categoryClicked, MerchantStore store, Language language, String contextPath) throws Exception {
        BreadcrumbItem home = new BreadcrumbItem();
        home.setItemType(HOME);
        home.setLabel(messages.getMessage(HOME_MENU_KEY, getLocale(language)));
        home.setUrl(filePathUtils.buildStoreUri(store, contextPath) + SHOP_URI);

        Breadcrumb breadCrumb = new Breadcrumb();
        breadCrumb.setLanguage(language);

        List<BreadcrumbItem> items = new ArrayList<BreadcrumbItem>();
        items.add(home);
        List<String> categoryIds = parseCategoryLineage(categoryClicked.getLineage());
        List<Long> ids = new ArrayList<Long>();
        for (String c : categoryIds) {
            ids.add(parseLong(c));
        }

        ids.add(categoryClicked.getId());


        List<Category> categories = categoryService.listByIds(store, ids, language);
        for (Category c : categories) {
            BreadcrumbItem categoryBreadcrump = new BreadcrumbItem();
            categoryBreadcrump.setItemType(CATEGORY);
            categoryBreadcrump.setLabel(c.getDescription().getName());
            categoryBreadcrump.setUrl(filePathUtils.buildCategoryUrl(store, contextPath, c.getDescription().getSeUrl()));
            items.add(categoryBreadcrump);
        }

        breadCrumb.setUrlRefContent(buildBreadCrumb(ids));


        breadCrumb.setBreadCrumbs(items);
        breadCrumb.setItemType(CATEGORY);


        return breadCrumb;
    }
	
	
	public Breadcrumb buildProductBreadcrumb(String refContent, ReadableProduct productClicked, MerchantStore store, Language language, String contextPath) throws Exception {
		BreadcrumbItem home = new BreadcrumbItem();
		home.setItemType(HOME);
		home.setLabel(messages.getMessage(HOME_MENU_KEY, getLocale(language)));
		home.setUrl(filePathUtils.buildStoreUri(store, contextPath) + SHOP_URI);

		Breadcrumb breadCrumb = new Breadcrumb();
		breadCrumb.setLanguage(language);

		List<BreadcrumbItem> items = new ArrayList<BreadcrumbItem>();
		items.add(home);

		if (!isBlank(refContent)) {

			List<String> categoryIds = parseBreadCrumb(refContent);
			List<Long> ids = new ArrayList<Long>();
			for (String c : categoryIds) {
				ids.add(parseLong(c));
			}


			List<Category> categories = categoryService.listByIds(store, ids, language);
			for (Category c : categories) {
				BreadcrumbItem categoryBreadcrump = new BreadcrumbItem();
				categoryBreadcrump.setItemType(CATEGORY);
				categoryBreadcrump.setLabel(c.getDescription().getName());
				categoryBreadcrump.setUrl(filePathUtils.buildCategoryUrl(store, contextPath, c.getDescription().getSeUrl()));
				items.add(categoryBreadcrump);
			}


			breadCrumb.setUrlRefContent(buildBreadCrumb(ids));
		}

		BreadcrumbItem productBreadcrump = new BreadcrumbItem();
		productBreadcrump.setItemType(PRODUCT);
		productBreadcrump.setLabel(productClicked.getDescription().getName());
		productBreadcrump.setUrl(filePathUtils.buildProductUrl(store, contextPath, productClicked.getDescription().getFriendlyUrl()));
		items.add(productBreadcrump);


		breadCrumb.setBreadCrumbs(items);
		breadCrumb.setItemType(CATEGORY);


		return breadCrumb;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes", "unused" })
	private List<String> parseBreadCrumb(String refContent) throws Exception {
        String[] categoryComa = refContent.split(":");
        String[] categoryIds = categoryComa[1].split(",");
        return new LinkedList(asList(categoryIds));


    }
	

	private List<String> parseCategoryLineage(String lineage) throws Exception {
		
		String[] categoryPath = lineage.split(Constants.CATEGORY_LINEAGE_DELIMITER);
		List<String> returnList = new LinkedList<String>();
		for(String c : categoryPath) {
			if(!StringUtils.isBlank(c)) {
				returnList.add(c);
			}
		}
		return returnList;

	}
	
	private String buildBreadCrumb(List<Long> ids) throws Exception {
		
		if(CollectionUtils.isEmpty(ids)) {
			return null;
		}
			StringBuilder sb = new StringBuilder();
			sb.append("c:");
			int count = 1;
			for(Long c : ids) {
				sb.append(c);
				if(count < ids.size()) {
					sb.append(",");
				}
				count++;
			}
		
		
		return sb.toString();
		
	}

}
