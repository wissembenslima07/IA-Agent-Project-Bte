import { TestBed } from '@angular/core/testing';

import { Idle } from './idle';

describe('Idle', () => {
  let service: Idle;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Idle);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
