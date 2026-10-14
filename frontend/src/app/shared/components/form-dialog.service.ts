import { Injectable, signal } from '@angular/core';
import { Command } from '../models/domain';
export interface Option { value: string | number; label: string }
export type LookupSource = 'skiers' | 'courses' | 'instructors' | 'pistes' | 'subscriptions' | 'equipment' | 'slots';
export interface Field {
  key: string; label: string; type?: 'text' | 'number' | 'date' | 'datetime-local' | 'time' | 'select' | 'textarea';
  required?: boolean; min?: number | string; max?: number | string; step?: number; options?: Option[]; source?: LookupSource;
  default?: unknown; hint?: string; full?: boolean;
}
export interface FormSpec {
  title: string; description?: string; fields: Field[]; initial?: Command; submitLabel?: string; danger?: boolean;
  validate?: (values: Command) => string | undefined;
  submit: (values: Command) => Promise<void>;
}
@Injectable({ providedIn: 'root' })
export class FormDialogService {
  readonly current = signal<FormSpec | null>(null);
  show(spec: FormSpec) { this.current.set(spec); }
  close() { this.current.set(null); }
}
