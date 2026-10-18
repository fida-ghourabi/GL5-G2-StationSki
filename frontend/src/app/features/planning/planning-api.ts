import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Availability, Command, PlanningSlot, Query } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class PlanningApi {
  private api = inject(ApiClient);
  slots(query: Query, available = false) { return this.api.get<PlanningSlot[]>(`/planning/slots${available ? '/available' : ''}`, query); }
  availability(id: number) { return this.api.get<Availability[]>(`/planning/instructors/${id}/availability`); }
  addAvailability(id: number, body: Command) { return this.api.post<Availability>(`/planning/availability/${id}`, body); }
}
