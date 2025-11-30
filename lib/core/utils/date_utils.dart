/// Universal date parser that handles multiple date formats from database
class DateUtils {
  /// Parses any date format and returns DateTime
  /// Supports:
  /// - Unix timestamp (milliseconds or seconds)
  /// - ISO 8601 with 'T' separator (yyyy-MM-ddTHH:mm:ss.SSSSSS)
  /// - ISO 8601 with timezone (yyyy-MM-ddTHH:mm:ss.SSSSSS+HH:mm)
  /// - Brazilian format (dd/MM/yyyy HH:mm:ss)
  /// - US format (MM/dd/yyyy HH:mm:ss)
  /// - Various combinations with/without milliseconds/timezone
  static DateTime? parseUniversalDate(dynamic dateValue) {
    if (dateValue == null) return null;

    // If it's already a DateTime, return as is
    if (dateValue is DateTime) return dateValue;

    // If it's a number (Unix timestamp), convert it
    if (dateValue is num) {
      // Check if it's seconds (10 digits) or milliseconds (13 digits)
      if (dateValue.toString().length <= 10) {
        // Seconds
        return DateTime.fromMillisecondsSinceEpoch((dateValue * 1000).toInt());
      } else {
        // Milliseconds
        return DateTime.fromMillisecondsSinceEpoch(dateValue.toInt());
      }
    }

    // If it's a string, try multiple parsing strategies
    if (dateValue is String) {
      // First check if it's a numeric string that represents a timestamp
      final numericValue = int.tryParse(dateValue);
      if (numericValue != null) {
        return parseUniversalDate(numericValue);
      }

      return _parseDateString(dateValue);
    }

    return null;
  }

  static DateTime? _parseDateString(String dateStr) {
    // Remove any whitespace
    final cleanStr = dateStr.trim();

    // List of formats to try, in order of preference
    final formats = [
      // ISO 8601 with timezone and microseconds
      "yyyy-MM-ddTHH:mm:ss.SSSSSSZZZ",
      "yyyy-MM-ddTHH:mm:ss.SSSZZZ",
      "yyyy-MM-ddTHH:mm:ssZZZ",

      // ISO 8601 without timezone
      "yyyy-MM-ddTHH:mm:ss.SSSSSS",
      "yyyy-MM-ddTHH:mm:ss.SSS",
      "yyyy-MM-ddTHH:mm:ss",

      // Brazilian format (dd/MM/yyyy)
      "dd/MM/yyyy HH:mm:ss.SSS",
      "dd/MM/yyyy HH:mm:ss",
      "dd/MM/yyyy",

      // US format (MM/dd/yyyy)
      "MM/dd/yyyy HH:mm:ss.SSS",
      "MM/dd/yyyy HH:mm:ss",
      "MM/dd/yyyy",

      // yyyy/MM/dd format
      "yyyy/MM/dd HH:mm:ss.SSS",
      "yyyy/MM/dd HH:mm:ss",
      "yyyy/MM/dd",

      // Other common formats
      "yyyy-MM-dd HH:mm:ss.SSS",
      "yyyy-MM-dd HH:mm:ss",
      "yyyy-MM-dd",

      // Time only (combine with current date)
      "HH:mm:ss.SSS",
      "HH:mm:ss",
    ];

    for (final format in formats) {
      try {
        // For time-only formats, combine with current date
        if (format.startsWith("HH:mm")) {
          // This is a simplified approach - for production, you might want more sophisticated parsing
          continue;
        }

        // Try parsing with the current format
        // Note: Dart's DateFormat doesn't handle all these formats directly
        // We'll need a more robust approach

        return _parseWithFormat(cleanStr, format);
      } catch (e) {
        // Continue to next format
        continue;
      }
    }

    // If all parsing attempts fail, try some fallback strategies
    return _fallbackParsing(cleanStr);
  }

  static DateTime? _parseWithFormat(String dateStr, String format) {
    try {
      // Handle Brazilian format (dd/MM/yyyy HH:mm:ss)
      if (format.startsWith('dd/MM/yyyy')) {
        final parts = dateStr.split(' ');
        if (parts.isNotEmpty) {
          final dateParts = parts[0].split('/');
          if (dateParts.length == 3) {
            final day = int.tryParse(dateParts[0]);
            final month = int.tryParse(dateParts[1]);
            final year = int.tryParse(dateParts[2]);

            if (day != null && month != null && year != null) {
              var hour = 0, minute = 0, second = 0;

              if (parts.length == 2) {
                final timeParts = parts[1].split(':');
                if (timeParts.isNotEmpty) {
                  hour = int.tryParse(timeParts[0]) ?? 0;
                }
                if (timeParts.length > 1) {
                  minute = int.tryParse(timeParts[1]) ?? 0;
                }
                if (timeParts.length > 2) {
                  second = int.tryParse(timeParts[2]) ?? 0;
                }
              }

              return DateTime(day, month, year, hour, minute, second);
            }
          }
        }
      }

      // Handle US format (MM/dd/yyyy HH:mm:ss)
      if (format.startsWith('MM/dd/yyyy')) {
        final parts = dateStr.split(' ');
        if (parts.isNotEmpty) {
          final dateParts = parts[0].split('/');
          if (dateParts.length == 3) {
            final month = int.tryParse(dateParts[0]);
            final day = int.tryParse(dateParts[1]);
            final year = int.tryParse(dateParts[2]);

            if (month != null && day != null && year != null) {
              var hour = 0, minute = 0, second = 0;

              if (parts.length == 2) {
                final timeParts = parts[1].split(':');
                if (timeParts.isNotEmpty) {
                  hour = int.tryParse(timeParts[0]) ?? 0;
                }
                if (timeParts.length > 1) {
                  minute = int.tryParse(timeParts[1]) ?? 0;
                }
                if (timeParts.length > 2) {
                  second = int.tryParse(timeParts[2]) ?? 0;
                }
              }

              return DateTime(year, month, day, hour, minute, second);
            }
          }
        }
      }

      // Handle yyyy/MM/dd format
      if (format.startsWith('yyyy/MM/dd')) {
        final parts = dateStr.split(' ');
        if (parts.isNotEmpty) {
          final dateParts = parts[0].split('/');
          if (dateParts.length == 3) {
            final year = int.tryParse(dateParts[0]);
            final month = int.tryParse(dateParts[1]);
            final day = int.tryParse(dateParts[2]);

            if (year != null && month != null && day != null) {
              var hour = 0, minute = 0, second = 0;

              if (parts.length == 2) {
                final timeParts = parts[1].split(':');
                if (timeParts.isNotEmpty) {
                  hour = int.tryParse(timeParts[0]) ?? 0;
                }
                if (timeParts.length > 1) {
                  minute = int.tryParse(timeParts[1]) ?? 0;
                }
                if (timeParts.length > 2) {
                  second = int.tryParse(timeParts[2]) ?? 0;
                }
              }

              return DateTime(year, month, day, hour, minute, second);
            }
          }
        }
      }

      // For ISO formats, try DateTime.parse
      if (format.contains('T') || format.startsWith('yyyy-MM-dd')) {
        return DateTime.tryParse(dateStr);
      }

      return null;
    } catch (e) {
      return null;
    }
  }

  static DateTime? _fallbackParsing(String dateStr) {
    // Last resort: try DateTime.parse which handles many formats
    try {
      return DateTime.parse(dateStr);
    } catch (e) {
      // If everything fails, return null
      return null;
    }
  }

  /// Formats DateTime to a consistent string format
  static String formatDateTime(DateTime? dateTime) {
    if (dateTime == null) return '';

    return '${dateTime.year}-${dateTime.month.toString().padLeft(2, '0')}-${dateTime.day.toString().padLeft(2, '0')} '
        '${dateTime.hour.toString().padLeft(2, '0')}:${dateTime.minute.toString().padLeft(2, '0')}:${dateTime.second.toString().padLeft(2, '0')}';
  }

  /// Gets current timestamp in milliseconds
  static int currentTimestamp() {
    return DateTime.now().millisecondsSinceEpoch;
  }
}
