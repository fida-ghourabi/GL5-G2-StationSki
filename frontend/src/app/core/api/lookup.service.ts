import { Injectable, inject } from '@angular/core';
import { map, Observable } from 'rxjs';
import { SkierApi } from '../../features/skiers/skier-api';
import { PisteApi } from '../../features/pistes/piste-api';
import { CourseApi } from '../../features/courses/course-api';
import { InstructorApi } from '../../features/instructors/instructor-api';
import { SubscriptionApi } from '../../features/subscriptions/subscription-api';
import { EquipmentApi } from '../../features/equipment/equipment-api';
import { PlanningApi } from '../../features/planning/planning-api';
import { LookupSource, Option } from '../../shared/components/form-dialog.service';
import { person, label, courseName, day, dateRange, today } from '../../shared/utilities/format';
@Injectable({ providedIn: 'root' })
export class LookupService {
  private skiers = inject(SkierApi); private pistes = inject(PisteApi); private courses = inject(CourseApi);
  private instructors = inject(InstructorApi); private subscriptions = inject(SubscriptionApi); private equipment = inject(EquipmentApi); private planning = inject(PlanningApi);
  options(source: LookupSource): Observable<Option[]> {
    switch (source) {
      case 'skiers': return this.skiers.all().pipe(map(rows => rows.map(r => ({ value: r.numSkier, label: `${person(r)} · ${r.city} · #${r.numSkier}` }))));
      case 'pistes': return this.pistes.all().pipe(map(rows => rows.map(r => ({ value: r.numPiste, label: `${r.namePiste} · ${label(r.color)}` }))));
      case 'courses': return this.courses.all().pipe(map(rows => rows.map(r => ({ value: r.numCourse, label: `${courseName(r)} · #${r.numCourse}` }))));
      case 'instructors': return this.instructors.all().pipe(map(rows => rows.map(r => ({ value: r.numInstructor, label: person(r) }))));
      case 'subscriptions': return this.subscriptions.all().pipe(map(rows => rows.map(r => ({ value: r.numSub, label: `#${r.numSub} · ${label(r.typeSub)} · ${day(r.startDate)}` }))));
      case 'equipment': return this.equipment.all().pipe(map(rows => rows.filter(r => r.status !== 'MAINTENANCE' && r.status !== 'OUT_OF_SERVICE').map(r => ({ value: r.id, label: `${r.inventoryCode} · ${label(r.type)} · ${r.size}` }))));
      case 'slots': return this.planning.slots(dateRange(today(), today(90)), true).pipe(map(rows => rows.map(r => ({ value: r.id, label: `${day(r.startTime, true)} · ${courseName(r.course)} · ${person(r.instructor)}` }))));
    }
  }
}
