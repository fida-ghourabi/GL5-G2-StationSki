import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Command, Query, Rental } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class RentalApi {
  private api = inject(ApiClient);
  search(query: Query) { return this.api.get<Rental[]>('/rentals', query); }
  bySkier(id: number) { return this.api.get<Rental[]>(`/rentals/skier/${id}`); }
  reserve(body: Command) { return this.api.post<Rental>('/rentals', body); }
}
