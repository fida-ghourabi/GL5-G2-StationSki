import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Instructor, Course } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class InstructorApi {
  private api = inject(ApiClient);
  all() { return this.api.get<Instructor[]>('/instructor/all'); }
  courses(id: number) { return this.api.get<Course[]>(`/instructor/${id}/courses`); }
  assign(id: number, courseId: number) { return this.api.put<Instructor>(`/instructor/${id}/courses/${courseId}`); }
  unassign(id: number, courseId: number) { return this.api.delete(`/instructor/${id}/courses/${courseId}`); }
}
