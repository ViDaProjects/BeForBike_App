import '../../../../domain/entities/activity.dart';

class StatisticsState {
  final bool isLoading;
  final Activity? selectedActivity;
  final List<Map<String, dynamic>> activityData;
  final Map<String, dynamic> activityStats;

  /// Represents the state of the statistics screen.
  ///
  /// [isLoading] indicates whether the screen is in a loading state.
  /// [selectedActivity] the currently selected activity for detailed statistics.
  /// [activityData] historical data points for the selected activity.
  /// [activityStats] pre-calculated max and average statistics for the activity.
  const StatisticsState({
    required this.isLoading,
    this.selectedActivity,
    required this.activityData,
    required this.activityStats,
  });

  /// Creates an initial state for the statistics screen.
  factory StatisticsState.initial() {
    return const StatisticsState(
      isLoading: false,
      selectedActivity: null,
      activityData: [],
      activityStats: {},
    );
  }

  /// Creates a copy of this state object with the provided changes.
  StatisticsState copyWith({
    bool? isLoading,
    Activity? selectedActivity,
    List<Map<String, dynamic>>? activityData,
    Map<String, dynamic>? activityStats,
  }) {
    return StatisticsState(
      isLoading: isLoading ?? this.isLoading,
      selectedActivity: selectedActivity ?? this.selectedActivity,
      activityData: activityData ?? this.activityData,
      activityStats: activityStats ?? this.activityStats,
    );
  }
}