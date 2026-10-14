import { STATION } from '../../core/config/station.config';
import { Entity, Subscription } from '../models/domain';

export const LABELS: Record<string, string> = {
  ANNUAL: 'Annuel', MONTHLY: 'Mensuel', SEMESTRIEL: 'Semestriel',
  INDIVIDUAL: 'Individuel', COLLECTIVE_CHILDREN: 'Collectif enfants', COLLECTIVE_ADULT: 'Collectif adultes',
  SKI: 'Ski', SNOWBOARD: 'Snowboard', GREEN: 'Verte · facile', BLUE: 'Bleue · intermédiaire', RED: 'Rouge · difficile', BLACK: 'Noire · experte',
  OPEN: 'Ouvert', FULL: 'Complet', CANCELLED: 'Annulé', AVAILABLE: 'Disponible', RENTED: 'Loué', MAINTENANCE: 'En maintenance', OUT_OF_SERVICE: 'Hors service',
  RESERVED: 'Réservé', ACTIVE: 'Actif', RETURNED: 'Retourné', EXPIRED: 'Expiré', UPCOMING: 'À venir', EXPIRING: 'Échéance proche',
  PENDING: 'En attente', PAID: 'Encaissé', FAILED: 'Échoué', PARTIALLY_REFUNDED: 'Remboursé en partie', REFUNDED: 'Remboursé',
  CARD: 'Carte', CASH: 'Espèces', BANK_TRANSFER: 'Virement', SKIS: 'Skis', BOOTS: 'Chaussures', POLES: 'Bâtons', HELMET: 'Casque',
  MONDAY: 'Lundi', TUESDAY: 'Mardi', WEDNESDAY: 'Mercredi', THURSDAY: 'Jeudi', FRIDAY: 'Vendredi', SATURDAY: 'Samedi', SUNDAY: 'Dimanche',
};
export const label = (value: unknown): string => value == null || value === '' ? '—' : LABELS[String(value)] ?? String(value);
export const money = (value: unknown): string => value == null ? '—' : `${new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }).format(Number(value))} ${STATION.moneyUnit}`;
export function day(value: unknown, time = false): string {
  if (!value) return '—';
  const text = String(value);
  const date = new Date(/^\d{4}-\d{2}-\d{2}$/.test(text) ? `${text}T12:00:00` : text);
  if (isNaN(date.getTime())) return '—';
  return new Intl.DateTimeFormat('fr-FR', { day: '2-digit', month: 'short', year: 'numeric', ...(time ? { hour: '2-digit', minute: '2-digit' } as const : {}) }).format(date);
}
export function today(offset = 0): string {
  const date = new Date(); date.setDate(date.getDate() + offset);
  return localDate(date);
}
export function localDate(date: Date): string {
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
}
export function dateRange(from: string, to: string): { from: string; to: string } {
  const end = new Date(`${to}T12:00:00`); end.setDate(end.getDate() + 1);
  return { from: `${from}T00:00:00`, to: `${localDate(end)}T00:00:00` };
}
export const person = (p: Entity | undefined): string => p ? `${p['firstName'] ?? ''} ${p['lastName'] ?? ''}`.trim() : '—';
export const courseName = (p: Entity | undefined): string => p ? `${label(p['support'])} · niveau ${p['level']} · ${label(p['typeCourse'])}` : '—';
export function read(row: Entity, path: string): unknown {
  return path.split('.').reduce<unknown>((value, key) => value != null && typeof value === 'object' ? (value as Entity)[key] : undefined, row);
}
export function subscriptionStatus(subscription: Subscription | Entity): string {
  const start = String(subscription['startDate'] ?? ''), end = String(subscription['endDate'] ?? '');
  if (!start || !end) return 'Dates manquantes';
  return today() < start ? 'UPCOMING' : today() >= end ? 'EXPIRED' : end <= today(30) ? 'EXPIRING' : 'ACTIVE';
}
