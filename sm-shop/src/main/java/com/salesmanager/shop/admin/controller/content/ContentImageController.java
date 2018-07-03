package com.salesmanager.shop.admin.controller.content;

import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.InputContentFile;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.shop.admin.controller.ControllerConstants;
import com.salesmanager.shop.admin.model.content.ContentFiles;
import com.salesmanager.shop.admin.model.web.Menu;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.utils.ImageFilePath;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.ByteArrayInputStream;
import java.util.*;

import static com.salesmanager.core.business.utils.ajax.AjaxResponse.RESPONSE_STATUS_FAIURE;
import static com.salesmanager.core.model.content.FileContentType.IMAGE;
import static com.salesmanager.shop.admin.controller.ControllerConstants.Tiles;
import static com.salesmanager.shop.admin.controller.ControllerConstants.Tiles.ContentImages;
import static com.salesmanager.shop.admin.controller.ControllerConstants.Tiles.ContentImages.*;
import static com.salesmanager.shop.constants.Constants.ADMIN_STORE;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;


public class ContentImageController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(ContentImageController.class);
	
	@Inject
	private ContentService contentService;
	
	@Inject
	@Qualifier("img")
	private ImageFilePath imageUtils;

    @PreAuthorize("hasRole('CONTENT')")
    @RequestMapping(value = {"/admin/content/fileBrowser.html"}, method = RequestMethod.GET)
    public String displayFileBrowser(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {


        return fileBrowser;

    }


    @PreAuthorize("hasRole('CONTENT')")
    @RequestMapping(value = {"/admin/content/contentImages.html", "/admin/content/contentManagement.html"}, method = RequestMethod.GET)
    public String getContentImages(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {

        this.setMenu(model, request);
        return contentImages;

    }
	
	
	@SuppressWarnings({ "unchecked"})
	@PreAuthorize("hasRole('CONTENT')")
	@RequestMapping(value="/admin/content/images/paging.html", method=RequestMethod.POST)
	public @ResponseBody ResponseEntity<String> pageImages(HttpServletRequest request, HttpServletResponse response) {
		AjaxResponse resp = new AjaxResponse();

		try {
			

			MerchantStore store = (MerchantStore)request.getAttribute(Constants.ADMIN_STORE);
			
			List<String> imageNames = contentService.getContentFilesNames(store.getCode(),FileContentType.IMAGE);
			
			if(imageNames!=null) {

				for(String name : imageNames) {

					@SuppressWarnings("rawtypes")
					Map entry = new HashMap();
					entry.put("picture", new StringBuilder().append(request.getContextPath()).append(imageUtils.buildStaticImageUtils(store, name)).toString());
					
					entry.put("name", name);
					entry.put("id", name);
					resp.addDataEntry(entry);

				}
			
			}
			
			resp.setStatus(AjaxResponse.RESPONSE_STATUS_SUCCESS);

		} catch (Exception e) {
			LOGGER.error("Error while paging content images", e);
			resp.setStatus(AjaxResponse.RESPONSE_STATUS_FAIURE);
		}
		
		String returnString = resp.toJSONString();
		final HttpHeaders httpHeaders= new HttpHeaders();
	    httpHeaders.setContentType(MediaType.APPLICATION_JSON_UTF8);
		return new ResponseEntity<String>(returnString,httpHeaders,HttpStatus.OK);
	}

    @PreAuthorize("hasRole('CONTENT')")
    @RequestMapping(value = "/admin/content/createContentImages.html", method = RequestMethod.GET)
    public String displayContentImagesCreate(final Model model, final HttpServletRequest request, final HttpServletResponse response) throws Exception {

        return addContentImages;

    }

    @PreAuthorize("hasRole('CONTENT')")
    @RequestMapping(value = "/admin/content/saveContentImages.html", method = RequestMethod.POST)
    public String saveContentImages(@ModelAttribute(value = "contentFiles") @Valid final ContentFiles contentImages, final BindingResult bindingResult, final Model model, final HttpServletRequest request) throws Exception {

        this.setMenu(model, request);
        if (bindingResult.hasErrors()) {
            LOGGER.info("Found {} Validation errors", bindingResult.getErrorCount());
            return addContentImages;

        }
        final List<InputContentFile> contentImagesList = new ArrayList<InputContentFile>();
        final MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);
        if (isNotEmpty(contentImages.getFile())) {
            LOGGER.info("Saving {} content images for merchant {}", contentImages.getFile().size(), store.getId());
            for (final MultipartFile multipartFile : contentImages.getFile()) {
                if (!multipartFile.isEmpty()) {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(multipartFile.getBytes());
                    InputContentFile cmsContentImage = new InputContentFile();
                    cmsContentImage.setFileName(multipartFile.getOriginalFilename());
                    cmsContentImage.setMimeType(multipartFile.getContentType());
                    cmsContentImage.setFile(inputStream);
                    cmsContentImage.setFileContentType(IMAGE);
                    contentImagesList.add(cmsContentImage);
                }
            }

            if (isNotEmpty(contentImagesList)) {
                contentService.addContentFiles(store.getCode(), contentImagesList);
            } else {
                // show error message on UI
            }
        }

        return contentImages;
    }


    @PreAuthorize("hasRole('CONTENT')")
    @RequestMapping(value = "/admin/content/removeImage.html", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<String> removeImage(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        String imageName = request.getParameter("name");

        MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);

        AjaxResponse resp = new AjaxResponse();


        try {


            contentService.removeFile(store.getCode(), IMAGE, imageName);


        } catch (Exception e) {
            LOGGER.error("Error while deleting product", e);
            resp.setStatus(RESPONSE_STATUS_FAIURE);
            resp.setErrorMessage(e);
        }

        String returnString = resp.toJSONString();
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(APPLICATION_JSON_UTF8);
        return new ResponseEntity<String>(returnString, httpHeaders, OK);
    }
	
	private void setMenu(Model model, HttpServletRequest request) throws Exception {
        Map<String, String> activeMenus = new HashMap<String, String>();
        activeMenus.put("content", "content");
        activeMenus.put("content-images", "content-images");

        @SuppressWarnings("unchecked")
        Map<String, Menu> menus = (Map<String, Menu>) request.getAttribute("MENUMAP");

        Menu currentMenu = (Menu) menus.get("content");
        model.addAttribute("currentMenu", currentMenu);
        model.addAttribute("activeMenus", activeMenus);

    }

}
