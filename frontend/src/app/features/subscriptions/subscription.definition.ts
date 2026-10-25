import { ResourceDefinition } from '../../shared/models/resource-definition';
import { dateField, selectField, moneyField } from '../../shared/utilities/form-fields';
import { today } from '../../shared/utilities/format';
export const SUBSCRIPTIONS: ResourceDefinition = {
  key: 'subscriptions', title: 'Abonnements', singular: 'abonnement', icon: 'ticket', idKey: 'numSub',
  description: 'Suivez la validité des forfaits et anticipez les prochaines échéances.',
  listPath: '/subscription/all/', getPath: '/subscription/get/', createPath: '/subscription/add', updatePath: '/subscription/update', deletePath: '/subscription/delete/',
  columns: [{ key: 'numSub', label: 'N° abonnement' }, { key: 'typeSub', label: 'Formule' }, { key: 'startDate', label: 'Début', format: 'date' }, { key: 'endDate', label: 'Fin exclusive', format: 'date' }, { key: 'price', label: 'Prix', format: 'money' }, { key: '$status', label: 'Validité', format: 'subscriptionStatus' }],
  fields: [selectField('typeSub', 'Formule', ['ANNUAL', 'MONTHLY', 'SEMESTRIEL']), dateField('startDate', 'Date de début', today()), moneyField()],
  filters: [selectField('type', 'Formule', ['ANNUAL', 'MONTHLY', 'SEMESTRIEL'], false), { ...dateField('from', 'Début à partir du'), required: false }, { ...dateField('to', 'Début jusqu’au'), required: false }, selectField('state', 'Validité', ['ACTIVE', 'EXPIRING', 'EXPIRED', 'UPCOMING'], false)],
  note: 'La fin est calculée par le serveur. Un abonnement lié à un paiement ne peut plus être modifié.',
};
