import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Command, Payment, Receipt } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class PaymentApi {
  private api = inject(ApiClient);
  bySkier(id: number) { return this.api.get<Payment[]>(`/payments/skier/${id}`); }
  bySubscription(id: number) { return this.api.get<Payment[]>(`/payments/subscription/${id}`); }
  record(body: Command) { return this.api.post<Payment>('/payments', body); }
  receipt(id: number) { return this.api.get<Receipt>(`/payments/${id}/receipt`); }
}
