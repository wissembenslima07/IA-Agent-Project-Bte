import { TestBed } from '@angular/core/testing';

import { AiAnalysis } from './ai-analysis';

describe('AiAnalysis', () => {
  let service: AiAnalysis;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(AiAnalysis);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
