import { Injectable, signal } from '@angular/core';
@Injectable({ providedIn: 'root' })
export class NotificationService {
  readonly message = signal('');
  private timeout?: ReturnType<typeof setTimeout>;
  success(message: string) { clearTimeout(this.timeout); this.message.set(message); this.timeout = setTimeout(() => this.message.set(''), 6000); }
}
