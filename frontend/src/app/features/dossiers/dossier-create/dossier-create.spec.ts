import { ComponentFixture, TestBed } from '@angular/core/testing';

import { DossierCreate } from './dossier-create';

describe('DossierCreate', () => {
  let component: DossierCreate;
  let fixture: ComponentFixture<DossierCreate>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [DossierCreate],
    }).compileComponents();

    fixture = TestBed.createComponent(DossierCreate);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
