package org.recap.model.response;

import org.junit.Test;
import org.recap.BaseTestCaseUT;
import org.recap.model.response.ItemCheckoutResponse;

import java.util.Date;

import static org.junit.Assert.assertNotNull;

/**
 * Created by hemalathas on 3/4/17.
 */
public class ItemCheckoutResponseUT extends BaseTestCaseUT {

    @Test
    public void testItemCheckoutResponse() {
        ItemCheckoutResponse itemCheckoutResponse = new ItemCheckoutResponse();
        itemCheckoutResponse.setRenewal(true);
        itemCheckoutResponse.setMagneticMedia(true);
        itemCheckoutResponse.setDesensitize(true);
        itemCheckoutResponse.setTransactionDate(new Date().toString());
        itemCheckoutResponse.setInstitutionID("1");
        itemCheckoutResponse.setPatronIdentifier("414654557");
        itemCheckoutResponse.setTitleIdentifier("test");
        itemCheckoutResponse.setDueDate(new Date().toString());
        itemCheckoutResponse.setFeeType("test");
        itemCheckoutResponse.setMediaType("test");
        itemCheckoutResponse.setBibId("1");
        itemCheckoutResponse.setIsbn("5664471");
        itemCheckoutResponse.setLccn("56646547");
        itemCheckoutResponse.setJobId("4");
        itemCheckoutResponse.setProcessed(true);
        itemCheckoutResponse.setUpdatedDate(new Date().toString());
        itemCheckoutResponse.setCreatedDate(new Date().toString());
        itemCheckoutResponse.setSecurityInhibit("test");
        itemCheckoutResponse.setCurrencyType("test");
        itemCheckoutResponse.setFeeAmount("test");


        assertNotNull(itemCheckoutResponse.getRenewal());
        assertNotNull(itemCheckoutResponse.getMagneticMedia());
        assertNotNull(itemCheckoutResponse.getDesensitize());
        assertNotNull(itemCheckoutResponse.getTransactionDate());
        assertNotNull(itemCheckoutResponse.getInstitutionID());
        assertNotNull(itemCheckoutResponse.getPatronIdentifier());
        assertNotNull(itemCheckoutResponse.getTitleIdentifier());
        assertNotNull(itemCheckoutResponse.getDueDate());
        assertNotNull(itemCheckoutResponse.getFeeType());
        assertNotNull(itemCheckoutResponse.getSecurityInhibit());
        assertNotNull(itemCheckoutResponse.getCurrencyType());
        assertNotNull(itemCheckoutResponse.getFeeAmount());
        assertNotNull(itemCheckoutResponse.getMediaType());
        assertNotNull(itemCheckoutResponse.getBibId());
        assertNotNull(itemCheckoutResponse.getIsbn());
        assertNotNull(itemCheckoutResponse.getLccn());
        assertNotNull(itemCheckoutResponse.getJobId());
        assertNotNull(itemCheckoutResponse.isProcessed());
        assertNotNull(itemCheckoutResponse.getUpdatedDate());
        assertNotNull(itemCheckoutResponse.getCreatedDate());

    }

}