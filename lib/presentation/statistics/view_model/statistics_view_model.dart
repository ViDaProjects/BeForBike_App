import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../core/utils/date_utils.dart';
import '../../../domain/entities/activity.dart';
import 'state/statistics_state.dart';

final statisticsViewModelProvider =
    NotifierProvider.autoDispose<StatisticsViewModel, StatisticsState>(
      () => StatisticsViewModel(),
    );

/// Processes chart data in a background isolate to avoid blocking the UI thread.
List<Map<String, dynamic>> processChartData(List<dynamic> chartData) {
  return chartData.map((item) {
    final map = item as Map<dynamic, dynamic>;
    return {
      'timestamp':
          DateUtils.parseUniversalDate(map['timestamp']) ?? DateTime.now(),
      'speed': _validateValue((map['speed'] as num?)?.toDouble() ?? 0.0),
      'cadence': _validateValue((map['cadence'] as num?)?.toDouble() ?? 0.0),
      'power': _validateValue((map['power'] as num?)?.toDouble() ?? 0.0),
      'altitude': _validateValue((map['altitude'] as num?)?.toDouble() ?? 0.0),
    };
  }).toList();
}

/// Validates a numeric value, returning 0.0 for invalid values.
double _validateValue(double value) {
  if (value.isNaN || value.isInfinite || value < 0) {
    return 0.0;
  }
  return value;
}

class StatisticsViewModel extends Notifier<StatisticsState> {
  static const platform = MethodChannel('com.beforbike.app/database');

  Timer? _debounceTimer;

  @override
  StatisticsState build() {
    return StatisticsState.initial();
  }

  /// Sets the selected activity for detailed statistics.
  void setSelectedActivity(Activity? activity) {
    // Cancel any pending debounce timer
    _debounceTimer?.cancel();

    // Set a new debounce timer to delay the data loading
    _debounceTimer = Timer(const Duration(milliseconds: 300), () {
      state = state.copyWith(selectedActivity: activity);
      if (activity != null) {
        loadActivityData(activity.id);
      } else {
        // Clear data when no activity is selected
        state = state.copyWith(activityData: []);
      }
    });
  }

  /// Loads statistics data for the selected activity from SQLite database.
  Future<void> loadActivityData(String activityId) async {
    state = state.copyWith(isLoading: true);

    try {
      // Get max/avg statistics from database
      final Map<dynamic, dynamic> stats = await platform
          .invokeMethod('getActivityData', {'activityId': activityId})
          .timeout(
            const Duration(seconds: 10),
            onTimeout: () {
              return <String, dynamic>{};
            },
          );

      // Get raw data for charts
      final List<dynamic> chartData = await platform
          .invokeMethod('getActivityChartData', {'activityId': activityId})
          .timeout(
            const Duration(seconds: 10),
            onTimeout: () {
              return <dynamic>[];
            },
          );

      // Process chart data in background isolate to avoid blocking UI
      final activityData = await compute(processChartData, chartData);

      // Store both statistics and chart data in state
      state = state.copyWith(
        isLoading: false,
        activityData: activityData,
        activityStats: stats.cast<String, dynamic>(),
      );
    } catch (e) {
      // No data available if database is not available
      state = state.copyWith(
        isLoading: false,
        activityData: [],
        activityStats: {},
      );
    }
  }

  /// Gets the maximum value for a given data type from pre-calculated statistics.
  double getMaxValue(String dataType) {
    if (state.activityStats.isEmpty) return 0.0;

    try {
      final key = 'max_$dataType';
      final value = (state.activityStats[key] as num?)?.toDouble() ?? 0.0;
      return _validateValue(value);
    } catch (e) {
      return 0.0;
    }
  }

  /// Gets the average value for a given data type from pre-calculated statistics.
  double getAverageValue(String dataType) {
    if (state.activityStats.isEmpty) return 0.0;

    try {
      final key = 'avg_$dataType';
      final value = (state.activityStats[key] as num?)?.toDouble() ?? 0.0;
      return _validateValue(value);
    } catch (e) {
      return 0.0;
    }
  }
}
