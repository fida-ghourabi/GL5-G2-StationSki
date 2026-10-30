import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiClient } from '../../core/api/api-client';
import { Command, Payment, Receipt } from '../../shared/models/domain';
import { PaymentApi } from './payment-api';

describe('PaymentApi with a mocked ApiClient', () => {
  const api = {
    get: vi.fn(),
    post: vi.fn(),
  };

  beforeEach(() => {
    api.get.mockReset();
    api.post.mockReset();
    TestBed.configureTestingModule({
      providers: [PaymentApi, { provide: ApiClient, useValue: api }],
    });
  });

  it('loads payments for a skier', () => {
    api.get.mockReturnValue('payments');
    expect(TestBed.inject(PaymentApi).bySkier(17)).toBe('payments');
    expect(api.get).toHaveBeenCalledWith('/payments/skier/17');
  });

  it('loads payments for a subscription', () => {
    api.get.mockReturnValue('payments');
    expect(TestBed.inject(PaymentApi).bySubscription(42)).toBe('payments');
    expect(api.get).toHaveBeenCalledWith('/payments/subscription/42');
  });

  it('records a payment using the supplied command', () => {
    const command: Command = { skierId: 17, subscriptionId: 42, amount: 80, method: 'CARD' };
    api.post.mockReturnValue('payment');
    expect(TestBed.inject(PaymentApi).record(command)).toBe('payment');
    expect(api.post).toHaveBeenCalledWith('/payments', command);
  });

  it('loads a payment receipt', () => {
    api.get.mockReturnValue('receipt');
    expect(TestBed.inject(PaymentApi).receipt(9)).toBe('receipt');
    expect(api.get).toHaveBeenCalledWith('/payments/9/receipt');
  });
});

describe('PaymentApi through the HTTP client', () => {
  let api: PaymentApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [PaymentApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(PaymentApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends a GET request for a skier payment history', () => {
    let result: Payment[] | undefined;
    api.bySkier(17).subscribe(value => result = value);
    http.expectOne('/stationSki/payments/skier/17').flush([]);
    expect(result).toEqual([]);
  });

  it('sends a GET request for an subscription payment history', () => {
    let result: Payment[] | undefined;
    api.bySubscription(42).subscribe(value => result = value);
    http.expectOne('/stationSki/payments/subscription/42').flush([]);
    expect(result).toEqual([]);
  });

  it('posts the payment command and returns the created payment', () => {
    const command: Command = { skierId: 17, subscriptionId: 42, amount: 80, method: 'CARD' };
    const expected = { id: 9, amount: 80 } as Payment;
    let result: Payment | undefined;
    api.record(command).subscribe(value => result = value);
    const request = http.expectOne('/stationSki/payments');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual(command);
    request.flush(expected);
    expect(result).toEqual(expected);
  });

  it('sends a GET request for a receipt', () => {
    const expected = { paymentId: 9, netAmount: 80 } as Receipt;
    let result: Receipt | undefined;
    api.receipt(9).subscribe(value => result = value);
    http.expectOne('/stationSki/payments/9/receipt').flush(expected);
    expect(result).toEqual(expected);
  });
});
