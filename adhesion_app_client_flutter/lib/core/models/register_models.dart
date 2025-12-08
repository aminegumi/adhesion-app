class RegisterRequest {
  final String email;
  final String password;
  final String displayName;
  final String birthDate; // YYYY-MM-DD
  final String gender; // "Male" | "Female"

  RegisterRequest({
    required this.email,
    required this.password,
    required this.displayName,
    required this.birthDate,
    required this.gender,
  });

  Map<String, dynamic> toJson() => {
    'email': email,
    'password': password,
    'displayName': displayName,
    'birthDate': birthDate,
    'gender': gender,
  };
}
