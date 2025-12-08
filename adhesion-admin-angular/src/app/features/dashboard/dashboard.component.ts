import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterLink } from '@angular/router';
import { BaseChartDirective } from 'ng2-charts';
import { ChartConfiguration, ChartData } from 'chart.js';
import { ApiService } from '../../core/services/api.service';
import { DashboardStats, Patient, PsychologicalProfile } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, RouterLink, BaseChartDirective],
  template: `
    <div class="dashboard">
      <!-- Stats Grid -->
      <div class="grid grid-cols-4">
        <div class="stat-card">
          <div class="icon" style="background: #dbeafe;">
            <span class="material-icons" style="color: #3b82f6;">people</span>
          </div>
          <div class="value">{{ stats()?.totalPatients || 0 }}</div>
          <div class="label">Total Patients</div>
        </div>
        
        <div class="stat-card">
          <div class="icon" style="background: #d1fae5;">
            <span class="material-icons" style="color: #10b981;">verified</span>
          </div>
          <div class="value">{{ stats()?.averageAdherence || 0 }}%</div>
          <div class="label">Avg. Adherence</div>
        </div>
        
        <div class="stat-card">
          <div class="icon" style="background: #fef3c7;">
            <span class="material-icons" style="color: #f59e0b;">assignment</span>
          </div>
          <div class="value">{{ stats()?.completedAssessments || 0 }}</div>
          <div class="label">Completed Tests</div>
        </div>
        
        <div class="stat-card">
          <div class="icon" style="background: #fee2e2;">
            <span class="material-icons" style="color: #ef4444;">warning</span>
          </div>
          <div class="value">{{ stats()?.highRiskPatients || 0 }}</div>
          <div class="label">High Risk Patients</div>
        </div>
      </div>
      
      <!-- Charts Row -->
      <div class="grid grid-cols-2">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Adherence Trends</h3>
          </div>
          <canvas baseChart
            [data]="adherenceChartData"
            [options]="lineChartOptions"
            type="line">
          </canvas>
        </div>
        
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Profile Distribution</h3>
          </div>
          <canvas baseChart
            [data]="profileChartData"
            [options]="doughnutChartOptions"
            type="doughnut">
          </canvas>
        </div>
      </div>
      
      <!-- Recent Activity -->
      <div class="grid grid-cols-2">
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Recent Patients</h3>
            <a routerLink="/patients" class="btn btn-secondary">View All</a>
          </div>
          
          @if (recentPatients().length === 0) {
            <div class="empty-state">
              <span class="material-icons icon">people</span>
              <h3>No patients yet</h3>
              <p>Patients will appear here once registered</p>
            </div>
          } @else {
            <table class="table">
              <thead>
                <tr>
                  <th>Name</th>
                  <th>Email</th>
                  <th>Profile</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                @for (patient of recentPatients(); track patient.id) {
                  <tr>
                    <td>{{ patient.firstName }} {{ patient.lastName }}</td>
                    <td>{{ patient.email }}</td>
                    <td>
                      <span class="badge" [ngClass]="getProfileBadgeClass(patient.profileType)">
                        {{ patient.profileType || 'Pending' }}
                      </span>
                    </td>
                    <td>
                      <span class="badge badge-success">Active</span>
                    </td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>
        
        <div class="card">
          <div class="card-header">
            <h3 class="card-title">Recent Profiles</h3>
            <a routerLink="/profiles" class="btn btn-secondary">View All</a>
          </div>
          
          @if (recentProfiles().length === 0) {
            <div class="empty-state">
              <span class="material-icons icon">psychology</span>
              <h3>No profiles assessed</h3>
              <p>Profiles will appear after assessments</p>
            </div>
          } @else {
            <table class="table">
              <thead>
                <tr>
                  <th>Patient</th>
                  <th>Profile Type</th>
                  <th>Risk Level</th>
                  <th>Date</th>
                </tr>
              </thead>
              <tbody>
                @for (profile of recentProfiles(); track profile.id) {
                  <tr>
                    <td>Patient {{ profile.userId.slice(0, 8) }}</td>
                    <td>
                      <span class="badge" [ngClass]="getProfileBadgeClass(profile.profileType)">
                        {{ profile.profileType }}
                      </span>
                    </td>
                    <td>
                      <span class="badge" [ngClass]="getRiskBadgeClass(profile.riskLevel)">
                        {{ profile.riskLevel }}
                      </span>
                    </td>
                    <td>{{ profile.assessedAt | date:'short' }}</td>
                  </tr>
                }
              </tbody>
            </table>
          }
        </div>
      </div>
    </div>
  `,
  styles: [`
    .dashboard {
      display: flex;
      flex-direction: column;
      gap: 24px;
    }
  `]
})
export class DashboardComponent implements OnInit {
  private api = inject(ApiService);

  stats = signal<DashboardStats | null>(null);
  recentPatients = signal<Patient[]>([]);
  recentProfiles = signal<PsychologicalProfile[]>([]);

  // Chart configurations
  adherenceChartData: ChartData<'line'> = {
    labels: ['Week 1', 'Week 2', 'Week 3', 'Week 4'],
    datasets: [
      {
        data: [72, 78, 75, 82],
        label: 'Adherence %',
        fill: true,
        tension: 0.4,
        borderColor: '#4f46e5',
        backgroundColor: 'rgba(79, 70, 229, 0.1)'
      }
    ]
  };

  lineChartOptions: ChartConfiguration<'line'>['options'] = {
    responsive: true,
    plugins: {
      legend: { display: false }
    },
    scales: {
      y: { beginAtZero: true, max: 100 }
    }
  };

  profileChartData: ChartData<'doughnut'> = {
    labels: ['Resilient', 'Anxious', 'Depressive', 'Low Motivation', 'Mixed'],
    datasets: [{
      data: [35, 25, 15, 15, 10],
      backgroundColor: ['#10b981', '#f59e0b', '#ef4444', '#6b7280', '#8b5cf6']
    }]
  };

  doughnutChartOptions: ChartConfiguration<'doughnut'>['options'] = {
    responsive: true,
    plugins: {
      legend: { position: 'bottom' }
    }
  };

  ngOnInit(): void {
    this.loadDashboardData();
  }

  loadDashboardData(): void {
    // Load stats
    this.api.getDashboardStats().subscribe({
      next: (stats) => this.stats.set(stats),
      error: () => {
        // Use mock data if API not available
        this.stats.set({
          totalPatients: 156,
          activePatients: 142,
          totalTests: 12,
          completedAssessments: 423,
          averageAdherence: 78,
          highRiskPatients: 23
        });
      }
    });

    // Load recent patients
    this.api.getPatients().subscribe({
      next: (patients) => this.recentPatients.set(patients.slice(0, 5)),
      error: () => this.recentPatients.set([])
    });

    // Load recent profiles
    this.api.getProfiles().subscribe({
      next: (profiles) => this.recentProfiles.set(profiles.slice(0, 5)),
      error: () => this.recentProfiles.set([])
    });
  }

  getProfileBadgeClass(profileType?: string): string {
    const classes: Record<string, string> = {
      'RESILIENT': 'badge-success',
      'ANXIOUS': 'badge-warning',
      'DEPRESSIVE': 'badge-danger',
      'LOW_MOTIVATION': 'badge-info',
      'MIXED': 'badge-info',
      'COMPLEX': 'badge-danger'
    };
    return classes[profileType || ''] || 'badge-info';
  }

  getRiskBadgeClass(riskLevel: string): string {
    const classes: Record<string, string> = {
      'LOW': 'badge-success',
      'MEDIUM': 'badge-warning',
      'HIGH': 'badge-danger'
    };
    return classes[riskLevel] || 'badge-info';
  }
}
