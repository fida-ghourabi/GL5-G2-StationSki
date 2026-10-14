import { ResourceDefinition } from '../../shared/models/resource-definition';
import { dateField, periodFields, selectField } from '../../shared/utilities/form-fields';
import { today } from '../../shared/utilities/format';
export const RENTALS: ResourceDefinition = {
  key: 'rentals', title: 'Locations', singular: 'location', icon: 'equipment', idKey: 'id',
  description: 'Préparez les remises de matériel et gardez un œil sur les retours attendus.',
  listPath: '/rentals', getPath: '/rentals/', createPath: '/rentals',
  columns: [{ key: 'equipment.inventoryCode', label: 'Exemplaire' }, { key: 'skier', label: 'Skieur', format: 'person' }, { key: 'startDate', label: 'Départ', format: 'date' }, { key: 'expectedEndDate', label: 'Retour prévu', format: 'date' }, { key: 'actualReturnDate', label: 'Retour réel', format: 'date' }, { key: 'totalAmount', label: 'Montant', format: 'money' }, { key: 'status', label: 'Statut', format: 'badge' }],
  fields: [{ key: 'skierId', label: 'Skieur', type: 'select', source: 'skiers' }, { key: 'equipmentId', label: 'Exemplaire', type: 'select', source: 'equipment' }, { ...dateField('startDate', 'Début', today()), min: today() }, { ...dateField('expectedEndDate', 'Retour prévu (exclu)', today(1)), min: today(1) }],
  filters: [...periodFields(), { key: 'skierId', label: 'Historique complet d’un skieur', type: 'select', source: 'skiers', required: false }, selectField('status', 'Statut', ['RESERVED', 'ACTIVE', 'RETURNED', 'CANCELLED'], false)],
  note: 'Le tarif est figé à la réservation. Le montant réel est recalculé au retour ; les règles de facturation sont appliquées par le serveur.',
};
