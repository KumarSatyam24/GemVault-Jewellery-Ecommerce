package com.examly.springapp;

import com.examly.springapp.model.Feedback;
import com.examly.springapp.model.JewelleryItem;
import com.examly.springapp.model.JewelleryOrder;
import com.examly.springapp.model.ProductSearchHistory;
import com.examly.springapp.model.SavedAddress;
import com.examly.springapp.model.User;
import com.examly.springapp.model.WishlistItem;
import com.examly.springapp.repository.SavedAddressRepo;
import com.examly.springapp.repository.ProductSearchHistoryRepo;
import com.examly.springapp.repository.WishlistItemRepo;
import com.examly.springapp.repository.UserRepo;
import com.examly.springapp.repository.JewelleryOrderRepo;
import com.examly.springapp.repository.FeedbackRepo;
import com.examly.springapp.service.BillingCalculator;
import com.examly.springapp.service.FeedbackService;
import com.examly.springapp.service.JewelleryItemService;
import com.examly.springapp.service.JewelleryOrderService;
import com.examly.springapp.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Comparator;

@Component
public class SeedData implements CommandLineRunner {
  private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
  private static final String SATYAM = "sk7474481@gmail.com";
  private static final String RAHUL = "vangavetirahul@gmail.com";
  private static final String HISTORY_KEY = "GemVault demo history v1: ";
  private static final String STARTER_KEY = "GemVault original demo v2: ";
  private static final int[] ORIGINAL_ORDER_DAYS_AGO = { 65, 51, 37, 23, 9, 4 };

  private final UserService users;
  private final JewelleryItemService items;
  private final JewelleryOrderService orders;
  private final FeedbackService feedbacks;
  private final SavedAddressRepo addresses;
  private final WishlistItemRepo wishlists;
  private final ProductSearchHistoryRepo searchHistory;
  private final UserRepo existingUsers;
  private final JewelleryOrderRepo orderRecords;
  private final FeedbackRepo feedbackRecords;

  public SeedData(
      UserService users,
      JewelleryItemService items,
      JewelleryOrderService orders,
      FeedbackService feedbacks,
      SavedAddressRepo addresses,
      WishlistItemRepo wishlists,
      ProductSearchHistoryRepo searchHistory,
      UserRepo existingUsers,
      JewelleryOrderRepo orderRecords,
      FeedbackRepo feedbackRecords) {
    this.users = users;
    this.items = items;
    this.orders = orders;
    this.feedbacks = feedbacks;
    this.addresses = addresses;
    this.wishlists = wishlists;
    this.searchHistory = searchHistory;
    this.existingUsers = existingUsers;
    this.orderRecords = orderRecords;
    this.feedbackRecords = feedbackRecords;
  }

  @Override
  @Transactional
  public void run(String... args) {
    boolean freshDatabase = existingUsers.count() == 0;
    var accounts = seedUsers();
    seedCatalogue();
    upgradeExistingSeedData();
    if (freshDatabase)
      seedStarterOrders(accounts);
    seedOrderHistory(accounts);
    if (freshDatabase) {
      seedBuyerActivity(accounts);
      seedStarterReviews(accounts);
    }
    seedHistoryReviews();
  }

  private Map<String, User> seedUsers() {
    var accounts = new LinkedHashMap<String, User>();
    addAccount(accounts, "Adrij Bhadra", "adrijbhadra@gmail.com", "Admin", "9000000001",
        "Sample residence", "Chandannagar", "West Bengal", "712136");
    addAccount(accounts, "Satyam Kumar", SATYAM, "User", "9000000002",
        "12 Lotus Street", "Chandannagar", "West Bengal", "712136");
    addAccount(accounts, "Rahul Gupta", RAHUL, "User", "9000000003",
        "8 Garden Road", "Hajipur", "Bihar", "844101");
    addAccount(accounts, "Pooja S", "poojasenthilkumar03@gmail.com", "User", "9000000004",
        "14 Sample Garden Street", "Karur", "Tamil Nadu", "639001");
    addAccount(accounts, "Srinitha Mammai", "srinithamammai2706@gmail.com", "User", "9000000005",
        "22 Sample Residency Road", "Hyderabad", "Andhra Pradesh", "500001");
    addAccount(accounts, "Gauri Verma", "vermagaurii2003@gmail.com", "User", "9000000006",
        "9 Sample Park Lane", "Delhi", "Delhi", "110001");
    addAccount(accounts, "Shubham Dhiman", "shubhamkrish966@gmail.com", "Admin", "9000000007",
        "Sample residence (update before delivery)", "Pahadi", "Himachal Pradesh", "000000");
    return accounts;
  }

  private void addAccount(Map<String, User> accounts, String name, String email, String role,
      String mobile, String line, String city, String state, String postalCode) {
    User account = existingUsers.findByEmailIgnoreCase(email).orElseGet(() -> {
      User created = user(name, email, "Admin".equals(role) ? "Admin@1234" : "Buyer@1234", role, mobile);
      address(created, "Home", line, city, state, postalCode);
      if (SATYAM.equals(email))
        address(created, "Office", "24 Market Road", "Delhi", "Delhi", "110001");
      return created;
    });
    accounts.put(email, account);
  }

  private void seedCatalogue() {
    item(
        "Celestial Gold Ring",
        "A luminous gold band frames a brilliant centre stone, finished with fine detail along the shoulders. Designed to catch the light from every angle.",
        "https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=1200&q=85",
        "Ring",
        "Gold",
        "22K / 916",
        5.2,
        6200,
        1800,
        6,
        2);
    item(
        "Moonlit Pearl Necklace",
        "A graceful strand of freshwater pearls, individually chosen for their soft lustre and finished with a discreet sterling silver clasp.",
        "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?auto=format&fit=crop&w=1200&q=85",
        "Necklace",
        "Silver",
        "925 Sterling Silver",
        8.0,
        250,
        2800,
        9,
        3);
    item(
        "Gilded Twin Pendant",
        "Twin gold forms frame a deep blue gemstone. The open setting gives the stone room to glow against the warmth of the chain.",
        "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?auto=format&fit=crop&w=1200&q=85",
        "Pendant",
        "Gold",
        "22K / 916",
        6.2,
        6200,
        2600,
        4,
        2);
    item(
        "Dewdrop Gold Necklace",
        "Fine gold chains sit lightly at the collarbone, ending in a polished dewdrop pendant that moves naturally with the wearer.",
        "https://images.unsplash.com/photo-1611652022419-a9419f74343d?auto=format&fit=crop&w=1200&q=85",
        "Necklace",
        "Gold",
        "18K / 750",
        4.4,
        4800,
        1800,
        2,
        1);
    item(
        "Sapphire Diamond Earrings",
        "A matched pair of sapphires held in sculpted platinum, with diamond accents that lend a quiet glimmer beside the face.",
        "https://images.unsplash.com/photo-1535632066927-ab7c9ab60908?auto=format&fit=crop&w=1200&q=85",
        "Earring",
        "Diamond",
        "950 Platinum",
        3.2,
        8000,
        4500,
        3,
        1);
    item(
        "Rose Sapphire Gold Ring",
        "A rose gold ring set with a blush pink sapphire and pavé diamonds.",
        "https://images.unsplash.com/photo-1603561591411-07134e71a2a9?auto=format&fit=crop&w=1200&q=85",
        "Ring",
        "Gold",
        "18K / 750",
        4.8,
        6200,
        2400,
        0,
        2);
    item(
        "Aurora Gold Ring",
        "A warm 22K gold ring with a softly sculpted band for everyday wear.",
        "https://images.unsplash.com/photo-1605100804763-247f67b3557e?auto=format&fit=crop&w=1200&q=85",
        "Ring",
        "Gold",
        "22K / 916",
        4.6,
        6200,
        2100,
        5,
        2);
    item(
        "Sterling Pearl Necklace",
        "A refined silver and pearl necklace with a delicate sterling clasp.",
        "https://images.unsplash.com/photo-1515562141207-7a88fb7ce338?auto=format&fit=crop&w=1200&q=85",
        "Necklace",
        "Silver",
        "925 Sterling Silver",
        7.4,
        250,
        2400,
        7,
        2);
  }

  private void seedStarterOrders(Map<String, User> accounts) {
    User satyam = accounts.get(SATYAM), rahul = accounts.get(RAHUL);
    order(satyam, piece("Gilded Twin Pendant"), 2, "Delivered", deliveryAddress(satyam), 0);
    order(rahul, piece("Dewdrop Gold Necklace"), 1, "Delivered", deliveryAddress(rahul), 1);
    order(satyam, piece("Celestial Gold Ring"), 1, "Delivered", deliveryAddress(satyam), 2);
    order(rahul, piece("Celestial Gold Ring"), 1, "Delivered", deliveryAddress(rahul), 3);
    order(satyam, piece("Moonlit Pearl Necklace"), 1, "Delivered", deliveryAddress(satyam), 4);
    order(rahul, piece("Moonlit Pearl Necklace"), 1, "Rejected", deliveryAddress(rahul), 5);
  }

  /**
   * Additive demo fixtures: identifiers stay fixed so subsequent startups do not
   * duplicate history.
   */
  private void seedOrderHistory(Map<String, User> accounts) {
    List<User> buyers = accounts.values().stream().filter(u -> "User".equals(u.getUserRole())).toList();
    List<JewelleryItem> catalogue = items.getAllItems().stream().filter(i -> !Boolean.FALSE.equals(i.getActive()))
        .sorted(Comparator.comparing(JewelleryItem::getItemId)).toList();
    if (catalogue.isEmpty() || buyers.isEmpty())
      return;
    LocalDate today = LocalDate.now(INDIA);
    // Insert oldest first: increasing order IDs agree with chronological order in
    // the dashboard.
    for (int i = 59; i >= 0; i--) {
      String marker = HISTORY_KEY + i;
      if (orderRecords.existsBySeedKey(marker))
        continue;
      User buyer = buyers.get(i % buyers.size());
      JewelleryItem piece = catalogue.get((i * 7 + i / 9) % catalogue.size());
      int quantity = i % 11 == 0 ? 2 : 1;
      String status = i % 13 == 5 ? "Cancelled" : i % 17 == 9 ? "Rejected" : "Delivered";
      String delivery = deliveryAddress(buyer);
      JewelleryOrder record = new JewelleryOrder();
      record.setUser(buyer);
      record.setItem(piece);
      record.setQuantity(quantity);
      record.setDeliveryAddress(delivery);
      record.setOrderDate(today.minusDays(i + i / 2));
      record.setSeedKey(marker);
      // Only new, open sample orders reserve live stock. Historical quantities
      // describe earlier sales.
      if (i < 3 && piece.getStockQuantity() >= quantity) {
        record = orders.addOrder(record);
        if (i == 1) {
          JewelleryOrder update = new JewelleryOrder();
          update.setStatus("Approved");
          record = orders.updateOrder(record.getOrderId(), update);
        }
      } else {
        record.setStatus(status);
        if ("Cancelled".equals(status))
          record.setCancellationReason("Buyer changed their plans before dispatch");
        if ("Rejected".equals(status))
          record.setCancellationReason("Selected finish was unavailable");
        var bill = BillingCalculator.forItem(piece);
        record.setBillingSnapshot(bill, quantity);
        record.setInvoiceItemSnapshot(piece);
        record.setTotalPrice(BillingCalculator.round(bill.grandTotal() * quantity));
      }
      orderRecords.save(record);
    }
  }

  private void seedHistoryReviews() {
    LocalDate today = LocalDate.now(INDIA);
    for (JewelleryOrder record : orderRecords.findAll()) {
      String key = record.getSeedKey();
      if (key == null || !key.startsWith(HISTORY_KEY) || !"Delivered".equals(record.getStatus()))
        continue;
      int index = Integer.parseInt(key.substring(HISTORY_KEY.length()));
      if (index % 4 != 0)
        continue;
      LocalDate reviewDate = record.getOrderDate().plusDays(2);
      feedback(record.getUser(), record.getItem(), index % 3 == 0 ? 4 : 5,
          index % 3 == 0 ? "Beautiful finish and comfortable to wear. The packaging could be a little smaller."
              : "The detailing looks lovely in person. It arrived safely and feels well made.",
          reviewDate.isAfter(today) ? today : reviewDate);
    }
  }

  private void seedBuyerActivity(Map<String, User> accounts) {
    User buyerOne = accounts.get(SATYAM), buyerTwo = accounts.get(RAHUL);
    JewelleryItem ring = piece("Celestial Gold Ring"), pearlNecklace = piece("Moonlit Pearl Necklace"),
        twinPendant = piece("Gilded Twin Pendant"), dewdropNecklace = piece("Dewdrop Gold Necklace");
    wishlist(buyerOne, ring);
    wishlist(buyerOne, pearlNecklace);
    wishlist(buyerTwo, twinPendant);
    wishlist(buyerTwo, dewdropNecklace);
    search(buyerOne, "gold ring");
    search(buyerOne, "silver necklace");
    search(buyerTwo, "gold pendant");
  }

  private void seedStarterReviews(Map<String, User> accounts) {
    User buyerOne = accounts.get(SATYAM), buyerTwo = accounts.get(RAHUL);
    JewelleryItem ring = piece("Celestial Gold Ring"), pearlNecklace = piece("Moonlit Pearl Necklace"),
        twinPendant = piece("Gilded Twin Pendant"), dewdropNecklace = piece("Dewdrop Gold Necklace");
    feedback(buyerOne, twinPendant, 5,
        "The blue stone has a beautiful depth of colour, and the pendant feels carefully finished.");
    feedback(buyerTwo, dewdropNecklace, 4, "Light enough for everyday wear. The little pendant moves beautifully.");
    feedback(buyerOne, ring, 5, "Beautifully set and comfortable on the hand. The detail is even finer in person.");
    feedback(buyerTwo, ring, 4, "A lovely ring with a bright centre stone. It arrived exactly as pictured.");
    feedback(buyerOne, pearlNecklace, 5, "The pearls have a soft glow and the silver clasp feels secure.");
  }

  private void upgradeExistingSeedData() {
    updateOriginalSeedPrices();
    spreadOriginalDemoOrders();
    migrateOrderSeedKeys();
  }

  /** Move old internal identifiers out of the customer-facing buyer note. */
  private void migrateOrderSeedKeys() {
    for (JewelleryOrder record : orderRecords.findAll()) {
      String note = record.getComments();
      if (record.getSeedKey() == null && note != null
          && (note.matches("GemVault demo history v1: [0-9]+")
              || note.matches("GemVault original demo v2: [0-9]+"))) {
        record.setSeedKey(note);
        record.setComments(null);
        orderRecords.save(record);
      }
    }
    orderRecords.flush();
  }

  /**
   * Correct only the exact original six-order fixture, never arbitrary customer
   * purchases.
   */
  private void spreadOriginalDemoOrders() {
    var first = orderRecords.findAll().stream().sorted(Comparator.comparing(JewelleryOrder::getOrderId))
        .limit(6).toList();
    if (first.size() != 6)
      return;
    String[] names = { "Gilded Twin Pendant", "Dewdrop Gold Necklace", "Celestial Gold Ring",
        "Celestial Gold Ring", "Moonlit Pearl Necklace", "Moonlit Pearl Necklace" };
    LocalDate originalDate = first.get(0).getOrderDate();
    if (originalDate == null)
      return;
    for (int i = 0; i < first.size(); i++) {
      JewelleryOrder record = first.get(i);
      boolean satyam = i % 2 == 0;
      if (record.getCheckout() != null || record.getComments() != null || record.getSeedKey() != null
          || !originalDate.equals(record.getOrderDate())
          || !names[i].equals(record.getItem().getItemName())
          || !Integer.valueOf(i == 0 ? 2 : 1).equals(record.getQuantity())
          || !(i == 5 ? "Rejected" : "Delivered").equals(record.getStatus())
          || !(satyam ? "sk7474481@gmail.com" : "vangavetirahul@gmail.com")
              .equalsIgnoreCase(record.getUser().getEmail())
          || !(satyam ? "12 Lotus Street, Coimbatore" : "8 Garden Road, Chennai").equals(record.getDeliveryAddress()))
        return;
    }
    for (int i = 0; i < first.size(); i++) {
      JewelleryOrder record = first.get(i);
      record.setOrderDate(originalDate.minusDays(ORIGINAL_ORDER_DAYS_AGO[i]));
      record.setSeedKey(STARTER_KEY + i);
      orderRecords.save(record);
    }
  }

  private void updateOriginalSeedPrices() {
    for (JewelleryItem piece : items.getAllItems()) {
      boolean changed = false;
      if ("Gilded Twin Pendant".equals(piece.getItemName()) && Double.valueOf(8.5).equals(piece.getWeightInGrams())) {
        piece.setWeightInGrams(6.2);
        changed = true;
      }
      if ("Sapphire Diamond Earrings".equals(piece.getItemName())
          && Double.valueOf(42000).equals(piece.getStoneValue())) {
        piece.setStoneValue(22000.0);
        changed = true;
      }
      if (changed)
        items.updateItem(piece.getItemId(), piece);
    }
  }

  private JewelleryItem piece(String name) {
    return items.getAllItems().stream().filter(item -> name.equals(item.getItemName())).findFirst().orElseThrow();
  }

  private String deliveryAddress(User buyer) {
    return addresses.findByUserUserIdOrderByAddressIdAsc(buyer.getUserId()).stream().findFirst()
        .map(a -> a.getLine1() + ", " + a.getCity() + ", " + a.getState() + " " + a.getPostalCode())
        .orElse("Sample delivery address; update before delivery");
  }

  private User user(String name, String email, String password, String role, String mobile) {
    User user = new User();
    user.setUsername(name);
    user.setEmail(email);
    user.setPassword(password);
    user.setUserRole(role);
    user.setMobileNumber(mobile);
    user.setEmailVerified(true);
    user.setPasswordResetRequired(false);
    return users.createUser(user);
  }

  private JewelleryItem item(
      String name,
      String description,
      String imageUrl,
      String category,
      String material,
      String purity,
      double weight,
      double pricePerGram,
      double makingCharges,
      int stock,
      int threshold) {
    var existing = items.getAllItems().stream().filter(i -> name.equalsIgnoreCase(i.getItemName())).findFirst();
    if (existing.isPresent())
      return existing.get();
    JewelleryItem item = new JewelleryItem();
    item.setItemName(name);
    item.setDescription(description);
    item.setImageUrl(imageUrl);
    item.setCategory(category);
    item.setMaterial(material);
    item.setPurity(purity);
    item.setWeightInGrams(weight);
    item.setPricePerGram(pricePerGram);
    item.setMakingCharges(makingCharges);
    item.setStoneWeightInGrams(0.0);
    item.setStoneValue(0.0);
    item.setVaCharges(Math.round(makingCharges * 0.35 * 100.0) / 100.0);
    item.setDiscountAmount(0.0);
    item.setCgstRate(1.5);
    item.setSgstRate(1.5);
    item.setRoundOff(0.0);
    if ("Diamond".equals(material)) {
      item.setStoneClarity("VVS1");
      item.setStoneWeightInGrams(0.6);
      item.setStoneValue(22000.0);
      item.setVaCharges(1000.0);
    }
    item.setStockQuantity(stock);
    item.setReorderThreshold(threshold);
    return items.addItem(item);
  }

  private void order(User buyer, JewelleryItem item, int quantity, String status, String address, int seedIndex) {
    JewelleryOrder order = new JewelleryOrder();
    order.setUserId(buyer.getUserId());
    order.setItemId(item.getItemId());
    order.setQuantity(quantity);
    order.setDeliveryAddress(address);
    order.setOrderDate(LocalDate.now(INDIA).minusDays(ORIGINAL_ORDER_DAYS_AGO[seedIndex]));
    order.setSeedKey(STARTER_KEY + seedIndex);
    JewelleryOrder saved = orders.addOrder(order);
    if (!"Pending".equals(status)) {
      JewelleryOrder update = new JewelleryOrder();
      update.setStatus(status);
      orders.updateOrder(saved.getOrderId(), update);
    }
  }

  private void feedback(User buyer, JewelleryItem item, int rating, String text) {
    feedback(buyer, item, rating, text, LocalDate.now(INDIA));
  }

  private void feedback(User buyer, JewelleryItem item, int rating, String text, LocalDate date) {
    if (feedbackRecords.existsByUserUserIdAndItemItemId(buyer.getUserId(), item.getItemId()))
      return;
    Feedback feedback = new Feedback();
    feedback.setUserId(buyer.getUserId());
    feedback.setItemId(item.getItemId());
    feedback.setFeedbackText(text);
    feedback.setRating(rating);
    feedback.setDate(date);
    feedbacks.createFeedback(feedback);
  }

  private void wishlist(User buyer, JewelleryItem item) {
    WishlistItem wishlistItem = new WishlistItem();
    wishlistItem.setUser(buyer);
    wishlistItem.setItem(item);
    wishlists.save(wishlistItem);
  }

  private void search(User buyer, String term) {
    ProductSearchHistory entry = new ProductSearchHistory();
    entry.setUser(buyer);
    entry.setSearchTerm(term);
    searchHistory.save(entry);
  }

  private void address(
      User user, String label, String line, String city, String state, String postalCode) {
    SavedAddress address = new SavedAddress();
    address.setUser(user);
    address.setLabel(label);
    address.setLine1(line);
    address.setCity(city);
    address.setState(state);
    address.setPostalCode(postalCode);
    addresses.save(address);
  }
}
