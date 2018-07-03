package com.salesmanager.shop.admin.controller.content;

import java.io.ByteArrayInputStream;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.inject.Inject;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import com.salesmanager.core.model.content.FileContentType;
import com.salesmanager.core.model.content.InputContentFile;
import com.salesmanager.core.business.services.content.ContentService;
import com.salesmanager.core.model.merchant.MerchantStore;
import com.salesmanager.core.business.utils.ajax.AjaxResponse;
import com.salesmanager.shop.admin.controller.ControllerConstants;
import com.salesmanager.shop.admin.model.content.ContentFiles;
import com.salesmanager.shop.admin.model.web.Menu;
import com.salesmanager.shop.constants.Constants;
import com.salesmanager.shop.utils.ImageFilePath;

import static com.salesmanager.core.business.utils.ajax.AjaxResponse.*;
import static com.salesmanager.core.model.content.FileContentType.STATIC_FILE;
import static com.salesmanager.shop.admin.controller.ControllerConstants.Tiles;
import static com.salesmanager.shop.admin.controller.ControllerConstants.Tiles.ContentFiles.contentFiles;
import static com.salesmanager.shop.constants.Constants.ADMIN_STORE;
import static java.net.URLConnection.getFileNameMap;
import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;
import static org.springframework.http.HttpStatus.OK;
import static org.springframework.http.MediaType.APPLICATION_JSON_UTF8;


@Controller
public class StaticContentController {
	
	private static final Logger LOGGER = LoggerFactory.getLogger(StaticContentController.class);
	
	@Inject
	private ContentService contentService;
	

	@Inject
	@Qualifier("img")
	private ImageFilePath imageUtils;
	

	@PreAuthorize("hasRole('CONTENT')")
	@RequestMapping(value={"/admin/content/static/contentFiles.html","/admin/content/static/contentManagement.html"}, method=RequestMethod.GET)
	public String getContentImages(Model model, HttpServletRequest request, HttpServletResponse response) throws Exception {

		this.setMenu(model, request);
		return ControllerConstants.Tiles.ContentFiles.contentFiles;
		
	}


    @SuppressWarnings({"unchecked"})
    @PreAuthorize("hasRole('CONTENT')")
    @RequestMapping(value = "/admin/content/static/page.html", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<String> pageStaticContent(HttpServletRequest request, HttpServletResponse response) {

        AjaxResponse resp = new AjaxResponse();
        final HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.setContentType(APPLICATION_JSON_UTF8);

        try {


            MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);

            List<String> fileNames = contentService.getContentFilesNames(store.getCode(), STATIC_FILE);
			
/*			Map<String,String> configurations = (Map<String, String>)request.getSession().getAttribute(Constants.STORE_CONFIGURATION);
			String scheme = Constants.HTTP_SCHEME;
			if(configurations!=null) {
				scheme = (String)configurations.get("scheme");
			}
			

			StringBuilder storePath = new StringBuilder();
			storePath.append(scheme).append("://")
			.append(store.getDomainName())
			.append(request.getContextPath());
*/

            if (fileNames != null) {

                for (String name : fileNames) {

                    String mimeType = getFileNameMap().getContentTypeFor(name);

                    //StringBuilder filePath = new StringBuilder();

                    //filePath.append(storePath.toString()).append(filePathUtils.buildStaticFilePath(store,name));

                    String filePath = imageUtils.buildStaticContentFilePath(store, name);

                    //String filePath = filePathUtils.buildStaticFileAbsolutePath(store, name);


                    @SuppressWarnings("rawtypes")
                    Map entry = new HashMap();
                    entry.put("name", name);
                    entry.put("path", filePath.toString());
                    entry.put("mimeType", mimeType);
                    resp.addDataEntry(entry);

                }

            }

            resp.setStatus(RESPONSE_STATUS_SUCCESS);

        } catch (Exception e) {
            LOGGER.error("Error while paging content images", e);
            resp.setStatus(RESPONSE_STATUS_FAIURE);
        }

        String returnString = resp.toJSONString();
        return new ResponseEntity<String>(returnString, httpHeaders, OK);
    }


    @PreAuthorize("hasRole('CONTENT')")
    @RequestMapping(value = "/admin/content/static/saveFiles.html", method = RequestMethod.POST)
    public String saveFiles(@ModelAttribute(value = "contentFiles") @Valid final ContentFiles contentFiles, final BindingResult bindingResult, final Model model, final HttpServletRequest request) throws Exception {

        this.setMenu(model, request);
        if (bindingResult.hasErrors()) {
            LOGGER.info("Found {} Validation errors", bindingResult.getErrorCount());
            return contentFiles;

        }
        final List<InputContentFile> contentFilesList = new ArrayList<InputContentFile>();
        final MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);
        if (isNotEmpty(contentFiles.getFile())) {
            LOGGER.info("Saving {} content files for merchant {}", contentFiles.getFile().size(), store.getId());
            for (final MultipartFile multipartFile : contentFiles.getFile()) {
                if (!multipartFile.isEmpty()) {
                    ByteArrayInputStream inputStream = new ByteArrayInputStream(multipartFile.getBytes());
                    InputContentFile cmsContentImage = new InputContentFile();
                    cmsContentImage.setFileName(multipartFile.getOriginalFilename());
                    cmsContentImage.setFileContentType(STATIC_FILE);
                    cmsContentImage.setFile(inputStream);
                    contentFilesList.add(cmsContentImage);
                }
            }

            if (isNotEmpty(contentFilesList)) {
                contentService.addContentFiles(store.getCode(), contentFilesList);
            } else {
                // show error message on UI
            }
        }

        return contentFiles;
    }


    @PreAuthorize("hasRole('CONTENT')")
    @RequestMapping(value = "/admin/content/static/removeFile.html", method = RequestMethod.POST)
    public @ResponseBody
    ResponseEntity<String> removeFile(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        String fileName = request.getParameter("name");

        MerchantStore store = (MerchantStore) request.getAttribute(ADMIN_STORE);

        AjaxResponse resp = new AjaxResponse();


        try {


            contentService.removeFile(store.getCode(), STATIC_FILE, fileName);

            resp.setStatus(RESPONSE_OPERATION_COMPLETED);

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
        activeMenus.put("content-files", "content-files");

        @SuppressWarnings("unchecked")
        Map<String, Menu> menus = (Map<String, Menu>) request.getAttribute("MENUMAP");

        Menu currentMenu = (Menu) menus.get("content");
        model.addAttribute("currentMenu", currentMenu);
        model.addAttribute("activeMenus", activeMenus);

    }

}
