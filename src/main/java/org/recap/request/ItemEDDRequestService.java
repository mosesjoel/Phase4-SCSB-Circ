package org.recap.request;

import org.apache.camel.Exchange;
import org.apache.commons.lang3.StringUtils;
import org.recap.ReCAPConstants;
import org.recap.controller.RequestItemValidatorController;
import org.recap.ils.model.response.ItemInformationResponse;
import org.recap.model.ItemEntity;
import org.recap.model.ItemRequestInformation;
import org.recap.repository.ItemDetailsRepository;
import org.recap.repository.RequestTypeDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;

import java.util.List;

/**
 * Created by sudhishk on 1/12/16.
 */
@Component
public class ItemEDDRequestService {

    private static final Logger logger = LoggerFactory.getLogger(ItemEDDRequestService.class);

    @Autowired
    private ItemDetailsRepository itemDetailsRepository;

    @Autowired
    private RequestTypeDetailsRepository requestTypeDetailsRepository;

    @Autowired
    private ItemRequestService itemRequestService;

    public ItemDetailsRepository getItemDetailsRepository() {
        return itemDetailsRepository;
    }

    public RequestTypeDetailsRepository getRequestTypeDetailsRepository() {
        return requestTypeDetailsRepository;
    }

    public ItemRequestService getItemRequestService() {
        return itemRequestService;
    }

    public ItemInformationResponse getItemInformationResponse(){
        return new ItemInformationResponse();
    }

    public ItemInformationResponse eddRequestItem(ItemRequestInformation itemRequestInfo, Exchange exchange) {

        String messagePublish;
        boolean bsuccess;
        List<ItemEntity> itemEntities;
        ItemEntity itemEntity;
        ItemInformationResponse itemResponseInformation = getItemInformationResponse();
        try {
            itemEntities = getItemDetailsRepository().findByBarcodeIn(itemRequestInfo.getItemBarcodes());

            if (itemEntities != null && !itemEntities.isEmpty()) {
                logger.info("Item Exists in SCSB Database");
                itemEntity = itemEntities.get(0);
                if (itemEntity.getBibliographicEntities().get(0).getOwningInstitutionBibId().trim().length() <= 0) {
                    itemRequestInfo.setBibId(itemEntity.getBibliographicEntities().get(0).getOwningInstitutionBibId());
                }
                itemRequestInfo.setItemOwningInstitution(itemEntity.getInstitutionEntity().getInstitutionCode());
                itemRequestInfo.setTitleIdentifier(getItemRequestService().getTitle(itemRequestInfo.getTitleIdentifier(), itemEntity));
                itemRequestInfo.setCustomerCode(itemEntity.getCustomerCode());
                itemResponseInformation.setItemId(itemEntity.getItemId());
                itemResponseInformation.setPatronBarcode(itemRequestInfo.getPatronBarcode());
                itemRequestInfo.setRequestNotes(getNotes(itemRequestInfo));

                Integer requestId;
                if (getItemRequestService().getGfaService().isUseQueueLasCall()) {
                    requestId = getItemRequestService().updateRecapRequestItem(itemRequestInfo, itemEntity, ReCAPConstants.REQUEST_STATUS_PENDING);
                } else {
                    requestId = getItemRequestService().updateRecapRequestItem(itemRequestInfo, itemEntity, ReCAPConstants.REQUEST_STATUS_EDD);
                }
                itemResponseInformation.setRequestId(requestId);
                itemResponseInformation = getItemRequestService().updateGFA(itemRequestInfo, itemResponseInformation);
                bsuccess = true;
                messagePublish = "EDD requests is successfull";
            } else {
                messagePublish = ReCAPConstants.WRONG_ITEM_BARCODE;
                bsuccess = false;
            }
            logger.info("Finish Processing");
            itemResponseInformation.setScreenMessage(messagePublish);
            itemResponseInformation.setSuccess(bsuccess);
            itemResponseInformation.setItemOwningInstitution(itemRequestInfo.getItemOwningInstitution());
            itemResponseInformation.setDueDate(itemRequestInfo.getExpirationDate());
            itemResponseInformation.setRequestingInstitution(itemRequestInfo.getRequestingInstitution());
            itemResponseInformation.setTitleIdentifier(itemRequestInfo.getTitleIdentifier());
            itemResponseInformation.setBibID(itemRequestInfo.getBibId());
            itemResponseInformation.setItemBarcode(itemRequestInfo.getItemBarcodes().get(0));
            itemResponseInformation.setRequestType(itemRequestInfo.getRequestType());
            itemResponseInformation.setEmailAddress(itemRequestInfo.getEmailAddress());
            itemResponseInformation.setDeliveryLocation(itemRequestInfo.getDeliveryLocation());
            itemResponseInformation.setRequestNotes(getItemRequestService().getNotes(bsuccess, messagePublish, itemRequestInfo.getRequestNotes()));
            // Update Topics
            getItemRequestService().sendMessageToTopic(itemRequestInfo.getRequestingInstitution(), itemRequestInfo.getRequestType(), itemResponseInformation, exchange);
        } catch (RestClientException ex) {
            logger.error(ReCAPConstants.REQUEST_EXCEPTION_REST, ex);
        } catch (Exception ex) {
            logger.error(ReCAPConstants.REQUEST_EXCEPTION, ex);
        }
        return itemResponseInformation;
    }

    private String getNotes(ItemRequestInformation itemRequestInfo) {
        String notes = "";
        if (!StringUtils.isBlank(itemRequestInfo.getRequestNotes())) {
            notes = String.format("User: %s", itemRequestInfo.getRequestNotes());
        }
        notes += String.format("\nStart Page: %s ;End Page: %s ;Volume Number: %s ;Issue: %s ;Article Author: %s ;Article/Chapter Title: %s ", itemRequestInfo.getStartPage(), itemRequestInfo.getEndPage(), itemRequestInfo.getVolume(), itemRequestInfo.getIssue(), itemRequestInfo.getAuthor(), itemRequestInfo.getChapterTitle());
        return notes;
    }
}
