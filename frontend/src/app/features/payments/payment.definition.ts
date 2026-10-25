import { ResourceDefinition } from '../../shared/models/resource-definition';
import { moneyField, selectField, textField } from '../../shared/utilities/form-fields';
export const PAYMENTS: ResourceDefinition = {
  key: 'payments', title: 'Paiements', singular: 'paiement', icon: 'wallet', idKey: 'id',
  description: 'Des encaissements locaux traçables, des remboursements et des reçus clairs.',
  listPath: '/payments', getPath: '/payments/', createPath: '/payments',
  columns: [{ key: 'receiptNumber', label: 'Référence du reçu' }, { key: 'skier', label: 'Skieur', format: 'person' }, { key: 'paymentDate', label: 'Enregistré le', format: 'datetime' }, { key: 'amount', label: 'Montant', format: 'money' }, { key: 'method', label: 'Moyen' }, { key: 'status', label: 'Statut', format: 'badge' }],
  fields: [{ key: 'skierId', label: 'Skieur', type: 'select', source: 'skiers' }, { key: 'subscriptionId', label: 'Abonnement affecté à ce skieur', type: 'select', source: 'subscriptions' }, { ...moneyField('amount', 'Montant'), min: 0.01 }, selectField('method', 'Moyen de paiement', ['CARD', 'CASH', 'BANK_TRANSFER']), { ...textField('transactionReference', 'Référence de transaction', false), full: true, hint: 'Facultative, mais unique si renseignée. Aucune donnée de carte bancaire.' }],
  filters: [{ key: 'skierId', label: 'Paiements d’un skieur', type: 'select', source: 'skiers', required: false }, { key: 'subscriptionId', label: 'Ou d’un abonnement', type: 'select', source: 'subscriptions', required: false }],
  note: 'Choisissez un skieur ou un abonnement pour consulter son historique. Chaque paiement est créé en attente, puis confirmé séparément.',
};
