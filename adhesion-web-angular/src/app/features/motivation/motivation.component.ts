import { Component, inject, signal, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { AuthService } from '../../core/services/auth.service';

interface MotivationCategory {
  name: string;
  icon: string;
  color: string;
  prompt: string;
}

@Component({
  selector: 'app-motivation',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="motivation-page">
      <!-- Header -->
      <header class="page-header">
        <a routerLink="/dashboard" class="back-btn">
          <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M20 11H7.83l5.59-5.59L12 4l-8 8 8 8 1.41-1.41L7.83 13H20v-2z"/></svg>
        </a>
        <div class="header-content">
          <h1>Get Motivated</h1>
          <p>Choose a topic for personalized inspiration</p>
        </div>
      </header>

      <div class="content">
        <!-- Categories Grid -->
        <section class="categories-section">
          <h2>Select a Topic</h2>
          <div class="categories-grid">
            @for (category of categories; track category.name; let i = $index) {
              <button 
                class="category-card" 
                [class.active]="currentCategory() === i"
                [style.--accent-color]="category.color"
                (click)="selectCategory(i)"
              >
                <div class="category-icon">{{ category.icon }}</div>
                <span class="category-name">{{ category.name }}</span>
              </button>
            }
          </div>
        </section>

        <!-- Motivation Display -->
        <section class="motivation-section">
          <div class="motivation-card" [class.loading]="loading()">
            @if (loading()) {
              <div class="loading-state">
                <div class="pulse-circle"></div>
                <p>Generating your motivation...</p>
              </div>
            } @else if (motivationMessage()) {
              <div class="motivation-content animate-fade-in">
                <div class="quote-icon">"</div>
                <p class="motivation-text">{{ motivationMessage() }}</p>
                <div class="motivation-footer">
                  <span class="category-tag">{{ categories[currentCategory()].name }}</span>
                </div>
              </div>
            } @else {
              <div class="empty-state">
                <div class="sparkle-icon">✨</div>
                <h3>Ready for Inspiration?</h3>
                <p>Select a topic above and click the button below to receive personalized motivation.</p>
              </div>
            }
          </div>

          @if (error()) {
            <div class="error-banner">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="currentColor"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z"/></svg>
              {{ error() }}
            </div>
          }

          <button 
            class="generate-btn" 
            (click)="getMotivation()"
            [disabled]="loading()"
            [style.--btn-color]="categories[currentCategory()].color"
          >
            @if (loading()) {
              <div class="spinner"></div>
              <span>Generating...</span>
            } @else {
              <svg width="24" height="24" viewBox="0 0 24 24" fill="currentColor"><path d="M12 17.27L18.18 21l-1.64-7.03L22 9.24l-7.19-.61L12 2 9.19 8.63 2 9.24l5.46 4.73L5.82 21z"/></svg>
              <span>Get New Motivation</span>
            }
          </button>
        </section>

        <!-- Tips Section -->
        <section class="tips-section">
          <h2>Quick Tips</h2>
          <div class="tips-list">
            <div class="tip-item">
              <span class="tip-icon">💡</span>
              <p>Take a deep breath before reading your motivation</p>
            </div>
            <div class="tip-item">
              <span class="tip-icon">📝</span>
              <p>Write down messages that resonate with you</p>
            </div>
            <div class="tip-item">
              <span class="tip-icon">🔄</span>
              <p>Come back daily for fresh inspiration</p>
            </div>
          </div>
        </section>
      </div>
    </div>
  `,
  styles: [`
    .motivation-page {
      min-height: 100vh;
      background: linear-gradient(135deg, #FEF3C7 0%, #FDE68A 50%, #FBBF24 100%);
    }

    .page-header {
      display: flex;
      align-items: flex-start;
      gap: 16px;
      padding: 24px;
      background: rgba(255, 255, 255, 0.8);
      backdrop-filter: blur(10px);
      border-bottom: 1px solid rgba(245, 158, 11, 0.2);
    }

    .back-btn {
      width: 44px;
      height: 44px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: white;
      border-radius: 12px;
      color: var(--gray-600);
      box-shadow: var(--shadow-sm);
      transition: all 0.2s;

      &:hover {
        transform: translateY(-2px);
        box-shadow: var(--shadow-md);
      }
    }

    .header-content {
      h1 {
        font-size: 24px;
        font-weight: 700;
        color: var(--gray-900);
        margin-bottom: 4px;
      }
      p {
        font-size: 14px;
        color: var(--gray-600);
      }
    }

    .content {
      max-width: 600px;
      margin: 0 auto;
      padding: 24px;
    }

    .categories-section {
      margin-bottom: 32px;

      h2 {
        font-size: 16px;
        font-weight: 600;
        color: var(--gray-800);
        margin-bottom: 16px;
      }
    }

    .categories-grid {
      display: grid;
      grid-template-columns: repeat(3, 1fr);
      gap: 12px;
    }

    .category-card {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 8px;
      padding: 16px 12px;
      background: white;
      border: 2px solid transparent;
      border-radius: 16px;
      cursor: pointer;
      transition: all 0.3s cubic-bezier(0.34, 1.56, 0.64, 1);
      box-shadow: var(--shadow-sm);

      &:hover {
        transform: translateY(-4px);
        box-shadow: var(--shadow-lg);
      }

      &.active {
        border-color: var(--accent-color);
        background: linear-gradient(180deg, white 0%, rgba(var(--accent-color), 0.05) 100%);
        transform: scale(1.02);
      }
    }

    .category-icon {
      font-size: 28px;
    }

    .category-name {
      font-size: 12px;
      font-weight: 600;
      color: var(--gray-700);
      text-align: center;
    }

    .motivation-section {
      margin-bottom: 32px;
    }

    .motivation-card {
      background: white;
      border-radius: 24px;
      padding: 40px 32px;
      min-height: 220px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      box-shadow: 0 20px 40px rgba(245, 158, 11, 0.2);
      margin-bottom: 24px;
      position: relative;
      overflow: hidden;

      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        height: 4px;
        background: linear-gradient(90deg, #F59E0B, #FBBF24, #FCD34D);
      }
    }

    .loading-state {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 16px;

      p {
        color: var(--gray-500);
        font-size: 14px;
      }
    }

    .pulse-circle {
      width: 60px;
      height: 60px;
      background: linear-gradient(135deg, #F59E0B, #FBBF24);
      border-radius: 50%;
      animation: pulse 1.5s ease-in-out infinite;
    }

    @keyframes pulse {
      0%, 100% { transform: scale(1); opacity: 1; }
      50% { transform: scale(1.1); opacity: 0.7; }
    }

    .motivation-content {
      text-align: center;
      width: 100%;
    }

    .quote-icon {
      font-size: 48px;
      color: var(--warning);
      font-family: Georgia, serif;
      line-height: 1;
      margin-bottom: 8px;
    }

    .motivation-text {
      font-size: 18px;
      line-height: 1.8;
      color: var(--gray-700);
      margin-bottom: 20px;
    }

    .motivation-footer {
      display: flex;
      justify-content: center;
    }

    .category-tag {
      padding: 6px 16px;
      background: rgba(245, 158, 11, 0.1);
      color: #B45309;
      border-radius: 20px;
      font-size: 12px;
      font-weight: 600;
    }

    .empty-state {
      text-align: center;

      .sparkle-icon {
        font-size: 48px;
        margin-bottom: 16px;
      }

      h3 {
        font-size: 20px;
        margin-bottom: 8px;
        color: var(--gray-800);
      }

      p {
        color: var(--gray-600);
        font-size: 14px;
        line-height: 1.6;
      }
    }

    .error-banner {
      display: flex;
      align-items: center;
      gap: 8px;
      padding: 12px 16px;
      background: rgba(239, 68, 68, 0.1);
      color: var(--error);
      border-radius: 12px;
      font-size: 14px;
      margin-bottom: 16px;
    }

    .generate-btn {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 12px;
      padding: 18px 24px;
      background: linear-gradient(135deg, var(--btn-color, #F59E0B), #FBBF24);
      color: white;
      border: none;
      border-radius: 16px;
      font-size: 16px;
      font-weight: 600;
      cursor: pointer;
      transition: all 0.3s;
      box-shadow: 0 8px 24px rgba(245, 158, 11, 0.4);

      &:hover:not(:disabled) {
        transform: translateY(-2px);
        box-shadow: 0 12px 32px rgba(245, 158, 11, 0.5);
      }

      &:disabled {
        opacity: 0.7;
        cursor: not-allowed;
      }
    }

    .spinner {
      width: 20px;
      height: 20px;
      border: 2px solid rgba(255, 255, 255, 0.3);
      border-top-color: white;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .tips-section {
      h2 {
        font-size: 16px;
        font-weight: 600;
        color: var(--gray-800);
        margin-bottom: 16px;
      }
    }

    .tips-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }

    .tip-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 16px;
      background: white;
      border-radius: 14px;
      box-shadow: var(--shadow-sm);

      .tip-icon {
        font-size: 20px;
      }

      p {
        font-size: 14px;
        color: var(--gray-700);
      }
    }

    .animate-fade-in {
      animation: fadeIn 0.5s ease-out;
    }

    @keyframes fadeIn {
      from { opacity: 0; transform: translateY(10px); }
      to { opacity: 1; transform: translateY(0); }
    }

    @media (max-width: 480px) {
      .categories-grid {
        grid-template-columns: repeat(2, 1fr);
      }
    }
  `]
})
export class MotivationComponent implements OnInit {
  private api = inject(ApiService);
  private auth = inject(AuthService);

  motivationMessage = signal<string | null>(null);
  loading = signal(false);
  error = signal<string | null>(null);
  currentCategory = signal(0);
  adherenceScore = signal<number | null>(null);

  categories: MotivationCategory[] = [
    {
      name: 'General Wellness',
      icon: '❤️',
      color: '#EF4444',
      prompt: 'mental wellness and self-care'
    },
    {
      name: 'Medication',
      icon: '💊',
      color: '#10B981',
      prompt: 'staying consistent with medication and treatment'
    },
    {
      name: 'Stress Relief',
      icon: '🧘',
      color: '#8B5CF6',
      prompt: 'managing stress and finding inner peace'
    },
    {
      name: 'Positive Mind',
      icon: '🧠',
      color: '#F59E0B',
      prompt: 'developing a positive mindset and overcoming negative thoughts'
    },
    {
      name: 'Daily Boost',
      icon: '☀️',
      color: '#0EA5E9',
      prompt: 'starting the day with energy and purpose'
    },
    {
      name: 'Self Love',
      icon: '💜',
      color: '#EC4899',
      prompt: 'self-love, acceptance, and building confidence'
    }
  ];

  ngOnInit(): void {
    this.loadUserContext();
    // Auto-generate motivation on page load
    this.getMotivation();
  }

  async loadUserContext(): Promise<void> {
    const userId = this.auth.getUserId();
    if (userId) {
      this.api.getAdherenceScore(userId).subscribe({
        next: (score) => this.adherenceScore.set(score),
        error: () => this.adherenceScore.set(50)
      });
    }
  }

  selectCategory(index: number): void {
    this.currentCategory.set(index);
  }

  getMotivation(): void {
    this.loading.set(true);
    this.error.set(null);

    const category = this.categories[this.currentCategory()];

    this.api.getMotivation(category.prompt).subscribe({
      next: (res) => {
        this.motivationMessage.set(this.cleanResponse(res.motivation));
        this.loading.set(false);
      },
      error: (err) => {
        // Fallback motivational messages based on category
        const fallbacks = this.getFallbackMessage(category.name);
        this.motivationMessage.set(fallbacks);
        this.loading.set(false);
      }
    });
  }

  cleanResponse(text: string): string {
    let cleaned = text;
    cleaned = cleaned.replace(/\[Your Name\]/gi, '');
    cleaned = cleaned.replace(/\[Name\]/gi, '');
    cleaned = cleaned.replace(/^#{1,6}\s*/gm, '');
    cleaned = cleaned.replace(/\*\*(.+?)\*\*/g, '$1');
    cleaned = cleaned.replace(/\*(.+?)\*/g, '$1');
    cleaned = cleaned.replace(/\n{3,}/g, '\n\n');
    return cleaned.trim();
  }

  getFallbackMessage(category: string): string {
    const fallbacks: Record<string, string[]> = {
      'General Wellness': [
        'Every step you take toward better health is a victory. Celebrate your progress, no matter how small.',
        'Your wellbeing matters. Taking time for yourself is not selfish - it is necessary.',
        'You are capable of amazing things. Trust the process and be patient with yourself.'
      ],
      'Medication': [
        'Consistency is key. Each dose you take is an investment in your future health.',
        'Your commitment to your treatment plan shows incredible strength and self-care.',
        'Remember, taking your medication on time is one of the most powerful things you can do for yourself.'
      ],
      'Stress Relief': [
        'Take a deep breath. This moment of peace is yours. Let go of what you cannot control.',
        'Stress is temporary, but your inner strength is permanent. You have overcome challenges before.',
        'Find your calm within the storm. You have the power to choose peace.'
      ],
      'Positive Mind': [
        'Your thoughts shape your reality. Choose thoughts that empower and uplift you.',
        'Every day is a new opportunity to think positively and create the life you want.',
        'Positive thinking is not about ignoring problems - it is about approaching them with hope.'
      ],
      'Daily Boost': [
        'Today is full of possibilities. Embrace them with open arms and an open heart.',
        'Rise and shine! This day is yours to make beautiful. Start with gratitude.',
        'Good morning to new beginnings! Every sunrise brings a fresh chance to grow.'
      ],
      'Self Love': [
        'You are worthy of love and kindness - especially from yourself.',
        'Treat yourself with the same compassion you would offer a dear friend.',
        'Your worth is not measured by productivity. You are enough, just as you are.'
      ]
    };

    const messages = fallbacks[category] || fallbacks['General Wellness'];
    return messages[Math.floor(Math.random() * messages.length)];
  }
}

