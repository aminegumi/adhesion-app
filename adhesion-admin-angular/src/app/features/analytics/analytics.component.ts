import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { ApiService } from '../../core/services/api.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, FormsModule, BaseChartDirective],
  template: `
    <div class="analytics-page">
      <div class="page-header">
        <h2>Analytics Dashboard</h2>
        <div class="date-range">
          <input type="date" class="form-input" [(ngModel)]="startDate" />
          <span>to</span>
          <input type="date" class="form-input" [(ngModel)]="endDate" />
          <button class="btn btn-primary" (click)="loadAnalytics()">
            <span class="material-icons">refresh</span>
            Update
          </button>
        </div>
      </div>
      
      <!-- Summary Stats -->
      <div class="grid grid-cols-4">
        <div class="stat-card">
          <div class="icon" style="background: #dbeafe;">
            <span class="material-icons" style="color: #3b82f6;">people</span>
          </div>
          <div class="value">{{ stats.totalPatients }}</div>
          <div class="label">Total Patients</div>
          <div class="trend positive">
            <span class="material-icons">trending_up</span>
            +12% this month
          </div>
        </div>
        
        <div class="stat-card">
          <div class="icon" style="background: #d1fae5;">
            <span class="material-icons" style="color: #10b981;">verified</span>
          </div>
          <div class="value">{{ stats.avgAdherence }}%</div>
          <div class="label">Average Adherence</div>
          <div class="trend positive">
            <span class="material-icons">trending_up</span>
            +5% improvement
          </div>
        </div>
        
        <div class="stat-card">
          <div class="icon" style="background: #fef3c7;">
            <span class="material-icons" style="color: #f59e0b;">assignment_turned_in</span>
          </div>
          <div class="value">{{ stats.completedTests }}</div>
          <div class="label">Completed Tests</div>
          <div class="trend positive">
            <span class="material-icons">trending_up</span>
            +23 this week
          </div>
        </div>
        
        <div class="stat-card">
          <div class="icon" style="background: #fee2e2;">
            <span class="material-icons" style="color: #ef4444;">warning</span>
          </div>
          <div class="value">{{ stats.highRiskCount }}</div>
          <div class="label">High Risk Patients</div>
          <div class="trend negative">
            <span class="material-icons">trending_down</span>
            -3 this month
          </div>
        </div>
      </div>
      
      <!-- Main Charts -->
      <div class="grid grid-cols-2">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Adherence Trends Over Time</h3>
          </div>
          <canvas baseChart
            [data]="adherenceTrendData"
            [options]="lineChartOptions"
            type="line">
          </canvas>
        </div>
        
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Profile Type Distribution</h3>
          </div>
          <canvas baseChart
            [data]="profileDistributionData"
            [options]="pieChartOptions"
            type="pie">
          </canvas>
        </div>
      </div>
      
      <!-- Secondary Charts -->
      <div class="grid grid-cols-2">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Risk Level Distribution</h3>
          </div>
          <canvas baseChart
            [data]="riskDistributionData"
            [options]="barChartOptions"
            type="bar">
          </canvas>
        </div>
        
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Test Completion Rate</h3>
          </div>
          <canvas baseChart
            [data]="testCompletionData"
            [options]="lineChartOptions"
            type="line">
          </canvas>
        </div>
      </div>
      
      <!-- Detailed Stats -->
      <div class="grid grid-cols-3">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Top Performing Patients</h3>
          </div>
          <div class="leaderboard">
            @for (patient of topPatients; track patient.name; let i = $index) {
              <div class="leaderboard-item">
                <span class="rank">{{ i + 1 }}</span>
                <div class="patient-info">
                  <span class="name">{{ patient.name }}</span>
                  <span class="score">{{ patient.adherence }}% adherence</span>
                </div>
                <span class="badge badge-success">{{ patient.streak }} day streak</span>
              </div>
            }
          </div>
        </div>
        
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Patients Needing Attention</h3>
          </div>
          <div class="attention-list">
            @for (patient of attentionPatients; track patient.name) {
              <div class="attention-item">
                <div class="patient-info">
                  <span class="name">{{ patient.name }}</span>
                  <span class="reason">{{ patient.reason }}</span>
                </div>
                <span class="badge" [ngClass]="patient.urgency === 'HIGH' ? 'badge-danger' : 'badge-warning'">
                  {{ patient.urgency }}
                </span>
              </div>
            }
          </div>
        </div>
        
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">AI Insights</h3>
          </div>
          <div class="insights-list">
            @for (insight of aiInsights; track insight.title) {
              <div class="insight-item">
                <span class="material-icons" [style.color]="insight.color">{{ insight.icon }}</span>
                <div>
                  <span class="insight-title">{{ insight.title }}</span>
                  <span class="insight-desc">{{ insight.description }}</span>
                </div>
              </div>
            }
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    
    .date-range {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    
    .date-range .form-input {
      width: 160px;
    }
    
    .trend {
      display: flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      margin-top: 8px;
    }
    
    .trend .material-icons {
      font-size: 16px;
    }
    
    .trend.positive {
      color: #10b981;
    }
    
    .trend.negative {
      color: #ef4444;
    }
    
    .leaderboard {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .leaderboard-item {
      display: flex;
      align-items: center;
      gap: 12px;
      padding: 12px;
      background: var(--bg-primary);
      border-radius: 8px;
    }
    
    .rank {
      width: 28px;
      height: 28px;
      background: linear-gradient(135deg, #4f46e5, #7c3aed);
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-weight: 600;
      font-size: 12px;
    }
    
    .leaderboard-item .patient-info {
      flex: 1;
      display: flex;
      flex-direction: column;
    }
    
    .leaderboard-item .name {
      font-weight: 500;
    }
    
    .leaderboard-item .score {
      font-size: 12px;
      color: var(--text-secondary);
    }
    
    .attention-list {
      display: flex;
      flex-direction: column;
      gap: 12px;
    }
    
    .attention-item {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px;
      background: var(--bg-primary);
      border-radius: 8px;
    }
    
    .attention-item .patient-info {
      display: flex;
      flex-direction: column;
    }
    
    .attention-item .name {
      font-weight: 500;
    }
    
    .attention-item .reason {
      font-size: 12px;
      color: var(--text-secondary);
    }
    
    .insights-list {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }
    
    .insight-item {
      display: flex;
      gap: 12px;
      align-items: flex-start;
    }
    
    .insight-item .material-icons {
      font-size: 24px;
    }
    
    .insight-item > div {
      display: flex;
      flex-direction: column;
    }
    
    .insight-title {
      font-weight: 500;
      margin-bottom: 4px;
    }
    
    .insight-desc {
      font-size: 13px;
      color: var(--text-secondary);
    }
  `]
})
export class AnalyticsComponent implements OnInit {
  private api = inject(ApiService);

  startDate = this.formatDate(new Date(Date.now() - 30 * 24 * 60 * 60 * 1000));
  endDate = this.formatDate(new Date());

  stats = {
    totalPatients: 156,
    avgAdherence: 78,
    completedTests: 423,
    highRiskCount: 23
  };

  topPatients = [
    { name: 'Sarah Johnson', adherence: 98, streak: 45 },
    { name: 'Michael Chen', adherence: 95, streak: 32 },
    { name: 'Emily Davis', adherence: 93, streak: 28 },
    { name: 'James Wilson', adherence: 91, streak: 21 },
    { name: 'Anna Martinez', adherence: 89, streak: 18 }
  ];

  attentionPatients = [
    { name: 'Robert Brown', reason: 'Missed 3 consecutive days', urgency: 'HIGH' },
    { name: 'Lisa Taylor', reason: 'Depression score increased', urgency: 'HIGH' },
    { name: 'David Lee', reason: 'Low motivation detected', urgency: 'MEDIUM' },
    { name: 'Jennifer Garcia', reason: 'Treatment plan expiring', urgency: 'MEDIUM' }
  ];

  aiInsights = [
    { icon: 'trending_up', color: '#10b981', title: 'Adherence Improving', description: 'Overall adherence has improved by 5% this month' },
    { icon: 'warning', color: '#f59e0b', title: 'Attention Needed', description: '4 patients show signs of declining engagement' },
    { icon: 'psychology', color: '#4f46e5', title: 'Profile Pattern', description: 'Anxious profiles respond best to morning reminders' },
    { icon: 'lightbulb', color: '#8b5cf6', title: 'Recommendation', description: 'Consider adding more exercise tasks for depressive profiles' }
  ];

  // Charts
  adherenceTrendData: ChartData<'line'> = {
    labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4', 'Week 5', 'Week 6'],
    datasets: [
      {
        data: [72, 74, 73, 78, 76, 82],
        label: 'Average Adherence %',
        fill: true,
        tension: 0.4,
        borderColor: '#4f46e5',
        backgroundColor: 'rgba(79, 70, 229, 0.1)'
      }
    ]
  };

  profileDistributionData: ChartData<'pie'> = {
    labels: ['Resilient', 'Anxious', 'Depressive', 'Low Motivation', 'Mixed', 'Complex'],
    datasets: [{
      data: [35, 25, 15, 12, 8, 5],
      backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#6b7280', '#8b5cf6', '#dc2626']
    }]
  };

  riskDistributionData: ChartData<'bar'> = {
    labels: ['Low Risk', 'Medium Risk', 'High Risk'],
    datasets: [{
      data: [98, 35, 23],
      backgroundColor: ['#10b981', '#f59e0b', '#ef4444']
    }]
  };

  testCompletionData: ChartData<'line'> = {
    labels: ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'],
    datasets: [
      {
        data: [12, 19, 15, 22, 18, 8, 5],
        label: 'Tests Completed',
        borderColor: '#10b981',
        backgroundColor: 'rgba(16, 185, 129, 0.1)',
        fill: true,
        tension: 0.4
      }
    ]
  };

  lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    plugins: {
      legend: { display: false }
    },
    scales: {
      y: { beginAtZero: true }
    }
  };

  pieChartOptions: ChartConfiguration<'pie'>['options'] = {
    responsive: true,
    plugins: {
      legend: { position: 'bottom' }
    }
  };

  barChartOptions: ChartConfiguration<'bar'>['options'] = {
    responsive: true,
    plugins: {
      legend: { display: false }
    },
    scales: {
      y: { beginAtZero: true }
    }
  };

  ngOnInit(): void {
    this.loadAnalytics();
  }

  loadAnalytics(): void {
    // Load real data from API when available
    this.api.getDashboardStats().subscribe({
      next: (stats) => {
        this.stats = {
          totalPatients: stats.totalPatients,
          avgAdherence: stats.averageAdherence,
          completedTests: stats.completedAssessments,
          highRiskCount: stats.highRiskPatients
        };
      }
    });
  }

  formatDate(date: Date): string {
    return date.toISOString().split('T')[0];
  }
}
