import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Course, Instructor, Registration } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class CourseApi {
  private api = inject(ApiClient);
  all() { return this.api.get<Course[]>('/course/all'); }
  instructors(id: number) { return this.api.get<Instructor[]>(`/course/${id}/instructors`); }
  registrations(id: number) { return this.api.get<Registration[]>(`/course/${id}/registrations`); }
  participants(id: number) { return this.api.get<number>(`/course/${id}/participants/count`); }
}
