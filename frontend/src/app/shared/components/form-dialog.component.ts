import { Component, ElementRef, ViewChild, effect, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { firstValueFrom } from 'rxjs';
import { FormDialogService, Field, Option } from './form-dialog.service';
import { LookupService } from '../../core/api/lookup.service';
import { NotificationService } from '../../core/layout/notification.service';
import { apiError } from '../../core/error-handling/api-error';
import { Command } from '../models/domain';
import { IconComponent } from './icon.component';
@Component({ selector: 'app-form-dialog', standalone: true, imports: [ReactiveFormsModule, IconComponent], template: `
  <dialog #dialog class="form-dialog" aria-labelledby="form-dialog-title" (cancel)="onCancel($event)">
    @if (store.current(); as spec) {
      <form [formGroup]="form" (ngSubmit)="submit()" novalidate>
        <header class="dialog-header"><div><p class="eyebrow">{{ spec.danger ? 'Confirmation' : 'Gestion de la station' }}</p><h2 id="form-dialog-title">{{ spec.title }}</h2></div><button type="button" class="icon-btn" aria-label="Fermer" [disabled]="busy()" (click)="close()"><app-icon name="close"/></button></header>
        <div class="dialog-body"><p class="muted">{{ spec.description }}</p>
          @if (error()) {<div class="notice error" role="alert">{{ error() }}</div>}
          @if (loading()) {<p role="status" class="notice">Chargement des choix disponibles…</p>}
          <div class="form-grid">@for (field of spec.fields; track field.key) {
            <div class="field" [class.full]="field.full"><label [for]="'field-' + field.key">{{ field.label }} @if (field.required !== false) {<span aria-hidden="true">*</span>}</label>
              @if (field.type === 'select') {
                <select [id]="'field-' + field.key" [formControlName]="field.key" [attr.aria-invalid]="invalid(field)" [attr.aria-describedby]="'help-' + field.key">
                  <option value="">Sélectionner…</option>@for (option of choices(field); track option.value) {<option [ngValue]="option.value">{{ option.label }}</option>}
                </select>
              } @else if (field.type === 'textarea') {
                <textarea [id]="'field-' + field.key" [formControlName]="field.key" rows="3" maxlength="255" [attr.aria-invalid]="invalid(field)" [attr.aria-describedby]="'help-' + field.key"></textarea>
              } @else {
                <input [id]="'field-' + field.key" [type]="field.type || 'text'" [formControlName]="field.key" [attr.min]="field.min ?? null" [attr.max]="field.max ?? null" [attr.step]="field.step ?? (field.type === 'number' ? 1 : null)" [attr.maxlength]="field.type === 'text' || !field.type ? 255 : null" [attr.aria-invalid]="invalid(field)" [attr.aria-describedby]="'help-' + field.key"/>
              }
              <small [id]="'help-' + field.key" [class.field-error]="invalid(field)">{{ invalid(field) ? 'Vérifiez ce champ : une valeur valide est requise.' : field.hint || (field.source && choices(field).length === 0 && !loading() ? 'Aucun choix disponible. Créez d’abord la ressource associée.' : '') }}</small>
            </div>
          }</div>
        </div>
        <footer class="dialog-footer"><button type="button" class="btn secondary" [disabled]="busy()" (click)="close()">Annuler</button><button type="submit" class="btn" [class.danger]="spec.danger" [disabled]="busy() || loading() || lookupFailed()">@if (busy()) {<span class="spinner"></span>}{{ busy() ? 'Enregistrement…' : spec.submitLabel || 'Enregistrer' }}</button></footer>
      </form>
    }
  </dialog>
` })
export class FormDialogComponent {
  readonly store = inject(FormDialogService); private lookup = inject(LookupService); private notifications = inject(NotificationService);
  @ViewChild('dialog', { static: true }) dialog!: ElementRef<HTMLDialogElement>;
  form = new FormGroup<Record<string, FormControl>>({});
  readonly error = signal(''); readonly busy = signal(false); readonly loading = signal(false); readonly lookupFailed = signal(false);
  readonly options = signal<Record<string, Option[]>>({});
  private generation = 0;
  constructor() {
    effect(() => {
      const spec = this.store.current(); const version = ++this.generation;
      if (!spec) { this.dialog?.nativeElement.close(); return; }
      this.error.set(''); this.lookupFailed.set(false); this.options.set({});
      const controls: Record<string, FormControl> = {};
      spec.fields.forEach(f => {
        const validators = [];
        if (f.required !== false) validators.push(Validators.required);
        if (f.type === 'number') { if (typeof f.min === 'number') validators.push(Validators.min(f.min)); if (typeof f.max === 'number') validators.push(Validators.max(f.max)); }
        else validators.push(Validators.maxLength(255));
        controls[f.key] = new FormControl(spec.initial?.[f.key] ?? f.default ?? '', validators);
      });
      this.form = new FormGroup(controls);
      queueMicrotask(() => { if (version === this.generation && !this.dialog.nativeElement.open) this.dialog.nativeElement.showModal(); });
      const sources = [...new Set(spec.fields.map(f => f.source).filter(s => s !== undefined))];
      this.loading.set(sources.length > 0);
      Promise.all(sources.map(async source => [source, await firstValueFrom(this.lookup.options(source))] as const))
        .then(entries => { if (version === this.generation) this.options.set(Object.fromEntries(entries)); })
        .catch(error => { if (version === this.generation) { this.lookupFailed.set(true); this.error.set(apiError(error)); } })
        .finally(() => { if (version === this.generation) this.loading.set(false); });
    });
  }
  choices(field: Field) { return field.options ?? (field.source ? this.options()[field.source] ?? [] : []); }
  invalid(field: Field) { const control = this.form.controls[field.key]; return !!control && control.invalid && control.touched; }
  close() { if (!this.busy()) this.store.close(); }
  onCancel(event: Event) { event.preventDefault(); this.close(); }
  async submit() {
    const spec = this.store.current(); if (!spec || this.busy() || this.loading() || this.lookupFailed()) return;
    this.form.markAllAsTouched(); if (this.form.invalid) return;
    const values: Command = { ...this.form.getRawValue() };
    for (const field of spec.fields) {
      let value = values[field.key];
      if (typeof value === 'string') value = value.trim();
      if (field.required !== false && value === '') { this.error.set('Les champs obligatoires ne peuvent pas être vides.'); return; }
      if (field.type === 'number' && value !== '') {
        value = Number(value);
        if ((field.step ?? 1) === 1 && !Number.isInteger(value)) { this.error.set(`Le champ « ${field.label} » attend un nombre entier.`); return; }
        if (!Number.isFinite(value) || (field.step === 0.01 && Math.abs(Number(value) * 100 - Math.round(Number(value) * 100)) > 0.00001)) { this.error.set('Les montants acceptent au plus deux décimales.'); return; }
      }
      if ((field.type === 'date' || field.type === 'datetime-local') && value) {
        if ((typeof field.min === 'string' && String(value) < field.min) || (typeof field.max === 'string' && String(value) > field.max)) { this.error.set(`La date « ${field.label} » est hors des limites autorisées.`); return; }
      }
      if (field.type === 'datetime-local' && String(value).length === 16) value = `${value}:00`;
      if (field.type === 'time' && String(value).length === 5) value = `${value}:00`;
      if (field.required === false && value === '') delete values[field.key]; else values[field.key] = value;
    }
    const invalid = spec.validate?.(values); if (invalid) { this.error.set(invalid); return; }
    this.busy.set(true); this.error.set('');
    try { await spec.submit(values); this.store.close(); this.notifications.success(`${spec.title} : opération effectuée.`); }
    catch (error) { this.error.set(apiError(error)); }
    finally { this.busy.set(false); }
  }
}
