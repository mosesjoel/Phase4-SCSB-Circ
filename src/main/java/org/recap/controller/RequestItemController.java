package org.recap.controller;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.recap.RecapConstants;
import org.recap.RecapCommonConstants;
import org.recap.ils.ILSProtocolConnectorFactory;
import org.recap.ils.JSIPConnectorFactory;
import org.recap.model.AbstractResponseItem;
import org.recap.model.BulkRequestInformation;
import org.recap.model.ItemRefileRequest;
import org.recap.ils.model.response.ItemCheckinResponse;
import org.recap.ils.model.response.ItemCheckoutResponse;
import org.recap.ils.model.response.ItemCreateBibResponse;
import org.recap.ils.model.response.ItemHoldResponse;
import org.recap.ils.model.response.ItemInformationResponse;
import org.recap.ils.model.response.ItemRecallResponse;
import org.recap.ils.model.response.PatronInformationResponse;
import org.recap.model.jpa.ItemRefileResponse;
import org.recap.model.jpa.ItemRequestInformation;
import org.recap.model.jpa.ReplaceRequest;
import org.recap.request.ItemRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Field;
import java.util.Map;

/**
 * Created by sudhishk on 16/11/16.
 * Class for all service part of Requesting Item Functionality
 */
@RestController
@RequestMapping("/requestItem")
public class RequestItemController {

    private static final Logger logger = LoggerFactory.getLogger(RequestItemController.class);

    @Autowired
    private JSIPConnectorFactory jsipConectorFactory;

    @Autowired
    private ItemRequestService itemRequestService;

    @Autowired
    private ILSProtocolConnectorFactory ilsProtocolConnectorFactory;

    /**
     * Gets JSIPConectorFactory object.
     *
     * @return the jsip conector factory
     */
    public JSIPConnectorFactory getJsipConectorFactory() {
        return jsipConectorFactory;
    }

    /**
     * Gets ItemRequestService object.
     *
     * @return the item request service
     */
    public ItemRequestService getItemRequestService() {
        return itemRequestService;
    }

    /**
     * Checkout item method is for processing SIP2 protocol function check out, This function converts SIP data to JSON format.
     *
     * @param itemRequestInformation the item request information
     * @param callInstitition        the call institition
     * @return the abstract response item
     */
    @PostMapping(value = "/checkoutItem", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AbstractResponseItem checkoutItem(@RequestBody ItemRequestInformation itemRequestInformation, String callInstitition) {
        ItemCheckoutResponse itemCheckoutResponse = new ItemCheckoutResponse();
        String itemBarcode;
        try {
            String callInst = callingInsttution(callInstitition, itemRequestInformation);
            if (!itemRequestInformation.getItemBarcodes().isEmpty()) {
                itemBarcode = itemRequestInformation.getItemBarcodes().get(0);
                itemCheckoutResponse = (ItemCheckoutResponse) ilsProtocolConnectorFactory.getIlsProtocolConnector(callInst).checkOutItem(itemBarcode, itemRequestInformation.getPatronBarcode());
            } else {
                itemCheckoutResponse.setSuccess(false);
                itemCheckoutResponse.setScreenMessage("Item Id not found");
            }
        } catch (Exception e) {
            itemCheckoutResponse.setSuccess(false);
            itemCheckoutResponse.setScreenMessage(e.getMessage());
            logger.error(RecapCommonConstants.REQUEST_EXCEPTION, e);
        }
        return itemCheckoutResponse;
    }

    /**
     * This method checkinItem is for processing SIP2 protocol function check in. This function converts SIP data to JSON format.
     *
     * @param itemRequestInformation the item request information
     * @param callInstitition        the call institition
     * @return the abstract response item
     */
    @PostMapping(value = "/checkinItem", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AbstractResponseItem checkinItem(@RequestBody ItemRequestInformation itemRequestInformation, String callInstitition) {
        ItemCheckinResponse itemCheckinResponse;
        String itemBarcode;
        try {
            String callInst = callingInsttution(callInstitition, itemRequestInformation);
            if (!itemRequestInformation.getItemBarcodes().isEmpty()) {
                itemBarcode = itemRequestInformation.getItemBarcodes().get(0);
                logger.info("Patron barcode and Institution info before CheckIn call : patron - {} , institution - {} ",itemRequestInformation.getPatronBarcode(),callInstitition);
                itemCheckinResponse = (ItemCheckinResponse) ilsProtocolConnectorFactory.getIlsProtocolConnector(callInst).checkInItem(itemBarcode, itemRequestInformation.getPatronBarcode());
                logger.info("CheckIn Response Message : {}",itemCheckinResponse.getScreenMessage());
            } else {
                itemCheckinResponse = new ItemCheckinResponse();
                itemCheckinResponse.setSuccess(false);
                itemCheckinResponse.setScreenMessage("Item Id not found");
            }
        } catch (Exception e) {
            itemCheckinResponse = new ItemCheckinResponse();
            itemCheckinResponse.setSuccess(false);
            itemCheckinResponse.setScreenMessage(e.getMessage());
            logger.error(RecapCommonConstants.REQUEST_EXCEPTION, e);
        }
        return itemCheckinResponse;
    }

    /**
     * Hold item abstract response item.
     *
     * @param itemRequestInformation the item request information
     * @param callInstitition        the call institition
     * @return the abstract response item
     */
    @PostMapping(value = "/holdItem", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AbstractResponseItem holdItem(@RequestBody ItemRequestInformation itemRequestInformation, String callInstitition) {
        ItemHoldResponse itemHoldResponse = new ItemHoldResponse();
        try {
            String callInst = callingInsttution(callInstitition, itemRequestInformation);
            String itembarcode = itemRequestInformation.getItemBarcodes().get(0);
            itemHoldResponse = (ItemHoldResponse) ilsProtocolConnectorFactory.getIlsProtocolConnector(callInst).placeHold(itembarcode, itemRequestInformation.getPatronBarcode(),
                    itemRequestInformation.getRequestingInstitution(),
                    itemRequestInformation.getItemOwningInstitution(),
                    itemRequestInformation.getExpirationDate(),
                    itemRequestInformation.getBibId(),
                    getPickupLocationDB(itemRequestInformation, callInst),
                    itemRequestInformation.getTrackingId(),
                    itemRequestInformation.getTitleIdentifier(),
                    itemRequestInformation.getAuthor(),
                    itemRequestInformation.getCallNumber());

        } catch (Exception e) {
            logger.info(RecapCommonConstants.REQUEST_EXCEPTION, e);
            itemHoldResponse.setSuccess(false);
            itemHoldResponse.setScreenMessage("ILS returned a invalid response");
        }
        return itemHoldResponse;
    }

    /**
     * Cancel hold item abstract response item.
     *
     * @param itemRequestInformation the item request information
     * @param callInstitition        the call institition
     * @return the abstract response item
     */
    @PostMapping(value = "/cancelHoldItem", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AbstractResponseItem cancelHoldItem(@RequestBody ItemRequestInformation itemRequestInformation, String callInstitition) {
        ItemHoldResponse itemHoldCancelResponse = null;
        String callInst = callingInsttution(callInstitition, itemRequestInformation);
        if (CollectionUtils.isNotEmpty(itemRequestInformation.getItemBarcodes())) {
            String itembarcode = itemRequestInformation.getItemBarcodes().get(0);
            itemHoldCancelResponse = (ItemHoldResponse) ilsProtocolConnectorFactory.getIlsProtocolConnector(callInst).cancelHold(itembarcode, itemRequestInformation.getPatronBarcode(),
                    itemRequestInformation.getRequestingInstitution(),
                    itemRequestInformation.getExpirationDate(),
                    itemRequestInformation.getBibId(),
                    getPickupLocationDB(itemRequestInformation, callInst), itemRequestInformation.getTrackingId());
        }
        return itemHoldCancelResponse;
    }

    /**
     * Create bibliogrphic item abstract response item.
     *
     * @param itemRequestInformation the item request information
     * @param callInstitition        the call institition
     * @return the abstract response item
     */
    @PostMapping(value = "/createBib", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AbstractResponseItem createBibliogrphicItem(@RequestBody ItemRequestInformation itemRequestInformation, String callInstitition) {
        ItemCreateBibResponse itemCreateBibResponse;
        String itemBarcode;
        logger.info("ESIP CALL FOR CREATE BIB -> {}" , callInstitition);
        String callInst = callingInsttution(callInstitition, itemRequestInformation);
        if (!itemRequestInformation.getItemBarcodes().isEmpty()) {
            itemBarcode = itemRequestInformation.getItemBarcodes().get(0);
            ItemInformationResponse itemInformation = (ItemInformationResponse) itemInformation(itemRequestInformation, itemRequestInformation.getRequestingInstitution());
            if (itemInformation.getScreenMessage().toUpperCase().contains(RecapConstants.REQUEST_ITEM_BARCODE_NOT_FOUND)) {
                itemCreateBibResponse = (ItemCreateBibResponse) ilsProtocolConnectorFactory.getIlsProtocolConnector(callInst).createBib(itemBarcode, itemRequestInformation.getPatronBarcode(), itemRequestInformation.getRequestingInstitution(), itemRequestInformation.getTitleIdentifier());
            } else {
                itemCreateBibResponse = new ItemCreateBibResponse();
                itemCreateBibResponse.setSuccess(true);
                itemCreateBibResponse.setScreenMessage("Item Barcode already Exist");
                itemCreateBibResponse.setItemBarcode(itemBarcode);
                itemCreateBibResponse.setBibId(itemInformation.getBibID());
            }
        } else {
            itemCreateBibResponse = new ItemCreateBibResponse();
        }
        return itemCreateBibResponse;
    }

    /**
     * Item information abstract response item.
     *
     * @param itemRequestInformation the item request information
     * @param callInstitition        the call institition
     * @return the abstract response item
     */
    @PostMapping(value = "/itemInformation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AbstractResponseItem itemInformation(@RequestBody ItemRequestInformation itemRequestInformation, String callInstitition) {
        AbstractResponseItem itemInformationResponse;
        String callInst = callingInsttution(callInstitition, itemRequestInformation);
        String itembarcode = itemRequestInformation.getItemBarcodes().get(0);
        itemInformationResponse = ilsProtocolConnectorFactory.getIlsProtocolConnector(callInst).lookupItem(itembarcode);
        return itemInformationResponse;
    }

    /**
     * Recall item abstract response item.
     *
     * @param itemRequestInformation the item request information
     * @param callInstitition        the call institition
     * @return the abstract response item
     */
    @PostMapping(value = "/recallItem", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AbstractResponseItem recallItem(@RequestBody ItemRequestInformation itemRequestInformation, String callInstitition) {
        ItemRecallResponse itemRecallResponse;
        logger.info("ESIP CALL FOR RECALL ITEM -> {}" , callInstitition);
        String callInst = callingInsttution(callInstitition, itemRequestInformation);
        String itembarcode = itemRequestInformation.getItemBarcodes().get(0);
        itemRecallResponse = (ItemRecallResponse) ilsProtocolConnectorFactory.getIlsProtocolConnector(callInst).recallItem(itembarcode, itemRequestInformation.getPatronBarcode(),
                itemRequestInformation.getRequestingInstitution(),
                itemRequestInformation.getExpirationDate(),
                itemRequestInformation.getBibId(),
                getPickupLocationDB(itemRequestInformation, callInst));
        return itemRecallResponse;
    }

    /**
     * Patron information abstract response item.
     *
     * @param itemRequestInformation the item request information
     * @param callInstitition        the call institition
     * @return the abstract response item
     */
    @PostMapping(value = "/patronInformation", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AbstractResponseItem patronInformation(@RequestBody ItemRequestInformation itemRequestInformation, String callInstitition) {
        PatronInformationResponse patronInformationResponse;
        String callInst = callingInsttution(callInstitition, itemRequestInformation);
        patronInformationResponse = (PatronInformationResponse) ilsProtocolConnectorFactory.getIlsProtocolConnector(callInst).lookupPatron(itemRequestInformation.getPatronBarcode());
        return patronInformationResponse;
    }

    /**
     * Refile item item refile response.
     *
     * @param itemRefileRequest the item refile request
     * @return the item refile response
     */
    @PostMapping(value = "/refile",  consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ItemRefileResponse refileItem(@RequestBody ItemRefileRequest itemRefileRequest) {
        ItemRefileResponse itemRefileResponse = new ItemRefileResponse();
        itemRefileResponse = getItemRequestService().reFileItem(itemRefileRequest,itemRefileResponse);

        if (itemRefileResponse.isSuccess()) {
            itemRefileResponse.setScreenMessage("Successfully Refiled");
        } else {
            if(itemRefileResponse.getScreenMessage()==null){
                itemRefileResponse.setScreenMessage("Cannot process Refile request");
            }
        }
        logger.info("Refile Response: {}",itemRefileResponse.getScreenMessage());
        return itemRefileResponse;
    }


    @PostMapping("/patronValidationBulkRequest")
    public Boolean patronValidationBulkRequest(@RequestBody BulkRequestInformation bulkRequestInformation) {
        return jsipConectorFactory.getJSIPConnector(bulkRequestInformation.getRequestingInstitution()).patronValidation(bulkRequestInformation.getRequestingInstitution(), bulkRequestInformation.getPatronBarcode());
    }

    /**
     * This method refiles the item in ILS. Currently only NYPL has the refile endpoint.
     *
     * @param itemRequestInformation the item request information
     * @param callInstitition        the call institition
     * @return the abstract response item
     */
    @PostMapping(value = "/refileItemInILS", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public AbstractResponseItem refileItemInILS(@RequestBody ItemRequestInformation itemRequestInformation, String callInstitition) {
        ItemRefileResponse itemRefileResponse;
        String itemBarcode;
        try {
            String callInst = callingInsttution(callInstitition, itemRequestInformation);
            if (!itemRequestInformation.getItemBarcodes().isEmpty()) {
                itemBarcode = itemRequestInformation.getItemBarcodes().get(0);
                itemRefileResponse = (ItemRefileResponse) ilsProtocolConnectorFactory.getIlsProtocolConnector(callInst).refileItem(itemBarcode);
            } else {
                itemRefileResponse = new ItemRefileResponse();
                itemRefileResponse.setSuccess(false);
                itemRefileResponse.setScreenMessage(RecapConstants.REQUEST_ITEM_BARCODE_NOT_FOUND);
            }
        } catch (Exception e) {
            itemRefileResponse = new ItemRefileResponse();
            itemRefileResponse.setSuccess(false);
            itemRefileResponse.setScreenMessage(e.getMessage());
            logger.error(RecapCommonConstants.REQUEST_EXCEPTION, e);
        }
        return itemRefileResponse;
    }

    /**
     * This method will replace the requests to LAS queue.
     *
     * @param replaceRequest the replace request
     * @return the string response
     */
    @PostMapping(value = "/replaceRequest", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> replaceRequest(@RequestBody ReplaceRequest replaceRequest) {
        return itemRequestService.replaceRequestsToLASQueue(replaceRequest);
    }

    /**
     * Gets pickup location.
     *
     * @param institution the institution
     * @return the pickup location
     */
    public String getPickupLocation(String institution) {
        String pickUpLocation = "";
        if (institution.equalsIgnoreCase(RecapCommonConstants.PRINCETON)) {
            pickUpLocation = RecapConstants.DEFAULT_PICK_UP_LOCATION_PUL;
        } else if (institution.equalsIgnoreCase(RecapCommonConstants.COLUMBIA)) {
            pickUpLocation = RecapConstants.DEFAULT_PICK_UP_LOCATION_CUL;
        } else if (institution.equalsIgnoreCase(RecapCommonConstants.NYPL)) {
            pickUpLocation = RecapConstants.DEFAULT_PICK_UP_LOCATION_NYPL;
        }
        return pickUpLocation;
    }

    /**
     * Log messages.
     *
     * @param logger    the logger
     * @param clsObject the cls object
     */
    public void logMessages(Logger logger, Object clsObject) {
        try {
            for (Field field : clsObject.getClass().getDeclaredFields()) {
                field.setAccessible(true);
                String name = field.getName();
                Object value = field.get(clsObject);
                if (!StringUtils.isBlank(name) && value != null) {
                    logger.info("Field name: {} Filed Value : {} ", name, value);
                }
            }
        } catch (IllegalAccessException e) {
            logger.error("", e);
        }
    }

    private String callingInsttution(String callingInst, ItemRequestInformation itemRequestInformation) {
        String inst;
        if (callingInst == null) {
            inst = itemRequestInformation.getItemOwningInstitution();
        } else {
            inst = callingInst;
        }
        return inst;
    }

    private String getPickupLocationDB(ItemRequestInformation itemRequestInformation, String callInstitution) {
        if (RecapCommonConstants.NYPL.equalsIgnoreCase(callInstitution)) {
            return itemRequestInformation.getDeliveryLocation();
        }
        return (StringUtils.isBlank(itemRequestInformation.getPickupLocation())) ? getPickupLocation(callInstitution) : itemRequestInformation.getPickupLocation();
    }
}
