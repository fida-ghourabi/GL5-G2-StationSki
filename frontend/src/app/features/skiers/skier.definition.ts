import { ResourceDefinition } from '../../shared/models/resource-definition';
import { personFields, textField, dateField } from '../../shared/utilities/form-fields';
import { today } from '../../shared/utilities/format';
export const SKIERS: ResourceDefinition = {
  key: 'skiers', title: 'Skieurs', singular: 'skieur', icon: 'people', idKey: 'numSkier',
  description: 'Un accueil fluide, du premier passage à la dernière descente.',
  listPath: '/skier/search', getPath: '/skier/get/', createPath: '/skier/add', updatePath: '/skier/update', deletePath: '/skier/delete/',
  columns: [{ key: '$', label: 'Skieur', format: 'person' }, { key: 'city', label: 'Ville' }, { key: 'dateOfBirth', label: 'Naissance', format: 'date' }, { key: 'subscription.typeSub', label: 'Abonnement', format: 'badge' }, { key: 'subscription.endDate', label: 'Fin de validité', format: 'date' }],
  fields: [...personFields(), textField('city', 'Ville'), { ...dateField('dateOfBirth', 'Date de naissance'), max: today(-1) }],
  filters: [{ ...textField('name', 'Nom ou prénom', false) }, textField('city', 'Ville', false)],
};
