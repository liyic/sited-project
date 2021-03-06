package io.sited.page.web.web;

import io.sited.App;
import io.sited.file.api.FileRepository;
import io.sited.page.api.PageCategoryWebService;
import io.sited.page.api.PageWebService;
import io.sited.page.api.category.CategoryQuery;
import io.sited.page.api.category.CategoryResponse;
import io.sited.page.api.category.CategoryStatus;
import io.sited.page.api.page.PageQuery;
import io.sited.page.api.page.PageResponse;
import io.sited.page.api.page.PageStatus;
import io.sited.page.web.PageWebOptions;
import io.sited.page.web.service.SitemapBuilder;
import io.sited.resource.Resource;
import io.sited.util.collection.QueryResponse;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Optional;

/**
 * @author chi
 */
@Path("/sitemap")
public class SitemapController {
    @Inject
    PageWebService pageWebService;

    @Inject
    PageCategoryWebService pageCategoryWebService;

    @Inject
    App app;

    @Inject
    PageWebOptions pageWebOptions;

    @Inject
    FileRepository fileRepository;

    @Inject
    UriInfo uriInfo;

    @Path("/sitemap.xml")
    @GET
    public Response index() {
        Optional<Resource> resource = fileRepository.get("sitemap/sitemap.xml");
        if (resource.isPresent()) {
            return Response.ok(resource.get())
                .type("text/xml").build();
        }

        CategoryQuery categoryQuery = new CategoryQuery();
        categoryQuery.page = 1;
        categoryQuery.limit = pageWebOptions.maxSitemapEntries;
        categoryQuery.status = CategoryStatus.ACTIVE;
        QueryResponse<CategoryResponse> categories = pageCategoryWebService.find(categoryQuery);

        PageQuery pageQuery = new PageQuery();
        pageQuery.page = 1;
        pageQuery.limit = pageWebOptions.maxSitemapEntries;
        pageQuery.status = PageStatus.ACTIVE;
        QueryResponse<PageResponse> pages = pageWebService.find(pageQuery);

        SitemapBuilder builder = new SitemapBuilder(app.baseURL(), fileRepository, pageWebOptions.maxSitemapEntries);
        builder.appendCategories(categories.items);
        builder.appendPages(pages.items);

        while (categories.items.size() == pageWebOptions.maxSitemapEntries) {
            categoryQuery.page = categoryQuery.page + 1;
            categories = pageCategoryWebService.find(categoryQuery);
            builder.appendCategories(categories.items);
        }

        while (pages.items.size() == pageWebOptions.maxSitemapEntries) {
            pageQuery.page = pageQuery.page + 1;
            pages = pageWebService.find(pageQuery);
            builder.appendPages(pages.items);
        }
        Resource sitemap = builder.build();
        return Response.ok(sitemap).type("text/xml").build();
    }

    @Path("/{s:.+}")
    @GET
    public Response sitemap() {
        String path = uriInfo.getPath();
        Optional<Resource> resource = fileRepository.get(path);
        if (!resource.isPresent()) {
            throw new NotFoundException("missing sitemap, path=" + path);
        }
        return Response.ok(resource.get())
            .type("text/xml").build();
    }
}
