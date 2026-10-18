import { Field, Option } from '../components/form-dialog.service';
import { label, today } from './format';
export const options = (...values: string[]): Option[] => values.map(value => ({ value, label: label(value) }));
export const textField = (key: string, label: string, required = true): Field => ({ key, label, required });
export const selectField = (key: string, label: string, values: string[], required = true): Field => ({ key, label, type: 'select', required, options: options(...values) });
export const moneyField = (key = 'price', label = 'Prix (unité monétaire de la station)'): Field => ({ key, label, type: 'number', min: 0, max: 9999999999.99, step: 0.01 });
export const dateField = (key: string, label: string, defaultValue?: string): Field => ({ key, label, type: 'date', default: defaultValue });
export const personFields = (): Field[] => [textField('firstName', 'Prénom'), textField('lastName', 'Nom')];
export const periodFields = (past = 30, future = 30): Field[] => [
  { ...dateField('from', 'Du', today(-past)), required: false }, { ...dateField('to', 'Au (inclus)', today(future)), required: false },
];
