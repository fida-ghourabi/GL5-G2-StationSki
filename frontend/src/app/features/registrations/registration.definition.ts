import { ResourceDefinition } from '../../shared/models/resource-definition';
import { periodFields } from '../../shared/utilities/form-fields';
export const REGISTRATIONS: ResourceDefinition = {
  key: 'registrations', title: 'Inscriptions', singular: 'inscription', icon: 'clipboard', idKey: 'numRegistration',
  description: 'La bonne place, au bon moment. Les capacités et les conflits sont vérifiés à l’inscription.',
  listPath: '/registration',
  columns: [{ key: '_skier', label: 'Skieur' }, { key: '_course', label: 'Cours' }, { key: 'numWeek', label: 'Semaine ISO' }, { key: 'planningSlot.startTime', label: 'Début du cours', format: 'datetime' }, { key: '_status', label: 'Statut', format: 'badge' }],
  fields: [{ key: 'skierId', label: 'Skieur', type: 'select', source: 'skiers' }, { key: 'slotId', label: 'Créneau ouvert', type: 'select', source: 'slots', hint: 'Créneaux ouverts des 90 prochains jours. Le serveur vérifie l’âge et les places.' }],
  filters: [...periodFields(), { key: 'skierId', label: 'Historique complet d’un skieur', type: 'select', source: 'skiers', required: false }, { key: 'courseId', label: 'Cours', type: 'select', source: 'courses', required: false }, { key: 'week', label: 'Semaine ISO', type: 'number', min: 1, max: 53, required: false }],
  note: 'Sans filtre skieur, seuls les créneaux datés de la période sont retournés. L’historique par skieur inclut aussi les anciennes inscriptions sans date.',
};
