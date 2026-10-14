import { ResourceDefinition } from '../../shared/models/resource-definition';
import { textField, selectField, moneyField, dateField } from '../../shared/utilities/form-fields';
export const EQUIPMENT: ResourceDefinition = {
  key: 'equipment', title: 'Matériel', singular: 'équipement', icon: 'equipment', idKey: 'id',
  description: 'Chaque paire, chaque casque : un inventaire précis des exemplaires physiques.',
  listPath: '/equipment', getPath: '/equipment/', createPath: '/equipment', updatePath: '/equipment/', deletePath: '/equipment/',
  columns: [{ key: 'inventoryCode', label: 'Code d’inventaire' }, { key: 'type', label: 'Type' }, { key: 'brand', label: 'Marque' }, { key: 'size', label: 'Taille' }, { key: 'dailyRate', label: 'Tarif / jour', format: 'money' }, { key: 'status', label: 'État', format: 'badge' }],
  fields: [textField('inventoryCode', 'Code d’inventaire unique'), selectField('type', 'Type', ['SKIS', 'BOOTS', 'POLES', 'HELMET']), textField('brand', 'Marque'), { ...textField('size', 'Taille'), hint: 'Ex. 170 cm, 42 ou M, selon le type.' }, moneyField('dailyRate', 'Tarif journalier'), selectField('status', 'État', ['AVAILABLE', 'MAINTENANCE', 'OUT_OF_SERVICE'])],
  filters: [selectField('type', 'Type', ['SKIS', 'BOOTS', 'POLES', 'HELMET'], false), textField('size', 'Taille', false), selectField('status', 'État', ['AVAILABLE', 'RENTED', 'MAINTENANCE', 'OUT_OF_SERVICE'], false), { ...dateField('from', 'Disponible du'), required: false }, { ...dateField('to', 'Au (retour exclu)'), required: false }],
  note: 'Renseignez deux dates pour vérifier les disponibilités auprès du serveur. Un exemplaire loué reste indisponible jusqu’à son retour.',
};
