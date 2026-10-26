package com.examly.springapp.service;

import com.examly.springapp.model.User;
import com.examly.springapp.repository.UserRepo;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Service
public class LowStockAlertService {
  private final UserRepo users;
  private final ResendEmailService email;

  public LowStockAlertService(UserRepo users, ResendEmailService email) {
    this.users = users;
    this.email = email;
  }

  @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
  public void sendLowStockAlert(LowStockAlertEvent event) {
    if (event.items().isEmpty()) return;
    StringBuilder rows = new StringBuilder();
    for (LowStockAlertEvent.LowStockAlert item : event.items()) {
      rows.append("<li><strong>").append(escape(item.itemName())).append("</strong> (SKU ")
          .append(escape(item.sku())).append("): ").append(item.stockQuantity())
          .append(" remaining; reorder threshold ").append(item.reorderThreshold()).append(".</li>");
    }
    String html = "<p>The following GemVault pieces need attention:</p><ul>" + rows
        + "</ul><p>Open Inventory to restock or update availability.</p>";
    List<User> admins = users.findByUserRole("Admin");
    for (User admin : admins) {
      if (admin.isEmailVerified()) email.send(admin.getEmail(), "GemVault low-stock alert", html);
    }
  }

  private String escape(String value) {
    return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
  }
}
