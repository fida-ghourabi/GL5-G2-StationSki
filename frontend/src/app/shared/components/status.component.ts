import { Component, Input } from '@angular/core';
import { label } from '../utilities/format';
@Component({ selector: 'app-status', standalone: true, template: '<span class="status" [attr.data-state]="value"><i></i>{{ text }}</span>' })
export class StatusComponent { @Input() value: unknown; get text() { return label(this.value); } }
