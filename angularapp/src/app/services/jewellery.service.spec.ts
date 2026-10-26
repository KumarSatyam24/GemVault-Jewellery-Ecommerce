import { TestBed } from '@angular/core/testing';

import { JewelleryService } from './jewellery.service';
import { HttpClientTestingModule } from '@angular/common/http/testing';

describe('JewelleryService', () => {
  let service: JewelleryService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
    });
    service = TestBed.inject(JewelleryService);
  });

  fit('Frontend_should_create_jewellery_service', () => {
    expect(service).toBeTruthy();
  });
});
