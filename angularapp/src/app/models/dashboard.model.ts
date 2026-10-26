export interface DashboardOverview {
    from: string;
    to: string;
    previousFrom: string;
    previousTo: string;
    generatedAt: string;
    granularity: 'day' | 'month';
    summary: {
      netSales: number;
      previousNetSales: number;
      orders: number;
      previousOrders: number;
      unitsSold: number;
      retainedOrders: number;
      averageOrderValue: number;
      refundsInitiated: number;
      buyers: number;
      activePieces: number;
      stockUnits: number;
      lowStockCount: number;
      outOfStockCount: number;
    };
    trend: { date: string; sales: number; orders: number }[];
    statuses: { status: string; count: number }[];
    categories: { category: string; sales: number; units: number }[];
    topPieces: {
      itemId: number;
      itemName: string;
      units: number;
      sales: number;
    }[];
    stockAlerts: {
      itemId: number;
      itemName: string;
      sku: string;
      stock: number;
      threshold: number;
    }[];
    recentOrders: {
      orderId: number;
      itemName: string;
      buyerName: string;
      date: string;
      status: string;
      paymentStatus: string;
      quantity: number;
      total: number;
    }[];
  }
  