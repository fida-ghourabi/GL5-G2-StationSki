import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { IconComponent } from './icon.component';
@Component({ selector: 'app-not-found', standalone: true, imports: [RouterLink, IconComponent], template: '<section class="not-found"><app-icon name="mountain"/><p class="eyebrow">Erreur 404 · Hors-piste</p><h1>Ce chemin ne mène<br>à aucune page.</h1><p>Le lien a peut-être changé. Retrouvez les outils de la station depuis le tableau de bord.</p><a class="btn" routerLink="/dashboard">Revenir au tableau de bord <app-icon name="arrow"/></a></section>' })
export class NotFoundComponent { }
