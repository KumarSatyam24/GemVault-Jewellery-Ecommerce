import { User } from "./user.model";

describe('User Model', () => {

  fit('Frontend_User_model_should_create_an_instance', () => {
    // Create a sample User object
    const user: User = {
      userId: 1,
      email: 'user@example.com',
      password: 'password123',
      username: 'user123',
      mobileNumber: '1234567890',
      userRole: 'farmer'
    };

    expect(user).toBeTruthy();
    expect(user.userId).toBeDefined();
    expect(user.email).toBeDefined();
    expect(user.password).toBeDefined();
    expect(user.username).toBeDefined();
    expect(user.mobileNumber).toBeDefined();
    expect(user.userRole).toBeDefined();
  });

});
