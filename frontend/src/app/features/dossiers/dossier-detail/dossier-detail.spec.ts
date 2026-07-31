import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DossierDetail } from './dossier-detail';

describe('DossierDetail', () => {
  let component: DossierDetail;
  let fixture: ComponentFixture<DossierDetail>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DossierDetail],
    }).compileComponents();

    fixture = TestBed.createComponent(DossierDetail);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
