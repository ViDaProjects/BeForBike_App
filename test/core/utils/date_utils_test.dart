import 'package:flutter_test/flutter_test.dart';
import 'package:be_for_bike/core/utils/date_utils.dart';

void main() {
  group('DateUtils.parseUniversalDate', () {
    test('should parse Unix timestamp (milliseconds)', () {
      final timestamp = 1641006000000; // 2022-01-01 00:00:00 UTC
      final result = DateUtils.parseUniversalDate(timestamp);

      expect(result, isNotNull);
      expect(result!.year, 2022);
      expect(result.month, 1);
      expect(result.day, 1);
    });

    test('should parse Unix timestamp (seconds)', () {
      final timestamp = 1641006000; // 2022-01-01 00:00:00 UTC
      final result = DateUtils.parseUniversalDate(timestamp);

      expect(result, isNotNull);
      expect(result!.year, 2022);
      expect(result.month, 1);
      expect(result.day, 1);
    });

    test('should parse ISO 8601 with T separator', () {
      final dateStr = '2022-01-01T12:30:45.123456';
      final result = DateUtils.parseUniversalDate(dateStr);

      expect(result, isNotNull);
      expect(result!.year, 2022);
      expect(result.month, 1);
      expect(result.day, 1);
      expect(result.hour, 12);
      expect(result.minute, 30);
    });

    test('should handle null input', () {
      final result = DateUtils.parseUniversalDate(null);
      expect(result, isNull);
    });

    test('should handle already parsed DateTime', () {
      final dateTime = DateTime(2022, 1, 1, 12, 30, 45);
      final result = DateUtils.parseUniversalDate(dateTime);

      expect(result, equals(dateTime));
    });

    test('should fallback to DateTime.parse for unknown formats', () {
      final dateStr = '2022-01-01T12:30:45Z'; // ISO with timezone
      final result = DateUtils.parseUniversalDate(dateStr);

      expect(result, isNotNull);
    });
  });

  group('DateUtils.formatDateTime', () {
    test('should format DateTime correctly', () {
      final dateTime = DateTime(2022, 1, 1, 12, 30, 45);
      final result = DateUtils.formatDateTime(dateTime);

      expect(result, '2022-01-01 12:30:45');
    });

    test('should handle null DateTime', () {
      final result = DateUtils.formatDateTime(null);
      expect(result, '');
    });
  });
}