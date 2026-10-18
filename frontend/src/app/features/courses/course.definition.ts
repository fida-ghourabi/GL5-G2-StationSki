import { ResourceDefinition } from '../../shared/models/resource-definition';
import { selectField, moneyField } from '../../shared/utilities/form-fields';
export const COURSES: ResourceDefinition = {
  key: 'courses', title: 'Cours', singular: 'cours', icon: 'book', idKey: 'numCourse',
  description: 'Des premières glisses au perfectionnement, organisez chaque apprentissage.',
  listPath: '/course/search', getPath: '/course/get/', createPath: '/course/add', updatePath: '/course/update', deletePath: '/course/delete/',
  columns: [{ key: '$', label: 'Cours', format: 'course' }, { key: 'support', label: 'Discipline', format: 'badge' }, { key: 'level', label: 'Niveau' }, { key: 'typeCourse', label: 'Public' }, { key: 'price', label: 'Prix', format: 'money' }],
  fields: [{ key: 'level', label: 'Niveau', type: 'number', min: 1 }, selectField('typeCourse', 'Type de cours', ['INDIVIDUAL', 'COLLECTIVE_CHILDREN', 'COLLECTIVE_ADULT']), selectField('support', 'Discipline', ['SKI', 'SNOWBOARD']), moneyField()],
  filters: [selectField('type', 'Type de cours', ['INDIVIDUAL', 'COLLECTIVE_CHILDREN', 'COLLECTIVE_ADULT'], false), selectField('support', 'Discipline', ['SKI', 'SNOWBOARD'], false), { key: 'level', label: 'Niveau', type: 'number', min: 1, required: false }],
  note: 'La capacité et les horaires sont définis sur chaque créneau : 1 place en individuel, jusqu’à 6 en collectif.',
};
