import { Component, OnInit, inject, signal, computed } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { PlanningApi } from './planning-api';
import { ApiClient } from '../../core/api/api-client';
import { LookupService } from '../../core/api/lookup.service';
import { apiError } from '../../core/error-handling/api-error';
import { PlanningSlot, Availability, Entity } from '../../shared/models/domain';
import { Field, FormDialogService, Option } from '../../shared/components/form-dialog.service';
import { availabilityFields, availabilityValidation } from '../../shared/components/resource-detail.component';
import { IconComponent } from '../../shared/components/icon.component';
import { StateComponent } from '../../shared/components/state.component';
import { StatusComponent } from '../../shared/components/status.component';
import { DataTableComponent, Column } from '../../shared/components/data-table.component';
import { day, person, courseName, dateRange, today, localDate } from '../../shared/utilities/format';

@Component({ selector: 'app-planning', standalone: true, imports: [FormsModule, IconComponent, StateComponent, StatusComponent, DataTableComponent], template: `
  <section class="page-intro"><div><p class="eyebrow">Coordination des équipes</p><h1>Planning</h1><p>Chaque moniteur à sa place. Chaque cours dans son créneau.</p></div><div class="actions"><button class="btn secondary" (click)="addAvailability()">Ajouter une disponibilité</button><button class="btn" (click)="slotForm()"><app-icon name="plus"/>Créer un créneau</button></div></section>
  <section class="panel calendar-panel">
    <div class="calendar-toolbar"><div class="segmented" aria-label="Vue du planning">@for (mode of modes; track mode.value) {<button [class.active]="view() === mode.value" [attr.aria-pressed]="view() === mode.value" (click)="changeView(mode.value)">{{ mode.label }}</button>}</div><div class="actions"><button class="icon-btn" aria-label="Période précédente" (click)="move(-1)">←</button><button class="btn small secondary" (click)="goToday()">Aujourd’hui</button><button class="icon-btn" aria-label="Période suivante" (click)="move(1)">→</button><strong>{{ heading() }}</strong></div></div>
    <form class="filter-bar" (ngSubmit)="load()"><div class="field"><label for="planning-anchor">{{ view() === 'list' ? 'Du' : 'Date de repère' }}</label><input id="planning-anchor" type="date" name="anchor" [(ngModel)]="anchor" required/></div>@if (view() === 'list') {<div class="field"><label for="planning-to">Au (inclus)</label><input id="planning-to" type="date" name="to" [(ngModel)]="listTo" required/></div>}
      <div class="field"><label for="planning-instructor">Moniteur</label><select id="planning-instructor" name="instructor" [(ngModel)]="instructorId"><option value="">Toute l’équipe</option>@for (option of instructors(); track option.value) {<option [ngValue]="option.value">{{ option.label }}</option>}</select></div>
      <div class="field"><label for="planning-course">Cours</label><select id="planning-course" name="course" [(ngModel)]="courseId"><option value="">Tous les cours</option>@for (option of courses(); track option.value) {<option [ngValue]="option.value">{{ option.label }}</option>}</select></div>
      <div class="field"><label for="planning-status">État</label><select id="planning-status" name="status" [(ngModel)]="status"><option value="">Tous les créneaux</option><option value="OPEN">Disponibles</option><option value="FULL">Complets</option><option value="CANCELLED">Annulés</option></select></div><button class="btn secondary filter-submit" type="submit" [disabled]="loading()">Afficher</button>
    </form>
    @if (lookupError()) {<p class="notice error" role="alert">{{ lookupError() }} <button class="text-button" (click)="loadChoices()">Réessayer</button></p>}
    <div class="calendar-legend"><span><i class="legend-dot open"></i>Ouvert</span><span><i class="legend-dot full"></i>Complet</span><span><i class="legend-dot cancelled"></i>Annulé</span><span class="muted">{{ slots().length }} créneau{{ slots().length > 1 ? 'x' : '' }} · heures locales de la station</span></div>
    <app-state [loading]="loading()" [error]="error()" [empty]="!slots().length" title="Le planning est libre sur cette période" description="Ajoutez les disponibilités des moniteurs, puis créez les premiers créneaux." (retry)="load()"/>
    @if (!loading() && !error() && slots().length) {
      @if (view() === 'week') {
        <div class="week-scroll" tabindex="0" aria-label="Planning hebdomadaire, défilement horizontal"><div class="week-board">@for (date of days(); track date) {<section class="day-lane" [class.is-today]="date === currentDay"><header><span>{{ weekday(date) }}</span><strong>{{ date.slice(8) }}</strong></header><div class="lane-body">@for (slot of onDay(date); track slot.id) {<button class="slot-card" [attr.data-state]="slot.status" (click)="selectSlot(slot)"><span class="slot-time">{{ time(slot.startTime) }} — {{ time(slot.endTime) }}</span><strong>{{ courseName(slot.course) }}</strong><span>{{ person(slot.instructor) }}</span><footer><app-status [value]="slot.status"/><small>{{ slot.maxCapacity }} place{{ slot.maxCapacity > 1 ? 's' : '' }} max.</small></footer></button>} @empty {<p class="lane-empty">Aucun créneau</p>}</div></section>}</div></div>
      } @else if (view() === 'day') {
        <div class="day-agenda">@for (slot of slots(); track slot.id) {<button class="agenda-row" (click)="selectSlot(slot)"><div class="agenda-time"><strong>{{ time(slot.startTime) }}</strong><small>{{ time(slot.endTime) }}</small></div><div><strong>{{ courseName(slot.course) }}</strong><p>{{ person(slot.instructor) }}</p></div><span>{{ slot.maxCapacity }} places max.</span><app-status [value]="slot.status"/><app-icon name="chevron"/></button>}</div>
      } @else {<app-data-table [rows]="slots()" [columns]="slotColumns" caption="Créneaux de la période" (select)="selectEntity($event)"/>}
    }
  </section>
  @if (selected(); as slot) {
    <section class="panel selected-slot" id="selected-slot"><div><p class="eyebrow">Créneau #{{ slot.id }}</p><h2>{{ courseName(slot.course) }}</h2><p>{{ date(slot.startTime, true) }} → {{ time(slot.endTime) }} · {{ person(slot.instructor) }} · {{ slot.maxCapacity }} places maximum</p><app-status [value]="slot.status"/></div><div class="actions">@if (slot.status === 'OPEN' && future(slot)) {<button class="btn" (click)="register(slot)">Inscrire un skieur</button>}@if (slot.status !== 'CANCELLED' && future(slot)) {<button class="btn secondary" (click)="slotForm(slot)">Modifier</button><button class="btn secondary danger-text" (click)="cancel(slot)">Annuler le créneau</button>}<button class="icon-btn" aria-label="Fermer le détail du créneau" (click)="selected.set(null)"><app-icon name="close"/></button></div></section>
  }
  @if (instructorId) {<section class="panel"><div class="panel-heading"><h2>Disponibilités du moniteur sélectionné</h2></div><app-state [loading]="availabilityLoading()" [error]="availabilityError()" [empty]="!availabilityRows().length" title="Aucune disponibilité enregistrée" description="Ajoutez une plage récurrente pour pouvoir créer des créneaux." (retry)="loadAvailability()"/>@if (!availabilityLoading() && !availabilityError() && availabilityRows().length) {<app-data-table [rows]="availabilityRows()" [columns]="availabilityColumns" [interactive]="false" caption="Disponibilités du moniteur"/>}</section>}
` })
export class PlanningComponent implements OnInit {
  private api = inject(ApiClient); private planning = inject(PlanningApi); private lookup = inject(LookupService); private dialogs = inject(FormDialogService); private route = inject(ActivatedRoute);
  readonly view = signal<'week' | 'day' | 'list'>('week'); readonly slots = signal<PlanningSlot[]>([]); readonly selected = signal<PlanningSlot | null>(null);
  readonly loading = signal(false); readonly error = signal(''); readonly lookupError = signal('');
  readonly instructors = signal<Option[]>([]); readonly courses = signal<Option[]>([]);
  readonly availabilityRows = signal<Availability[]>([]); readonly availabilityLoading = signal(false); readonly availabilityError = signal('');
  readonly modes = [{ value: 'day' as const, label: 'Jour' }, { value: 'week' as const, label: 'Semaine' }, { value: 'list' as const, label: 'Liste' }];
  anchor = today(); listTo = today(30); instructorId: number | string = ''; courseId: number | string = ''; status = ''; readonly currentDay = today();
  private rangeTick = signal(0); private generation = 0;
  readonly days = computed(() => { this.rangeTick(); const date = new Date(`${this.anchor}T12:00:00`); const dow = (date.getDay() + 6) % 7; date.setDate(date.getDate() - dow); return Array.from({ length: 7 }, (_, i) => { const d = new Date(date); d.setDate(d.getDate() + i); return localDate(d); }); });
  readonly heading = computed(() => { this.rangeTick(); return this.view() === 'week' ? `Semaine du ${day(this.days()[0])}` : this.view() === 'day' ? day(this.anchor) : `${day(this.anchor)} — ${day(this.listTo)}`; });
  readonly date = day; readonly person = person; readonly courseName = courseName;
  readonly slotColumns: Column[] = [{ key: 'startTime', label: 'Début', format: 'datetime' }, { key: 'endTime', label: 'Fin', format: 'datetime' }, { key: 'course', label: 'Cours', format: 'course' }, { key: 'instructor', label: 'Moniteur', format: 'person' }, { key: 'maxCapacity', label: 'Places max.' }, { key: 'status', label: 'État', format: 'badge' }];
  readonly availabilityColumns: Column[] = [{ key: 'dayOfWeek', label: 'Jour' }, { key: 'startTime', label: 'De' }, { key: 'endTime', label: 'À' }, { key: 'validFrom', label: 'Valide du', format: 'date' }, { key: 'validUntil', label: 'Au', format: 'date' }];
  ngOnInit() { this.instructorId = Number(this.route.snapshot.queryParamMap.get('instructorId')) || ''; this.courseId = Number(this.route.snapshot.queryParamMap.get('courseId')) || ''; this.loadChoices(); this.load(); }
  async loadChoices() { try { const [instructors, courses] = await Promise.all([firstValueFrom(this.lookup.options('instructors')), firstValueFrom(this.lookup.options('courses'))]); this.instructors.set(instructors); this.courses.set(courses); this.lookupError.set(''); } catch (error) { this.lookupError.set(apiError(error)); } }
  changeView(view: 'week' | 'day' | 'list') { this.view.set(view); this.load(); }
  goToday() { this.anchor = today(); this.listTo = today(30); this.load(); }
  move(direction: number) { const d = new Date(`${this.anchor}T12:00:00`); d.setDate(d.getDate() + direction * (this.view() === 'day' ? 1 : this.view() === 'week' ? 7 : 30)); this.anchor = localDate(d); if (this.view() === 'list') { d.setDate(d.getDate() + 30); this.listTo = localDate(d); } this.load(); }
  async load() {
    const generation = ++this.generation; this.rangeTick.update(n => n + 1); this.loading.set(true); this.error.set(''); this.selected.set(null);
    try {
      if (!this.anchor || (this.view() === 'list' && (!this.listTo || this.anchor > this.listTo))) throw new Error('Choisissez une période valide.');
      const start = this.view() === 'week' ? this.days()[0] : this.anchor, end = this.view() === 'week' ? this.days()[6] : this.view() === 'day' ? this.anchor : this.listTo;
      const rows = await firstValueFrom(this.planning.slots({ ...dateRange(start, end), courseId: this.courseId, ...(this.status !== 'OPEN' ? { instructorId: this.instructorId } : {}) }, this.status === 'OPEN'));
      if (generation === this.generation) this.slots.set(rows.filter(slot => (!this.status || slot.status === this.status) && (!this.instructorId || slot.instructor.numInstructor === Number(this.instructorId))).sort((a, b) => a.startTime.localeCompare(b.startTime)));
    } catch (error) { if (generation === this.generation) this.error.set(apiError(error)); }
    finally { if (generation === this.generation) this.loading.set(false); }
    this.loadAvailability();
  }
  async loadAvailability() { if (!this.instructorId) { this.availabilityRows.set([]); return; } this.availabilityLoading.set(true); this.availabilityError.set(''); try { this.availabilityRows.set(await firstValueFrom(this.planning.availability(Number(this.instructorId)))); } catch (error) { this.availabilityError.set(apiError(error)); } finally { this.availabilityLoading.set(false); } }
  onDay(date: string) { return this.slots().filter(slot => slot.startTime.startsWith(date)); }
  weekday(date: string) { return new Intl.DateTimeFormat('fr-FR', { weekday: 'short' }).format(new Date(`${date}T12:00:00`)); }
  time(value: string) { return value.slice(11, 16); }
  future(slot: PlanningSlot) { return new Date(slot.startTime).getTime() > Date.now(); }
  selectSlot(slot: PlanningSlot) { this.selected.set(slot); setTimeout(() => document.getElementById('selected-slot')?.scrollIntoView({ behavior: window.matchMedia('(prefers-reduced-motion: reduce)').matches ? 'instant' : 'smooth', block: 'nearest' })); }
  selectEntity(row: Entity) { const slot = this.slots().find(slot => slot.id === row['id']); if (slot) this.selectSlot(slot); }
  slotForm(slot?: PlanningSlot) {
    const fields: Field[] = [{ key: 'courseId', label: 'Cours', type: 'select', source: 'courses' }, { key: 'instructorId', label: 'Moniteur associé au cours', type: 'select', source: 'instructors' }, { key: 'startTime', label: 'Début du créneau', type: 'datetime-local', min: `${today()}T00:00` }, { key: 'endTime', label: 'Fin du créneau', type: 'datetime-local', min: `${today()}T00:00` }, { key: 'maxCapacity', label: 'Capacité maximale', type: 'number', min: 1, max: 6, default: 6, hint: '1 place en individuel ; de 1 à 6 en collectif.' }];
    this.dialogs.show({ title: slot ? 'Modifier le créneau' : 'Créer un créneau', description: 'Le moniteur doit être associé au cours et disponible sur toute la plage. Un créneau avec inscriptions ne peut pas être déplacé.', fields, initial: slot ? { courseId: slot.course.numCourse, instructorId: slot.instructor.numInstructor, startTime: slot.startTime.slice(0, 16), endTime: slot.endTime.slice(0, 16), maxCapacity: slot.maxCapacity } : { instructorId: this.instructorId || '', courseId: this.courseId || '' }, validate: values => String(values['startTime']) >= String(values['endTime']) ? 'La fin doit être postérieure au début.' : String(values['startTime']).slice(0, 10) !== String(values['endTime']).slice(0, 10) ? 'Le créneau doit se dérouler sur une seule journée.' : undefined,
      submit: async values => { await firstValueFrom(slot ? this.api.put(`/planning/slots/${slot.id}`, values) : this.api.post('/planning/slots', values)); this.anchor = String(values['startTime']).slice(0, 10); if (this.view() === 'list' && this.anchor > this.listTo) this.listTo = this.anchor; await this.load(); },
    });
  }
  addAvailability() { this.dialogs.show({ title: 'Ajouter une disponibilité', fields: [{ key: 'instructorId', label: 'Moniteur', type: 'select', source: 'instructors' }, ...availabilityFields()], initial: { instructorId: this.instructorId || '' }, validate: availabilityValidation, submit: async values => { const { instructorId, ...body } = values; await firstValueFrom(this.planning.addAvailability(Number(instructorId), body)); this.instructorId = Number(instructorId); await this.load(); } }); }
  register(slot: PlanningSlot) { this.dialogs.show({ title: 'Inscrire un skieur', description: `${courseName(slot.course)} · ${day(slot.startTime, true)}. L’âge, les conflits et la capacité seront contrôlés.`, fields: [{ key: 'skierId', label: 'Skieur', type: 'select', source: 'skiers' }], submit: async values => { await firstValueFrom(this.api.post(`/registration/skier/${values['skierId']}/slot/${slot.id}`)); await this.load(); } }); }
  cancel(slot: PlanningSlot) { this.dialogs.show({ title: 'Annuler le créneau', description: 'Confirmer l’annulation de ce créneau et de toutes ses inscriptions ? Les historiques seront conservés.', danger: true, submitLabel: 'Confirmer l’annulation', fields: [], submit: async () => { await firstValueFrom(this.api.delete(`/planning/slots/${slot.id}`)); await this.load(); } }); }
}
