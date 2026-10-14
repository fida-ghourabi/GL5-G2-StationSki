import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { Observable, firstValueFrom } from 'rxjs';
import { ApiClient } from '../../core/api/api-client';
import { ResourceGateway } from '../../core/api/resource-gateway';
import { apiError } from '../../core/error-handling/api-error';
import { ResourceDefinition, ResourceKey } from '../models/resource-definition';
import { RESOURCES } from '../models/resources';
import { Entity, Command, Registration, PlanningSlot, Receipt } from '../models/domain';
import { FormDialogService, Field } from './form-dialog.service';
import { DataTableComponent, Column } from './data-table.component';
import { StateComponent } from './state.component';
import { IconComponent } from './icon.component';
import { StatusComponent } from './status.component';
import { courseName, day, dateRange, label, money, person, read, subscriptionStatus, today } from '../utilities/format';
import { dateField, options } from '../utilities/form-fields';

interface Section { title: string; rows: Entity[]; columns: Column[]; loading: boolean; error: string; resource?: ResourceKey; note?: string }
const SLOT_COLUMNS: Column[] = [{ key: 'startTime', label: 'Début', format: 'datetime' }, { key: 'instructor', label: 'Moniteur', format: 'person' }, { key: 'maxCapacity', label: 'Places maximum' }, { key: 'status', label: 'Statut', format: 'badge' }];
const AVAILABILITY_COLUMNS: Column[] = [{ key: 'dayOfWeek', label: 'Jour' }, { key: 'startTime', label: 'De' }, { key: 'endTime', label: 'À' }, { key: 'validFrom', label: 'Valide du', format: 'date' }, { key: 'validUntil', label: 'Au', format: 'date' }];
const REFUND_COLUMNS: Column[] = [{ key: 'refundDate', label: 'Date', format: 'datetime' }, { key: 'amount', label: 'Montant', format: 'money' }, { key: 'reason', label: 'Motif' }];

@Component({ selector: 'app-resource-detail', standalone: true, imports: [RouterLink, DataTableComponent, StateComponent, IconComponent, StatusComponent], template: `
  <a class="back-link" [routerLink]="'/' + def.key">← Retour · {{ def.title }}</a>
  <app-state [loading]="loading()" [error]="error()" (retry)="load()"/>
  @if (row(); as current) {
    <section class="detail-banner"><div class="profile-mark"><app-icon [name]="def.icon"/></div><div><p class="eyebrow">{{ def.title }} / Fiche n° {{ id }}</p><h1>{{ title(current) }}</h1><p>{{ subtitle(current) }}</p></div>
      <div class="actions">@if (def.updatePath && !(def.key === 'equipment' && current['status'] === 'RENTED')) {<button class="btn secondary" (click)="edit()"><app-icon name="edit"/>Modifier</button>}@if (def.deletePath) {<button class="icon-btn danger-text" (click)="remove()" aria-label="Supprimer cette fiche"><app-icon name="trash"/></button>}</div>
    </section>
    <div class="detail-layout"><div class="detail-main">
      <section class="panel"><div class="panel-heading"><h2>Informations {{ def.key === 'payments' ? 'de paiement' : 'du dossier' }}</h2>@if (current['status']) {<app-status [value]="current['status']"/>}@if (def.key === 'subscriptions') {<app-status [value]="subStatus(current)"/>}</div>
        <dl class="detail-facts">@for (col of def.columns; track col.key) {<div><dt>{{ col.label }}</dt><dd>{{ display(current, col) }}</dd></div>}
          @if (def.key === 'rentals') {<div><dt>Retour réel</dt><dd>{{ date(current['actualReturnDate']) }}</dd></div>}
          @if (def.key === 'payments') {<div><dt>Montant remboursé</dt><dd>{{ money(current['refundedAmount']) }}</dd></div><div><dt>Encaissement confirmé</dt><dd>{{ date(current['paidAt'], true) }}</dd></div>}
        </dl>
      </section>
      @if (def.key === 'courses') {<section class="panel"><div class="panel-heading"><h2>Participation au cours</h2></div><p class="context-note">{{ participantsLoading() ? 'Chargement…' : participantsError() || (participants() + ' skieur(s) distinct(s) avec une inscription non annulée.') }}</p><p class="context-note">La capacité précise de chaque séance figure dans les créneaux ci-dessous.</p></section>}
      @for (section of sections(); track section.title) {
        <section class="panel"><div class="panel-heading"><div><h2>{{ section.title }}</h2><span class="muted">{{ section.rows.length }} élément{{ section.rows.length > 1 ? 's' : '' }}</span></div></div>
          @if (section.note) {<p class="context-note">{{ section.note }}</p>}
          <app-state [loading]="section.loading" [error]="section.error" [empty]="!section.rows.length" title="Aucun élément associé" description="Les données apparaîtront ici après une affectation ou une opération." (retry)="load()"/>
          @if (!section.loading && !section.error && section.rows.length) {<app-data-table [rows]="section.rows" [columns]="section.columns" [caption]="section.title" [interactive]="!!section.resource" (select)="openRelated(section.resource!, $event)"/>}
        </section>
      }
      @if (receipt(); as receipt) {
        <section class="panel receipt"><div class="panel-heading"><h2>Reçu de paiement</h2><button class="btn small secondary" (click)="downloadReceipt()"><app-icon name="download"/>Télécharger le JSON</button></div><div class="receipt-body"><p class="eyebrow">{{ receipt.receiptNumber }}</p><h3>{{ receipt.skierName }}</h3><p>Abonnement #{{ receipt.subscriptionId }} · {{ date(receipt.createdAt, true) }}</p><dl><div><dt>Montant enregistré</dt><dd>{{ money(receipt.amount) }}</dd></div><div><dt>Remboursements</dt><dd>{{ money(receipt.refundedAmount) }}</dd></div><div class="receipt-total"><dt>Net encaissé</dt><dd>{{ money(receipt.netAmount) }}</dd></div></dl><p class="muted">Reçu local · aucun traitement bancaire externe.</p></div></section>
      }
    </div><aside class="detail-aside"><section class="panel action-panel"><p class="eyebrow">Opérations</p><h2>Agir sur ce dossier</h2>
      @switch (def.key) {
        @case ('skiers') {<button class="btn secondary" (click)="assignSkier('subscription')"><app-icon name="ticket"/>Affecter un abonnement</button><button class="btn secondary" (click)="assignSkier('piste')"><app-icon name="piste"/>Affecter une piste</button><a class="btn secondary" routerLink="/registrations" [queryParams]="{skierId:id}"><app-icon name="clipboard"/>Voir les inscriptions</a>}
        @case ('instructors') {<button class="btn secondary" (click)="assignCourse()"><app-icon name="plus"/>Associer un cours</button><button class="btn secondary" (click)="unassignCourse()">Retirer un cours</button><button class="btn secondary" (click)="availability()"><app-icon name="calendar"/>Ajouter une disponibilité</button><a class="btn secondary" routerLink="/planning" [queryParams]="{instructorId:id}">Voir le planning</a>}
        @case ('courses') {<a class="btn secondary" routerLink="/planning" [queryParams]="{courseId:id}"><app-icon name="calendar"/>Voir les créneaux</a><a class="btn secondary" routerLink="/instructors">Gérer le moniteur associé</a>}
        @case ('registrations') {@if (!current['cancelled']) {<button class="btn secondary danger-text" (click)="confirm('Annuler l’inscription', '/registration/' + id, 'DELETE')">Annuler l’inscription</button>}}
        @case ('rentals') {
          @if (current['status'] === 'RESERVED') {<button class="btn" (click)="confirm('Activer la location', '/rentals/' + id + '/activate', 'PUT', false)">Remettre le matériel</button><button class="btn secondary danger-text" (click)="confirm('Annuler la réservation', '/rentals/' + id + '/cancel', 'PUT')">Annuler la réservation</button>}
          @if (current['status'] === 'ACTIVE') {<button class="btn" (click)="returnRental()">Enregistrer le retour</button>}
        }
        @case ('payments') {
          @if (current['status'] === 'PENDING') {<button class="btn" (click)="paymentState('PAID')">Confirmer l’encaissement</button><button class="btn secondary danger-text" (click)="paymentState('FAILED')">Déclarer un échec</button>}
          @if (current['status'] === 'PAID' || current['status'] === 'PARTIALLY_REFUNDED') {<button class="btn secondary" (click)="refund()">Enregistrer un remboursement</button>}
          <button class="btn secondary" (click)="loadReceipt()"><app-icon name="ticket"/>Consulter le reçu</button>
        }
        @case ('subscriptions') {<a class="btn secondary" routerLink="/payments" [queryParams]="{subscriptionId:id}">Consulter les paiements</a>}
        @case ('equipment') {<a class="btn secondary" routerLink="/rentals">Réserver cet exemplaire</a>}
        @default {<p class="muted">Les affectations aux pistes se font depuis le dossier du skieur.</p>}
      }
      <hr><p class="muted small-copy">{{ def.note || 'Les opérations sont contrôlées par le serveur. Les historiques et les associations peuvent empêcher une suppression.' }}</p>
    </section></aside></div>
  }
` })
export class ResourceDetailComponent implements OnInit {
  private route = inject(ActivatedRoute); private router = inject(Router); private api = inject(ApiClient); private gateway = inject(ResourceGateway); private dialogs = inject(FormDialogService);
  readonly def = this.route.snapshot.data['resource'] as ResourceDefinition;
  readonly id = Number(this.route.snapshot.paramMap.get('id'));
  readonly row = signal<Entity | null>(null); readonly sections = signal<Section[]>([]); readonly receipt = signal<Receipt | null>(null);
  readonly loading = signal(false); readonly error = signal('');
  readonly participants = signal<number | null>(null); readonly participantsLoading = signal(false); readonly participantsError = signal('');
  readonly money = money; readonly date = day; readonly subStatus = subscriptionStatus;
  ngOnInit() { this.load(); }
  title(row: Entity) { return ['skiers', 'instructors'].includes(this.def.key) ? person(row) : this.def.key === 'pistes' ? String(row['namePiste']) : this.def.key === 'courses' ? courseName(row) : this.def.key === 'equipment' ? String(row['inventoryCode']) : `${this.def.singular.charAt(0).toUpperCase() + this.def.singular.slice(1)} #${this.id}`; }
  subtitle(row: Entity) { return this.def.key === 'skiers' ? String(row['city']) : this.def.key === 'equipment' ? `${label(row['type'])} · ${row['brand']} · ${row['size']}` : 'Dossier de la station · informations et historique'; }
  display(row: Entity, col: Column) {
    const value = read(row, col.key);
    switch (col.format) {
      case 'date': return day(value); case 'datetime': return day(value, true); case 'money': return money(value);
      case 'person': return person((col.key === '$' ? row : value) as Entity | undefined);
      case 'course': return courseName((col.key === '$' ? row : value) as Entity | undefined);
      case 'count': return String(Array.isArray(value) ? value.length : 0);
      case 'subscriptionStatus': return label(subscriptionStatus(row));
      default: return `${label(value)}${value == null ? '' : col.suffix ?? ''}`;
    }
  }
  async load() {
    if (!Number.isInteger(this.id) || this.id <= 0) { this.error.set('Identifiant de dossier invalide.'); return; }
    this.loading.set(true); this.error.set(''); this.sections.set([]);
    try { this.row.set(await this.gateway.get(this.def, this.id, Number(this.route.snapshot.queryParamMap.get('skierId')) || undefined)); this.loadSections(); }
    catch (error) { this.row.set(null); this.error.set(apiError(error)); }
    finally { this.loading.set(false); }
  }
  private section(title: string, request: Observable<Entity[]> | Promise<Entity[]>, columns: Column[], resource?: ResourceKey, note?: string) {
    const section: Section = { title, rows: [], columns, resource, note, loading: true, error: '' }; this.sections.update(items => [...items, section]);
    const promise = request instanceof Promise ? request : firstValueFrom(request);
    promise.then(rows => { section.rows = rows; }).catch(error => { section.error = apiError(error); }).finally(() => { section.loading = false; this.sections.update(items => [...items]); });
  }
  private loadSections() {
    const id = this.id, range = dateRange(today(), today(90));
    const section = (title: string, path: string, resource: ResourceKey) => this.section(title, this.api.get<Entity[]>(path), RESOURCES[resource].columns, resource);
    switch (this.def.key) {
      case 'skiers':
        if (this.row()?.['subscription']) this.section('Abonnement affecté', Promise.resolve([this.row()!['subscription'] as Entity]), RESOURCES.subscriptions.columns, 'subscriptions');
        this.section('Inscriptions', firstValueFrom(this.api.get<Registration[]>(`/registration/skier/${id}`)).then(rows => this.gateway.enrichRegistrations(rows)), RESOURCES.registrations.columns, 'registrations');
        section('Locations', `/rentals/skier/${id}`, 'rentals'); section('Paiements', `/payments/skier/${id}`, 'payments'); break;
      case 'pistes': section('Skieurs affectés', `/piste/${id}/skiers`, 'skiers'); break;
      case 'courses':
        this.loadParticipants();
        section('Moniteur associé', `/course/${id}/instructors`, 'instructors');
        this.section('Inscriptions au cours', firstValueFrom(this.api.get<Registration[]>(`/course/${id}/registrations`)).then(rows => this.gateway.enrichRegistrations(rows)), RESOURCES.registrations.columns, 'registrations');
        this.section('Créneaux des 90 prochains jours', this.api.get<PlanningSlot[]>('/planning/slots', { ...range, courseId: id }), SLOT_COLUMNS); break;
      case 'instructors':
        section('Cours associés', `/instructor/${id}/courses`, 'courses');
        this.section('Disponibilités récurrentes', this.api.get<Entity[]>(`/planning/instructors/${id}/availability`), AVAILABILITY_COLUMNS);
        this.section('Créneaux des 90 prochains jours', this.api.get<PlanningSlot[]>('/planning/slots', { ...range, instructorId: id }), SLOT_COLUMNS); break;
      case 'subscriptions': section('Historique des paiements', `/payments/subscription/${id}`, 'payments'); break;
      case 'payments': this.section('Remboursements', this.api.get<Entity[]>(`/payments/${id}/refunds`), REFUND_COLUMNS); break;
      default: break;
    }
  }
  private async loadParticipants() { this.participantsLoading.set(true); this.participantsError.set(''); try { this.participants.set(await firstValueFrom(this.api.get<number>(`/course/${this.id}/participants/count`))); } catch (error) { this.participantsError.set(apiError(error)); } finally { this.participantsLoading.set(false); } }
  openRelated(resource: ResourceKey, row: Entity) { this.router.navigate(['/', resource, row[RESOURCES[resource].idKey]], { queryParams: resource === 'registrations' ? { skierId: row['skierId'] } : {} }); }
  edit() { const row = this.row()!; this.dialogs.show({ title: `Modifier ${this.def.singular === 'piste' ? 'la' : 'le'} ${this.def.singular}`, description: this.def.note, fields: this.def.fields, initial: row, submit: async values => { await this.gateway.save(this.def, values, row); await this.load(); } }); }
  remove() { this.dialogs.show({ title: 'Supprimer cette fiche', description: 'Confirmez la suppression. Le serveur refusera l’opération si cette fiche possède un historique ou des associations à préserver.', fields: [], danger: true, submitLabel: 'Confirmer la suppression', submit: async () => { await firstValueFrom(this.api.delete(`${this.def.deletePath}${this.id}`)); await this.router.navigate(['/', this.def.key]); } }); }
  confirm(title: string, path: string, method: 'PUT' | 'DELETE', danger = true, body: Command = {}) {
    this.dialogs.show({ title, description: 'Confirmez cette opération. Les contrôles métier et les règles de conservation de l’historique restent appliqués.', fields: [], danger, submitLabel: 'Confirmer', submit: async () => { await firstValueFrom(method === 'PUT' ? this.api.put(path, body) : this.api.delete(path)); await this.load(); } });
  }
  assignSkier(kind: 'subscription' | 'piste') {
    this.dialogs.show({ title: kind === 'piste' ? 'Affecter une piste' : 'Affecter un abonnement', fields: [{ key: 'target', label: kind === 'piste' ? 'Piste' : 'Abonnement', type: 'select', source: kind === 'piste' ? 'pistes' : 'subscriptions' }], submit: async values => { await firstValueFrom(this.api.put(`/skier/${kind === 'piste' ? 'assignToPiste' : 'assignToSub'}/${this.id}/${values['target']}`)); await this.load(); } });
  }
  assignCourse() { this.dialogs.show({ title: 'Associer un cours', description: 'Un cours ne peut être associé qu’à un seul moniteur.', fields: [{ key: 'courseId', label: 'Cours', type: 'select', source: 'courses' }], submit: async values => { await firstValueFrom(this.api.put(`/instructor/${this.id}/courses/${values['courseId']}`)); await this.load(); } }); }
  unassignCourse() {
    const courses = this.row()?.['courses'] as Entity[] ?? [];
    this.dialogs.show({ title: 'Retirer un cours', description: 'Confirmez le cours à retirer. Annulez d’abord ses créneaux futurs.', danger: true, submitLabel: 'Confirmer le retrait', fields: [{ key: 'courseId', label: 'Cours associé', type: 'select', options: courses.map(c => ({ value: Number(c['numCourse']), label: courseName(c) })) }], submit: async values => { await firstValueFrom(this.api.delete(`/instructor/${this.id}/courses/${values['courseId']}`)); await this.load(); } });
  }
  availability() { this.dialogs.show({ title: 'Ajouter une disponibilité', fields: availabilityFields(), validate: availabilityValidation, submit: async values => { await firstValueFrom(this.api.post(`/planning/availability/${this.id}`, values)); await this.load(); } }); }
  returnRental() { this.dialogs.show({ title: 'Enregistrer le retour', description: 'Confirmez la restitution physique. Le serveur recalculera le montant selon la durée réelle.', submitLabel: 'Confirmer le retour', fields: [{ ...dateField('actualReturnDate', 'Date réelle du retour', today()), min: String(this.row()!['startDate']), max: today() }], submit: async values => { await firstValueFrom(this.api.put(`/rentals/${this.id}/return`, values)); await this.load(); } }); }
  paymentState(state: 'PAID' | 'FAILED') { this.confirm(state === 'PAID' ? 'Confirmer l’encaissement' : 'Déclarer le paiement échoué', `/payments/${this.id}/status`, 'PUT', state === 'FAILED', { status: state }); }
  refund() {
    const remaining = Number(this.row()!['amount']) - Number(this.row()!['refundedAmount']);
    this.dialogs.show({ title: 'Enregistrer un remboursement', description: `Confirmez le montant et le motif. Reste remboursable : ${money(remaining)}. Aucun virement bancaire automatique.`, danger: true, submitLabel: 'Confirmer le remboursement', fields: [{ key: 'amount', label: 'Montant à rembourser', type: 'number', min: 0.01, max: remaining, step: 0.01 }, { key: 'reason', label: 'Motif', type: 'textarea', full: true }], submit: async values => { await firstValueFrom(this.api.post(`/payments/${this.id}/refunds`, values)); this.receipt.set(null); await this.load(); } });
  }
  async loadReceipt() { try { this.receipt.set(await firstValueFrom(this.api.get<Receipt>(`/payments/${this.id}/receipt`))); } catch (error) { this.error.set(apiError(error)); } }
  downloadReceipt() { const receipt = this.receipt(); if (!receipt) return; const url = URL.createObjectURL(new Blob([JSON.stringify(receipt, null, 2)], { type: 'application/json' })); const a = document.createElement('a'); a.href = url; a.download = `${receipt.receiptNumber}.json`; a.click(); setTimeout(() => URL.revokeObjectURL(url), 1000); }
}

export function availabilityFields(): Field[] {
  return [{ key: 'dayOfWeek', label: 'Jour de la semaine', type: 'select', options: options('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY', 'SUNDAY') }, { key: 'startTime', label: 'Heure de début', type: 'time' }, { key: 'endTime', label: 'Heure de fin', type: 'time' }, dateField('validFrom', 'Valide du', today()), dateField('validUntil', 'Valide jusqu’au', today(90))];
}
export function availabilityValidation(values: Command): string | undefined {
  if (String(values['startTime']) >= String(values['endTime'])) return 'L’heure de fin doit être postérieure au début.';
  if (String(values['validFrom']) > String(values['validUntil'])) return 'La fin de validité doit être postérieure ou égale au début.';
  return undefined;
}
