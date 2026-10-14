import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Equipment, Query } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class EquipmentApi {
  private api = inject(ApiClient);
  all() { return this.api.get<Equipment[]>('/equipment'); }
  available(query: Query) { return this.api.get<Equipment[]>('/equipment/available', query); }
}
