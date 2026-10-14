import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Query, Registration } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class RegistrationApi {
  private api = inject(ApiClient);
  search(query: Query) { return this.api.get<Registration[]>('/registration', query); }
  bySkier(id: number) { return this.api.get<Registration[]>(`/registration/skier/${id}`); }
  register(skier: number, slot: number) { return this.api.post<Registration>(`/registration/skier/${skier}/slot/${slot}`); }
  cancel(id: number) { return this.api.delete(`/registration/${id}`); }
}
