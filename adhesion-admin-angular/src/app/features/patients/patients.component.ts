import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/services/api.service';
import { Patient } from '../../core/models';

@Component({
  selector: 'app-patients',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  template: `
    <div class="patients-page">
      <div class="page-header">
        <div class="search-box">
          <span class="material-icons">search</span>
          <input 
            type="text" 
            placeholder="Search patients..."
            [(ngModel)]="searchQuery"
            (input)="filterPatients()"
          />
        </div>
        <button class="btn btn-primary" (click)="showAddModal = true">
          <span class="material-icons">add</span>
          Add Patient
        </button>
      </div>
      
      <div class="card">
        @if (loading()) {
          <div class="loading-container">
            <span class="loading-spinner"></span>
            <p>Loading patients...</p>
          </div>
        } @else if (filteredPatients().length === 0) {
          <div class="empty-state">
            <span class="material-icons icon">people</span>
            <h3>No patients found</h3>
            <p>Add your first patient to get started</p>
          </div>
        } @else {
          <table class="table">
            <thead>
              <tr>
                <th>Name</th>
                <th>Email</th>
                <th>Profile Type</th>
                <th>Risk Level</th>
                <th>Adherence</th>
                <th>Registered</th>
                <th>Actions</th>
              </tr>
            </thead>
            <tbody>
              @for (patient of filteredPatients(); track patient.id) {
                <tr>
                  <td>
                    <div class="patient-name">
                      <div class="avatar">{{ getInitials(patient) }}</div>
                      <div>
                        <div class="name">{{ patient.firstName }} {{ patient.lastName }}</div>
                        <div class="id">ID: {{ patient.id.slice(0, 8) }}...</div>
                      </div>
                    </div>
                  </td>
                  <td>{{ patient.email }}</td>
                  <td>
                    <span class="badge" [ngClass]="getProfileBadgeClass(patient.profileType)">
                      {{ patient.profileType || 'Not Assessed' }}
                    </span>
                  </td>
                  <td>
                    <span class="badge badge-success">Low</span>
                  </td>
                  <td>
                    <div class="adherence-bar">
                      <div class="bar" [style.width.%]="75"></div>
                      <span>75%</span>
                    </div>
                  </td>
                  <td>{{ patient.createdAt | date:'mediumDate' }}</td>
                  <td>
                    <div class="action-buttons">
                      <a [routerLink]="['/patients', patient.id]" class="btn-icon" title="View Details">
                        <span class="material-icons">visibility</span>
                      </a>
                      <button class="btn-icon" title="Edit" (click)="editPatient(patient)">
                        <span class="material-icons">edit</span>
                      </button>
                      <button class="btn-icon danger" title="Delete" (click)="confirmDelete(patient)">
                        <span class="material-icons">delete</span>
                      </button>
                    </div>
                  </td>
                </tr>
              }
            </tbody>
          </table>
        }
      </div>
      
      <!-- Add/Edit Patient Modal -->
      @if (showAddModal) {
        <div class="modal-overlay" (click)="showAddModal = false">
          <div class="modal" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2 class="modal-title">{{ editingPatient ? 'Edit Patient' : 'Add New Patient' }}</h2>
              <button class="modal-close" (click)="showAddModal = false">&times;</button>
            </div>
            
            <form (ngSubmit)="savePatient()">
              <div class="form-group">
                <label class="form-label">First Name</label>
                <input type="text" class="form-input" [(ngModel)]="patientForm.firstName" name="firstName" required />
              </div>
              
              <div class="form-group">
                <label class="form-label">Last Name</label>
                <input type="text" class="form-input" [(ngModel)]="patientForm.lastName" name="lastName" required />
              </div>
              
              <div class="form-group">
                <label class="form-label">Email</label>
                <input type="email" class="form-input" [(ngModel)]="patientForm.email" name="email" required />
              </div>
              
              @if (!editingPatient) {
                <div class="form-group">
                  <label class="form-label">Password</label>
                  <input type="password" class="form-input" [(ngModel)]="patientForm.password" name="password" required />
                </div>
              }
              
              <div class="modal-footer">
                <button type="button" class="btn btn-secondary" (click)="showAddModal = false">Cancel</button>
                <button type="submit" class="btn btn-primary">
                  {{ editingPatient ? 'Update' : 'Create' }} Patient
                </button>
              </div>
            </form>
          </div>
        </div>
      }
      
      <!-- Delete Confirmation Modal -->
      @if (patientToDelete) {
        <div class="modal-overlay" (click)="patientToDelete = null">
          <div class="modal" (click)="$event.stopPropagation()">
            <div class="modal-header">
              <h2 class="modal-title">Confirm Delete</h2>
              <button class="modal-close" (click)="patientToDelete = null">&times;</button>
            </div>
            
            <p>Are you sure you want to delete patient <strong>{{ patientToDelete.firstName }} {{ patientToDelete.lastName }}</strong>?</p>
            <p class="text-danger">This action cannot be undone.</p>
            
            <div class="modal-footer">
              <button class="btn btn-secondary" (click)="patientToDelete = null">Cancel</button>
              <button class="btn btn-danger" (click)="deletePatient()">Delete</button>
            </div>
          </div>
        </div>
      }
    </div>
  `,
  styles: [`
    .page-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 24px;
    }
    
    .search-box {
      display: flex;
      align-items: center;
      gap: 8px;
      background: white;
      border: 1px solid var(--border-color);
      border-radius: 8px;
      padding: 8px 16px;
      width: 300px;
    }
    
    .search-box input {
      border: none;
      outline: none;
      flex: 1;
      font-size: 14px;
    }
    
    .loading-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding: 60px;
      gap: 16px;
    }
    
    .patient-name {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    
    .avatar {
      width: 36px;
      height: 36px;
      background: linear-gradient(135deg, #4f46e5, #7c3aed);
      border-radius: 8px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: white;
      font-size: 12px;
      font-weight: 600;
    }
    
    .name {
      font-weight: 500;
    }
    
    .id {
      font-size: 12px;
      color: var(--text-secondary);
    }
    
    .adherence-bar {
      display: flex;
      align-items: center;
      gap: 8px;
    }
    
    .adherence-bar .bar {
      height: 6px;
      background: #10b981;
      border-radius: 3px;
      max-width: 60px;
    }
    
    .action-buttons {
      display: flex;
      gap: 4px;
    }
    
    .btn-icon {
      background: none;
      border: none;
      padding: 6px;
      border-radius: 6px;
      cursor: pointer;
      color: var(--text-secondary);
      transition: all 0.2s;
      text-decoration: none;
    }
    
    .btn-icon:hover {
      background: var(--bg-primary);
      color: var(--primary-color);
    }
    
    .btn-icon.danger:hover {
      background: #fee2e2;
      color: var(--danger-color);
    }
    
    .text-danger {
      color: var(--danger-color);
      font-size: 13px;
    }
  `]
})
export class PatientsComponent implements OnInit {
  private api = inject(ApiService);

  patients = signal<Patient[]>([]);
  filteredPatients = signal<Patient[]>([]);
  loading = signal(true);
  searchQuery = '';
  showAddModal = false;
  editingPatient: Patient | null = null;
  patientToDelete: Patient | null = null;

  patientForm = {
    firstName: '',
    lastName: '',
    email: '',
    password: ''
  };

  ngOnInit(): void {
    this.loadPatients();
  }

  loadPatients(): void {
    this.loading.set(true);
    this.api.getPatients().subscribe({
      next: (patients) => {
        this.patients.set(patients);
        this.filteredPatients.set(patients);
        this.loading.set(false);
      },
      error: () => {
        this.loading.set(false);
        // Mock data for demo
        const mockPatients: Patient[] = [
          { id: '1', email: 'john@example.com', firstName: 'John', lastName: 'Doe', role: 'PATIENT', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), profileType: 'RESILIENT' },
          { id: '2', email: 'jane@example.com', firstName: 'Jane', lastName: 'Smith', role: 'PATIENT', createdAt: new Date().toISOString(), updatedAt: new Date().toISOString(), profileType: 'ANXIOUS' },
        ];
        this.patients.set(mockPatients);
        this.filteredPatients.set(mockPatients);
      }
    });
  }

  filterPatients(): void {
    const query = this.searchQuery.toLowerCase();
    const filtered = this.patients().filter(p => 
      p.firstName.toLowerCase().includes(query) ||
      p.lastName.toLowerCase().includes(query) ||
      p.email.toLowerCase().includes(query)
    );
    this.filteredPatients.set(filtered);
  }

  getInitials(patient: Patient): string {
    return `${patient.firstName[0]}${patient.lastName[0]}`.toUpperCase();
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

  editPatient(patient: Patient): void {
    this.editingPatient = patient;
    this.patientForm = {
      firstName: patient.firstName,
      lastName: patient.lastName,
      email: patient.email,
      password: ''
    };
    this.showAddModal = true;
  }

  savePatient(): void {
    if (this.editingPatient) {
      this.api.updatePatient(this.editingPatient.id, this.patientForm).subscribe({
        next: () => {
          this.showAddModal = false;
          this.editingPatient = null;
          this.loadPatients();
        }
      });
    } else {
      this.api.createPatient(this.patientForm).subscribe({
        next: () => {
          this.showAddModal = false;
          this.resetForm();
          this.loadPatients();
        }
      });
    }
  }

  confirmDelete(patient: Patient): void {
    this.patientToDelete = patient;
  }

  deletePatient(): void {
    if (this.patientToDelete) {
      this.api.deletePatient(this.patientToDelete.id).subscribe({
        next: () => {
          this.patientToDelete = null;
          this.loadPatients();
        }
      });
    }
  }

  resetForm(): void {
    this.patientForm = { firstName: '', lastName: '', email: '', password: '' };
    this.editingPatient = null;
  }
}
