# Adhesion Web - Angular Mental Health Application

A modern, high-quality Angular 19 web application for mental health and wellness tracking. This is the web companion to the Flutter mobile app.

## Features

### User Features
- 🔐 **Authentication** - Secure login/registration with consent handling
- 📊 **Dashboard** - Wellness score, stats, and quick actions
- 🧪 **Psychological Tests** - Take standardized mental health assessments
- 🎭 **Emotion Detection** - AI-powered camera and text emotion analysis
- 🤖 **AI Assistant (Serenity)** - Health-focused AI chatbot
- ⭐ **Motivation** - Get personalized motivational messages
- 📈 **Predictions** - AI adherence predictions
- 💊 **Medications** - Track your medications
- 📋 **Treatment Plans** - View and manage treatment plans
- 💡 **Recommendations** - AI-generated wellness tips
- 📜 **History** - View test and adherence history
- 👤 **Profile** - View and edit profile with consent toggle

### Admin Features
- 🛡️ **Admin Portal** - Secure admin access
- 📝 **Test Management** - Create, edit, delete psychological tests
- 📥 **JSON Import** - Import tests from JSON files
- 👥 **User Data** - View consented user data
- 👑 **Admin Management** - Create new admin accounts

## Tech Stack

- **Angular 19** - Latest Angular with standalone components
- **TypeScript 5.6** - Type-safe development
- **SCSS** - Modern styling with CSS variables
- **RxJS** - Reactive programming
- **Chart.js** - Data visualization

## Prerequisites

- Node.js 20+
- npm 10+
- Angular CLI 19+

## Installation

```bash
# Navigate to project directory
cd adhesion-web-angular

# Install dependencies
npm install

# Start development server
npm start
```

The app will be available at `http://localhost:4200`

## Backend Configuration

The app connects to:
- **Spring Boot Backend**: `http://localhost:8080` (main API)
- **Flask AI Backend**: `http://localhost:5000` (emotion detection & AI)

Update `src/environments/environment.ts` to change these URLs.

## Project Structure

```
src/
├── app/
│   ├── core/
│   │   ├── guards/          # Auth & Admin route guards
│   │   └── services/        # API, Auth services
│   ├── features/
│   │   ├── admin/           # Admin pages
│   │   ├── auth/            # Login, Register
│   │   ├── dashboard/       # Main dashboard
│   │   ├── emotions/        # Emotion detection
│   │   ├── ai-assistant/    # Serenity chatbot
│   │   ├── motivation/      # Motivation page
│   │   ├── tests/           # Psychological tests
│   │   ├── predictions/     # AI predictions
│   │   ├── medications/     # Medication tracking
│   │   ├── treatment-plans/ # Treatment plans
│   │   ├── recommendations/ # AI tips
│   │   ├── history/         # History view
│   │   └── profile/         # User profile
│   ├── app.component.ts
│   └── app.routes.ts
├── environments/
├── styles.scss              # Global styles & design system
└── index.html
```

## Design System

The app uses a comprehensive design system with:
- **CSS Variables** for theming
- **Modern Typography** (Plus Jakarta Sans)
- **Smooth Animations** (fade, slide, scale)
- **Glassmorphism** effects
- **Gradient accents**
- **Dark theme** for admin portal

## Scripts

```bash
npm start       # Start dev server
npm run build   # Production build
npm run watch   # Watch mode
```

## Admin Access

Default admin account:
- **Email**: admin@gmail.com
- **Password**: admin123

## API Endpoints

The app uses these main API endpoints:
- `/api/auth/*` - Authentication
- `/api/users/*` - User management
- `/api/tests/*` - Test management
- `/api/sessions/*` - Test sessions
- `/api/predictions/*` - AI predictions
- `/api/treatment-plans/*` - Treatment plans
- `/api/admin/*` - Admin endpoints

Flask endpoints:
- `/health` - Health check
- `/chat` - AI chat
- `/emotion` - Camera emotion
- `/motivation` - Get motivation
- `/analyze-text` - Text emotion analysis

## License

MIT

