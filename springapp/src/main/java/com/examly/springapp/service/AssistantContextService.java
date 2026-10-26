package com.examly.springapp.service;

import com.examly.springapp.config.UserPrinciple;
import com.examly.springapp.model.JewelleryItem;
import com.examly.springapp.model.JewelleryOrder;
import com.examly.springapp.model.WishlistItem;
import com.examly.springapp.repository.FeedbackRepo;
import com.examly.springapp.repository.JewelleryItemRepo;
import com.examly.springapp.repository.JewelleryOrderRepo;
import com.examly.springapp.repository.ProductSearchHistoryRepo;
import com.examly.springapp.repository.SavedAddressRepo;
import com.examly.springapp.repository.WishlistItemRepo;
import com.fasterxml.jackson.databind.JsonNode;
import java.text.NumberFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * Read-only, server-authorized facts for questions that a static FAQ cannot
 * answer.
 */
@Service
public class AssistantContextService {
  private static final Pattern AMOUNT = Pattern.compile(
      "(?:under|below|less than|up to)\\s*(?:rs\\.?|inr|₹)?\\s*([\\d,]+(?:\\.\\d+)?)(k)?",
      Pattern.CASE_INSENSITIVE);
  private static final Pattern ORDER_ID = Pattern.compile("(?:order\\s*#?\\s*)(\\d+)", Pattern.CASE_INSENSITIVE);
  private static final Set<String> FILLER = Set.of(
      "a", "an", "and", "are", "available", "below", "can", "cost", "do", "for", "have",
      "how", "i", "in", "is", "item", "jewellery", "less", "me", "my", "of", "or", "piece",
      "please", "price", "product", "recommend", "show", "stock", "suggest", "than", "the",
      "to", "under", "what", "with", "you");

  private final JewelleryItemRepo items;
  private final JewelleryOrderRepo orders;
  private final WishlistItemRepo wishlists;
  private final SavedAddressRepo addresses;
  private final ProductSearchHistoryRepo searches;
  private final FeedbackRepo feedbacks;
  private final MetalRateService rates;

  public AssistantContextService(JewelleryItemRepo items, JewelleryOrderRepo orders,
      WishlistItemRepo wishlists, SavedAddressRepo addresses, ProductSearchHistoryRepo searches,
      FeedbackRepo feedbacks, MetalRateService rates) {
    this.items = items;
    this.orders = orders;
    this.wishlists = wishlists;
    this.addresses = addresses;
    this.searches = searches;
    this.feedbacks = feedbacks;
    this.rates = rates;
  }

  public record Answer(String text, String topic) {
  }

  /** The model supplies filters, never an account identifier. */
  public Map<String, Object> execute(String tool, JsonNode args, UserPrinciple user) {
    return switch (tool) {
      case "search_products" -> Map.of("results", searchProducts(args));
      case "get_product_details" -> Map.of("results", productDetails(args));
      case "get_metal_rate" -> rateFacts(args);
      case "get_my_orders" -> orderFacts(args, user);
      case "get_my_wishlist" -> wishlistFacts(user);
      case "get_my_addresses" -> addressFacts(user);
      case "get_recommendations" -> recommendationFacts(args, user);
      case "get_low_stock_products" -> lowStockFacts(user);
      default -> Map.of("error", "Unknown tool");
    };
  }

  private List<Map<String, Object>> searchProducts(JsonNode args) {
    String query = arg(args, "query");
    String category = categoryArg(args);
    String material = arg(args, "material");
    String purity = arg(args, "purity");
    double ceiling = args.path("max_price").asDouble(Double.MAX_VALUE);
    int limit = Math.max(1, Math.min(5, args.path("limit").asInt(3)));
    Set<String> terms = tokens(query);
    return items.findAll().stream().filter(this::canShow)
        .filter(item -> item.getTotalPrice() != null && item.getTotalPrice() <= ceiling)
        .filter(item -> category.isBlank() || item.getCategory().equalsIgnoreCase(category))
        .filter(item -> material.isBlank() || item.getMaterial().equalsIgnoreCase(material))
        .filter(item -> purity.isBlank() || item.getPurity() != null && item.getPurity().equalsIgnoreCase(purity))
        .filter(item -> terms.isEmpty() || score(item, terms) > 0)
        .sorted(Comparator.<JewelleryItem>comparingInt(item -> score(item, terms)).reversed()
            .thenComparing(JewelleryItem::getItemId))
        .limit(limit).map(this::productFacts).toList();
  }

  private List<Map<String, Object>> productDetails(JsonNode args) {
    long id = args.path("id").asLong(0);
    if (id > 0)
      return items.findById(id).stream().filter(this::canShow)
          .map(this::productFacts).toList();
    return searchProducts(args);
  }

  private Map<String, Object> productFacts(JewelleryItem item) {
    Object[] rating = feedbacks.ratingForItem(item.getItemId()).get(0);
    return Map.of("id", item.getItemId(), "name", item.getItemName(),
        "category", item.getCategory(), "material", item.getMaterial(),
        "purity", item.getPurity() == null ? "" : item.getPurity(),
        "description", item.getDescription() == null ? "" : item.getDescription(),
        "price_inr", item.getTotalPrice() == null ? 0 : item.getTotalPrice(),
        "stock", item.getStockQuantity() == null ? 0 : item.getStockQuantity(),
        "rating", rating[0] == null ? "unrated" : rating[0].toString());
  }

  private Map<String, Object> rateFacts(JsonNode args) {
    String material = arg(args, "material").equalsIgnoreCase("Silver") ? "Silver" : "Gold";
    String purity = arg(args, "purity");
    if (purity.isBlank())
      purity = material.equals("Gold") ? "22K / 916" : "999";
    try {
      var quote = rates.quote(material, purity);
      return Map.of("material", material, "purity", purity, "rate_per_gram_inr",
          quote.ratePerGram(), "source", quote.source() == null ? "" : quote.source());
    } catch (RuntimeException unavailable) {
      return Map.of("error", "Live rate unavailable");
    }
  }

  private Map<String, Object> orderFacts(JsonNode args, UserPrinciple user) {
    if (!isBuyer(user))
      return Map.of("error", "Buyer sign-in required");
    long requested = args.path("order_id").asLong(0);
    var own = orders.findByUserUserId(user.getUserId()).stream()
        .filter(order -> requested <= 0 || requested == order.getOrderId())
        .sorted(Comparator.comparing(JewelleryOrder::getOrderId).reversed()).limit(5)
        .map(order -> Map.<String, Object>of("id", order.getOrderId(),
            "product", order.getItem().getItemName(), "status", order.getStatus(),
            "date", order.getOrderDate() == null ? "" : order.getOrderDate().toString(),
            "quantity", order.getQuantity(), "total_inr",
            order.getTotalPrice() == null ? 0 : order.getTotalPrice()))
        .toList();
    return Map.of("orders", own);
  }

  private Map<String, Object> wishlistFacts(UserPrinciple user) {
    if (!isBuyer(user))
      return Map.of("error", "Buyer sign-in required");
    return Map.of("products", wishlists.findByUserUserId(user.getUserId()).stream()
        .limit(5).map(entry -> productFacts(entry.getItem())).toList());
  }

  private Map<String, Object> addressFacts(UserPrinciple user) {
    if (!isBuyer(user))
      return Map.of("error", "Buyer sign-in required");
    return Map.of("addresses", addresses.findByUserUserIdOrderByAddressIdAsc(user.getUserId())
        .stream().limit(5).map(address -> Map.of("label", address.getLabel(),
            "address", address.formatted()))
        .toList());
  }

  private Map<String, Object> recommendationFacts(JsonNode args, UserPrinciple user) {
    if (!isBuyer(user))
      return Map.of("error", "Buyer sign-in required");
    Set<Long> savedIds = new HashSet<>();
    Set<String> categories = new HashSet<>();
    wishlists.findByUserUserId(user.getUserId()).forEach(entry -> {
      savedIds.add(entry.getItem().getItemId());
      categories.add(entry.getItem().getCategory().toLowerCase(Locale.ROOT));
    });
    Set<String> terms = new HashSet<>();
    searches.findTop10ByUserUserIdOrderBySearchedAtDesc(user.getUserId())
        .forEach(entry -> terms.addAll(tokens(entry.getSearchTerm())));
    String category = categoryArg(args);
    var result = items.findAll().stream().filter(this::canShow)
        .filter(item -> !savedIds.contains(item.getItemId()))
        .filter(item -> category.isBlank() || item.getCategory().equalsIgnoreCase(category))
        .filter(item -> categories.isEmpty() && terms.isEmpty()
            || recommendationScore(item, categories, terms) > 0)
        .sorted(Comparator.<JewelleryItem>comparingInt(
            item -> recommendationScore(item, categories, terms)).reversed()
            .thenComparing(JewelleryItem::getItemId))
        .limit(3).map(this::productFacts).toList();
    return Map.of("products", result, "basis", "buyer's wishlist and recent searches");
  }

  private Map<String, Object> lowStockFacts(UserPrinciple user) {
    if (!isAdmin(user))
      return Map.of("error", "Administrator access required");
    return Map.of("products", items.findLowStockItems().stream().limit(10)
        .map(item -> Map.of("id", item.getItemId(), "name", item.getItemName(),
            "stock", item.getStockQuantity(), "reorder_threshold", item.getReorderThreshold()))
        .toList());
  }

  private String arg(JsonNode args, String name) {
    String value = args.path(name).asText("").trim();
    return value.length() > 100 ? value.substring(0, 100) : value;
  }

  private String categoryArg(JsonNode args) {
    String category = arg(args, "category").toLowerCase(Locale.ROOT);
    if (category.endsWith("s"))
      category = category.substring(0, category.length() - 1);
    return category;
  }

  public Answer answer(String question, UserPrinciple user) {
    String q = question.toLowerCase(Locale.ROOT).trim();
    boolean howTo = q.startsWith("how ") || q.startsWith("can i ") || q.startsWith("why ");

    if (!howTo && (q.contains("rate") || q.contains("price per gram")
        || ((q.contains("today") || q.contains("current")) && q.contains("price")))
        && (q.contains("gold") || q.contains("silver"))
        && !q.startsWith("where "))
      return liveRate(q);
    if (!howTo && q.contains("order") && (q.contains("my ") || q.contains("status")
        || q.contains("track") || q.contains("where is") || ORDER_ID.matcher(q).find()))
      return myOrders(q, user);
    if (!howTo && (q.contains("my wishlist") || q.contains("saved piece")
        || q.contains("saved product") || q.contains("what's in my wishlist")))
      return myWishlist(user);
    if (!howTo && (q.contains("my address") || q.contains("saved address")))
      return myAddresses(user);
    if (!howTo && (q.contains("recommend") || q.contains("suggest")
        || q.contains("picked for me")))
      return recommend(q, user);
    if (!howTo && (q.contains("low stock") || q.contains("need restock")))
      return isAdmin(user) ? lowStock()
          : new Answer("Sign in as an administrator to check which products need restocking.", "Low stock");
    if (!howTo && isProductQuestion(q))
      return products(q);
    return null;
  }

  private Answer liveRate(String q) {
    String material = q.contains("silver") ? "Silver" : "Gold";
    String purity = material.equals("Silver") ? "999" : "22K / 916";
    Matcher karat = Pattern.compile("\\b(18|22|23|24)\\s*k\\b").matcher(q);
    if (karat.find() && material.equals("Gold"))
      purity = karat.group(1) + "K";
    else if (q.contains("925") && material.equals("Silver"))
      purity = "925";
    try {
      MetalRateService.Quote quote = rates.quote(material, purity);
      return new Answer("The current " + material.toLowerCase(Locale.ROOT) + " rate for " + purity
          + " is " + money(quote.ratePerGram()) + " per gram. Source: " + quote.source()
          + ". Product prices also include making charges, stones and taxes.", "Live rate");
    } catch (RuntimeException unavailable) {
      return new Answer("I can't fetch the live " + material.toLowerCase(Locale.ROOT)
          + " rate right now. Please try the Market Rate control again shortly.", "Live rate");
    }
  }

  private Answer myOrders(String q, UserPrinciple user) {
    if (!isBuyer(user))
      return buyerSignIn("orders");
    List<JewelleryOrder> own = orders.findByUserUserId(user.getUserId());
    Matcher id = ORDER_ID.matcher(q);
    if (id.find()) {
      try {
        long requested = Long.parseLong(id.group(1));
        own = own.stream().filter(order -> requested == order.getOrderId()).toList();
      } catch (NumberFormatException invalid) {
        return new Answer("That order number is too long. Check it in My Orders.", "My orders");
      }
    }
    if (own.isEmpty())
      return new Answer(id.reset().find()
          ? "I couldn't find that order in your account. Check the order number in My Orders."
          : "You don't have any orders yet. Browse the collection to find your first piece.", "My orders");
    StringBuilder reply = new StringBuilder("Here are your recent orders:\n");
    own.stream().sorted(Comparator.comparing(JewelleryOrder::getOrderId).reversed())
        .limit(5).forEach(order -> reply.append("• #").append(order.getOrderId()).append(" — ")
            .append(order.getItem().getItemName()).append(" · ").append(order.getStatus())
            .append(" · ").append(order.getOrderDate()).append("\n"));
    return new Answer(reply.append("Open My Orders for the full details or invoice.").toString(), "My orders");
  }

  private Answer myWishlist(UserPrinciple user) {
    if (!isBuyer(user))
      return buyerSignIn("wishlist");
    List<WishlistItem> saved = wishlists.findByUserUserId(user.getUserId());
    if (saved.isEmpty())
      return new Answer("Your wishlist is empty. Save a piece from the collection to see it here.", "My wishlist");
    StringBuilder reply = new StringBuilder("Your saved pieces:\n");
    saved.stream().limit(5).forEach(entry -> reply.append("• ")
        .append(entry.getItem().getItemName()).append(" — ")
        .append(money(entry.getItem().getTotalPrice())).append("\n"));
    return new Answer(reply.append("Open My Wishlist to view them all.").toString(), "My wishlist");
  }

  private Answer myAddresses(UserPrinciple user) {
    if (!isBuyer(user))
      return buyerSignIn("saved addresses");
    var saved = addresses.findByUserUserIdOrderByAddressIdAsc(user.getUserId());
    if (saved.isEmpty())
      return new Answer("You don't have any saved addresses yet. Add one in Account.", "My addresses");
    StringBuilder reply = new StringBuilder("Your saved addresses:\n");
    saved.stream().limit(5).forEach(address -> reply.append("• ").append(address.getLabel())
        .append(": ").append(address.formatted()).append("\n"));
    return new Answer(reply.append("You can edit them in Account.").toString(), "My addresses");
  }

  private Answer recommend(String q, UserPrinciple user) {
    if (!isBuyer(user))
      return buyerSignIn("personal recommendations");
    List<WishlistItem> saved = wishlists.findByUserUserId(user.getUserId());
    Set<Long> savedIds = new HashSet<>();
    Set<String> categories = new HashSet<>();
    saved.forEach(entry -> {
      savedIds.add(entry.getItem().getItemId());
      categories.add(entry.getItem().getCategory().toLowerCase(Locale.ROOT));
    });
    Set<String> searchTerms = new HashSet<>();
    searches.findTop10ByUserUserIdOrderBySearchedAtDesc(user.getUserId())
        .forEach(entry -> searchTerms.addAll(tokens(entry.getSearchTerm())));
    List<JewelleryItem> candidates = items.findAll().stream()
        .filter(this::canShow).filter(item -> !savedIds.contains(item.getItemId()))
        .filter(item -> requestedCategory(q) == null
            || item.getCategory().equalsIgnoreCase(requestedCategory(q)))
        .filter(item -> requestedMaterial(q) == null
            || item.getMaterial().equalsIgnoreCase(requestedMaterial(q)))
        .sorted(Comparator.<JewelleryItem>comparingInt(item -> recommendationScore(item, categories, searchTerms))
            .reversed()
            .thenComparing(JewelleryItem::getItemId))
        .toList();
    if (!saved.isEmpty() || !searchTerms.isEmpty())
      candidates = candidates.stream().filter(item -> recommendationScore(item, categories, searchTerms) > 0).toList();
    return productAnswer(candidates.stream().limit(3).toList(),
        "Based on your saved pieces and searches, you may like:",
        "I don't have a close match for your saved pieces and searches right now.");
  }

  private int recommendationScore(JewelleryItem item, Set<String> categories, Set<String> terms) {
    int score = categories.contains(item.getCategory().toLowerCase(Locale.ROOT)) ? 5 : 0;
    String text = searchable(item);
    for (String term : terms)
      if (text.contains(term))
        score += 2;
    return score;
  }

  private Answer lowStock() {
    List<JewelleryItem> low = items.findLowStockItems();
    if (low.isEmpty())
      return new Answer("No products are at or below their reorder threshold right now.", "Low stock");
    StringBuilder reply = new StringBuilder("Products needing restock:\n");
    low.stream().limit(5).forEach(item -> reply.append("• ").append(item.getItemName())
        .append(" — ").append(item.getStockQuantity()).append(" in stock; threshold ")
        .append(item.getReorderThreshold()).append("\n"));
    return new Answer(reply.append("Open Restock to add stock.").toString(), "Low stock");
  }

  private Answer products(String q) {
    Set<String> terms = tokens(q);
    double ceiling = maxPrice(q);
    String category = requestedCategory(q);
    String material = requestedMaterial(q);
    String purity = requestedPurity(q);
    List<JewelleryItem> matches = items.findAll().stream().filter(this::canShow)
        .filter(item -> item.getTotalPrice() != null && item.getTotalPrice() <= ceiling)
        .filter(item -> category == null || item.getCategory().equalsIgnoreCase(category))
        .filter(item -> material == null || item.getMaterial().equalsIgnoreCase(material))
        .filter(item -> purity == null || item.getPurity() != null
            && item.getPurity().toLowerCase(Locale.ROOT).contains(purity))
        .filter(item -> terms.isEmpty() || score(item, terms) > 0)
        .sorted(Comparator.<JewelleryItem>comparingInt(item -> score(item, terms)).reversed()
            .thenComparing(JewelleryItem::getItemId))
        .limit(3).toList();
    if (matches.isEmpty()) {
      var unavailable = items.findAll().stream().filter(item -> Boolean.TRUE.equals(item.getActive()))
          .filter(item -> item.getStockQuantity() != null && item.getStockQuantity() == 0)
          .filter(item -> score(item, terms) >= 10).findFirst();
      if (unavailable.isPresent())
        return new Answer(unavailable.get().getItemName()
            + " is currently out of stock. You can browse similar available pieces in Jewellery.", "Products");
    }
    return productAnswer(matches, "Here are the closest pieces in the collection:",
        "I couldn't find a matching available piece. Try another material, category or price range.");
  }

  private Answer productAnswer(List<JewelleryItem> products, String heading, String empty) {
    if (products.isEmpty())
      return new Answer(empty, "Products");
    StringBuilder reply = new StringBuilder(heading).append("\n");
    for (JewelleryItem item : products) {
      Object[] rating = feedbacks.ratingForItem(item.getItemId()).get(0);
      reply.append("• ").append(item.getItemName()).append(" — ")
          .append(money(item.getTotalPrice())).append(" · ")
          .append(item.getStockQuantity()).append(" available");
      if ((Long) rating[1] > 0)
        reply.append(" · ")
            .append(String.format(Locale.ROOT, "%.1f", (Double) rating[0])).append("/5");
      reply.append("\n").append(item.getDescription()).append("\n");
    }
    return new Answer(reply.append("Open a piece in the collection for its full details.").toString(), "Products");
  }

  private boolean isProductQuestion(String q) {
    return q.contains("ring") || q.contains("necklace") || q.contains("pendant")
        || q.contains("earring") || q.contains("bracelet") || q.contains("bangle")
        || q.contains("product") || q.contains("jewellery") || q.contains("piece")
        || q.contains("in stock") || q.contains("available") || q.contains("how much");
  }

  private String requestedCategory(String q) {
    if (q.matches(".*\\brings?\\b.*"))
      return "Ring";
    if (q.matches(".*\\bnecklaces?\\b.*"))
      return "Necklace";
    if (q.matches(".*\\bpendants?\\b.*"))
      return "Pendant";
    if (q.matches(".*\\bearrings?\\b.*"))
      return "Earring";
    if (q.matches(".*\\bbangles?\\b.*"))
      return "Bangle";
    return null;
  }

  private String requestedMaterial(String q) {
    if (q.matches(".*\\bgold\\b.*"))
      return "Gold";
    if (q.matches(".*\\bsilver\\b.*"))
      return "Silver";
    if (q.matches(".*\\bplatinum\\b.*"))
      return "Platinum";
    return null;
  }

  private String requestedPurity(String q) {
    Matcher karat = Pattern.compile("\\b(18|22|23|24)\\s*k\\b").matcher(q);
    return karat.find() ? karat.group(1) + "k" : null;
  }

  private boolean canShow(JewelleryItem item) {
    return Boolean.TRUE.equals(item.getActive()) && item.getStockQuantity() != null
        && item.getStockQuantity() > 0;
  }

  private int score(JewelleryItem item, Set<String> terms) {
    String name = item.getItemName().toLowerCase(Locale.ROOT);
    String category = item.getCategory().toLowerCase(Locale.ROOT);
    String material = item.getMaterial().toLowerCase(Locale.ROOT);
    String description = item.getDescription() == null ? "" : item.getDescription().toLowerCase(Locale.ROOT);
    int score = 0;
    for (String term : terms) {
      if (name.contains(term))
        score += 5;
      else if (category.contains(term) || material.contains(term))
        score += 4;
      else if (description.contains(term))
        score += 1;
    }
    return score;
  }

  private String searchable(JewelleryItem item) {
    return (item.getItemName() + " " + item.getCategory() + " " + item.getMaterial())
        .toLowerCase(Locale.ROOT);
  }

  private Set<String> tokens(String text) {
    Set<String> result = new HashSet<>();
    Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
        .filter(word -> word.length() > 2 && !FILLER.contains(word) && !word.matches("\\d+"))
        .map(word -> word.length() > 4 && word.endsWith("s") ? word.substring(0, word.length() - 1) : word)
        .forEach(result::add);
    return result;
  }

  private double maxPrice(String q) {
    Matcher match = AMOUNT.matcher(q);
    if (!match.find())
      return Double.MAX_VALUE;
    try {
      double amount = Double.parseDouble(match.group(1).replace(",", ""));
      return match.group(2) == null ? amount : amount * 1000;
    } catch (NumberFormatException ignored) {
      return Double.MAX_VALUE;
    }
  }

  private boolean isBuyer(UserPrinciple user) {
    return user != null && user.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("ROLE_User"));
  }

  private boolean isAdmin(UserPrinciple user) {
    return user != null && user.getAuthorities().stream()
        .anyMatch(authority -> authority.getAuthority().equals("ROLE_Admin"));
  }

  private Answer buyerSignIn(String subject) {
    return new Answer("Sign in as a buyer and ask me again to see your " + subject + ".", "Account");
  }

  private String money(Double amount) {
    return NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        .format(amount == null ? 0 : amount);
  }
}
