import { Injectable, inject } from '@angular/core';
import { ApiClient } from '../../core/api/api-client';
import { Page, Query, Skier } from '../../shared/models/domain';
@Injectable({ providedIn: 'root' })
export class SkierApi {
  private api = inject(ApiClient);
  all() { return this.api.get<Skier[]>('/skier/all'); }
  search(query: Query) { return this.api.get<Page<Skier>>('/skier/search', query); }
  get(id: number) { return this.api.get<Skier>(`/skier/get/${id}`); }
  assign(id: number, kind: 'piste' | 'subscription', target: number) { return this.api.put<Skier>(`/skier/${kind === 'piste' ? 'assignToPiste' : 'assignToSub'}/${id}/${target}`); }
}
