import { JewelleryOrder } from "./jewellery-order.model";

describe('JewelleryOrder Model', () => {

  fit('Frontend_should_create_an_instance_JewelleryOrder_with_defined_properties', () => {
    // Create a sample JewelleryOrder object
    const order: JewelleryOrder = {
      orderId: 2001,
      userId: 101,
      itemId: 202,
      quantity: 1,
      orderDate: '2024-11-05',
      status: 'Pending',
      deliveryAddress: '221B Baker Street',
      comments: 'Please gift-wrap'
    };

    expect(order).toBeTruthy();
    expect(order.orderId).toBeDefined();
    expect(order.userId).toBeDefined();
    expect(order.itemId).toBeDefined();
    expect(order.quantity).toBeDefined();
    expect(order.orderDate).toBeDefined();
    expect(order.status).toBeDefined();
    expect(order.deliveryAddress).toBeDefined();
    expect(order.comments).toBeDefined();
  });

  fit('Frontend_should_allow_creation_without_optional_properties', () => {
    // Create a JewelleryOrder object without optional properties
    const order: JewelleryOrder = {
      userId: 101,
      itemId: 202,
      quantity: 1,
      status: 'Pending'
    };

    expect(order).toBeTruthy();
    expect(order.comments).toBeUndefined();
  });

});
