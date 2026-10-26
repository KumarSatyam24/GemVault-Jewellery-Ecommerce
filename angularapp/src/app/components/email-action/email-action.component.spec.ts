import { ComponentFixture, TestBed } from '@angular/core/testing';

import { EmailActionComponent } from './email-action.component';

describe('EmailActionComponent', () => {
  let component: EmailActionComponent;
  let fixture: ComponentFixture<EmailActionComponent>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [EmailActionComponent]
    });
    fixture = TestBed.createComponent(EmailActionComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
