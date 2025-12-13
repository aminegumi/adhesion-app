/// Utility class for text formatting and cleaning
class TextFormatter {
  /// Cleans markdown formatting and returns plain readable text
  static String cleanMarkdown(String text) {
    String cleaned = text;
    
    // Remove placeholders like [Your Name], [Name], etc.
    cleaned = cleaned.replaceAll(RegExp(r'\[Your Name\]', caseSensitive: false), '');
    cleaned = cleaned.replaceAll(RegExp(r'\[Name\]', caseSensitive: false), '');
    cleaned = cleaned.replaceAll(RegExp(r'\[Patient\]', caseSensitive: false), '');
    cleaned = cleaned.replaceAll(RegExp(r'\[User\]', caseSensitive: false), '');
    
    // Remove headers (##, ###, etc.)
    cleaned = cleaned.replaceAll(RegExp(r'^#{1,6}\s*', multiLine: true), '');
    
    // Remove horizontal rules
    cleaned = cleaned.replaceAll(
      RegExp(r'^\*{3,}$|^-{3,}$|^_{3,}$', multiLine: true),
      '',
    );
    
    // Convert **bold** to just the text
    cleaned = cleaned.replaceAllMapped(
      RegExp(r'\*\*(.+?)\*\*'),
      (match) => match.group(1) ?? '',
    );
    
    // Convert *italic* to just the text
    cleaned = cleaned.replaceAllMapped(
      RegExp(r'\*(.+?)\*'),
      (match) => match.group(1) ?? '',
    );
    
    // Convert __bold__ to just the text
    cleaned = cleaned.replaceAllMapped(
      RegExp(r'__(.+?)__'),
      (match) => match.group(1) ?? '',
    );
    
    // Convert _italic_ to just the text
    cleaned = cleaned.replaceAllMapped(
      RegExp(r'_(.+?)_'),
      (match) => match.group(1) ?? '',
    );
    
    // Remove bullet points and convert to readable format
    cleaned = cleaned.replaceAll(
      RegExp(r'^\s*[-*+]\s+', multiLine: true),
      '• ',
    );
    
    // Remove numbered list markdown
    cleaned = cleaned.replaceAllMapped(
      RegExp(r'^\s*\d+\.\s+', multiLine: true),
      (match) => '',
    );
    
    // Clean up extra whitespace and newlines
    cleaned = cleaned.replaceAll(RegExp(r'\n{3,}'), '\n\n');
    
    // Clean up double spaces
    cleaned = cleaned.replaceAll(RegExp(r'  +'), ' ');
    
    return cleaned.trim();
  }

  /// Formats a date to a readable string
  static String formatDate(DateTime date) {
    const months = [
      '',
      'Jan',
      'Feb',
      'Mar',
      'Apr',
      'May',
      'Jun',
      'Jul',
      'Aug',
      'Sep',
      'Oct',
      'Nov',
      'Dec',
    ];
    return '${months[date.month]} ${date.day}, ${date.year}';
  }

  /// Formats time from DateTime
  static String formatTime(DateTime date) {
    final hour = date.hour.toString().padLeft(2, '0');
    final minute = date.minute.toString().padLeft(2, '0');
    return '$hour:$minute';
  }

  /// Capitalizes first letter of a string
  static String capitalize(String s) {
    if (s.isEmpty) return s;
    return s[0].toUpperCase() + s.substring(1).toLowerCase();
  }
}
