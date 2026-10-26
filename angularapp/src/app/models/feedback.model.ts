export class Feedback {
  feedbackId?: number;
  userId = 0;
  itemId?: number;
  feedbackText = '';
  rating = 0;
  date?: Date | string;
  item?: { itemId: number; itemName: string };
  user?: { userId: number; username: string };
}
