import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { STATION } from '../config/station.config';
import { Command, Query } from '../../shared/models/domain';

@Injectable({ providedIn: 'root' })
export class ApiClient {
  private readonly http = inject(HttpClient);
  get<T>(path: string, query: Query = {}) {
    let params = new HttpParams();
    Object.entries(query).forEach(([key, value]) => { if (value !== undefined && value !== null && value !== '') params = params.set(key, String(value)); });
    return this.http.get<T>(`${STATION.apiBase}${path}`, { params });
  }
  post<T>(path: string, body: Command = {}) { return this.http.post<T>(`${STATION.apiBase}${path}`, body); }
  put<T>(path: string, body: Command = {}) { return this.http.put<T>(`${STATION.apiBase}${path}`, body); }
  delete(path: string) { return this.http.delete<void>(`${STATION.apiBase}${path}`); }
}
