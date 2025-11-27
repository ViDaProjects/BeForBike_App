import 'dart:async';
import 'package:flutter/services.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';

import '../../../domain/entities/activity.dart';
import 'state/statistics_state.dart';

final statisticsViewModelProvider =
    NotifierProvider.autoDispose<StatisticsViewModel, StatisticsState>(
  () => StatisticsViewModel(),
);

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
      final Map<dynamic, dynamic> stats = await platform.invokeMethod('getActivityData', {
        'activityId': activityId,
      }).timeout(const Duration(seconds: 10), onTimeout: () {
        return <String, dynamic>{};
      });

      // Get raw data for charts
      final List<dynamic> chartData = await platform.invokeMethod('getActivityChartData', {
        'activityId': activityId,
      }).timeout(const Duration(seconds: 10), onTimeout: () {
        return <dynamic>[];
      });

      // Convert chart data to the expected format
      final activityData = chartData.map((item) {
        final map = item as Map<dynamic, dynamic>;
        return {
          'timestamp': map['timestamp'] != null
              ? DateTime.fromMillisecondsSinceEpoch(map['timestamp'] as int)
              : DateTime.now(),
          'speed': map['speed'] != null ? (map['speed'] as num).toDouble() : 0.0,
          'cadence': map['cadence'] != null ? (map['cadence'] as num).toDouble() : 0.0,
          'power': map['power'] != null ? (map['power'] as num).toDouble() : 0.0,
          'altitude': map['altitude'] != null ? (map['altitude'] as num).toDouble() : 0.0,
        };
      }).toList();

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
      return (state.activityStats[key] as num?)?.toDouble() ?? 0.0;
    } catch (e) {
      return 0.0;
    }
  }

  /// Gets the average value for a given data type from pre-calculated statistics.
  double getAverageValue(String dataType) {
    if (state.activityStats.isEmpty) return 0.0;

    try {
      final key = 'avg_$dataType';
      return (state.activityStats[key] as num?)?.toDouble() ?? 0.0;
    } catch (e) {
      return 0.0;
    }
  }
}