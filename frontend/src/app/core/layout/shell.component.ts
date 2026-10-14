import { Component, OnDestroy, computed, inject, signal } from '@angular/core';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';
import { STATION } from '../config/station.config';
import { NotificationService } from './notification.service';
import { IconComponent } from '../../shared/components/icon.component';
import { FormDialogComponent } from '../../shared/components/form-dialog.component';
@Component({ selector: 'app-root', standalone: true, imports: [RouterLink, RouterLinkActive, RouterOutlet, IconComponent, FormDialogComponent], template: `
  <a class="skip-link" href="#main-content">Aller au contenu</a>
  @if (menuOpen()) {<button class="nav-backdrop" aria-label="Fermer la navigation" (click)="closeMenu()"></button>}
  <aside id="main-navigation" class="sidebar" [class.menu-open]="menuOpen()" [attr.inert]="mobile() && !menuOpen() ? '' : null">
    @if (mobile() && menuOpen()) {<button class="icon-btn drawer-close" (click)="closeMenu()" aria-label="Fermer la navigation"><app-icon name="close"/></button>}
    <a routerLink="/dashboard" class="brand"><span class="brand-mark"><app-icon name="mountain"/></span><div><strong>{{ station.name }}</strong><span>{{ station.subtitle }}</span></div></a>
    <div class="nav-section-label">Votre station</div><nav aria-label="Navigation principale">@for (item of navigation; track item.path) {
      <a [routerLink]="'/' + item.path" routerLinkActive="active" [class.active]="item.path === 'equipment' && section() === 'rentals'" ariaCurrentWhenActive="page" (click)="menuOpen.set(false)"><app-icon [name]="item.icon"/><span>{{ item.title }}</span><app-icon class="nav-chevron" name="chevron"/></a>
      @if (item.path === 'equipment' && (section() === 'equipment' || section() === 'rentals')) {<a routerLink="/rentals" routerLinkActive="active" class="subnav-link" (click)="menuOpen.set(false)">Locations & retours</a>}
    }</nav>
    <footer class="sidebar-footer"><div class="topo-lines" aria-hidden="true"></div><span class="eyebrow">Un même cap.</span><strong>Une station bien organisée.</strong><p>Outils de gestion de l’équipe</p><div class="local-mode"><span></span>Gestion locale</div></footer>
  </aside>
  <div class="workspace" [attr.inert]="mobile() && menuOpen() ? '' : null"><header class="topbar"><div class="header-left"><button id="menu-toggle" class="icon-btn mobile-menu" (click)="openMenu()" aria-label="Ouvrir la navigation" aria-controls="main-navigation" [attr.aria-expanded]="menuOpen()"><app-icon name="menu"/></button><nav aria-label="Fil d’Ariane" class="breadcrumb"><a routerLink="/dashboard">{{ station.name }}</a><span>/</span><span>{{ activeTitle() }}</span>@if (isDetail()) {<span>/</span><span>Dossier</span>}</nav></div><div class="header-right"><span class="header-date">{{ date }}</span><span class="team-avatar" aria-label="Espace équipe">ÉQ</span></div></header>
    <main id="main-content" tabindex="-1"><router-outlet/></main><footer class="workspace-footer"><span>{{ station.name }} · Poste de station</span><span>Données métier réelles · API locale</span></footer>
  </div>
  <app-form-dialog/>
  @if (notifications.message()) {<div class="toast" role="status" aria-live="polite"><span class="toast-check"><app-icon name="check"/></span><span>{{ notifications.message() }}</span><button class="icon-btn" aria-label="Fermer la notification" (click)="notifications.message.set('')"><app-icon name="close"/></button></div>}
`, host: { '(window:keydown)': 'navigationKey($event)', '(window:resize)': 'resize()' } })
export class ShellComponent implements OnDestroy {
  readonly station = STATION; readonly notifications = inject(NotificationService); private router = inject(Router);
  readonly menuOpen = signal(false); readonly mobile = signal(window.innerWidth <= 1024); readonly url = signal(this.router.url);
  readonly section = computed(() => this.url().split('/')[1]?.split('?')[0]);
  readonly isDetail = computed(() => this.url().split('?')[0].split('/').filter(Boolean).length > 1);
  readonly activeTitle = computed(() => this.section() === 'rentals' ? 'Matériel et locations' : this.navigation.find(item => item.path === this.section())?.title ?? 'Page introuvable');
  readonly date = new Intl.DateTimeFormat('fr-FR', { day: 'numeric', month: 'long', year: 'numeric' }).format(new Date());
  readonly navigation = [
    { path: 'dashboard', title: 'Tableau de bord', icon: 'grid' }, { path: 'skiers', title: 'Skieurs', icon: 'people' }, { path: 'pistes', title: 'Pistes', icon: 'piste' },
    { path: 'courses', title: 'Cours', icon: 'book' }, { path: 'instructors', title: 'Moniteurs', icon: 'badge' }, { path: 'registrations', title: 'Inscriptions', icon: 'clipboard' },
    { path: 'planning', title: 'Planning', icon: 'calendar' }, { path: 'equipment', title: 'Matériel et locations', icon: 'equipment' }, { path: 'subscriptions', title: 'Abonnements', icon: 'ticket' },
    { path: 'payments', title: 'Paiements', icon: 'wallet' }, { path: 'reports', title: 'Rapports', icon: 'chart' },
  ];
  private navigationSubscription = this.router.events.pipe(filter(event => event instanceof NavigationEnd)).subscribe(event => { this.url.set(event.urlAfterRedirects); this.menuOpen.set(false); setTimeout(() => document.getElementById('main-content')?.focus({ preventScroll: true })); });
  closeMenu() { const wasOpen = this.menuOpen(); this.menuOpen.set(false); if (wasOpen) setTimeout(() => document.getElementById('menu-toggle')?.focus()); }
  openMenu() { this.menuOpen.set(true); setTimeout(() => document.querySelector<HTMLButtonElement>('.drawer-close')?.focus()); }
  navigationKey(event: KeyboardEvent) {
    if (!this.mobile() || !this.menuOpen()) return;
    if (event.key === 'Escape') { event.preventDefault(); this.closeMenu(); }
    if (event.key === 'Tab') {
      const elements = Array.from(document.querySelectorAll<HTMLElement>('#main-navigation a, #main-navigation button'));
      const first = elements[0], last = elements[elements.length - 1];
      if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
      else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
    }
  }
  resize() { this.mobile.set(window.innerWidth <= 1024); if (!this.mobile()) this.menuOpen.set(false); }
  ngOnDestroy() { this.navigationSubscription.unsubscribe(); }
}
