import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { Observable, firstValueFrom } from 'rxjs';
import { SkierApi } from '../skiers/skier-api';
import { CourseApi } from '../courses/course-api';
import { PisteApi } from '../pistes/piste-api';
import { PlanningApi } from '../planning/planning-api';
import { RegistrationApi } from '../registrations/registration-api';
import { PlanningSlot } from '../../shared/models/domain';
import { IconComponent } from '../../shared/components/icon.component';
import { StatusComponent } from '../../shared/components/status.component';
import { StateComponent } from '../../shared/components/state.component';
import { apiError } from '../../core/error-handling/api-error';
import { dateRange, today, person, courseName } from '../../shared/utilities/format';
interface Metric { key: string; title: string; value: number | null; unit?: string; icon: string; note: string; link: string; error: string; loading: boolean }
@Component({ selector: 'app-dashboard', standalone: true, imports: [RouterLink, FormsModule, IconComponent, StatusComponent, StateComponent], template: `
  <section class="page-intro"><div><p class="eyebrow">Le point sur votre station</p><h1>Bonjour, l’équipe.</h1><p>Une vue d’ensemble pour préparer la journée et garder le cap.</p></div><a class="btn" routerLink="/planning"><app-icon name="calendar"/>Ouvrir le planning</a></section>
  <section class="station-brief"><div class="brief-copy"><span class="eyebrow">Carnet d’exploitation</span><h2>Tout commence<br>par une station bien organisée.</h2><p>{{ currentDate }}<br>Les chiffres ci-dessous proviennent des données de votre station.</p><a routerLink="/registrations">Gérer les inscriptions <app-icon name="arrow"/></a></div><div class="mountain-art" aria-hidden="true"><svg viewBox="0 0 620 240" fill="none"><path d="M-30 230 85 145l45 25L255 12l75 93 38-35 126 142 45-52 110 83" fill="#ffffff0b" stroke="#b1e0e2" stroke-width="2"/><path d="m160 134 95-122 75 93-48-22-25 29-29-40-30 58Z" fill="#d1eaef"/><path d="m299 154 69-84 126 142-89-60-23 13-22-33Z" fill="#ffffff19"/><path d="M-20 238c125-90 240 10 357-40s175-17 290 40M-20 215c125-90 240 10 357-40s175-17 290 40M-20 192c125-90 240 10 357-40s175-17 290 40" stroke="#6cb9c0" stroke-opacity=".25"/></svg><span>BORÉAL / POSTE DE STATION</span></div></section>
  <form class="period-line" (ngSubmit)="load()"><div><h2>Votre activité, en un regard</h2><p class="muted">Choisissez la période des inscriptions à consulter.</p></div><div class="actions"><label class="sr-only" for="dashboard-from">Du</label><input id="dashboard-from" type="date" name="from" [(ngModel)]="from" required/><span>au</span><label class="sr-only" for="dashboard-to">Au inclus</label><input id="dashboard-to" type="date" name="to" [(ngModel)]="to" required/><button class="btn secondary" type="submit"><app-icon name="refresh"/>Actualiser</button></div></form>
  @if (periodError()) {<p class="notice error" role="alert">{{ periodError() }}</p>}
  <section class="metric-strip" aria-label="Indicateurs de la station">@for (metric of metrics(); track metric.key) {<a class="metric-tile" [routerLink]="metric.link"><div><span class="metric-label">{{ metric.title }}</span><app-icon [name]="metric.icon"/></div><strong [class.metric-unavailable]="metric.error">{{ metric.loading ? '…' : metric.error ? 'Indisponible' : metric.value }}<small>{{ metric.error || metric.loading ? '' : metric.unit }}</small></strong><p>{{ metric.error || metric.note }}</p></a>}</section>
  <div class="dashboard-columns"><section class="panel"><div class="panel-heading"><div><p class="eyebrow">Sur le terrain</p><h2>Les prochains départs</h2></div><a routerLink="/planning" class="text-link">Tout le planning <app-icon name="arrow"/></a></div><p class="context-note">Créneaux non annulés des 7 prochains jours, à partir de maintenant.</p><app-state [loading]="slotsLoading()" [error]="slotsError()" [empty]="!slots().length" title="Aucun départ prévu" description="Les prochains cours apparaîtront ici après leur planification." (retry)="load()"/>
    @if (!slotsLoading() && !slotsError()) {<p class="context-note">{{ slots().length }} créneau{{ slots().length > 1 ? 'x' : '' }} à venir · les 5 prochains départs ci-dessous.</p><div class="departure-list">@for (slot of slots().slice(0, 5); track slot.id) {<a routerLink="/planning" [queryParams]="{courseId:slot.course.numCourse}" class="departure"><span class="departure-time">{{ time(slot.startTime) }}<small>{{ shortDay(slot.startTime) }}</small></span><div><strong>{{ courseName(slot.course) }}</strong><p>{{ person(slot.instructor) }}</p></div><app-status [value]="slot.status"/></a>}</div>}
  </section><section class="panel action-panel"><p class="eyebrow">Accueil de la station</p><h2>Les actions du quotidien</h2><a routerLink="/skiers" class="btn secondary"><app-icon name="people"/>Accueillir un skieur</a><a routerLink="/registrations" class="btn secondary"><app-icon name="clipboard"/>Gérer les inscriptions</a><a routerLink="/subscriptions" class="btn secondary"><app-icon name="ticket"/>Consulter les abonnements</a><a routerLink="/payments" class="btn secondary"><app-icon name="wallet"/>Enregistrer un paiement</a><hr><p class="muted small-copy">Retrouvez les affectations et les historiques dans le dossier de chaque skieur.</p></section></div>
` })
export class DashboardComponent implements OnInit {
  private skiers = inject(SkierApi); private courses = inject(CourseApi); private pistes = inject(PisteApi); private planning = inject(PlanningApi); private registrationApi = inject(RegistrationApi);
  from = today(-30); to = today(); readonly currentDate = new Intl.DateTimeFormat('fr-FR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date());
  readonly periodError = signal(''); readonly slots = signal<PlanningSlot[]>([]); readonly slotsLoading = signal(false); readonly slotsError = signal('');
  readonly metrics = signal<Metric[]>([
    { key: 'skiers', title: 'Skieurs', value: null, icon: 'people', note: 'Au répertoire de la station', link: '/skiers', error: '', loading: true },
    { key: 'courses', title: 'Cours', value: null, icon: 'book', note: 'Toutes disciplines confondues', link: '/courses', error: '', loading: true },
    { key: 'registrations', title: 'Inscriptions', value: null, icon: 'clipboard', note: 'Non annulées, datées dans la période', link: '/registrations', error: '', loading: true },
    { key: 'pistes', title: 'Pistes', value: null, icon: 'piste', note: 'Parcours enregistrés', link: '/pistes', error: '', loading: true },
  ]);
  readonly person = person; readonly courseName = courseName; private generation = 0;
  ngOnInit() { this.load(); }
  time(value: string) { return value.slice(11, 16); }
  shortDay(value: string) { return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short' }).format(new Date(value)); }
  private async metric<T>(key: string, request: Observable<T>, count: (data: T) => number, generation: number) {
    this.metrics.update(rows => rows.map(row => row.key === key ? { ...row, loading: true, error: '' } : row));
    try { const data = await firstValueFrom(request); if (generation === this.generation) this.metrics.update(rows => rows.map(row => row.key === key ? { ...row, value: count(data), loading: false } : row)); }
    catch (error) { if (generation === this.generation) this.metrics.update(rows => rows.map(row => row.key === key ? { ...row, value: null, error: apiError(error), loading: false } : row)); }
  }
  async load() {
    if (!this.from || !this.to || this.from > this.to) { this.periodError.set('Choisissez une période valide.'); return; }
    const generation = ++this.generation; this.periodError.set(''); const range = dateRange(this.from, this.to);
    this.metric('skiers', this.skiers.search({ page: 0, size: 1 }), data => data.totalElements, generation);
    this.metric('courses', this.courses.all(), rows => rows.length, generation); this.metric('pistes', this.pistes.all(), rows => rows.length, generation);
    this.metric('registrations', this.registrationApi.search(range), rows => rows.filter(row => !row.cancelled).length, generation);
    this.slotsLoading.set(true); this.slotsError.set('');
    try {
      const rows = await firstValueFrom(this.planning.slots(dateRange(today(), today(7))));
      if (generation === this.generation) this.slots.set(rows.filter(row => row.status !== 'CANCELLED' && new Date(row.startTime).getTime() >= Date.now()).sort((a, b) => a.startTime.localeCompare(b.startTime)));
    } catch (error) { if (generation === this.generation) this.slotsError.set(apiError(error)); }
    finally { if (generation === this.generation) this.slotsLoading.set(false); }
  }
}
