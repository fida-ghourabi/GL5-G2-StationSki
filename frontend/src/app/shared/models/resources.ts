import { SKIERS } from '../../features/skiers/skier.definition';
import { PISTES } from '../../features/pistes/piste.definition';
import { COURSES } from '../../features/courses/course.definition';
import { INSTRUCTORS } from '../../features/instructors/instructor.definition';
import { REGISTRATIONS } from '../../features/registrations/registration.definition';
import { SUBSCRIPTIONS } from '../../features/subscriptions/subscription.definition';
import { PAYMENTS } from '../../features/payments/payment.definition';
import { ResourceDefinition, ResourceKey } from './resource-definition';
export const RESOURCES: Record<ResourceKey, ResourceDefinition> = { skiers: SKIERS, pistes: PISTES, courses: COURSES, instructors: INSTRUCTORS, registrations: REGISTRATIONS, subscriptions: SUBSCRIPTIONS, payments: PAYMENTS };
