import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Piste, Skier } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class PisteApi {
  private api = inject(ApiClient);
  all() { return this.api.get<Piste[]>('/piste/all'); }
  skiers(id: number) { return this.api.get<Skier[]>(`/piste/${id}/skiers`); }
}
