import { Component, Input } from '@angular/core';
const paths: Record<string, string> = {
  mountain: 'M2 20 9 5l5 9 3-5 5 11ZM6 12l3 3 3-3',
  grid: 'M3 3h7v7H3ZM14 3h7v7h-7ZM3 14h7v7H3ZM14 14h7v7h-7Z',
  people: 'M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2M16 3a4 4 0 0 1 0 8M22 21v-2a4 4 0 0 0-3-3.87M13 7a4 4 0 1 1-8 0 4 4 0 0 1 8 0Z',
  piste: 'm5 3 3 4-2 6 7 3 3 5M15 3l4 4-3 5M3 21h18',
  book: 'M12 7c-3-3-7-3-10-2v15c4-1 7-1 10 1 3-2 6-2 10-1V5c-3-1-7-1-10 2ZM12 7v14',
  badge: 'M12 3a5 5 0 1 0 0 10 5 5 0 0 0 0-10ZM8 12 6 22l6-3 6 3-2-10',
  check: 'm5 12 4 4L19 6',
  clipboard: 'M9 4H5v18h14V4h-4M9 2h6v4H9ZM8 12l2 2 5-5M8 18h8',
  calendar: 'M3 5h18v16H3ZM3 10h18M7 2v6M17 2v6M7 14h2M12 14h2M7 18h2',
  ticket: 'M3 6h18v4a2 2 0 0 0 0 4v4H3v-4a2 2 0 0 0 0-4ZM15 6v3M15 11v2M15 15v3',
  wallet: 'M3 5h16v3H3v13h18V8H3ZM16 12h5v5h-5Z',
  plus: 'M12 5v14M5 12h14', search: 'M21 21l-5-5M18 10a8 8 0 1 1-16 0 8 8 0 0 1 16 0Z',
  arrow: 'M5 12h14m-5-5 5 5-5 5', chevron: 'm9 5 7 7-7 7',
  close: 'm6 6 12 12M6 18 18 6', menu: 'M3 6h18M3 12h18M3 18h18',
  refresh: 'M20 7V2l-3 3a9 9 0 1 0 3 12M20 7h-5',
  clock: 'M12 8v5l3 2M22 12a10 10 0 1 1-20 0 10 10 0 0 1 20 0Z',
  alert: 'm12 3 10 18H2ZM12 9v5M12 17v1',
  edit: 'm16 3 5 5-12 12-6 1 1-6ZM14 5l5 5',
  trash: 'M3 6h18M9 6V3h6v3M5 6l1 15h12l1-15M10 10v7M14 10v7',
  eye: 'M2 12s4-7 10-7 10 7 10 7-4 7-10 7S2 12 2 12ZM15 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0Z',
  download: 'M12 3v12m-5-5 5 5 5-5M3 16v5h18v-5',
};
@Component({ selector: 'app-icon', standalone: true, template: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.7" stroke-linecap="round" stroke-linejoin="round" aria-hidden="true"><path [attr.d]="path"/></svg>', styles: [':host{display:inline-flex;width:20px;height:20px;flex-shrink:0}svg{width:100%;height:100%}'] })
export class IconComponent { @Input() name = 'mountain'; get path() { return paths[this.name] ?? paths['mountain']; } }
