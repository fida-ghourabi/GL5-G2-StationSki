import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Occupancy, Query, Revenue, SubscriptionReport, WeeklyRegistration } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class ReportApi {
  private api = inject(ApiClient);
  revenue(query: Query) { return this.api.get<Revenue>('/reports/revenue', query); }
  subscriptions(asOf: string) { return this.api.get<SubscriptionReport>('/reports/subscriptions', { asOf }); }
  occupancy(query: Query) { return this.api.get<Occupancy[]>('/reports/courses/occupancy', query); }
  registrations(query: Query) { return this.api.get<WeeklyRegistration[]>('/reports/registrations', query); }
}
