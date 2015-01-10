package de.htw.sdf.photoplatform.manager.common;

import java.io.File;
import java.io.InputStream;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.elasticsearch.common.mvel2.optimizers.impl.refl.nodes.ThisValueAccessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.paypal.api.payments.*;
import com.paypal.core.ConfigManager;
import com.paypal.core.rest.APIContext;
import com.paypal.core.rest.OAuthTokenCredential;
import com.paypal.core.rest.PayPalRESTException;
import com.paypal.core.rest.PayPalResource;

import de.htw.sdf.photoplatform.common.Messages;
import de.htw.sdf.photoplatform.exception.common.AbstractBaseException;
import de.htw.sdf.photoplatform.exception.common.ServiceException;
import de.htw.sdf.photoplatform.persistence.model.Image;
import de.htw.sdf.photoplatform.persistence.model.PurchaseItem;
import de.htw.sdf.photoplatform.persistence.model.User;
import de.htw.sdf.photoplatform.webservice.Endpoints;

@Service
public class PaypalService extends DAOReferenceCollector {
	
	private Logger Log = Logger.getLogger(PaypalService.class.getName());

    @Resource
    private Messages messages;
    
    /*
     * OAuth Token with additional information
     */
    private OAuthTokenCredential OAuthToken;

	/**
	 * Default Constructor
	 */
    public PaypalService() {
    	try {
	    	//Need to be called so ConfigMap is initialized everywhere in the SDK/API, however it still loads the sdk_config file
	    	PayPalResource.initializeToDefault();
	    	Log.info("ClientID: " + PayPalResource.getClientID());
	    	Log.info("Secret: " + PayPalResource.getClientSecret());
    	} catch(PayPalRESTException ex) {
    		Log.error("Paypal Rest API threw an error on initialisation: " + ex.getMessage());
    	}
    }
    
    /*
     *	Generates a new OAuthToken, replacing the old one
     */
    private void generateNewOAuthToken() {
    	OAuthToken = new OAuthTokenCredential(PayPalResource.getClientID(), PayPalResource.getClientSecret());
    }

    /*
     *	Returns a non expired OAuthToken
     *
     *	@return valid OAuth Paypal token
     */
    public String getOAuthToken() throws PayPalRESTException {
    	if((OAuthToken == null) || (OAuthToken.expiresIn() < 0)) {
    		generateNewOAuthToken();
    	}
    	
    	return OAuthToken.getAccessToken();
    }
    
    /*
     * Creates a payment on paypal
     * 
     * @param Items the to be puchased items.
     * @param BaseURL The BaseURL where paypal will redirect the buyer after approval or cancel.
     * 
     * @returns the paypal response as JSON
     */
    public String CreatePayment( List<Image> Items, String BaseURL) throws AbstractBaseException {
    	// Use nulled Local so String.format will use . as decimal delimiter
    	Locale l = null;
    	double Total = 0;
    	
    	//ItemList
    	NumberFormat formatter = new DecimalFormat("#0.00");   
    	List<Item> PaypalItems = new ArrayList<Item>();
    	for (Image Image : Items) {
    		Item item = new Item("1", Image.getName(), String.format(l, "%.2f" ,Image.getPrice()), "EUR");
    		Total = Total + Image.getPrice();
    		PaypalItems.add(item);
    	}
    	
    	ItemList itemList = new ItemList();
    	itemList.setItems(PaypalItems);

    	//Amount
    	Amount amount = new Amount("EUR", String.format(l, "%.2f" ,Total));
    	
    	//Transaction
    	Transaction transaction = new Transaction();
    	transaction.setAmount(amount);
    	transaction.setItemList(itemList);
    	transaction.setDescription(messages.getMessage("paypal.transaction.description"));

		List<Transaction> transactions = new ArrayList<Transaction>();
		transactions.add(transaction);
		
    	//Payer
		Payer payer = new Payer("paypal");
    	
    	// Payment
		Payment payment = new Payment("sale", payer);
		payment.setTransactions(transactions);
    	
    	//RedirectURLS
		Log.info("BaseURL: " + BaseURL);
		RedirectUrls redirectUrls = new RedirectUrls();
		redirectUrls.setCancelUrl(BaseURL + "/paypaltest");
		redirectUrls.setReturnUrl(BaseURL + "/paypaltest/approval");
		payment.setRedirectUrls(redirectUrls);
		
    	// Create
		Payment newPayment;
		try {
			newPayment = payment.create(getOAuthToken());
		} catch(PayPalRESTException ex)
		{
			//Error
    		Log.error("Error while trying to create Paypal Payment: " + ex.getMessage());
    		throw new ServiceException(AbstractBaseException.PAYPAL_REST_ERROR);
		}
		/*
		// Get redirect link to paypal page
		Iterator<Links> links = newPayment.getLinks().iterator();
		while (links.hasNext()) {
			Links link = links.next();
			if (link.getRel().equalsIgnoreCase("approval_url")) {
		    	// return to redirect
				return  link.getHref();
			}
		}

		//Null means no redirect something went wrong
		Log.warn("No paypal redirect link found!");
		return null;	
		*/
		
		return newPayment.toJSON();
    }
    
    /*
     * Executes the payment on paypal, so it is fullfilled
     * 
     * @param PaymentId Payment ID of this payment process.
     * @param PayerID Payer ID of the buyer; provided by the paypal redirect.
     * 
     * @returns the paypal response as JSON
     */
    public String ExecutePayment(String PaymentId, String PayerID) throws AbstractBaseException {
    	Payment payment = new Payment();
    	payment.setId(PaymentId);
    	
    	PaymentExecution paymentExecution = new PaymentExecution();
		paymentExecution.setPayerId(PayerID);
		
		Payment FullfilledPayment;
		try {
			FullfilledPayment = payment.execute(getOAuthToken(), paymentExecution);
		} catch(PayPalRESTException ex)
		{
			//Error
    		Log.error("Error while trying to execute Paypal Payment: " + ex.getMessage());
    		throw new ServiceException(AbstractBaseException.PAYPAL_REST_ERROR);
		}
		
		return FullfilledPayment.toJSON();
    }
}
