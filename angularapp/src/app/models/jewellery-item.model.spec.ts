import { JewelleryItem } from "./jewellery-item.model";

describe('JewelleryItem Model', () => {

  fit('Frontend_should_create_an_instance_JewelleryItem_with_defined_properties', () => {
    // Create a sample JewelleryItem object
    const item: JewelleryItem = {
      itemId: 101,
      itemName: 'Classic Solitaire Ring',
      description: 'An 18K gold ring with a single round-cut diamond.',
      category: 'Ring',
      material: 'Gold',
      purity: '18K',
      weightInGrams: 4.5,
      makingCharges: 1500,
      pricePerGram: 6200,
      totalPrice: 29400,
      stockQuantity: 12,
      reorderThreshold: 3
    };

    expect(item).toBeTruthy();
    expect(item.itemId).toBeDefined();
    expect(item.itemName).toBeDefined();
    expect(item.description).toBeDefined();
    expect(item.category).toBeDefined();
    expect(item.material).toBeDefined();
    expect(item.purity).toBeDefined();
    expect(item.weightInGrams).toBeDefined();
    expect(item.makingCharges).toBeDefined();
    expect(item.pricePerGram).toBeDefined();
    expect(item.totalPrice).toBeDefined();
    expect(item.stockQuantity).toBeDefined();
    expect(item.reorderThreshold).toBeDefined();
  });

});
