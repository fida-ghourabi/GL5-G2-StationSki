import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ApiClient } from './api-client';
import { ResourceDefinition } from '../../shared/models/resource-definition';
import { Command, Entity, Registration, SubscriptionType } from '../../shared/models/domain';
import { SkierApi } from '../../features/skiers/skier-api';
import { SubscriptionApi } from '../../features/subscriptions/subscription-api';
import { RegistrationApi } from '../../features/registrations/registration-api';
import { PaymentApi } from '../../features/payments/payment-api';
import { CourseApi } from '../../features/courses/course-api';
import { dateRange, subscriptionStatus, person, courseName } from '../../shared/utilities/format';

export type Filters = Record<string, string | number>;
@Injectable({ providedIn: 'root' })
export class ResourceGateway {
  private api = inject(ApiClient); private skiers = inject(SkierApi); private subscriptions = inject(SubscriptionApi);
  private registrations = inject(RegistrationApi); private payments = inject(PaymentApi);
  private courses = inject(CourseApi);
  async list(def: ResourceDefinition, filters: Filters, page = 0): Promise<{ rows: Entity[]; total: number }> {
    const from = String(filters['from'] ?? ''), to = String(filters['to'] ?? '');
    const completeHistory = def.key === 'registrations' && !!filters['skierId'];
    if (filters['week'] && (!Number.isInteger(Number(filters['week'])) || Number(filters['week']) < 1 || Number(filters['week']) > 53)) throw new Error('Le numéro de semaine doit être un entier compris entre 1 et 53.');
    if (!completeHistory && ((from && !to) || (!from && to))) throw new Error('Renseignez les deux dates de la période.');
    if (!completeHistory && from && to && from > to) throw new Error('La date de fin doit être postérieure ou égale à la date de début.');
    let rows: Entity[];
    switch (def.key) {
      case 'skiers': {
        const result = await firstValueFrom(this.skiers.search({ ...filters, page, size: 20 }));
        return { rows: result.content, total: result.totalElements };
      }
      case 'subscriptions': {
        const result = await firstValueFrom(from && to ? this.subscriptions.byPeriod(from, to) : filters['type'] ? this.subscriptions.byType(filters['type'] as SubscriptionType) : this.subscriptions.all());
        rows = result.filter(row => (!filters['type'] || row.typeSub === filters['type']) && (!filters['state'] || subscriptionStatus(row) === filters['state'] || (filters['state'] === 'ACTIVE' && subscriptionStatus(row) === 'EXPIRING')));
        break;
      }
      case 'payments': {
        if (filters['skierId'] && filters['subscriptionId']) throw new Error('Choisissez un seul filtre : skieur ou abonnement.');
        rows = filters['skierId'] ? await firstValueFrom(this.payments.bySkier(Number(filters['skierId']))) : filters['subscriptionId'] ? await firstValueFrom(this.payments.bySubscription(Number(filters['subscriptionId']))) : [];
        break;
      }
      case 'registrations': {
        if (!filters['skierId'] && (!from || !to)) throw new Error('Choisissez une période ou un skieur.');
        const result = filters['skierId'] ? await firstValueFrom(this.registrations.bySkier(Number(filters['skierId']))) : await firstValueFrom(this.registrations.search({ ...dateRange(from, to), courseId: filters['courseId'] }));
        rows = await this.enrichRegistrations(result.filter(row => (!filters['week'] || row.numWeek === Number(filters['week'])) && (!filters['courseId'] || row.courseId === Number(filters['courseId']))));
        break;
      }
      default: rows = await firstValueFrom(this.api.get<Entity[]>(def.listPath, filters));
    }
    return { rows, total: rows.length };
  }
  async enrichRegistrations(rows: Registration[]): Promise<Entity[]> {
    const [skiers, courses] = await Promise.allSettled([firstValueFrom(this.skiers.all()), firstValueFrom(this.courses.all())]);
    return rows.map(row => ({ ...row,
      _skier: skiers.status === 'fulfilled' ? person(skiers.value.find(s => s.numSkier === row.skierId)) : `Skieur #${row.skierId}`,
      _course: row.planningSlot ? courseName(row.planningSlot.course) : courses.status === 'fulfilled' ? courseName(courses.value.find(c => c.numCourse === row.courseId)) : `Cours #${row.courseId}`,
      _status: row.cancelled ? 'CANCELLED' : 'ACTIVE',
    }));
  }
  async get(def: ResourceDefinition, id: number, skierId?: number): Promise<Entity> {
    if (def.key === 'registrations') {
      if (!skierId) throw new Error('Ouvrez cette inscription depuis la liste ou le dossier du skieur.');
      const rows = await firstValueFrom(this.registrations.bySkier(skierId));
      const row = rows.find(r => r.numRegistration === id);
      if (!row) throw new Error('Inscription introuvable pour ce skieur.');
      return (await this.enrichRegistrations([row]))[0];
    }
    return firstValueFrom(this.api.get<Entity>(`${def.getPath}${id}`));
  }
  async save(def: ResourceDefinition, values: Command, row?: Entity): Promise<Entity> {
    if (def.key === 'registrations') return firstValueFrom(this.registrations.register(Number(values['skierId']), Number(values['slotId'])));
    const body = { ...values };
    if (row) {
      body[def.idKey] = row[def.idKey];
      const path = `${def.updatePath}${def.updatePath?.endsWith('/') ? row[def.idKey] : ''}`;
      return firstValueFrom(this.api.put<Entity>(path, body));
    } else return firstValueFrom(this.api.post<Entity>(def.createPath!, body));
  }
}
