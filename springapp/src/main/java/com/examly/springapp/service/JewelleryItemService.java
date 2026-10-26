package com.examly.springapp.service;

import com.examly.springapp.model.JewelleryItem;
import java.util.*;

public interface JewelleryItemService {
  JewelleryItem addItem(JewelleryItem item);

  Optional<JewelleryItem> getItemById(Long itemId);

  List<JewelleryItem> getAllItems();

  JewelleryItem updateItem(Long itemId, JewelleryItem updatedItem);

  JewelleryItem deleteItem(Long itemId);

  JewelleryItem archiveItem(Long itemId);

  JewelleryItem restoreItem(Long itemId);

  JewelleryItem restockItem(Long itemId, int quantity);

  List<JewelleryItem> getLowStockItems();
}
