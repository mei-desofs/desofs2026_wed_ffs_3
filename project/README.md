# Cafeteria Management System

## Tech Requirements

- Java 17 or higher
- Maven 3.6 or higher
- Spring Boot 3.1+
- Spring Data JPA
- H2 Database (in-memory)
- Spring Security with JWT Authentication
- Jakarta Validation

## Authentication & Authorization

The system uses JWT (JSON Web Token) authentication with role-based access control:

### Roles and Permissions:

**🔑 Admin:**
- Full access to all endpoints
- Can manage all entities (ingredients, dishes, menus, users, purchases)

**👨‍🍳 Employee:**
- Can view, create, edit, or delete ingredients and dishes
- Can view, create, edit, or delete menus (only for future dates)
- Cannot access user management or purchases

**👤 Client:**
- Can view all ingredients, dishes, and menus
- Can view and edit their own user information (`/api/users/me`)
- Can create, edit, and delete their own purchases (only for future dates)
- Cannot access other users' information

### Authentication Process:
1. Login with username and password at `/api/auth/login`
2. Receive JWT token in response
3. Include token in `Authorization: Bearer <token>` header for all subsequent requests

## How to Run

1. Install Java 17+ and Maven 3.6+
2. Clone the repository
3. In the project directory, run:
	 ```bash
	 mvn spring-boot:run
	 ```
4. The app will start at `http://localhost:8080`

## How to Test

Run all tests:
```bash
mvn test
```

## Entities

- **Ingredient**: name, type (enum), allergen (enum) - Vanessa
- **Dish**: name, list of ingredients, price - Joao
- **Menu**: date, meat dish, fish dish, vegetarian dish - Gondar
- **User**: username, encrypted password, type (Employee/Client/Admin), balance (clients only) - Leo
- **Purchase**: client, dish, date - Diogo

## Field Validations

- Ingredient and Dish names: only letters and spaces
- Ingredient types: CEREALS_AND_DERIVATIVES, TUBERS, VEGETABLES, FRUIT, DAIRY_PRODUCTS, MEAT, FISH, EGGS, LEGUMES, FATS_AND_OILS
- Allergens: NONE, GLUTEN_CONTAINING_CEREALS, NUTS, CRUSTACEANS, CELERY, EGGS, MUSTARD, FISH, SESAME_SEEDS, PEANUTS, SULPHITES, SOYBEANS, LUPINS, MILK_AND_MILK_PRODUCTS, MOLLUSCS
- Meat/fish ingredients cannot be added to vegetarian dishes
- Menus and purchases: only for future dates
- Clients must have sufficient balance for purchases
- Employees and Admins don't have balance
- Ingredients, Dishes, Menus cannot be updated/deleted if used in another entity

## Available Endpoints

### Authentication
- `POST /api/auth/login` - Login with username/password to get JWT token

### Ingredients
- `GET /api/ingredients` - View all (All roles)
- `GET /api/ingredients/{id}` - View by ID (All roles)
- `POST /api/ingredients` - Create (Admin, Employee)
- `PUT /api/ingredients/{id}` - Update (Admin, Employee)
- `DELETE /api/ingredients/{id}` - Delete (Admin, Employee)

### Dishes
- `GET /api/dishes` - View all (All roles)
- `GET /api/dishes/{id}` - View by ID (All roles)
- `POST /api/dishes` - Create (Admin, Employee)
- `PUT /api/dishes/{id}` - Update (Admin, Employee)
- `DELETE /api/dishes/{id}` - Delete (Admin, Employee)

### Menus
- `GET /api/menus` - View all (All roles)
- `GET /api/menus/{id}` - View by ID (All roles)
- `GET /api/menus/date/{date}` - View by date (All roles)
- `POST /api/menus` - Create for future dates (Admin, Employee)
- `PUT /api/menus/{id}` - Update for future dates (Admin, Employee)
- `DELETE /api/menus/{id}` - Delete for future dates (Admin, Employee)

### Users
- `GET /api/users` - View all users (Admin only)
- `GET /api/users/{id}` - View user by ID (Admin only)
- `GET /api/users/username/{username}` - View user by username (Admin only)
- `GET /api/users/me` - View own profile (Admin, Client)
- `POST /api/users` - Create user (Admin only)
- `PUT /api/users/{id}` - Update any user (Admin only)
- `PUT /api/users/me` - Update own profile (Admin, Client)
- `DELETE /api/users/{id}` - Delete user (Admin only)

### Purchases
- `GET /api/purchases` - View all (Admin) or own purchases (Client)
- `GET /api/purchases/{id}` - View purchase by ID (Admin, Client - own only)
- `GET /api/purchases/client/{clientId}` - View purchases by client (Admin, Client - own only)
- `GET /api/purchases/date/{date}` - View purchases by date (Admin, Client - own only)
- `POST /api/purchases` - Create for future dates (Admin, Client - own only)
- `PUT /api/purchases/{id}` - Update for future dates (Admin, Client - own only)
- `DELETE /api/purchases/{id}` - Delete for future dates (Admin, Client - own only)

## JSON Examples for POST Requests

### Login (Authentication)
```json
{
  "username": "admin",
  "password": "admin123"
}
```

### Ingredient
```json
{
  "name": "Fresh Spinach",
  "type": "VEGETABLES",
  "allergen": "NONE"
}
```

### Dish
```json
{
  "name": "Mediterranean Quinoa Bowl",
  "ingredientNames": [
    "Tomatoes",
    "Lettuce", 
    "Carrots",
    "Olive Oil"
  ],
  "price": 13.75
}
```

### Menu
```json
{
  "date": "2025-10-16",
  "meatDishName": "Grilled Chicken with Rice",
  "fishDishName": "Salmon with Broccoli",
  "vegetarianDishName": "Vegetarian Salad"
}
```

### User (Client)
```json
{
  "username": "jane_doe",
  "password": "secure123",
  "type": "CLIENT",
  "balance": 50.00
}
```

### User (Employee - no balance)
```json
{
  "username": "john_chef",
  "password": "emp123",
  "type": "EMPLOYEE"
}
```

### User (Admin)
```json
{
  "username": "super_admin",
  "password": "admin123",
  "type": "ADMIN"
}
```

### Purchase
```json
{
  "clientUsername": "mary_client",
  "dishName": "Grilled Chicken with Rice",
  "date": "2025-10-01"
}
```

## Working with Sample Data

The application comes with preloaded sample data:

### Available Ingredients:
- Chicken Breast, Salmon Fillet, Cod Fillet, Beef (meat/fish)
- Tomatoes, Lettuce, Carrots, Broccoli, Potatoes, Onions (vegetables)  
- Rice, Black Beans, Eggs, Cheese, Olive Oil

### Available Dishes:
- "Grilled Chicken with Rice" (€12.50)
- "Salmon with Broccoli" (€15.00)
- "Vegetarian Salad" (€8.00)
- "Fish and Chips" (€14.00)
- "Beef Stew" (€13.50)
- "Veggie Bowl" (€9.50)

### Sample Users:
- **admin** (ADMIN, €1000.00 balance)
- **john_employee** (EMPLOYEE, no balance)
- **mary_client** (CLIENT, €50.00 balance)
- **peter_client** (CLIENT, €75.00 balance)
- **susan_employee** (EMPLOYEE, no balance)

### Sample Menus:
- **2025-10-01**: Grilled Chicken with Rice (meat), Salmon with Broccoli (fish), Vegetarian Salad (vegetarian)
- **2025-10-02**: Beef Stew (meat), Fish and Chips (fish), Veggie Bowl (vegetarian)
- **2025-10-03**: Grilled Chicken with Rice (meat), Salmon with Broccoli (fish), Vegetarian Salad (vegetarian)

**Note**: All sample passwords are encrypted in the database. Passwords are never returned in API responses for security.

## Example API Calls

### Login to get JWT token:
```bash
curl -X POST "http://localhost:8080/api/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "admin123"}'
```

Response:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "username": "admin",
  "role": "ADMIN",
  "message": "Login successful"
}
```

### Use JWT token in subsequent requests:
```bash
curl -X GET "http://localhost:8080/api/users" \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiJ9..."
```

### Get current user profile (clients):
```bash
curl -X GET "http://localhost:8080/api/users/me" \
  -H "Authorization: Bearer <your-token>"
```

### Create a new ingredient (admin/employee):
```bash
curl -X POST "http://localhost:8080/api/ingredients" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"name": "Fresh Spinach", "type": "VEGETABLES", "allergen": "NONE"}'
```

### Create a new dish using existing ingredients (admin/employee):
```bash
curl -X POST "http://localhost:8080/api/dishes" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"name": "Chicken Caesar Salad", "ingredientNames": ["Chicken Breast", "Lettuce", "Cheese"], "price": 11.50}'
```

### Create a menu for future date (admin/employee):
```bash
curl -X POST "http://localhost:8080/api/menus" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"date": "2025-10-16", "meatDishName": "Grilled Chicken with Rice", "fishDishName": "Salmon with Broccoli", "vegetarianDishName": "Vegetarian Salad"}'
```

### Make a purchase for future date (client - own purchases only):
```bash
curl -X POST "http://localhost:8080/api/purchases" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <your-token>" \
  -d '{"clientUsername": "mary_client", "dishName": "Grilled Chicken with Rice", "date": "2025-10-16"}'
```

**Note**: Replace `<your-token>` with the actual JWT token received from login.
