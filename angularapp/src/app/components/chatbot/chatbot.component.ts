import { Component, ElementRef, OnInit, ViewChild } from '@angular/core';
import { ChatBubble, ChatMessage } from '../../models/chat.model';
import { ChatService } from '../../services/chat.service';
import { AuthService } from '../../services/auth.service';
import { NavigationEnd, Router } from '@angular/router';
import { filter } from 'rxjs';

@Component({
  selector: 'app-chatbot',
  templateUrl: './chatbot.component.html',
})
export class ChatbotComponent implements OnInit {
  displayText(value: string): string {
    if (
      /```|\bpublic\s+class\s+\w+|\bSystem\.out\.print|\bconsole\.log\s*\(/i.test(
        value,
      )
    )
      return 'I can help with GemVault jewellery, orders, your account and shopping questions. What would you like to know?';
    return value
      .replace(/\r\n?/g, '\n')
      .replace(/^[ \t]*#{1,6}[ \t]+/gm, '')
      .replace(/^[ \t]*[-*][ \t]+/gm, '')
      .replace(/\*\*([^*\n]+)\*\*/g, '$1')
      .replace(/(?<!\*)\*([^*\n]+)\*(?!\*)/g, '$1')
      .replace(/^[ \t]*`{3}[^\n]*\n?/gm, '')
      .replace(/`/g, '')
      .replace(/\n{3,}/g, '\n\n')
      .trim();
  }
  @ViewChild('scrollArea') scrollArea?: ElementRef<HTMLElement>;
  @ViewChild('messageInput') messageInput?: ElementRef<HTMLInputElement>;
  @ViewChild('launcher') launcher?: ElementRef<HTMLButtonElement>;
  get suggestions(): string[] {
    if (this.auth.isAdmin())
      return [
        'Which products need restock?',
        'Can I import products from CSV?',
        'Show me gold rings',
      ];
    if (this.auth.isCustomer())
      return [
        "What's in my wishlist?",
        'What is the status of my orders?',
        'Recommend pieces for me',
      ];
    return [
      'Show me gold rings',
      'How do I place an order?',
      'How do product ratings work?',
    ];
  }
  open = false;
  draft = '';
  sending = false;
  loadingHistory = false;
  error = '';
  bubbles: ChatBubble[] = [];
  private sessionId: string | null = null;
  private identity = '';

  constructor(
    private chat: ChatService,
    private auth: AuthService,
    private router: Router,
  ) {}

  ngOnInit(): void {
    this.syncIdentity();
    this.router.events
      .pipe(filter((event) => event instanceof NavigationEnd))
      .subscribe(() => this.syncIdentity());
  }

  private syncIdentity(): void {
    const identity = this.auth.isAuthenticated()
      ? String(this.auth.getUserId())
      : 'guest';
    if (identity === this.identity) return;
    this.identity = identity;
    this.bubbles = [];
    this.sessionId = null;
    this.draft = '';
    this.error = '';
    this.sending = false;
    this.loadingHistory = false;
    const stored = sessionStorage.getItem(this.sessionKey());
    if (!stored || !/^[0-9a-f-]{36}$/i.test(stored)) return;
    this.sessionId = stored;
    this.loadingHistory = true;
    this.chat.history(stored).subscribe({
      next: (history) => {
        if (identity !== this.identity) return;
        this.bubbles = [];
        history.forEach((entry: ChatMessage) => {
          this.bubbles.push({ role: 'user', text: entry.userMessage });
          this.bubbles.push({ role: 'assistant', text: entry.botReply });
        });
        this.loadingHistory = false;
        this.scrollToBottom();
      },
      error: () => {
        if (identity === this.identity) this.loadingHistory = false;
      },
    });
  }

  private sessionKey(): string {
    return `gemvault.chat.session.${this.identity}`;
  }

  toggle(): void {
    this.syncIdentity();
    this.open = !this.open;
    const reducedMotion = window.matchMedia(
      '(prefers-reduced-motion: reduce)',
    ).matches;
    if (this.open) {
      this.scrollToBottom();
      if (window.matchMedia('(pointer: fine)').matches) {
        window.setTimeout(
          () => {
            if (this.open) this.messageInput?.nativeElement.focus();
          },
          reducedMotion ? 0 : 520,
        );
      }
      return;
    }
    window.setTimeout(
      () => {
        if (!this.open) this.launcher?.nativeElement.focus();
      },
      reducedMotion ? 0 : 540,
    );
  }

  send(text = this.draft): void {
    const previousIdentity = this.identity;
    this.syncIdentity();
    if (previousIdentity !== this.identity) return;
    const message = text.trim();
    if (!message || this.sending || message.length > 1000) return;
    this.error = '';
    this.sending = true;
    this.draft = '';
    this.bubbles.push({ role: 'user', text: message });
    this.scrollToBottom();
    const identity = this.identity;
    this.chat
      .sendMessage({
        message,
        ...(this.sessionId ? { sessionId: this.sessionId } : {}),
      })
      .subscribe({
        next: (response) => {
          if (identity !== this.identity) return;
          this.sessionId = response.sessionId;
          sessionStorage.setItem(this.sessionKey(), response.sessionId);
          this.bubbles.push({ role: 'assistant', text: response.reply });
          this.sending = false;
          this.scrollToBottom();
          window.setTimeout(() => this.messageInput?.nativeElement.focus(), 0);
        },
        error: () => {
          if (identity !== this.identity) return;
          this.bubbles.pop();
          this.draft = message;
          this.error = 'Could not send that message. Please try again.';
          this.sending = false;
        },
      });
  }

  clear(): void {
    if (this.sending) return;
    const previous = this.sessionId;
    this.sessionId = null;
    sessionStorage.removeItem(this.sessionKey());
    this.bubbles = [];
    this.draft = '';
    this.error = '';
    if (previous)
      this.chat.clearMemory(previous).subscribe({ error: () => {} });
    window.setTimeout(() => this.messageInput?.nativeElement.focus(), 0);
  }

  private scrollToBottom(): void {
    window.setTimeout(() => {
      const area = this.scrollArea?.nativeElement;
      if (area) area.scrollTop = area.scrollHeight;
    }, 0);
  }
}
