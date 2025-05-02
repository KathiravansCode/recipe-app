Product Requirement Document (PRD)
Project Title: Tasty Recipes
Prepared By: Software Architect
Version: 1.1
Date: 2025-05-01
1. Project Overview
Tasty Recipes is a simple, user-friendly web application that allows users to register, log in, and share their favorite recipes. Registered users can add recipes with images, ingredients, steps, and descriptions. They can view, rate, and comment on other users' recipes. Users can also manage their accounts, including logging out and deleting their profiles.

2. Goals and Objectives
Provide a platform for sharing and discovering recipes.

Allow users to rate and comment on recipes.

Ensure account and content management capabilities.

Use a maintainable and scalable backend with a relational database.

3. Features and Requirements
3.1. User Management
3.1.1. User Registration
Input: Full name, email, password.

Passwords must be stored securely (BCrypt hashing).

Email must be unique.

3.1.2. Login/Logout
Email and password-based authentication.

Session or JWT-based login system.

Logout feature to end the session.

3.1.3. Delete Account
User can permanently delete their account.

All associated data (recipes, reviews) will be deleted or anonymized.

3.2. Recipe Management
3.2.1. Add Recipe
Fields:

Title (text)

Description (text)

Ingredients (text or list)

Cooking Steps (text or list)

Image Upload (optional)

Users can create, edit, and delete their own recipes.

3.2.2. Browse/View Recipes
All users (including guests) can view recipe listings.

Pagination and search functionality.

Clicking a recipe shows full details: image, ingredients, steps, reviews.

3.3. Ratings and Reviews
3.3.1. Add Review
Logged-in users can rate (1–5 stars) and comment on a recipe.

Each user can leave only one review per recipe.

3.3.2. View Reviews
Reviews and average rating displayed on the recipe detail page.

4. Non-Functional Requirements
Security:

Password hashing using BCrypt.

Input validation and sanitation.

Role-based access control for protected endpoints.

Performance:

Efficient SQL queries and indexing.

Responsiveness:

Fully responsive layout using CSS media queries.

Scalability:

Designed to handle moderate traffic and future enhancements.


Database Design Overview
(High-level, can be further expanded)

Tables:
users: id, name, email, password, created_at

recipes: id, title, description, ingredients, steps, image_url, user_id, created_at

reviews: id, user_id, recipe_id, rating, comment, created_at

7. User Roles
Guest: Can browse recipes.

Registered User: Can add/edit/delete their own recipes, rate, comment, and delete account.

