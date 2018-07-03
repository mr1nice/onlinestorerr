package com.salesmanager.core.business.utils.query;

public class ProductQueryBuilder {
	
	public static String buildProductQuery() {

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

        return qs.toString();
    }

}
