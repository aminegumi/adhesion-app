class User {
  final int? id;
  final String? email;
  final String? displayName;
  final String? birthDate; // ISO date string
  final String? gender;
  final bool? active;

  User({
    this.id,
    this.email,
    this.displayName,
    this.birthDate,
    this.gender,
    this.active,
  });

  factory User.fromJson(Map<String, dynamic> json) => User(
    id: json['id'] as int?,
    email: json['email'] as String?,
    displayName: json['displayName'] as String?,
    birthDate: json['birthDate'] as String?,
    gender: json['gender'] as String?,
    active: json['active'] as bool?,
  );
}

class LoginResponse {
  final String token;
  final User user;

  LoginResponse({required this.token, required this.user});

  factory LoginResponse.fromJson(Map<String, dynamic> json) => LoginResponse(
    token: (json['token'] ?? '') as String,
    user: User.fromJson(json['user'] as Map<String, dynamic>),
  );
}
