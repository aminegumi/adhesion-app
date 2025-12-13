class User {
  final int? id;
  final String? email;
  final String? displayName;
  final String? birthDate; // ISO date string
  final String? gender;
  final bool? active;
  final bool? consentGiven; // Data sharing consent
  final String? role; // USER or ADMIN

  User({
    this.id,
    this.email,
    this.displayName,
    this.birthDate,
    this.gender,
    this.active,
    this.consentGiven,
    this.role,
  });

  factory User.fromJson(Map<String, dynamic> json) => User(
    id: json['id'] as int?,
    email: json['email'] as String?,
    displayName: json['displayName'] as String?,
    birthDate: json['birthDate'] as String?,
    gender: json['gender'] as String?,
    active: json['active'] as bool?,
    consentGiven: json['consentGiven'] as bool? ?? false,
    role: json['role'] as String? ?? 'USER',
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'email': email,
    'displayName': displayName,
    'birthDate': birthDate,
    'gender': gender,
    'active': active,
    'consentGiven': consentGiven,
    'role': role,
  };

  User copyWith({
    int? id,
    String? email,
    String? displayName,
    String? birthDate,
    String? gender,
    bool? active,
    bool? consentGiven,
    String? role,
  }) {
    return User(
      id: id ?? this.id,
      email: email ?? this.email,
      displayName: displayName ?? this.displayName,
      birthDate: birthDate ?? this.birthDate,
      gender: gender ?? this.gender,
      active: active ?? this.active,
      consentGiven: consentGiven ?? this.consentGiven,
      role: role ?? this.role,
    );
  }
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
