import { Column } from '../components/data-table.component';
import { Field } from '../components/form-dialog.service';
export type ResourceKey = 'skiers' | 'pistes' | 'courses' | 'instructors' | 'registrations' | 'equipment' | 'rentals' | 'subscriptions' | 'payments';
export interface ResourceDefinition {
  key: ResourceKey; title: string; singular: string; description: string; icon: string; idKey: string;
  listPath: string; getPath?: string; createPath?: string; updatePath?: string; deletePath?: string;
  columns: Column[]; fields: Field[]; filters: Field[]; note?: string;
}
