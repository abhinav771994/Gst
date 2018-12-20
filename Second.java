package com.axelor.gst.service.Impl;

import com.axelor.common.ObjectUtils;
import com.axelor.gst.db.AddressGst;
import com.axelor.gst.db.CompanyGst;
import com.axelor.gst.db.Invoice;
import com.axelor.gst.db.InvoiceLine;
import com.axelor.gst.db.ProductGst;
import com.axelor.gst.db.State;
import com.axelor.gst.db.repo.InvoiceLineRepository;
import com.axelor.gst.service.InvoiceLineService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;

public class InvoiceLineServiceImpl implements InvoiceLineService {

  @Inject private InvoiceLineRepository invoiceLineServiceImpl;

  @Override
  @Transactional
  public InvoiceLine getproductGstRate(InvoiceLine invoiceLine) {
    // TODO Auto-generated method stub

    invoiceLine = invoiceLineServiceImpl.find(invoiceLine.getId());
    ProductGst productGst = invoiceLine.getProduct();

    invoiceLine.setGstRate(productGst.getGstrate());
    invoiceLine.setItem(productGst.getName() + " ".concat(productGst.getCode()));
    System.out.println("Produact Name : " + productGst.getName());
    System.out.println("Product Rate : " + productGst.getGstrate());
    invoiceLineServiceImpl.save(invoiceLine);

    return invoiceLine;
  }

  @Override
  @Transactional
  public InvoiceLine getNetAmount(InvoiceLine invoiceLine) {
    // TODO Auto-generated method stub
    invoiceLine = invoiceLineServiceImpl.find(invoiceLine.getId());
    BigDecimal netAmount = BigDecimal.ZERO;
    BigDecimal price = BigDecimal.ZERO;
    Integer qty = 0;
    price = invoiceLine.getPrice();
    qty = invoiceLine.getQty();
    netAmount = price.multiply(new BigDecimal(qty));
    System.out.println("InvoiceLine Price : " + invoiceLine.getPrice());
    System.out.println("InvoiceLine Qty: " + invoiceLine.getQty());
    System.out.println("InvoiceLine NetAmount: " + netAmount);
    invoiceLine.setNetAmount(netAmount);

    invoiceLineServiceImpl.save(invoiceLine);

    return invoiceLine;
  }

  @Override
  public Invoice calculate(Invoice invoice) {
    // TODO Auto-generated method stub
    BigDecimal netAmount = BigDecimal.ZERO;
    BigDecimal netGstAmount = BigDecimal.ZERO;
    Boolean stateStatus=false;
   
    if (ObjectUtils.isEmpty(invoice.getInvoiceItems())) {
      return invoice;
    }

    if (!ObjectUtils.isEmpty(invoice.getInvoiceItems())) {
    	
    	 State stateInline=invoice.getCompany().getAddressgst().getState1();
    	 State state2Inline=invoice.getInvoiceAddress().getState1();
    	 
    	 if(stateInline.equals(state2Inline)) {
    		 
    		 stateStatus=true;
    	 }
    	 

      for (InvoiceLine invoiceLine : invoice.getInvoiceItems()) {

        // State Logic : Company

        /*
        
        CompanyGst companyGst = invoice.getCompany();

        AddressGst addressGst = companyGst.getAddressgst();

        State state = addressGst.getState1();
        
       

        System.out.println("Company State : " + state.getName());
        //System.out.println(" Company Name Inline Way : "+stateInline.getName());

        // State Logic : Invoice Address
        AddressGst addressGst2 = invoice.getInvoiceAddress();

        State state2 = addressGst2.getState1();

       
        System.out.println(" Invoice Address State : " + state2.getName());
        System.out.println(" Invoice Address State Inline Way : "+state2Inline.getName());
        
        
        */
    	  ProductGst productGst = invoiceLine.getProduct();
          BigDecimal value = invoiceLine.getPrice().multiply(new BigDecimal(invoiceLine.getQty()));
    	  
    	  if(stateStatus) {
    		  
    		  
    	  }else {
    		
    		  BigDecimal gstAmount_IGCT_Single = value.multiply(productGst.getGstrate()).divide(new BigDecimal(100));
    		  
    	  }
    	  

        // Net Amount and Product Gst , All Product Gst
        
        BigDecimal gstAmount = value.multiply(productGst.getGstrate()).divide(new BigDecimal(100));

        System.out.println("Single Entry : " + value);
        System.out.println(
            "Single Entry  Gst : " + productGst.getName() + " Gst Amount :  " + gstAmount);

        netAmount = netAmount.add(value);
        netGstAmount = netGstAmount.add(gstAmount);
      }
      System.out.println(" All Entry : " + netAmount);
      System.out.println(" All Gst Amount :  " + netGstAmount);

      // gstAmount=netAmount.multiply(multiplicand)

    }

    return invoice;
  }
}
