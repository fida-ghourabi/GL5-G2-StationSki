import { bootstrapApplication } from '@angular/platform-browser';
import { provideHttpClient } from '@angular/common/http';
import { provideRouter, withInMemoryScrolling } from '@angular/router';
import { provideZonelessChangeDetection, LOCALE_ID } from '@angular/core';
import { registerLocaleData } from '@angular/common';
import localeFr from '@angular/common/locales/fr';
import { ShellComponent } from './app/core/layout/shell.component';
import { routes } from './app/app.routes';
registerLocaleData(localeFr);
bootstrapApplication(ShellComponent, {
  providers: [provideZonelessChangeDetection(), provideHttpClient(), provideRouter(routes, withInMemoryScrolling({ scrollPositionRestoration: 'enabled' })), { provide: LOCALE_ID, useValue: 'fr-FR' }],
}).catch(error => console.error('Impossible de démarrer le poste de station', error));
