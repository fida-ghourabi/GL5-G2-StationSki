import { ResourceDefinition } from '../../shared/models/resource-definition';
import { personFields, dateField, textField } from '../../shared/utilities/form-fields';
import { today } from '../../shared/utilities/format';
export const INSTRUCTORS: ResourceDefinition = {
  key: 'instructors', title: 'Moniteurs', singular: 'moniteur', icon: 'badge', idKey: 'numInstructor',
  description: 'Une équipe sur le terrain, des disponibilités et des cours bien coordonnés.',
  listPath: '/instructor/search', getPath: '/instructor/get/', createPath: '/instructor/add', updatePath: '/instructor/update', deletePath: '/instructor/delete/',
  columns: [{ key: '$', label: 'Moniteur', format: 'person' }, { key: 'dateOfHire', label: 'Dans l’équipe depuis', format: 'date' }, { key: 'courses', label: 'Cours associés', format: 'count' }],
  fields: [...personFields(), { ...dateField('dateOfHire', 'Date d’embauche'), max: today() }],
  filters: [textField('name', 'Nom ou prénom', false)],
};
