import { Routes } from '@angular/router';
import { RESOURCES } from './shared/models/resources';

const collection = () => import('./shared/components/resource-list.component').then(m => m.ResourceListComponent);
const detail = () => import('./shared/components/resource-detail.component').then(m => m.ResourceDetailComponent);
export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'dashboard' },
  { path: 'dashboard', title: 'Tableau de bord · Boréal', loadComponent: () => import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent) },
  ...Object.values(RESOURCES).flatMap(resource => [
    { path: resource.key, title: `${resource.title} · Boréal`, data: { resource }, loadComponent: collection },
    { path: `${resource.key}/:id`, title: `Dossier ${resource.singular} · Boréal`, data: { resource }, loadComponent: detail },
  ]),
  { path: 'planning', title: 'Planning · Boréal', loadComponent: () => import('./features/planning/planning.component').then(m => m.PlanningComponent) },
  { path: '**', title: 'Page introuvable · Boréal', loadComponent: () => import('./shared/components/not-found.component').then(m => m.NotFoundComponent) },
];
