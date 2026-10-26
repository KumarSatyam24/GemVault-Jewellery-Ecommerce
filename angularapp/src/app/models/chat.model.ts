export interface ChatRequest {
    message: string;
    sessionId?: string;
  }
  
  export interface ChatResponse {
    reply: string;
    matched: boolean;
    matchedQuestion: string | null;
    category: string | null;
    confidence: number;
    source: 'semantic' | 'lexical' | 'live' | 'none';
    sessionId: string;
    resolvedQuestion: string;
  }
  
  export interface ChatMessage {
    id: number;
    sessionId: string;
    userMessage: string;
    botReply: string;
    matchedFaqId: number | null;
    similarityScore: number | null;
    createdAt: string;
  }
  
  export interface ChatBubble {
    role: 'user' | 'assistant';
    text: string;
  }
  