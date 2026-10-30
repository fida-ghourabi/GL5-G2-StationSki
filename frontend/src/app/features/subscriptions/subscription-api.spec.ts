import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { ApiClient } from '../../core/api/api-client';
import { Subscription, SubscriptionType } from '../../shared/models/domain';
import { SubscriptionApi } from './subscription-api';

describe('SubscriptionApi with a mocked ApiClient', () => {
  const api = { get: vi.fn() };

  beforeEach(() => {
    api.get.mockReset();
    TestBed.configureTestingModule({
      providers: [SubscriptionApi, { provide: ApiClient, useValue: api }],
    });
  });

  it('loads subscriptions by type', () => {
    api.get.mockReturnValue('subscriptions');
    expect(TestBed.inject(SubscriptionApi).byType('ANNUAL')).toBe('subscriptions');
    expect(api.get).toHaveBeenCalledWith('/subscription/all/ANNUAL');
  });

  it('loads subscriptions for a date range', () => {
    api.get.mockReturnValue('subscriptions');
    expect(TestBed.inject(SubscriptionApi).byPeriod('2026-01-01', '2026-12-31')).toBe('subscriptions');
    expect(api.get).toHaveBeenCalledWith('/subscription/all/2026-01-01/2026-12-31');
  });

  it('loads all three subscription types and flattens the results', () => {
    api.get.mockImplementation((path: string) => path.endsWith('ANNUAL')
      ? ['annual']
      : path.endsWith('MONTHLY') ? ['monthly'] : ['semiannual']);
    let result: string[] | undefined;
    TestBed.inject(SubscriptionApi).all().subscribe(value => result = value as unknown as string[]);
    expect(api.get).toHaveBeenCalledTimes(3);
    expect(api.get).toHaveBeenNthCalledWith(1, '/subscription/all/ANNUAL');
    expect(api.get).toHaveBeenNthCalledWith(2, '/subscription/all/MONTHLY');
    expect(api.get).toHaveBeenNthCalledWith(3, '/subscription/all/SEMESTRIEL');
    expect(result).toEqual(['annual', 'monthly', 'semiannual']);
  });
});

describe('SubscriptionApi through the HTTP client', () => {
  let api: SubscriptionApi;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [SubscriptionApi, provideHttpClient(), provideHttpClientTesting()],
    });
    api = TestBed.inject(SubscriptionApi);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('sends a GET request for a subscription type', () => {
    let result: Subscription[] | undefined;
    api.byType('MONTHLY').subscribe(value => result = value);
    http.expectOne('/stationSki/subscription/all/MONTHLY').flush([]);
    expect(result).toEqual([]);
  });

  it('sends a GET request for a subscription date range', () => {
    let result: Subscription[] | undefined;
    api.byPeriod('2026-01-01', '2026-12-31').subscribe(value => result = value);
    http.expectOne('/stationSki/subscription/all/2026-01-01/2026-12-31').flush([]);
    expect(result).toEqual([]);
  });

  it('requests all subscription types and combines their responses', () => {
    let result: Subscription[] | undefined;
    api.all().subscribe(value => result = value);
    http.expectOne('/stationSki/subscription/all/ANNUAL').flush([{ numSub: 1 }]);
    http.expectOne('/stationSki/subscription/all/MONTHLY').flush([{ numSub: 2 }]);
    http.expectOne('/stationSki/subscription/all/SEMESTRIEL').flush([{ numSub: 3 }]);
    expect(result?.map(subscription => subscription.numSub)).toEqual([1, 2, 3]);
  });
});
