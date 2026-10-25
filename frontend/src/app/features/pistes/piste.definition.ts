import { ResourceDefinition } from '../../shared/models/resource-definition';
import { selectField, textField } from '../../shared/utilities/form-fields';
export const PISTES: ResourceDefinition = {
  key: 'pistes', title: 'Pistes', singular: 'piste', icon: 'piste', idKey: 'numPiste',
  description: 'Le domaine skiable, ses parcours et les skieurs qui leur sont affectés.',
  listPath: '/piste/search', getPath: '/piste/get/', createPath: '/piste/add', updatePath: '/piste/update', deletePath: '/piste/delete/',
  columns: [{ key: 'namePiste', label: 'Piste' }, { key: 'color', label: 'Difficulté', format: 'badge' }, { key: 'length', label: 'Longueur', suffix: ' m' }, { key: 'slope', label: 'Pente', suffix: '°' }],
  fields: [textField('namePiste', 'Nom de la piste'), selectField('color', 'Difficulté', ['GREEN', 'BLUE', 'RED', 'BLACK']), { key: 'length', label: 'Longueur (mètres)', type: 'number', min: 1 }, { key: 'slope', label: 'Pente (degrés)', type: 'number', min: 0, max: 90 }],
  filters: [selectField('color', 'Difficulté', ['GREEN', 'BLUE', 'RED', 'BLACK'], false)],
  note: 'Les couleurs décrivent la difficulté. L’état d’ouverture des pistes n’est pas encore disponible.',
};
