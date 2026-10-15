import { HttpErrorResponse } from '@angular/common/http';
export function apiError(error: unknown): string {
  if (error instanceof HttpErrorResponse) {
    if (error.status === 0 || error.status === 502 || error.status === 504) return 'Le serveur de la station est injoignable. Vérifiez que le backend est démarré sur le port 8087, puis réessayez.';
    if (typeof error.error?.message === 'string') return error.error.message;
    if (error.status === 404) return 'Cette ressource est introuvable. Actualisez la liste.';
    if (error.status === 409) return 'Cette opération est incompatible avec les données actuelles. Vérifiez les associations et les disponibilités.';
    if (error.status === 400) return 'Les informations saisies sont invalides. Vérifiez les champs et les dates.';
    return `Le serveur n’a pas pu traiter la demande (HTTP ${error.status}). Réessayez.`;
  }
  return error instanceof Error ? error.message : 'Une erreur est survenue. Réessayez.';
}
