export interface Entity { [key: string]: unknown }
export type SubscriptionType = 'ANNUAL' | 'MONTHLY' | 'SEMESTRIEL';
export type CourseType = 'INDIVIDUAL' | 'COLLECTIVE_CHILDREN' | 'COLLECTIVE_ADULT';
export type Support = 'SKI' | 'SNOWBOARD';
export type PisteColor = 'GREEN' | 'BLUE' | 'RED' | 'BLACK';
export type SlotStatus = 'OPEN' | 'FULL' | 'CANCELLED';
export type PaymentStatus = 'PENDING' | 'PAID' | 'FAILED' | 'PARTIALLY_REFUNDED' | 'REFUNDED';
export type PaymentMethod = 'CARD' | 'CASH' | 'BANK_TRANSFER';
export interface Subscription extends Entity { numSub: number; startDate: string; endDate: string; price: number; typeSub: SubscriptionType }
export interface Skier extends Entity { numSkier: number; firstName: string; lastName: string; city: string; dateOfBirth: string; subscription?: Subscription }
export interface Piste extends Entity { numPiste: number; namePiste: string; color: PisteColor; length: number; slope: number }
export interface Course extends Entity { numCourse: number; level: number; typeCourse: CourseType; support: Support; price: number; timeSlot: number }
export interface Instructor extends Entity { numInstructor: number; firstName: string; lastName: string; dateOfHire: string; courses: Course[] }
export interface PlanningSlot extends Entity { id: number; course: Course; instructor: Instructor; startTime: string; endTime: string; maxCapacity: number; status: SlotStatus }
export interface Availability extends Entity { id: number; instructor: Instructor; dayOfWeek: string; startTime: string; endTime: string; validFrom: string; validUntil: string }
export interface Registration extends Entity { numRegistration: number; numWeek: number; cancelled: boolean; skierId: number; courseId: number; planningSlot?: PlanningSlot }
export interface Payment extends Entity { id: number; skier: Skier; subscription: Subscription; amount: number; paymentDate: string; paidAt?: string; method: PaymentMethod; status: PaymentStatus; transactionReference?: string; receiptNumber: string; refundedAmount: number }
export interface Refund extends Entity { id: number; amount: number; refundDate: string; reason: string }
export interface Receipt extends Entity { receiptNumber: string; paymentId: number; skierId: number; skierName: string; subscriptionId: number; createdAt: string; paidAt?: string; method: PaymentMethod; status: PaymentStatus; amount: number; refundedAmount: number; netAmount: number; refunds: Refund[] }
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export type Query = Record<string, string | number | boolean | undefined | null>;
export type Command = Record<string, unknown>;
