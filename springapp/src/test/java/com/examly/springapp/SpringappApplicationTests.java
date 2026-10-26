package com.examly.springapp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * GemVault functional test suite (JWT + role-based access), modeled on the
 * Triptale/GridPulse/TurfMate-style test pattern: register/login for both
 * roles first, then exercise each endpoint's role gate as defined in
 * SecurityConfig, plus the signature Low-Stock Reorder Alerts feature and
 * its underlying stock-reservation business logic.
 *
 * ASSUMPTION TO VERIFY: same SecurityConfig/JwtAuthenticationEntryPoint
 * setup as the prior projects (no custom AccessDeniedHandler, entry point
 * hardcoded to 401) — this suite assumes 401 for every role-denied call
 * below. Please run this once and share the actual captured status codes
 * for any failing *_RoleDenied assertions so I can correct them.
 *
 * NOTE: SecurityConfig also lists PUT /api/feedback/{feedbackId} as
 * hasAnyRole("Admin","User"), but FeedbackController has no PUT mapping at
 * all — the same dead rule seen in GridPulse and TurfMate. No test written
 * for it.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SpringappApplicationTests {

        private String adminToken;
        private String userToken;
        private int buyerUserId;
        private Long itemId;
        private Long orderId;
        private Long feedbackId;

        private static final String ITEM_NAME = "Meridian Gold Bangle";

        @Autowired
        private TestRestTemplate restTemplate;

        @Autowired
        private ObjectMapper objectMapper;

        private HttpHeaders createHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                return headers;
        }

        private HttpHeaders createAuthHeaders(String token) {
                HttpHeaders headers = createHeaders();
                headers.set("Authorization", "Bearer " + token);
                return headers;
        }

        // ---------------------------------------------------------------
        // Auth
        // ---------------------------------------------------------------

        @Test
        @Order(1)
        void backend_testRegisterAdmin() {
                String requestBody = "{\"email\": \"catalog.admin@gemvault.com\", \"password\": \"Admin@1234\", "
                                + "\"username\": \"catalogAdmin\", \"userRole\": \"Admin\", \"mobileNumber\": \"9876543210\"}";
                ResponseEntity<String> response = restTemplate.postForEntity("/api/register",
                                new HttpEntity<>(requestBody, createHeaders()), String.class);

                Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }

        @Test
        @Order(2)
        void backend_testRegisterUser() {
                String requestBody = "{\"email\": \"buyer.one@gemvault.com\", \"password\": \"User@1234\", "
                                + "\"username\": \"buyerOne\", \"userRole\": \"User\", \"mobileNumber\": \"1122334455\"}";
                ResponseEntity<String> response = restTemplate.postForEntity("/api/register",
                                new HttpEntity<>(requestBody, createHeaders()), String.class);

                Assertions.assertEquals(HttpStatus.CREATED, response.getStatusCode());
        }

        @Test
        @Order(3)
        void backend_testLoginAdmin() throws Exception {
                String requestBody = "{\"email\": \"catalog.admin@gemvault.com\", \"password\": \"Admin@1234\"}";

                ResponseEntity<String> response = restTemplate.postForEntity("/api/login",
                                new HttpEntity<>(requestBody, createHeaders()), String.class);

                Assertions.assertNotNull(response.getBody(), "Response body is null!");
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                adminToken = responseBody.get("token").asText();

                Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
                Assertions.assertNotNull(adminToken);
        }

        @Test
        @Order(4)
        void backend_testLoginUser() throws Exception {
                String requestBody = "{\"email\": \"buyer.one@gemvault.com\", \"password\": \"User@1234\"}";

                ResponseEntity<String> response = restTemplate.postForEntity("/api/login",
                                new HttpEntity<>(requestBody, createHeaders()), String.class);

                Assertions.assertNotNull(response.getBody(), "Response body is null!");
                JsonNode responseBody = objectMapper.readTree(response.getBody());
                userToken = responseBody.get("token").asText();
                buyerUserId = responseBody.get("userId").asInt();

                Assertions.assertEquals(HttpStatus.OK, response.getStatusCode());
                Assertions.assertNotNull(userToken);
        }

        // ---------------------------------------------------------------
        // Jewellery Item (+ Low-Stock Reorder Alerts)
        // ---------------------------------------------------------------

        @Test
        @Order(5)
        void backend_testAddItem_AdminOnlyAccess() throws Exception {
                Assertions.assertNotNull(adminToken, "Admin token should not be null");
                Assertions.assertNotNull(userToken, "User token should not be null");

                // stockQuantity 5, reorderThreshold 3 — deliberately close so a single
                // 3-unit order (Order(10)) pushes it onto the Low-Stock Reorder Alerts list.
                String requestBody = "{"
                                + "\"itemName\": \"" + ITEM_NAME + "\","
                                + "\"description\": \"22K gold bangle with a floral engraved pattern.\","
                                + "\"category\": \"Bangle\","
                                + "\"material\": \"Gold\","
                                + "\"purity\": \"22K\","
                                + "\"weightInGrams\": 15.5,"
                                + "\"makingCharges\": 1500,"
                                + "\"pricePerGram\": 6000,"
                                + "\"stockQuantity\": 5,"
                                + "\"reorderThreshold\": 3"
                                + "}";

                // Admin should be able to add an item
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<String> adminRequest = new HttpEntity<>(requestBody, adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/jewellery-item", HttpMethod.POST, adminRequest, String.class);

                System.out.println("Admin POST Item Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.CREATED, adminResponse.getStatusCode(), "Admin should be able to add an item");

                JsonNode itemNode = objectMapper.readTree(adminResponse.getBody());
                itemId = itemNode.get("itemId").asLong();
                Assertions.assertEquals(94500.0, itemNode.get("totalPrice").asDouble(), 0.01,
                                "totalPrice should be weightInGrams * pricePerGram + makingCharges");

                // User should NOT be able to add an item
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<String> userRequest = new HttpEntity<>(requestBody, userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/jewellery-item", HttpMethod.POST, userRequest, String.class);

                System.out.println("User POST Item Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, userResponse.getStatusCode(), "User should NOT be allowed to add an item");
        }

        @Test
        @Order(6)
        void backend_testAddItem_DuplicateItemName_ReturnsConflict() {
                Assertions.assertNotNull(adminToken, "Admin token should not be null");

                String requestBody = "{"
                                + "\"itemName\": \"" + ITEM_NAME + "\","
                                + "\"description\": \"Duplicate attempt.\","
                                + "\"category\": \"Bangle\","
                                + "\"material\": \"Gold\","
                                + "\"purity\": \"22K\","
                                + "\"weightInGrams\": 10,"
                                + "\"makingCharges\": 1000,"
                                + "\"pricePerGram\": 6000,"
                                + "\"stockQuantity\": 2,"
                                + "\"reorderThreshold\": 1"
                                + "}";

                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<String> adminRequest = new HttpEntity<>(requestBody, adminHeaders);

                ResponseEntity<String> response = restTemplate.exchange(
                                "/api/jewellery-item", HttpMethod.POST, adminRequest, String.class);

                System.out.println("Admin POST Duplicate Item Status: " + response.getStatusCode());
                Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode(),
                                "Duplicate item name should return 409 Conflict");
        }

        @Test
        @Order(7)
        void backend_testGetItemById_PublicAccess() {
                Assertions.assertNotNull(itemId, "Item ID should not be null");

                ResponseEntity<String> response = restTemplate.getForEntity(
                                "/api/jewellery-item/" + itemId, String.class);

                System.out.println("GET Item by ID Status: " + response.getStatusCode());
                Assertions.assertEquals(HttpStatus.OK, response.getStatusCode(), "GET item by ID should be public");
        }


        @Test
        @Order(8)
        void backend_testUpdateItem_AdminOnlyAccess() {
                Assertions.assertNotNull(adminToken, "Admin token should not be null");
                Assertions.assertNotNull(userToken, "User token should not be null");
                Assertions.assertNotNull(itemId, "Item ID should not be null");

                String requestBody = "{"
                                + "\"itemName\": \"" + ITEM_NAME + "\","
                                + "\"description\": \"22K gold bangle - updated design notes.\","
                                + "\"category\": \"Bangle\","
                                + "\"material\": \"Gold\","
                                + "\"purity\": \"22K\","
                                + "\"weightInGrams\": 15.5,"
                                + "\"makingCharges\": 1600,"
                                + "\"pricePerGram\": 6000,"
                                + "\"stockQuantity\": 5,"
                                + "\"reorderThreshold\": 3"
                                + "}";

                // User should NOT be able to update an item
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<String> userRequest = new HttpEntity<>(requestBody, userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/jewellery-item/" + itemId, HttpMethod.PUT, userRequest, String.class);

                System.out.println("User PUT Item Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, userResponse.getStatusCode(), "User should NOT be allowed to update an item");

                // Admin should be able to update an item
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<String> adminRequest = new HttpEntity<>(requestBody, adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/jewellery-item/" + itemId, HttpMethod.PUT, adminRequest, String.class);

                System.out.println("Admin PUT Item Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode(), "Admin should be able to update an item");
        }

        // ---------------------------------------------------------------
        // Jewellery Order (stock reservation) + Low-Stock Reorder Alerts
        // ---------------------------------------------------------------

        @Test
        @Order(9)
        void backend_testAddOrder_UserOnlyAccess_ReservesStock() throws Exception {
                Assertions.assertNotNull(userToken, "User token should not be null");
                Assertions.assertNotNull(adminToken, "Admin token should not be null");
                Assertions.assertNotNull(itemId, "Item ID should not be null");

                String requestBody = "{"
                                + "\"userId\": " + buyerUserId + ","
                                + "\"itemId\": " + itemId + ","
                                + "\"quantity\": 3,"
                                + "\"deliveryAddress\": \"12 Lotus Street, Coimbatore\","
                                + "\"comments\": \"Gift wrap please\""
                                + "}";

                // Admin should NOT be able to place an order
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<String> adminRequest = new HttpEntity<>(requestBody, adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/jewellery-order", HttpMethod.POST, adminRequest, String.class);

                System.out.println("Admin POST Order Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, adminResponse.getStatusCode(),
                                "Admin should NOT be allowed to place an order");

                // User should be able to place an order
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<String> userRequest = new HttpEntity<>(requestBody, userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/jewellery-order", HttpMethod.POST, userRequest, String.class);

                System.out.println("User POST Order Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.CREATED, userResponse.getStatusCode(),
                                "User should be able to place an order");

                JsonNode orderNode = objectMapper.readTree(userResponse.getBody());
                orderId = orderNode.get("orderId").asLong();
                // Order(9) updated makingCharges to 1600 before this runs, so the item's
                // totalPrice here is 15.5*6000+1600 = 94600, times quantity 3 = 283800.
                Assertions.assertEquals(283800.0, orderNode.get("totalPrice").asDouble(), 0.01,
                                "totalPrice should be item.totalPrice * quantity");

                // Placing the order must reserve stock immediately: 5 - 3 = 2.
                ResponseEntity<String> itemResponse = restTemplate.getForEntity(
                                "/api/jewellery-item/" + itemId, String.class);
                JsonNode itemNode = objectMapper.readTree(itemResponse.getBody());
                Assertions.assertEquals(2, itemNode.get("stockQuantity").asInt(),
                                "Stock should be reserved (decremented) immediately when the order is placed");
        }

        @Test
        @Order(10)
        void backend_testAddOrder_InsufficientStock_ReturnsConflict() {
                Assertions.assertNotNull(userToken, "User token should not be null");
                Assertions.assertNotNull(itemId, "Item ID should not be null");

                // Only 2 units remain in stock after Order(10) — requesting 10 must fail.
                String requestBody = "{"
                                + "\"userId\": " + buyerUserId + ","
                                + "\"itemId\": " + itemId + ","
                                + "\"quantity\": 10,"
                                + "\"deliveryAddress\": \"12 Lotus Street, Coimbatore\""
                                + "}";

                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<String> userRequest = new HttpEntity<>(requestBody, userHeaders);

                ResponseEntity<String> response = restTemplate.exchange(
                                "/api/jewellery-order", HttpMethod.POST, userRequest, String.class);

                System.out.println("User POST Over-Quantity Order Status: " + response.getStatusCode());
                Assertions.assertEquals(HttpStatus.CONFLICT, response.getStatusCode(),
                                "Ordering more units than are in stock should return 409 Conflict");
        }

        @Test
        @Order(11)
        void backend_testGetLowStockItems_AdminOnlyAccess() throws Exception {
                Assertions.assertNotNull(adminToken, "Admin token should not be null");
                Assertions.assertNotNull(userToken, "User token should not be null");
                Assertions.assertNotNull(itemId, "Item ID should not be null");

                // User should NOT be able to view the Low-Stock Reorder Alerts list
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/jewellery-item/low-stock", HttpMethod.GET, userRequest, String.class);

                System.out.println("User GET Low-Stock Items Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, userResponse.getStatusCode(),
                                "User should NOT be allowed to view Low-Stock Reorder Alerts");

                // Admin should see the item — stock (2) is now at/below reorderThreshold (3).
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<Void> adminRequest = new HttpEntity<>(adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/jewellery-item/low-stock", HttpMethod.GET, adminRequest, String.class);

                System.out.println("Admin GET Low-Stock Items Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode(),
                                "Admin should be able to view Low-Stock Reorder Alerts");

                JsonNode items = objectMapper.readTree(adminResponse.getBody());
                boolean found = false;
                for (JsonNode node : items) {
                        if (node.get("itemId").asLong() == itemId) {
                                found = true;
                                break;
                        }
                }
                Assertions.assertTrue(found, "The bangle should appear on the Low-Stock Reorder Alerts list once stock <= reorderThreshold");
        }

        @Test
        @Order(12)
        void backend_testGetAllOrders_AdminOnlyAccess() {
                Assertions.assertNotNull(adminToken, "Admin token should not be null");
                Assertions.assertNotNull(userToken, "User token should not be null");

                // User should NOT be able to get all orders
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/jewellery-order", HttpMethod.GET, userRequest, String.class);

                System.out.println("User GET All Orders Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, userResponse.getStatusCode(),
                                "User should NOT be allowed to fetch all orders");

                // Admin should be able to get all orders
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<Void> adminRequest = new HttpEntity<>(adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/jewellery-order", HttpMethod.GET, adminRequest, String.class);

                System.out.println("Admin GET All Orders Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode(),
                                "Admin should be able to fetch all orders");
        }

        @Test
        @Order(13)
        void backend_testGetOrdersByUserId_UserOnlyAccess() {
                Assertions.assertNotNull(userToken, "User token should not be null");
                Assertions.assertNotNull(adminToken, "Admin token should not be null");

                // Admin should NOT be able to use this endpoint
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<Void> adminRequest = new HttpEntity<>(adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/jewellery-order/user/" + buyerUserId, HttpMethod.GET, adminRequest, String.class);

                System.out.println("Admin GET Orders by UserId Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, adminResponse.getStatusCode(),
                                "Admin should NOT be allowed to fetch orders by user ID");

                // User should be able to fetch their own orders
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/jewellery-order/user/" + buyerUserId, HttpMethod.GET, userRequest, String.class);

                System.out.println("User GET Orders by UserId Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.OK, userResponse.getStatusCode(),
                                "User should be able to fetch their own orders");
        }

        @Test
        @Order(14)
        void backend_testUpdateOrder_AdminOnlyAccess_RejectReleasesStock() throws Exception {
                Assertions.assertNotNull(adminToken, "Admin token should not be null");
                Assertions.assertNotNull(userToken, "User token should not be null");
                Assertions.assertNotNull(orderId, "Order ID should not be null");
                Assertions.assertNotNull(itemId, "Item ID should not be null");

                String requestBody = "{\"status\": \"Rejected\"}";

                // User should NOT be able to update an order
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<String> userRequest = new HttpEntity<>(requestBody, userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/jewellery-order/" + orderId, HttpMethod.PUT, userRequest, String.class);

                System.out.println("User PUT Order Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, userResponse.getStatusCode(),
                                "User should NOT be allowed to update an order");

                // Admin rejects the order — this must release the reserved stock.
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<String> adminRequest = new HttpEntity<>(requestBody, adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/jewellery-order/" + orderId, HttpMethod.PUT, adminRequest, String.class);

                System.out.println("Admin PUT Order (Reject) Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.OK, adminResponse.getStatusCode(),
                                "Admin should be able to update (reject) an order");

                // Stock should be released: 2 + 3 = 5, back above the reorder threshold.
                ResponseEntity<String> itemResponse = restTemplate.getForEntity(
                                "/api/jewellery-item/" + itemId, String.class);
                JsonNode itemNode = objectMapper.readTree(itemResponse.getBody());
                Assertions.assertEquals(5, itemNode.get("stockQuantity").asInt(),
                                "Rejecting an order should release its reserved stock back to the item");
        }

        // ---------------------------------------------------------------
        // Feedback
        // ---------------------------------------------------------------

        @Test
        @Order(15)
        void backend_testAddFeedback_UserOnlyAccess() throws Exception {
                Assertions.assertNotNull(userToken, "User token should not be null");
                Assertions.assertNotNull(adminToken, "Admin token should not be null");
                Assertions.assertNotNull(itemId, "Item ID should not be null");

                String requestBody = "{\"feedbackText\": \"Beautiful craftsmanship and true to the listed weight.\", \"date\": \"2026-12-21\", "
                                + "\"user\": {\"userId\": " + buyerUserId + "}, \"item\": {\"itemId\": " + itemId + "}}";

                // Admin should NOT be able to add feedback
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<String> adminRequest = new HttpEntity<>(requestBody, adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/feedback", HttpMethod.POST, adminRequest, String.class);

                System.out.println("Admin POST Feedback Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, adminResponse.getStatusCode(),
                                "Admin should NOT be allowed to add feedback");

                // User should be able to add feedback
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<String> userRequest = new HttpEntity<>(requestBody, userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/feedback", HttpMethod.POST, userRequest, String.class);

                System.out.println("User POST Feedback Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.CREATED, userResponse.getStatusCode(),
                                "User should be able to add feedback");

                JsonNode feedbackNode = objectMapper.readTree(userResponse.getBody());
                feedbackId = feedbackNode.get("feedbackId").asLong();
        }


        @Test
        @Order(16)
        void backend_testGetFeedbacksByUserId_UserOnlyAccess() {
                Assertions.assertNotNull(userToken, "User token should not be null");
                Assertions.assertNotNull(adminToken, "Admin token should not be null");

                // Admin should NOT be able to use this endpoint
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<Void> adminRequest = new HttpEntity<>(adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/feedback/user/" + buyerUserId, HttpMethod.GET, adminRequest, String.class);

                System.out.println("Admin GET Feedback by UserId Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, adminResponse.getStatusCode(),
                                "Admin should NOT be allowed to fetch feedback by user ID");

                // User should be able to fetch their own feedback
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/feedback/user/" + buyerUserId, HttpMethod.GET, userRequest, String.class);

                System.out.println("User GET Feedback by UserId Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.OK, userResponse.getStatusCode(),
                                "User should be able to fetch their own feedback");
        }

        @Test
        @Order(17)
        void backend_testDeleteFeedback_UserOnlyAccess() {
                Assertions.assertNotNull(userToken, "User token should not be null");
                Assertions.assertNotNull(adminToken, "Admin token should not be null");
                Assertions.assertNotNull(feedbackId, "Feedback ID should not be null");

                // Admin should NOT be able to delete feedback
                HttpHeaders adminHeaders = createAuthHeaders(adminToken);
                HttpEntity<Void> adminRequest = new HttpEntity<>(adminHeaders);

                ResponseEntity<String> adminResponse = restTemplate.exchange(
                                "/api/feedback/" + feedbackId, HttpMethod.DELETE, adminRequest, String.class);

                System.out.println("Admin DELETE Feedback Status: " + adminResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.UNAUTHORIZED, adminResponse.getStatusCode(),
                                "Admin should NOT be allowed to delete feedback");

                // User should be able to delete their own feedback
                HttpHeaders userHeaders = createAuthHeaders(userToken);
                HttpEntity<Void> userRequest = new HttpEntity<>(userHeaders);

                ResponseEntity<String> userResponse = restTemplate.exchange(
                                "/api/feedback/" + feedbackId, HttpMethod.DELETE, userRequest, String.class);

                System.out.println("User DELETE Feedback Status: " + userResponse.getStatusCode());
                Assertions.assertEquals(HttpStatus.OK, userResponse.getStatusCode(),
                                "User should be able to delete their own feedback");
        }
}