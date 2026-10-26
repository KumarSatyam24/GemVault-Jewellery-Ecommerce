package com.examly.springapp.service;

import com.examly.springapp.model.JewelleryItem;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ProductCsvImportService {
  public static final String HEADER = "itemId,itemName,sku,description,imageUrl,category,material,purity,stoneClarity,weightInGrams,stoneWeightInGrams,pricePerGram,stoneValue,makingCharges,vaCharges,discountAmount,cgstRate,sgstRate,roundOff,stockQuantity,reorderThreshold";
  private final JewelleryItemService items;

  public ProductCsvImportService(JewelleryItemService items) {
    this.items = items;
  }

  public record Result(int created, int updated, List<String> errors) {
  }

  public String exportAll() {
    StringBuilder csv = new StringBuilder(HEADER).append('\n');
    for (JewelleryItem item : items.getAllItems()) {
      Object[] values = { item.getItemId(), item.getItemName(), item.getSku(), item.getDescription(),
          item.getImageUrl(), item.getCategory(), item.getMaterial(), item.getPurity(), item.getStoneClarity(),
          item.getWeightInGrams(), item.getStoneWeightInGrams(), item.getPricePerGram(), item.getStoneValue(),
          item.getMakingCharges(), item.getVaCharges(), item.getDiscountAmount(), item.getCgstRate(),
          item.getSgstRate(), item.getRoundOff(), item.getStockQuantity(), item.getReorderThreshold() };
      for (int index = 0; index < values.length; index++) {
        if (index > 0)
          csv.append(',');
        csv.append(csvCell(values[index]));
      }
      csv.append('\n');
    }
    return csv.toString();
  }

  private String csvCell(Object value) {
    if (value == null)
      return "";
    String raw = value.toString().replace('\r', ' ').replace('\n', ' ');
    if (!(value instanceof Number) && !raw.isEmpty() && "=+-@".indexOf(raw.charAt(0)) >= 0)
      raw = "'" + raw;
    return "\"" + raw.replace("\"", "\"\"") + "\"";
  }

  public Result importFile(MultipartFile file) {
    if (file == null || file.isEmpty())
      throw new IllegalArgumentException("Choose a CSV file");
    int created = 0, updated = 0;
    List<String> errors = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
      List<String> header = parse(reader.readLine());
      if (!HEADER.equals(String.join(",", header)))
        throw new IllegalArgumentException("CSV headers do not match the downloadable template");
      String line;
      int row = 1;
      while ((line = reader.readLine()) != null) {
        row++;
        if (line.isBlank())
          continue;
        try {
          List<String> v = parse(line);
          if (v.size() != 21)
            throw new IllegalArgumentException("Expected 21 columns");
          JewelleryItem item = item(v);
          Long id = longValue(v.get(0));
          if (id != null) {
            items.updateItem(id, item);
            updated++;
          } else {
            items.addItem(item);
            created++;
          }
        } catch (Exception ex) {
          errors.add("Row " + row + ": " + (ex.getMessage() == null ? "Invalid data" : ex.getMessage()));
        }
      }
    } catch (IOException ex) {
      throw new IllegalArgumentException("Could not read CSV file");
    }
    return new Result(created, updated, errors);
  }

  private JewelleryItem item(List<String> v) {
    JewelleryItem i = new JewelleryItem();
    i.setItemName(v.get(1));
    i.setSku(v.get(2));
    i.setDescription(v.get(3));
    i.setImageUrl(v.get(4));
    i.setCategory(v.get(5));
    i.setMaterial(v.get(6));
    i.setPurity(v.get(7));
    i.setStoneClarity(v.get(8));
    i.setWeightInGrams(number(v.get(9)));
    i.setStoneWeightInGrams(number(v.get(10)));
    i.setPricePerGram(number(v.get(11)));
    i.setStoneValue(number(v.get(12)));
    i.setMakingCharges(number(v.get(13)));
    i.setVaCharges(number(v.get(14)));
    i.setDiscountAmount(number(v.get(15)));
    i.setCgstRate(number(v.get(16)));
    i.setSgstRate(number(v.get(17)));
    i.setRoundOff(number(v.get(18)));
    i.setStockQuantity(integer(v.get(19)));
    i.setReorderThreshold(integer(v.get(20)));
    return i;
  }

  private Double number(String value) {
    return value.isBlank() ? 0d : Double.valueOf(value);
  }

  private Integer integer(String value) {
    return value.isBlank() ? 0 : Integer.valueOf(value);
  }

  private Long longValue(String value) {
    return value.isBlank() ? null : Long.valueOf(value);
  }

  private List<String> parse(String line) {
    if (line == null)
      throw new IllegalArgumentException("CSV is empty");
    List<String> values = new ArrayList<>();
    StringBuilder field = new StringBuilder();
    boolean quoted = false;
    for (int p = 0; p < line.length(); p++) {
      char c = line.charAt(p);
      if (c == '\"') {
        if (quoted && p + 1 < line.length() && line.charAt(p + 1) == '\"') {
          field.append(c);
          p++;
        } else
          quoted = !quoted;
      } else if (c == ',' && !quoted) {
        values.add(field.toString().trim());
        field.setLength(0);
      } else
        field.append(c);
    }
    if (quoted)
      throw new IllegalArgumentException("Unclosed quoted value");
    values.add(field.toString().trim());
    return values;
  }
}
