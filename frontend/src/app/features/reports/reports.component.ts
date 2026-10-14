import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ReportApi } from './report-api';
import { CourseApi } from '../courses/course-api';
import { Revenue, SubscriptionReport, Occupancy, WeeklyRegistration, Course } from '../../shared/models/domain';
import { IconComponent } from '../../shared/components/icon.component';
import { StateComponent } from '../../shared/components/state.component';
import { dateRange, today, money, day, courseName } from '../../shared/utilities/format';
import { apiError } from '../../core/error-handling/api-error';
@Component({ selector: 'app-reports', standalone: true, imports: [FormsModule, IconComponent, StateComponent], template: `
  <section class="page-intro"><div><p class="eyebrow">Pilotage de la station</p><h1>Rapports</h1><p>Comprendre l’activité, avec des chiffres qui viennent du terrain.</p></div></section>
  <form class="panel filter-bar report-filter" (ngSubmit)="load()"><div class="field"><label for="report-from">Du</label><input id="report-from" name="from" type="date" [(ngModel)]="from" required/></div><div class="field"><label for="report-to">Au (inclus)</label><input id="report-to" name="to" type="date" [(ngModel)]="to" required/></div><div class="field"><label for="report-asof">Abonnements à la date du</label><input id="report-asof" name="asOf" type="date" [(ngModel)]="asOf" required/></div><button class="btn filter-submit" type="submit"><app-icon name="refresh"/>Actualiser les rapports</button></form>
  @if (validationError()) {<p class="notice error" role="alert">{{ validationError() }}</p>}
  <section class="panel"><div class="panel-heading"><div><p class="eyebrow">Flux financiers réels</p><h2>Encaissements & remboursements</h2></div><span class="muted">{{ date(from) }} → {{ date(to) }}</span></div><app-state [loading]="loading()" [error]="errors()['revenue'] || ''" (retry)="load()"/>
    @if (revenue(); as report) {
      <div class="financial-totals"><div><span>Revenu encaissé</span><strong>{{ money(report.collected) }}</strong></div><div><span>Remboursements</span><strong>{{ money(report.refunds) }}</strong></div><div class="net-total"><span>Revenu net</span><strong>{{ money(report.net) }}</strong></div></div>
      <p class="context-note">Encaissements à leur date de confirmation, remboursements à leur propre date. Les prix théoriques des abonnements ne sont pas inclus.</p>
      <app-state [empty]="!report.months.length" title="Aucun mouvement financier" description="Aucun encaissement ni remboursement n’a été enregistré sur la période."/>
      @if (report.months.length) {<figure class="revenue-chart"><figcaption><span><i class="legend-dot open"></i>Encaissé</span><span><i class="legend-dot refund"></i>Remboursé</span></figcaption>@for (month of report.months; track month.month) {<div class="chart-row"><strong>{{ monthName(month.month) }}</strong><div class="bar-group"><div class="chart-bar collected" [style.width.%]="bar(month.collected)" role="img" [attr.aria-label]="'Encaissé : ' + money(month.collected)"></div><div class="chart-bar refunded" [style.width.%]="bar(month.refunds)" role="img" [attr.aria-label]="'Remboursé : ' + money(month.refunds)"></div></div><span>{{ money(month.net) }}<small>net</small></span></div>}</figure>
      <div class="table-scroll" tabindex="0"><table><caption class="sr-only">Revenus mensuels détaillés</caption><thead><tr><th>Mois</th><th>Encaissé</th><th>Remboursé</th><th>Net</th></tr></thead><tbody>@for (month of report.months; track month.month) {<tr><td>{{ monthName(month.month) }}</td><td>{{ money(month.collected) }}</td><td>{{ money(month.refunds) }}</td><td>{{ money(month.net) }}</td></tr>}</tbody></table></div>}
    }
  </section>
  <section class="panel"><div class="panel-heading"><div><p class="eyebrow">Forfaits & fidélisation</p><h2>État des abonnements</h2></div><span class="muted">Au {{ date(asOf) }}</span></div><app-state [loading]="loading()" [error]="errors()['subscriptions'] || ''" (retry)="load()"/>
    @if (subscriptions(); as report) {<div class="subscription-report"><div><span class="status" data-state="ACTIVE">Actifs</span><strong>{{ report.active }}</strong></div><div><span class="status" data-state="EXPIRING">À échéance sous 30 jours</span><strong>{{ report.expiringWithin30Days }}</strong><small>Inclus dans les actifs</small></div><div><span class="status" data-state="EXPIRED">Expirés</span><strong>{{ report.expired }}</strong></div><div><span class="status" data-state="UPCOMING">À venir</span><strong>{{ report.upcoming }}</strong></div></div>}
  </section>
  <section class="panel"><div class="panel-heading"><div><p class="eyebrow">Cours collectifs</p><h2>Taux de remplissage</h2></div></div><p class="context-note">Inscriptions non annulées / capacité cumulée des créneaux commençant dans la période. Créneaux annulés exclus.</p><app-state [loading]="loading()" [error]="errors()['occupancy'] || ''" [empty]="!occupancy().length" title="Aucun cours collectif à analyser" description="Créez des cours collectifs et planifiez leurs créneaux." (retry)="load()"/>
    @if (!loading() && !errors()['occupancy'] && occupancy().length) {<div class="occupancy-list">@for (row of occupancy(); track row.courseId) {<div><header><strong>{{ name(row.courseId) }}</strong><b>{{ row.occupancyPercent }} %</b></header><meter min="0" max="100" [value]="row.occupancyPercent" [attr.aria-label]="name(row.courseId) + ' : ' + row.occupancyPercent + ' %'"></meter><p>{{ row.registrations }} inscription{{ row.registrations > 1 ? 's' : '' }} / {{ row.capacity }} places · {{ row.slotCount }} créneau{{ row.slotCount > 1 ? 'x' : '' }}</p></div>}</div>}
  </section>
  <section class="panel"><div class="panel-heading"><div><p class="eyebrow">Participation</p><h2>Inscriptions par semaine</h2></div></div><app-state [loading]="loading()" [error]="errors()['registrations'] || ''" [empty]="!registrations().length" title="Aucune inscription datée sur cette période" description="Les inscriptions historiques sans créneau daté ne sont pas incluses." (retry)="load()"/>
    @if (!loading() && !errors()['registrations'] && registrations().length) {<div class="table-scroll" tabindex="0"><table><caption class="sr-only">Inscriptions non annulées par cours et semaine ISO</caption><thead><tr><th>Cours</th><th>Année ISO</th><th>Semaine</th><th>Inscriptions</th></tr></thead><tbody>@for (row of registrations(); track row.courseId + '-' + row.weekBasedYear + '-' + row.week) {<tr><td>{{ name(row.courseId) }}</td><td>{{ row.weekBasedYear }}</td><td>S{{ row.week }}</td><td><strong>{{ row.registrations }}</strong></td></tr>}</tbody></table></div>}
  </section>
` })
export class ReportsComponent implements OnInit {
  private reports = inject(ReportApi); private courseApi = inject(CourseApi);
  from = today(-30); to = today(); asOf = today(); readonly loading = signal(false); readonly validationError = signal(''); readonly errors = signal<Record<string, string>>({});
  readonly revenue = signal<Revenue | null>(null); readonly subscriptions = signal<SubscriptionReport | null>(null); readonly occupancy = signal<Occupancy[]>([]); readonly registrations = signal<WeeklyRegistration[]>([]); private courses = signal<Course[]>([]); private generation = 0;
  readonly money = money; readonly date = day;
  ngOnInit() { this.load(); }
  name(id: number) { return courseName(this.courses().find(course => course.numCourse === id)) === '—' ? `Cours #${id}` : courseName(this.courses().find(course => course.numCourse === id)); }
  monthName(value: string) { return new Intl.DateTimeFormat('fr-FR', { month: 'long', year: 'numeric' }).format(new Date(`${value}-01T12:00:00`)); }
  bar(amount: number) { const maximum = Math.max(1, ...this.revenue()!.months.flatMap(month => [month.collected, month.refunds])); return amount / maximum * 100; }
  async load() {
    if (!this.from || !this.to || !this.asOf || this.from > this.to) { this.validationError.set('Vérifiez les dates de la période et de référence.'); return; }
    this.validationError.set(''); this.loading.set(true); this.errors.set({}); this.revenue.set(null); this.subscriptions.set(null); this.occupancy.set([]); this.registrations.set([]);
    const generation = ++this.generation, range = dateRange(this.from, this.to);
    const requests = [
      firstValueFrom(this.reports.revenue(range)).then(data => { if (generation === this.generation) this.revenue.set(data); }).catch(error => this.setError('revenue', error, generation)),
      firstValueFrom(this.reports.subscriptions(this.asOf)).then(data => { if (generation === this.generation) this.subscriptions.set(data); }).catch(error => this.setError('subscriptions', error, generation)),
      firstValueFrom(this.reports.occupancy(range)).then(data => { if (generation === this.generation) this.occupancy.set(data); }).catch(error => this.setError('occupancy', error, generation)),
      firstValueFrom(this.reports.registrations(range)).then(data => { if (generation === this.generation) this.registrations.set(data); }).catch(error => this.setError('registrations', error, generation)),
      firstValueFrom(this.courseApi.all()).then(data => { if (generation === this.generation) this.courses.set(data); }).catch(() => { /* Course identifiers remain visible if names are unavailable. */ }),
    ];
    await Promise.all(requests); if (generation === this.generation) this.loading.set(false);
  }
  private setError(key: string, error: unknown, generation: number) { if (generation === this.generation) this.errors.update(errors => ({ ...errors, [key]: apiError(error) })); }
}
