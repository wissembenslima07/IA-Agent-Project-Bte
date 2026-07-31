import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DossierList } from './dossier-list';

describe('DossierList', () => {
  let component: DossierList;
  let fixture: ComponentFixture<DossierList>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DossierList],
    }).compileComponents();

    fixture = TestBed.createComponent(DossierList);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
