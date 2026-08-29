export type Review = {
  id: number;
  productId: number;
  productName?: string | null;
  rating: number;
  body: string;
  authorName: string;
  authorUsername?: string | null;
  createdAt: string;
  helpfulCount: number;
  helpfulByCurrentUser: boolean;
  status?: string;
};

export type ReviewListResponse = {
  reviews: Review[];
  totalElements: number;
};

export type ReviewEligibleOrder = {
  orderId: number;
  orderNumber: string;
};

export type SubmitReviewPayload = {
  productId: number;
  orderId: number;
  rating: number;
  body: string;
};

export type HelpfulVoteResponse = {
  helpfulCount: number;
  helpfulByCurrentUser: boolean;
};
