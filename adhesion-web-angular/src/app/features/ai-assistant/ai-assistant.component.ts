import { Component, inject, signal, ViewChild, ElementRef, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';

interface Message {
  role: 'user' | 'assistant';
  content: string;
  timestamp: Date;
}

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="chat-page">
      <!-- Header -->
      <header class="chat-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
            <path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/>
          </svg>
        </a>
        <div class="header-info">
          <div class="avatar-wrapper">
            <div class="avatar">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor">
                <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/>
              </svg>
            </div>
            <span class="status-dot"></span>
          </div>
          <div class="header-text">
            <h1>Serenity AI</h1>
            <span class="status">Always here for you</span>
          </div>
        </div>
        <div class="header-actions">
          @if (detectedEmotion()) {
            <div class="emotion-badge">
              <span>{{ detectedEmotion() }}</span>
            </div>
          }
        </div>
      </header>

      <!-- Messages Area -->
      <div class="messages-container" #messagesContainer>
        <div class="messages-wrapper">
          <!-- Welcome Message -->
          @if (messages().length === 0) {
            <div class="welcome-card animate-fade-in">
              <div class="welcome-icon">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M11.99 2C6.47 2 2 6.48 2 12s4.47 10 9.99 10C17.52 22 22 17.52 22 12S17.52 2 11.99 2zM12 20c-4.42 0-8-3.58-8-8s3.58-8 8-8 8 3.58 8 8-3.58 8-8 8zm3.5-9c.83 0 1.5-.67 1.5-1.5S16.33 8 15.5 8 14 8.67 14 9.5s.67 1.5 1.5 1.5zm-7 0c.83 0 1.5-.67 1.5-1.5S9.33 8 8.5 8 7 8.67 7 9.5 7.67 11 8.5 11zm3.5 6.5c2.33 0 4.31-1.46 5.11-3.5H6.89c.8 2.04 2.78 3.5 5.11 3.5z"/>
                </svg>
              </div>
              <h2>Hello! I'm Serenity</h2>
              <p>Your AI wellness companion. I'm here to listen, support, and help you navigate your emotional wellbeing. How are you feeling today?</p>
              <div class="quick-prompts">
                <button class="prompt-btn" (click)="sendQuickPrompt('I am feeling stressed today')">
                  😰 Feeling stressed
                </button>
                <button class="prompt-btn" (click)="sendQuickPrompt('I need some motivation')">
                  💪 Need motivation
                </button>
                <button class="prompt-btn" (click)="sendQuickPrompt('I want to talk about my mood')">
                  🗣️ Talk about mood
                </button>
                <button class="prompt-btn" (click)="sendQuickPrompt('Help me relax')">
                  🧘 Help me relax
                </button>
              </div>
            </div>
          }

          <!-- Messages -->
          @for (message of messages(); track $index) {
            <div class="message" [class.user]="message.role === 'user'" [class.assistant]="message.role === 'assistant'">
              @if (message.role === 'assistant') {
                <div class="message-avatar">
                  <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/>
                  </svg>
                </div>
              }
              <div class="message-bubble">
                <p>{{ message.content }}</p>
                <span class="message-time">{{ formatTime(message.timestamp) }}</span>
              </div>
            </div>
          }

          <!-- Typing Indicator -->
          @if (loading()) {
            <div class="message assistant">
              <div class="message-avatar">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
                  <path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2z"/>
                </svg>
              </div>
              <div class="message-bubble typing">
                <div class="typing-dots">
                  <span></span>
                  <span></span>
                  <span></span>
                </div>
              </div>
            </div>
          }
        </div>
      </div>

      <!-- Input Area -->
      <div class="input-area">
        <div class="input-wrapper">
          <textarea
            [(ngModel)]="userMessage"
            (keydown.enter)="onEnterPress($event)"
            placeholder="Share how you're feeling..."
            rows="1"
            [disabled]="loading()"
            #messageInput
          ></textarea>
          <button 
            class="send-btn" 
            [disabled]="!userMessage.trim() || loading()"
            (click)="sendMessage()"
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor">
              <path d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z"/>
            </svg>
          </button>
        </div>
        <p class="input-hint">
          Press Enter to send • Serenity is here to support your mental wellness
        </p>
      </div>
    </div>
  `,
  styles: [`
    .chat-page {
      display: flex;
      flex-direction: column;
      height: 100vh;
      background: linear-gradient(180deg, #F0FDF4 0%, #ECFDF5 100%);
    }

    .chat-header {
      display: flex;
      align-items: center;
      gap: 16px;
      padding: 16px 24px;
      background: white;
      border-bottom: 1px solid var(--gray-100);
      box-shadow: var(--shadow-sm);
    }

    .back-btn {
      width: 40px;
      height: 40px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: var(--gray-100);
      border-radius: 12px;
      color: var(--gray-600);
      transition: all 0.2s;

      &:hover {
        background: var(--gray-200);
      }
    }

    .header-info {
      display: flex;
      align-items: center;
      gap: 12px;
      flex: 1;
    }

    .avatar-wrapper {
      position: relative;
    }

    .avatar {
      width: 44px;
      height: 44px;
      background: linear-gradient(135deg, #14B8A6, #2DD4BF);
      border-radius: 14px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
    }

    .status-dot {
      position: absolute;
      bottom: -2px;
      right: -2px;
      width: 14px;
      height: 14px;
      background: var(--success);
      border: 3px solid white;
      border-radius: 50%;
    }

    .header-text h1 {
      font-size: 18px;
      margin-bottom: 2px;
    }

    .status {
      font-size: 13px;
      color: var(--success);
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }

    .emotion-badge {
      padding: 6px 14px;
      background: rgba(20, 184, 166, 0.1);
      border-radius: 20px;
      color: #14B8A6;
      font-size: 13px;
      font-weight: 600;
    }

    .messages-container {
      flex: 1;
      overflow-y: auto;
      padding: 24px;
    }

    .messages-wrapper {
      max-width: 800px;
      margin: 0 auto;
    }

    .welcome-card {
      text-align: center;
      padding: 48px 32px;
      background: white;
      border-radius: 24px;
      box-shadow: var(--shadow-md);
      margin-bottom: 24px;
    }

    .welcome-icon {
      width: 80px;
      height: 80px;
      background: linear-gradient(135deg, #14B8A6, #2DD4BF);
      border-radius: 24px;
      display: flex;
      align-items: center;
      justify-content: center;
      margin: 0 auto 24px;
      color: white;
    }

    .welcome-card h2 {
      font-size: 24px;
      margin-bottom: 12px;
    }

    .welcome-card p {
      color: var(--gray-600);
      max-width: 400px;
      margin: 0 auto 24px;
      line-height: 1.6;
    }

    .quick-prompts {
      display: flex;
      flex-wrap: wrap;
      justify-content: center;
      gap: 10px;
    }

    .prompt-btn {
      padding: 10px 18px;
      background: var(--gray-100);
      border: none;
      border-radius: 20px;
      font-size: 14px;
      color: var(--gray-700);
      cursor: pointer;
      transition: all 0.2s;

      &:hover {
        background: #14B8A6;
        color: white;
      }
    }

    .message {
      display: flex;
      gap: 12px;
      margin-bottom: 16px;
      animation: fadeInUp 0.3s ease;

      &.user {
        justify-content: flex-end;

        .message-bubble {
          background: linear-gradient(135deg, #14B8A6, #2DD4BF);
          color: white;
          border-radius: 20px 20px 4px 20px;

          .message-time {
            color: rgba(255, 255, 255, 0.7);
          }
        }
      }

      &.assistant {
        .message-bubble {
          background: white;
          border-radius: 20px 20px 20px 4px;
          box-shadow: var(--shadow-sm);
        }
      }
    }

    .message-avatar {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #14B8A6, #2DD4BF);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      flex-shrink: 0;
    }

    .message-bubble {
      max-width: 70%;
      padding: 14px 18px;

      p {
        margin: 0;
        line-height: 1.5;
        color: inherit;
        white-space: pre-wrap;
      }

      &.typing {
        padding: 16px 24px;
      }
    }

    .message-time {
      display: block;
      font-size: 11px;
      color: var(--gray-400);
      margin-top: 6px;
    }

    .typing-dots {
      display: flex;
      gap: 4px;

      span {
        width: 8px;
        height: 8px;
        background: var(--gray-400);
        border-radius: 50%;
        animation: bounce 1.4s infinite ease-in-out both;

        &:nth-child(1) { animation-delay: -0.32s; }
        &:nth-child(2) { animation-delay: -0.16s; }
      }
    }

    @keyframes bounce {
      0%, 80%, 100% { transform: scale(0); }
      40% { transform: scale(1); }
    }

    .input-area {
      padding: 16px 24px 24px;
      background: white;
      border-top: 1px solid var(--gray-100);
    }

    .input-wrapper {
      max-width: 800px;
      margin: 0 auto;
      display: flex;
      gap: 12px;
      padding: 8px 8px 8px 20px;
      background: var(--gray-50);
      border: 2px solid var(--gray-200);
      border-radius: 24px;
      transition: all 0.2s;

      &:focus-within {
        border-color: #14B8A6;
        background: white;
      }

      textarea {
        flex: 1;
        border: none;
        background: transparent;
        font-size: 15px;
        resize: none;
        padding: 10px 0;
        max-height: 120px;
        font-family: inherit;

        &:focus {
          outline: none;
        }

        &::placeholder {
          color: var(--gray-400);
        }
      }
    }

    .send-btn {
      width: 44px;
      height: 44px;
      background: linear-gradient(135deg, #14B8A6, #2DD4BF);
      border: none;
      border-radius: 50%;
      color: white;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s;

      &:hover:not(:disabled) {
        transform: scale(1.05);
      }

      &:disabled {
        opacity: 0.5;
        cursor: not-allowed;
      }
    }

    .input-hint {
      text-align: center;
      font-size: 12px;
      color: var(--gray-400);
      margin-top: 10px;
    }

    @keyframes fadeInUp {
      from {
        opacity: 0;
        transform: translateY(10px);
      }
      to {
        opacity: 1;
        transform: translateY(0);
      }
    }
  `]
})
export class AiAssistantComponent implements AfterViewChecked {
  @ViewChild('messagesContainer') messagesContainer!: ElementRef;
  
  private api = inject(ApiService);

  messages = signal<Message[]>([]);
  userMessage = '';
  loading = signal(false);
  detectedEmotion = signal<string | null>(null);

  private shouldScroll = false;

  ngAfterViewChecked(): void {
    if (this.shouldScroll) {
      this.scrollToBottom();
      this.shouldScroll = false;
    }
  }

  scrollToBottom(): void {
    if (this.messagesContainer) {
      const container = this.messagesContainer.nativeElement;
      container.scrollTop = container.scrollHeight;
    }
  }

  onEnterPress(event: Event): void {
    const keyboardEvent = event as KeyboardEvent;
    if (!keyboardEvent.shiftKey) {
      event.preventDefault();
      this.sendMessage();
    }
  }

  sendQuickPrompt(prompt: string): void {
    this.userMessage = prompt;
    this.sendMessage();
  }

  sendMessage(): void {
    const message = this.userMessage.trim();
    if (!message || this.loading()) return;

    // Add user message
    const userMsg: Message = {
      role: 'user',
      content: message,
      timestamp: new Date()
    };
    this.messages.update(msgs => [...msgs, userMsg]);
    this.userMessage = '';
    this.loading.set(true);
    this.shouldScroll = true;

    // Build conversation history
    const history = this.messages().map(m => ({
      role: m.role,
      content: m.content
    }));

    // Send to API
    this.api.chat(message, history).subscribe({
      next: (response) => {
        const assistantMsg: Message = {
          role: 'assistant',
          content: response.bot_message,
          timestamp: new Date()
        };
        this.messages.update(msgs => [...msgs, assistantMsg]);
        this.detectedEmotion.set(response.detected_emotion);
        this.loading.set(false);
        this.shouldScroll = true;
      },
      error: () => {
        const errorMsg: Message = {
          role: 'assistant',
          content: "I'm having trouble connecting right now. Please try again in a moment.",
          timestamp: new Date()
        };
        this.messages.update(msgs => [...msgs, errorMsg]);
        this.loading.set(false);
        this.shouldScroll = true;
      }
    });
  }

  formatTime(date: Date): string {
    return date.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }
}

