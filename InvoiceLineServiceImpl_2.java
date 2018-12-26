package com.axelor.gst.service.Impl;

import com.axelor.common.ObjectUtils;
import com.axelor.gst.db.AddressGst;
import com.axelor.gst.db.ContactGst;
import com.axelor.gst.db.Invoice;
import com.axelor.gst.db.InvoiceLine;
import com.axelor.gst.db.Party;
import com.axelor.gst.db.ProductGst;
import com.axelor.gst.db.State;
import com.axelor.gst.db.repo.InvoiceLineRepository;
import com.axelor.gst.db.repo.InvoiceRepository;
import com.axelor.gst.service.InvoiceLineService;
import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import java.math.BigDecimal;
import java.util.List;

public class InvoiceLineServiceImpl implements InvoiceLineService {

  @Inject private InvoiceLineRepository invoiceLineServiceImpl;
  @Inject private InvoiceRepository invoiceRepository;

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
  @Transactional
  public Invoice calculate(Invoice invoice) {
    // TODO Auto-generated method stub
    BigDecimal netAmount = BigDecimal.ZERO;
    BigDecimal netGstAmount = BigDecimal.ZERO;
    BigDecimal totalGrossAmount = BigDecimal.ZERO;
    Boolean stateStatus = false;

    invoice = invoiceRepository.find(invoice.getId());

    if (ObjectUtils.isEmpty(invoice.getInvoiceItems())) {
      return invoice;
    }

    if (!ObjectUtils.isEmpty(invoice.getInvoiceItems())) {

      State stateInline = invoice.getCompany().getAddressgst().getState1();
      State state2Inline = invoice.getInvoiceAddress().getState1();

      if (stateInline.equals(state2Inline)) {

        stateStatus = true;
      }

      for (InvoiceLine invoiceLine : invoice.getInvoiceItems()) {

        // InvoiceLine Value Setting

        ProductGst productGst = invoiceLine.getProduct();
        BigDecimal value = invoiceLine.getPrice().multiply(new BigDecimal(invoiceLine.getQty()));
        invoiceLine.setNetAmount(value);
        if (stateStatus) {

          BigDecimal gstAmount_SGST_Single =
              value
                  .multiply(productGst.getGstrate())
                  .divide(new BigDecimal(100))
                  .divide(new BigDecimal(2));
          BigDecimal gstAmount_CGCT_Single =
              value
                  .multiply(productGst.getGstrate())
                  .divide(new BigDecimal(100))
                  .divide(new BigDecimal(2));
          invoiceLine.setGstRate(productGst.getGstrate());
          invoiceLine.setCGST(gstAmount_CGCT_Single);
          invoiceLine.setSGST(gstAmount_SGST_Single);
          invoiceLine.setItem(productGst.getName() + " " + productGst.getCode());

          // Gross Amount
          BigDecimal grossAmount = value.add(gstAmount_CGCT_Single).add(gstAmount_SGST_Single);

          invoiceLine.setGrossAmount(grossAmount);

        } else {

          BigDecimal gstAmount_IGCT_Single =
              value.multiply(productGst.getGstrate()).divide(new BigDecimal(100));
          invoiceLine.setGstRate(productGst.getGstrate());
          invoiceLine.setIGST(gstAmount_IGCT_Single);

          // Gross Amount
          BigDecimal grossAmount = value.add(gstAmount_IGCT_Single);
          invoiceLine.setGrossAmount(grossAmount);
        }

        // Net Amount and Product Gst , All Product Gst : Invoice Form Setting

        BigDecimal gstAmount = value.multiply(productGst.getGstrate()).divide(new BigDecimal(100));

        System.out.println("Single Entry : " + value);
        System.out.println(
            "Single Entry  Gst : " + productGst.getName() + " Gst Amount :  " + gstAmount);

        netAmount = netAmount.add(value);
        // Net Amount
        invoice.setNetAmount(netAmount);
        // This value also Consider as IGST GST Tax
        netGstAmount = netGstAmount.add(gstAmount);

        totalGrossAmount = netAmount.add(netGstAmount);
        // Gross Amount Of Invoice

        invoice.setGrossAmount(totalGrossAmount);

        if (stateStatus) {
          BigDecimal netSGST = netGstAmount.divide(new BigDecimal(2));
          BigDecimal netCGST = netGstAmount.divide(new BigDecimal(2));
          invoice.setNetCSGT(netCGST);
          invoice.setNetSGST(netSGST);
        } else {
          invoice.setNetIGST(netGstAmount);
        }
      }
      System.out.println(" All Entry : " + netAmount);
      System.out.println(" All Gst Amount :  " + netGstAmount);

      // gstAmount=netAmount.multiply(multiplicand)

    }
    invoiceRepository.save(invoice);
    return invoice;
  }

  @Override
  @Transactional
  public Invoice setInvoiceAdd_As_ShippingAddress(Invoice invoice) {
    // TODO Auto-generated method stub

    invoice.setShippingAddress(invoice.getInvoiceAddress());

    return invoice;
  }

  @Override
  @Transactional
  public Invoice setPartyPrimaryContact(Invoice invoice) {
    // TODO Auto-generated method stub

    invoice = invoiceRepository.find(invoice.getId());

    Party party = invoice.getParty();

    // Party Primary Contact

    List<ContactGst> objConatct = party.getContact();

    for (ContactGst cgst : objConatct) {

      if ((cgst.getType().equals("Primary")) && (party.getId() == cgst.getParty().getId())) {

        invoice.setPartyContact(cgst);
      }
    }

    // Party Address :  Default or Invoice

    List<AddressGst> addressGst = party.getAddress();

    for (AddressGst address : addressGst) {

      if (((address.getType().equals("invoice")) && (party.getId() == address.getParty().getId()))
          || ((address.getType().equals("default"))
              && (party.getId() == address.getParty().getId()))) {

        invoice.setInvoiceAddress(address);
      }

      if (((address.getType().equals("default")) && (party.getId() == address.getParty().getId()))
          || ((address.getType().equals("shipping"))
              && (party.getId() == address.getParty().getId()))) {

        invoice.setShippingAddress(address);
      }
    }

    invoiceRepository.save(invoice);

    return invoice;
  }
}

/*
 * == calculate Method == @Override
 *
 * public Invoice calculate(Invoice invoice) { // TODO Auto-generated method
 * stub BigDecimal netAmount = BigDecimal.ZERO; BigDecimal netGstAmount =
 * BigDecimal.ZERO; BigDecimal totalGrossAmount = BigDecimal.ZERO; Boolean
 * stateStatus = false;
 *
 * if (ObjectUtils.isEmpty(invoice.getInvoiceItems())) { return invoice; }
 *
 * if (!ObjectUtils.isEmpty(invoice.getInvoiceItems())) {
 *
 * State stateInline = invoice.getCompany().getAddressgst().getState1(); State
 * state2Inline = invoice.getInvoiceAddress().getState1();
 *
 * if (stateInline.equals(state2Inline)) {
 *
 * stateStatus = true; }
 *
 * for (InvoiceLine invoiceLine : invoice.getInvoiceItems()) {
 *
 * // State Logic : Company
 *
 * /*
 *
 * CompanyGst companyGst = invoice.getCompany();
 *
 * AddressGst addressGst = companyGst.getAddressgst();
 *
 * State state = addressGst.getState1();
 *
 *
 *
 * System.out.println("Company State : " + state.getName());
 * //System.out.println(" Company Name Inline Way : "+stateInline.getName());
 *
 * // State Logic : Invoice Address AddressGst addressGst2 =
 * invoice.getInvoiceAddress();
 *
 * State state2 = addressGst2.getState1();
 *
 *
 * System.out.println(" Invoice Address State : " + state2.getName());
 * System.out.println(" Invoice Address State Inline Way : "+state2Inline.
 * getName());
 *
 *
 */
/*
* ProductGst productGst = invoiceLine.getProduct(); BigDecimal value =
* invoiceLine.getPrice().multiply(new BigDecimal(invoiceLine.getQty()));
* invoiceLine.setNetAmount(value); if (stateStatus) {
*
* BigDecimal gstAmount_SGST_Single = value.multiply(productGst.getGstrate())
* .divide(new BigDecimal(100)).divide(new BigDecimal(2)); BigDecimal
* gstAmount_CGCT_Single = value.multiply(productGst.getGstrate()) .divide(new
* BigDecimal(100)).divide(new BigDecimal(2));
* invoiceLine.setGstRate(productGst.getGstrate());
* invoiceLine.setCGST(gstAmount_CGCT_Single);
* invoiceLine.setSGST(gstAmount_SGST_Single);
*
* // Gross Amount BigDecimal grossAmount =
* value.add(gstAmount_CGCT_Single).add(gstAmount_SGST_Single);
*
* invoiceLine.setGrossAmount(grossAmount);
*
* } else {
*
* BigDecimal gstAmount_IGCT_Single = value.multiply(productGst.getGstrate())
* .divide(new BigDecimal(100));
* invoiceLine.setGstRate(productGst.getGstrate());
* invoiceLine.setIGST(gstAmount_IGCT_Single);
*
* // Gross Amount BigDecimal grossAmount = value.add(gstAmount_IGCT_Single);
* invoiceLine.setGrossAmount(grossAmount);
*
* }
*
* // Net Amount and Product Gst , All Product Gst
*
* BigDecimal gstAmount = value.multiply(productGst.getGstrate()).divide(new
* BigDecimal(100));
*
* System.out.println("Single Entry : " + value);
* System.out.println("Single Entry  Gst : " + productGst.getName() +
* " Gst Amount :  " + gstAmount);
*
* netAmount = netAmount.add(value); // Net Amount
* invoice.setNetAmount(netAmount); // This value also Consider as IGST GST Tax
* netGstAmount = netGstAmount.add(gstAmount);
*
* totalGrossAmount = netAmount.add(netGstAmount); // Gross Amount Of Invoice
*
* invoice.setGrossAmount(totalGrossAmount);
*
* if (stateStatus) { BigDecimal netSGST = netGstAmount.divide(new
* BigDecimal(2)); BigDecimal netCGST = netGstAmount.divide(new BigDecimal(2));
* invoice.setNetCSGT(netCGST); invoice.setNetSGST(netSGST); } else {
* invoice.setNetIGST(netGstAmount); }
*
* } System.out.println(" All Entry : " + netAmount);
* System.out.println(" All Gst Amount :  " + netGstAmount);
*
* // gstAmount=netAmount.multiply(multiplicand)
*
* }
*
*
* @Override
*
* @Transactional public Invoice setSalePriceForInvoiceLinePrice(Invoice
* invoice) { // TODO Auto-generated method stub
*
* invoice = invoiceRepository.find(invoice.getId());
*
* // Make it List
*
* LinkedList<InvoiceLine> invoiceLine = (LinkedList<InvoiceLine>)
* invoice.getInvoiceItems();
*
* InvoiceLine invoiceLine_Single = invoiceLine.getLast();
*
* ProductGst productGst = invoiceLine_Single.getProduct();
*
* System.out.println(" product price : " + invoiceLine_Single.getProduct());
*
* invoiceLine_Single.setPrice(productGst.getSaleprice());
*
* invoiceRepository.save(invoice);
*
* System.out.println(" Invoice Line Price : " + invoiceLine_Single.getPrice());
* return invoice; }
*
* return invoice; }
*
*  @Override
 public InvoiceLine setSalePriceForInvoiceLinePrice(InvoiceLine invoiceLine) {
   // TODO Auto-generated method stub
   ProductGst productGst = invoiceLine.getProduct();
   System.out.println(" product price : " + productGst.getSaleprice());
   invoiceLine.setPrice(productGst.getSaleprice());
   // System.out.println(" Invoice Line Price : " + invoiceLine_Single.getPrice());
   return invoiceLine;
 }
*
*
*
*/
