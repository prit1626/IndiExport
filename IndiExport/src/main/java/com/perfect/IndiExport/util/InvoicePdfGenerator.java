package com.perfect.IndiExport.util;

import com.itextpdf.html2pdf.HtmlConverter;
import com.perfect.IndiExport.entity.Buyer;
import com.perfect.IndiExport.entity.Invoice;
import com.perfect.IndiExport.repository.BuyerRepository;

import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;

@Component
public class InvoicePdfGenerator {

    private final BuyerRepository buyerRepository;

    public InvoicePdfGenerator(BuyerRepository buyerRepository) {
        this.buyerRepository = buyerRepository;
    }

    public byte[] generatePdf(Invoice invoice) {
        String html = generateInvoiceHtml(invoice);
        
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            HtmlConverter.convertToPdf(html, outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate PDF", e);
        }
    }

    private String generateInvoiceHtml(Invoice invoice) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        
        Buyer buyer = buyerRepository.findById(invoice.getBuyer().getId())
        .orElseThrow(() -> new RuntimeException("Buyer profile not found"));


        return "<!DOCTYPE html>" +
                "<html>" +
                "<head>" +
                "<style>" +
                "body { font-family: Arial, sans-serif; margin: 40px; }" +
                ".header { text-align: center; margin-bottom: 30px; }" +
                ".invoice-number { font-size: 24px; font-weight: bold; color: #2563eb; }" +
                ".section { margin-bottom: 20px; }" +
                ".section-title { font-weight: bold; font-size: 14px; margin-bottom: 10px; color: #1e293b; }" +
                ".info-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }" +
                ".info-table td { padding: 8px; border-bottom: 1px solid #e2e8f0; }" +
                ".info-table .label { font-weight: bold; width: 150px; }" +
                ".items-table { width: 100%; border-collapse: collapse; margin: 20px 0; }" +
                ".items-table th, .items-table td { padding: 12px; text-align: left; border: 1px solid #e2e8f0; }" +
                ".items-table th { background-color: #f8fafc; font-weight: bold; }" +
                ".total-section { text-align: right; margin-top: 20px; }" +
                ".total-row { margin: 5px 0; }" +
                ".total-label { font-weight: bold; display: inline-block; width: 150px; }" +
                ".total-value { display: inline-block; width: 150px; }" +
                ".footer { margin-top: 40px; text-align: center; color: #64748b; font-size: 12px; }" +
                "</style>" +
                "</head>" +
                "<body>" +
                "<div class='header'>" +
                "<h1>INVOICE</h1>" +
                "<div class='invoice-number'>" + invoice.getInvoiceNumber() + "</div>" +
                "<div>Date: " + invoice.getCreatedAt().format(dateFormatter) + "</div>" +
                "</div>" +
                
                "<div style='display: flex; justify-content: space-between;'>" +
                "<div class='section' style='width: 48%;'>" +
                "<div class='section-title'>SELLER (EXPORTER)</div>" +
                "<table class='info-table'>" +
                "<tr><td class='label'>Business Name:</td><td>" + invoice.getSeller().getBusinessName() + "</td></tr>" +
                "<tr><td class='label'>GST Number:</td><td>" + (invoice.getSeller().getGstNumber() != null ? invoice.getSeller().getGstNumber() : "N/A") + "</td></tr>" +
                "<tr><td class='label'>Address:</td><td>" + invoice.getSeller().getAddress() + "</td></tr>" +
                "<tr><td class='label'>City:</td><td>" + invoice.getSeller().getCity() + "</td></tr>" +
                "<tr><td class='label'>State:</td><td>" + invoice.getSeller().getState() + "</td></tr>" +
                "<tr><td class='label'>Country:</td><td>INDIA</td></tr>" +
                "</table>" +
                "</div>" +
                
                "<div class='section' style='width: 48%;'>" +
                "<div class='section-title'>BUYER</div>" +
                "<table class='info-table'>" +
                "<tr><td class='label'>Name:</td><td>" + invoice.getBuyer().getName() + "</td></tr>" +
                "<tr><td class='label'>Email:</td><td>" + invoice.getBuyer().getEmail() + "</td></tr>" +
                "<tr><td class='label'>Country:</td><td>" + buyer.getCountry() + "</td></tr>" +
                "</table>" +
                "</div>" +
                "</div>" +
                
                "<div class='section'>" +
                "<div class='section-title'>PRODUCT DETAILS</div>" +
                "<table class='items-table'>" +
                "<thead>" +
                "<tr>" +
                "<th>Product Name</th>" +
                "<th>Category</th>" +
                "<th>Quantity</th>" +
                "<th>Unit Price (₹)</th>" +
                "<th>Total (₹)</th>" +
                "</tr>" +
                "</thead>" +
                "<tbody>" +
                "<tr>" +
                "<td>" + invoice.getProduct().getName() + "</td>" +
                "<td>" + (invoice.getProduct().getCategory() != null ? invoice.getProduct().getCategory() : "N/A") + "</td>" +
                "<td>" + invoice.getQuantity() + "</td>" +
                "<td>₹" + String.format("%.2f", invoice.getUnitPrice()) + "</td>" +
                "<td>₹" + String.format("%.2f", invoice.getTotalPrice()) + "</td>" +
                "</tr>" +
                "</tbody>" +
                "</table>" +
                "</div>" +
                
                "<div class='section'>" +
                "<div class='section-title'>SHIPPING INFORMATION</div>" +
                "<table class='info-table'>" +
                "<tr><td class='label'>Shipping Method:</td><td>" + (invoice.getShippingMethod() != null ? invoice.getShippingMethod() : "Not specified") + "</td></tr>" +
                (invoice.getShippingCost() != null ? "<tr><td class='label'>Shipping Cost:</td><td>₹" + String.format("%.2f", invoice.getShippingCost()) + "</td></tr>" : "") +
                "</table>" +
                "</div>" +
                
                "<div class='total-section'>" +
                "<div class='total-row'>" +
                "<span class='total-label'>Subtotal:</span>" +
                "<span class='total-value'>₹" + String.format("%.2f", invoice.getTotalPrice()) + "</span>" +
                "</div>" +
                (invoice.getShippingCost() != null && invoice.getShippingCost().compareTo(java.math.BigDecimal.ZERO) > 0 ?
                "<div class='total-row'>" +
                "<span class='total-label'>Shipping:</span>" +
                "<span class='total-value'>₹" + String.format("%.2f", invoice.getShippingCost()) + "</span>" +
                "</div>" : "") +
                "<div class='total-row' style='font-size: 18px; font-weight: bold; margin-top: 10px;'>" +
                "<span class='total-label'>Total Amount:</span>" +
                "<span class='total-value'>₹" + String.format("%.2f", invoice.getTotalAmount()) + "</span>" +
                "</div>" +
                (invoice.getConvertedAmount() != null && invoice.getConvertedCurrency() != null ?
                "<div class='total-row' style='margin-top: 10px; color: #64748b;'>" +
                "<span class='total-label'>Equivalent (" + invoice.getConvertedCurrency() + "):</span>" +
                "<span class='total-value'>" + String.format("%.2f", invoice.getConvertedAmount()) + " " + invoice.getConvertedCurrency() + "</span>" +
                "</div>" : "") +
                "</div>" +
                
                "<div class='footer'>" +
                "<p>This is a computer-generated invoice. No signature required.</p>" +
                "<p>Generated by IndiExport B2B Marketplace</p>" +
                "</div>" +
                "</body>" +
                "</html>";
    }
}



