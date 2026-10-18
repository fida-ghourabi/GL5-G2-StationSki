import { Component, Input, Output, EventEmitter } from '@angular/core';
import { IconComponent } from './icon.component';
@Component({ selector: 'app-state', standalone: true, imports: [IconComponent], template: `
  @if (loading) {
    <div class="loading-state" role="status" aria-live="polite"><span class="spinner"></span><span>Chargement des données de la station…</span></div>
    <div class="skeleton-list" aria-hidden="true"><i></i><i></i><i></i></div>
  } @else if (error) {
    <div class="empty-state error-state" role="alert"><span class="state-icon"><app-icon name="alert"/></span><h3>Les données ne sont pas disponibles</h3><p>{{ error }}</p><button class="btn secondary" (click)="retry.emit()"><app-icon name="refresh"/>Réessayer</button></div>
  } @else if (empty) {
    <div class="empty-state"><span class="state-icon"><app-icon [name]="icon"/></span><h3>{{ title }}</h3><p>{{ description }}</p></div>
  }
` })
export class StateComponent {
  @Input() loading = false; @Input() error = ''; @Input() empty = false;
  @Input() title = 'Aucun résultat pour le moment'; @Input() description = 'Ajoutez un premier élément ou ajustez les filtres.'; @Input() icon = 'mountain';
  @Output() retry = new EventEmitter<void>();
}
