import { Component, Input, Output, EventEmitter } from '@angular/core';
import { Entity } from '../models/domain';
import { label, money, day, read, person, courseName, subscriptionStatus } from '../utilities/format';
import { StatusComponent } from './status.component';
import { IconComponent } from './icon.component';
export interface Column { key: string; label: string; format?: 'date' | 'datetime' | 'money' | 'badge' | 'person' | 'course' | 'count' | 'subscriptionStatus'; suffix?: string }
@Component({ selector: 'app-data-table', standalone: true, imports: [StatusComponent, IconComponent], template: `
  <div class="table-scroll" tabindex="0" [attr.aria-label]="caption">
    <table><caption class="sr-only">{{ caption }}</caption><thead><tr>@for (column of columns; track column.key) {<th scope="col">{{ column.label }}</th>}@if (interactive) {<th scope="col"><span class="sr-only">Actions</span></th>}</tr></thead>
    <tbody>@for (row of rows; track $index) {<tr>
      @for (column of columns; track column.key) {<td [attr.data-label]="column.label">
        @if (column.format === 'badge') {<app-status [value]="value(row, column)"/>}
        @else if (column.format === 'subscriptionStatus') {<app-status [value]="subStatus(row)"/>}
        @else {<span [class.primary-cell]="$first">{{ display(row, column) }}</span>}
      </td>}
      @if (interactive) {<td class="row-action"><button class="btn small quiet" (click)="select.emit(row)" [attr.aria-label]="'Ouvrir la fiche ' + display(row, columns[0])">Voir <app-icon name="arrow"/></button></td>}
    </tr>}</tbody></table>
  </div>
` })
export class DataTableComponent {
  @Input() rows: Entity[] = []; @Input() columns: Column[] = []; @Input() caption = 'Liste des données'; @Input() interactive = true;
  @Output() select = new EventEmitter<Entity>();
  subStatus = subscriptionStatus;
  value(row: Entity, col: Column) { return read(row, col.key); }
  display(row: Entity, col: Column): string {
    const value = this.value(row, col);
    switch (col.format) {
      case 'date': return day(value);
      case 'datetime': return day(value, true);
      case 'money': return money(value);
      case 'person': return person((col.key === '$' ? row : value) as Entity | undefined);
      case 'course': return courseName((col.key === '$' ? row : value) as Entity | undefined);
      case 'count': return String(Array.isArray(value) ? value.length : 0);
      default: return `${label(value)}${value == null ? '' : col.suffix ?? ''}`;
    }
  }
}
