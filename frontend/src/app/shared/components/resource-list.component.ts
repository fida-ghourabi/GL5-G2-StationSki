import { Component, OnInit, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { ResourceGateway, Filters } from '../../core/api/resource-gateway';
import { LookupService } from '../../core/api/lookup.service';
import { apiError } from '../../core/error-handling/api-error';
import { Entity } from '../models/domain';
import { ResourceDefinition } from '../models/resource-definition';
import { FormDialogService, Field, Option } from './form-dialog.service';
import { IconComponent } from './icon.component';
import { StateComponent } from './state.component';
import { DataTableComponent } from './data-table.component';

@Component({ selector: 'app-resource-list', standalone: true, imports: [FormsModule, IconComponent, StateComponent, DataTableComponent], template: `
  <section class="page-intro"><div><p class="eyebrow">Gestion de la station</p><h1>{{ def.title }}</h1><p>{{ def.description }}</p></div><button class="btn" (click)="create()"><app-icon name="plus"/>{{ createLabel }}</button></section>
  <section class="panel">
    <form class="filter-bar" (ngSubmit)="search()">
      @for (field of def.filters; track field.key) {
        <div class="field"><label [for]="'filter-' + field.key">{{ field.label }}</label>
          @if (field.type === 'select') {<select [id]="'filter-' + field.key" [name]="field.key" [(ngModel)]="filters[field.key]"><option value="">Tous les choix</option>@for (option of choices(field); track option.value) {<option [ngValue]="option.value">{{ option.label }}</option>}</select>}
          @else {<input [id]="'filter-' + field.key" [name]="field.key" [type]="field.type || 'search'" [(ngModel)]="filters[field.key]" [attr.min]="field.min ?? null" [attr.max]="field.max ?? null"/>}
        </div>
      }
      <button class="btn secondary filter-submit" type="submit" [disabled]="loading()"><app-icon name="search"/>Rechercher</button><button type="button" class="btn quiet filter-submit" (click)="reset()">Réinitialiser</button>
    </form>
    @if (lookupError()) {<p class="notice error" role="alert">{{ lookupError() }} <button class="text-button" (click)="loadChoices()">Recharger les choix</button></p>}
    @if (def.note) {<p class="context-note"><app-icon name="alert"/>{{ def.note }}</p>}
    @if (filters['skierId'] && def.key === 'registrations') {<p class="notice">Historique complet du skieur sélectionné : les dates de période ne sont pas appliquées.</p>}
    <div class="panel-heading"><div><h2>{{ def.key === 'payments' ? 'Historique des paiements' : 'Répertoire' }}</h2><span class="muted">{{ loading() ? 'Actualisation…' : error() ? 'Données indisponibles' : total() + ' résultat' + (total() > 1 ? 's' : '') }}</span></div><button class="icon-btn" aria-label="Actualiser la liste" (click)="load()" [disabled]="loading()"><app-icon name="refresh"/></button></div>
    <app-state [loading]="loading()" [error]="error()" [empty]="!rows().length" [icon]="def.icon" [title]="def.key === 'payments' && !filters['skierId'] && !filters['subscriptionId'] ? 'Quel historique souhaitez-vous consulter ?' : 'Aucun résultat pour cette sélection'" [description]="def.key === 'payments' ? 'Sélectionnez un skieur ou un abonnement, ou enregistrez un premier paiement.' : 'Ajustez les filtres ou utilisez le bouton de création pour ajouter un élément.'" (retry)="load()"/>
    @if (!loading() && !error() && rows().length) {<app-data-table [rows]="rows()" [columns]="def.columns" [caption]="def.title" (select)="open($event)"/>}
    @if (def.key === 'skiers' && total() > 20) {<footer class="pagination"><span>Page {{ page() + 1 }} sur {{ pageCount }}</span><button class="btn small secondary" [disabled]="page() === 0 || loading()" (click)="changePage(-1)">Précédente</button><button class="btn small secondary" [disabled]="page() + 1 >= pageCount || loading()" (click)="changePage(1)">Suivante</button></footer>}
  </section>
` })
export class ResourceListComponent implements OnInit {
  readonly def = inject(ActivatedRoute).snapshot.data['resource'] as ResourceDefinition;
  private gateway = inject(ResourceGateway); private lookup = inject(LookupService); private dialogs = inject(FormDialogService); private router = inject(Router);
  readonly rows = signal<Entity[]>([]); readonly total = signal(0); readonly loading = signal(false); readonly error = signal(''); readonly lookupError = signal(''); readonly page = signal(0);
  readonly lookupOptions = signal<Record<string, Option[]>>({}); filters: Filters = {}; private generation = 0;
  get createLabel() { return this.def.key === 'registrations' ? 'Inscrire un skieur' : this.def.key === 'payments' ? 'Enregistrer un paiement' : `Ajouter ${this.def.singular === 'piste' ? 'une ' : 'un '}${this.def.singular}`; }
  get pageCount() { return Math.ceil(this.total() / 20); }
  ngOnInit() { this.defaults(); const query = injectQuery(this.router.url); for (const key of ['skierId', 'subscriptionId', 'courseId']) { if (query[key] && this.def.filters.some(field => field.key === key)) this.filters[key] = Number(query[key]); } this.loadChoices(); this.load(); }
  defaults() { this.filters = Object.fromEntries(this.def.filters.map(field => [field.key, field.default as string ?? ''])); }
  choices(field: Field) { return field.options ?? this.lookupOptions()[field.source ?? ''] ?? []; }
  async loadChoices() {
    this.lookupError.set('');
    try { const sources = [...new Set(this.def.filters.map(f => f.source).filter(s => s !== undefined))]; const entries = await Promise.all(sources.map(async source => [source, await firstValueFrom(this.lookup.options(source))])); this.lookupOptions.set(Object.fromEntries(entries)); }
    catch (error) { this.lookupError.set(apiError(error)); }
  }
  search() { this.page.set(0); this.load(); }
  reset() { this.defaults(); this.search(); }
  changePage(delta: number) { this.page.update(page => page + delta); this.load(); }
  async load() {
    const generation = ++this.generation; this.loading.set(true); this.error.set('');
    try { const result = await this.gateway.list(this.def, { ...this.filters }, this.page()); if (generation === this.generation) { this.rows.set(result.rows); this.total.set(result.total); } }
    catch (error) { if (generation === this.generation) this.error.set(apiError(error)); }
    finally { if (generation === this.generation) this.loading.set(false); }
  }
  open(row: Entity) { this.router.navigate(['/', this.def.key, row[this.def.idKey]], { queryParams: this.def.key === 'registrations' ? { skierId: row['skierId'] } : {} }); }
  create() {
    this.dialogs.show({ title: this.createLabel, fields: this.def.fields, description: this.def.note,
      submit: async values => {
        const created = await this.gateway.save(this.def, values);
        this.open(created);
      },
    });
  }
}
function injectQuery(url: string): Record<string, string> { return Object.fromEntries(new URLSearchParams(url.split('?')[1] ?? '')); }
