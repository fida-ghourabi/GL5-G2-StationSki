import { Injectable, inject } from '@angular/core';
import { forkJoin, map } from 'rxjs';
import { ApiClient } from '../../core/api/api-client';
import { Subscription, SubscriptionType } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class SubscriptionApi {
  private api = inject(ApiClient);
  all() {
    return forkJoin(['ANNUAL', 'MONTHLY', 'SEMESTRIEL'].map(type => this.byType(type as SubscriptionType))).pipe(map(groups => groups.flat()));
  }
  byType(type: SubscriptionType) { return this.api.get<Subscription[]>(`/subscription/all/${type}`); }
  byPeriod(from: string, to: string) { return this.api.get<Subscription[]>(`/subscription/all/${from}/${to}`); }
}
