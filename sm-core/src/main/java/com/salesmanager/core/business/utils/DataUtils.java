package com.salesmanager.core.business.utils;

import java.math.BigDecimal;

import com.salesmanager.core.constants.MeasureUnit;
import com.salesmanager.core.model.merchant.MerchantStore;

import static com.salesmanager.core.constants.MeasureUnit.*;
import static java.lang.String.valueOf;
import static java.math.BigDecimal.ROUND_HALF_UP;

public class DataUtils {

    public static String trimPostalCode(String postalCode) {

        String pc = postalCode.replaceAll("[^a-zA-Z0-9]", "");

        return pc;

    }


    public static double getWeight(double weight, MerchantStore store,
                                   String base) {

        double weightConstant = 2.2;
        if (base.equals(LB.name())) {
            if (store.getWeightunitcode().equals(LB.name())) {
                return new BigDecimal(valueOf(weight)).setScale(2,
                        ROUND_HALF_UP).doubleValue();
            } else {// pound = kilogram
                double answer = weight * weightConstant;
                BigDecimal w = new BigDecimal(answer);
                return w.setScale(2, ROUND_HALF_UP).doubleValue();
            }
        } else {// need KG
            if (store.getWeightunitcode().equals(KG.name())) {
                return new BigDecimal(valueOf(weight)).setScale(2,
                        ROUND_HALF_UP).doubleValue();
            } else {

                double answer = weight / weightConstant;
                BigDecimal w = new BigDecimal(answer);
                return w.setScale(2, ROUND_HALF_UP).doubleValue();

            }
        }
    }

    public static double getMeasure(double measure, MerchantStore store,
                                    String base) {

        if (base.equals(IN.name())) {
            if (store.getSeizeunitcode().equals(IN.name())) {
                return new BigDecimal(valueOf(measure)).setScale(2,
                        ROUND_HALF_UP).doubleValue();
            } else {// centimeter (inch to centimeter)
                double measureConstant = 2.54;

                double answer = measure * measureConstant;
                BigDecimal w = new BigDecimal(answer);
                return w.setScale(2, ROUND_HALF_UP).doubleValue();

            }
        } else {// need CM
            if (store.getSeizeunitcode().equals(CM.name())) {
                return new BigDecimal(valueOf(measure)).setScale(2)
                        .doubleValue();
            } else {// in (centimeter to inch)
                double measureConstant = 0.39;

                double answer = measure * measureConstant;
                BigDecimal w = new BigDecimal(answer);
                return w.setScale(2, ROUND_HALF_UP).doubleValue();

            }
        }

    }

}
