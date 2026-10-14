export interface Entity { [key: string]: unknown }
export type SubscriptionType = 'ANNUAL' | 'MONTHLY' | 'SEMESTRIEL';
export type CourseType = 'INDIVIDUAL' | 'COLLECTIVE_CHILDREN' | 'COLLECTIVE_ADULT';
export type Support = 'SKI' | 'SNOWBOARD';
export type PisteColor = 'GREEN' | 'BLUE' | 'RED' | 'BLACK';
export type EquipmentType = 'SKIS' | 'BOOTS' | 'POLES' | 'HELMET';
export type EquipmentStatus = 'AVAILABLE' | 'RENTED' | 'MAINTENANCE' | 'OUT_OF_SERVICE';
export type SlotStatus = 'OPEN' | 'FULL' | 'CANCELLED';
export type RentalStatus = 'RESERVED' | 'ACTIVE' | 'RETURNED' | 'CANCELLED';
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
export interface Equipment extends Entity { id: number; inventoryCode: string; type: EquipmentType; brand: string; size: string; dailyRate: number; status: EquipmentStatus }
export interface Rental extends Entity { id: number; skier: Skier; equipment: Equipment; startDate: string; expectedEndDate: string; actualReturnDate?: string; dailyRate: number; totalAmount: number; status: RentalStatus }
export interface Payment extends Entity { id: number; skier: Skier; subscription: Subscription; amount: number; paymentDate: string; paidAt?: string; method: PaymentMethod; status: PaymentStatus; transactionReference?: string; receiptNumber: string; refundedAmount: number }
export interface Refund extends Entity { id: number; amount: number; refundDate: string; reason: string }
export interface Receipt extends Entity { receiptNumber: string; paymentId: number; skierId: number; skierName: string; subscriptionId: number; createdAt: string; paidAt?: string; method: PaymentMethod; status: PaymentStatus; amount: number; refundedAmount: number; netAmount: number; refunds: Refund[] }
export interface MonthlyRevenue { month: string; collected: number; refunds: number; net: number }
export interface Revenue { from: string; to: string; collected: number; refunds: number; net: number; months: MonthlyRevenue[] }
export interface SubscriptionReport { asOf: string; active: number; expired: number; upcoming: number; expiringWithin30Days: number }
export interface Occupancy { courseId: number; slotCount: number; capacity: number; registrations: number; occupancyPercent: number }
export interface WeeklyRegistration { courseId: number; weekBasedYear: number; week: number; registrations: number }
export interface Page<T> { content: T[]; totalElements: number; totalPages: number; number: number; size: number }
export type Query = Record<string, string | number | boolean | undefined | null>;
export type Command = Record<string, unknown>;
