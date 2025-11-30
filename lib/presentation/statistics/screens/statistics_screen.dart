import 'dart:math';

import 'package:fl_chart/fl_chart.dart';
import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:intl/intl.dart';

import '../../../domain/entities/activity.dart';
import '../../common/core/utils/color_utils.dart';
import '../view_model/statistics_view_model.dart';

/// The statistics screen that displays activity statistics and charts.
class StatisticsScreen extends HookConsumerWidget {
  final Activity? selectedActivity;

  const StatisticsScreen({super.key, this.selectedActivity});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(statisticsViewModelProvider);
    final provider = ref.watch(statisticsViewModelProvider.notifier);

    // Update selected activity when this screen is built
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (selectedActivity != state.selectedActivity) {
        provider.setSelectedActivity(selectedActivity);
      }
    });

    return Scaffold(
      body: SafeArea(
        child: state.isLoading
            ? const Center(child: CircularProgressIndicator())
            : SingleChildScrollView(
                padding: const EdgeInsets.all(16.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const SizedBox(height: 20),
                    if (selectedActivity != null) ...[
                      // Time and Duration Cards (Top Priority)
                      Card(
                        elevation: 4,
                        child: Padding(
                          padding: const EdgeInsets.all(16.0),
                          child: Row(
                            mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                            children: [
                              // Before showing, ensure start <= end. If not, swap them.
                              // This avoids negative durations while preserving chronological order.
                              Builder(
                                builder: (ctx) {
                                  DateTime start =
                                      selectedActivity!.startDatetime;
                                  DateTime end = selectedActivity!.endDatetime;
                                  if (end.isBefore(start)) {
                                    final tmp = start;
                                    start = end;
                                    end = tmp;
                                  }

                                  // Start Time Column
                                  return Column(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      Icon(
                                        Icons.access_time,
                                        size: 24,
                                        color: ColorUtils.main,
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        'Start Time',
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color:
                                              Theme.of(context).brightness ==
                                                  Brightness.dark
                                              ? Colors.white
                                              : Colors.black87,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        DateFormat(
                                          'HH:mm:ss\nMM/dd/yyyy',
                                        ).format(start),
                                        style: const TextStyle(
                                          fontSize: 10,
                                          color: Colors.grey,
                                          height: 1.2,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                    ],
                                  );
                                },
                              ),
                              // Vertical separator
                              Container(
                                height: 60,
                                width: 1,
                                color:
                                    Theme.of(context).brightness ==
                                        Brightness.dark
                                    ? Colors.white.withValues(alpha: 0.3)
                                    : Colors.grey.withValues(alpha: 0.3),
                              ),
                              // End Time Column (uses same swapped start/end from Builder above)
                              Builder(
                                builder: (ctx) {
                                  DateTime start =
                                      selectedActivity!.startDatetime;
                                  DateTime end = selectedActivity!.endDatetime;
                                  if (end.isBefore(start)) {
                                    final tmp = start;
                                    start = end;
                                    end = tmp;
                                  }

                                  return Column(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      Icon(
                                        Icons.access_time_filled,
                                        size: 24,
                                        color: ColorUtils.main,
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        'End Time',
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color:
                                              Theme.of(context).brightness ==
                                                  Brightness.dark
                                              ? Colors.white
                                              : Colors.black87,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        DateFormat(
                                          'HH:mm:ss\nMM/dd/yyyy',
                                        ).format(end),
                                        style: const TextStyle(
                                          fontSize: 10,
                                          color: Colors.grey,
                                          height: 1.2,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                    ],
                                  );
                                },
                              ),
                              // Vertical separator
                              Container(
                                height: 60,
                                width: 1,
                                color:
                                    Theme.of(context).brightness ==
                                        Brightness.dark
                                    ? Colors.white.withValues(alpha: 0.3)
                                    : Colors.grey.withValues(alpha: 0.3),
                              ),
                              // Duration Column (calculates from ordered start/end)
                              Builder(
                                builder: (ctx) {
                                  DateTime start =
                                      selectedActivity!.startDatetime;
                                  DateTime end = selectedActivity!.endDatetime;
                                  if (end.isBefore(start)) {
                                    final tmp = start;
                                    start = end;
                                    end = tmp;
                                  }

                                  final duration = end.difference(start);

                                  return Column(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      Icon(
                                        Icons.timer,
                                        size: 24,
                                        color: ColorUtils.main,
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        'Duration',
                                        style: TextStyle(
                                          fontSize: 12,
                                          fontWeight: FontWeight.bold,
                                          color:
                                              Theme.of(context).brightness ==
                                                  Brightness.dark
                                              ? Colors.white
                                              : Colors.black87,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                      const SizedBox(height: 4),
                                      Text(
                                        _formatDuration(duration),
                                        style: const TextStyle(
                                          fontSize: 10,
                                          color: Colors.grey,
                                          height: 1.2,
                                        ),
                                        textAlign: TextAlign.center,
                                      ),
                                    ],
                                  );
                                },
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 16),

                      // Statistics Cards
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _buildStatCard(
                            context,
                            'Speed',
                            'Max: ${provider.getMaxValue('speed').toStringAsFixed(1)} km/h\nAvg: ${provider.getAverageValue('speed').toStringAsFixed(1)} km/h',
                            Icons.speed,
                          ),
                          _buildStatCard(
                            context,
                            'Cadence',
                            'Max: ${provider.getMaxValue('cadence').toStringAsFixed(0)} rpm\nAvg: ${provider.getAverageValue('cadence').toStringAsFixed(0)} rpm',
                            Icons.pedal_bike,
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _buildStatCard(
                            context,
                            'Power',
                            'Max: ${provider.getMaxValue('power').toStringAsFixed(0)} W\nAvg: ${provider.getAverageValue('power').toStringAsFixed(0)} W',
                            Icons.flash_on,
                          ),
                          _buildStatCard(
                            context,
                            'Altitude',
                            'Max: ${provider.getMaxValue('altitude').toStringAsFixed(0)} m\nAvg: ${provider.getAverageValue('altitude').toStringAsFixed(0)} m',
                            Icons.terrain,
                          ),
                        ],
                      ),
                      const SizedBox(height: 16),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.spaceEvenly,
                        children: [
                          _buildStatCard(
                            context,
                            'Distance',
                            '${selectedActivity!.distance.toStringAsFixed(2)} km',
                            Icons.straighten,
                          ),
                          _buildStatCard(
                            context,
                            'Calories',
                            '${selectedActivity!.calories.toStringAsFixed(0)} kcal',
                            Icons.local_fire_department,
                          ),
                        ],
                      ),
                      const SizedBox(height: 30),

                      // Speed Chart
                      _buildChartCard(
                        context,
                        'Speed (km/h)',
                        'speed',
                        state.activityData,
                      ),
                      const SizedBox(height: 20),

                      // Cadence Chart
                      _buildChartCard(
                        context,
                        'Cadence (rpm)',
                        'cadence',
                        state.activityData,
                      ),
                      const SizedBox(height: 20),

                      // Power Chart
                      _buildChartCard(
                        context,
                        'Power (W)',
                        'power',
                        state.activityData,
                      ),
                      const SizedBox(height: 20),

                      // Altitude Chart
                      _buildChartCard(
                        context,
                        'Altitude (m)',
                        'altitude',
                        state.activityData,
                      ),
                    ] else ...[
                      // General statistics when no activity is selected
                      Container(
                        height: 200,
                        padding: const EdgeInsets.all(16),
                        decoration: BoxDecoration(
                          color: Theme.of(context).brightness == Brightness.dark
                              ? const Color(0xFF2A2A2A)
                              : ColorUtils.white,
                          borderRadius: BorderRadius.circular(12),
                          boxShadow: [
                            BoxShadow(
                              color: Colors.grey.withValues(alpha: 0.1),
                              spreadRadius: 1,
                              blurRadius: 5,
                              offset: const Offset(0, 2),
                            ),
                          ],
                        ),
                        child: const Center(
                          child: Text(
                            'Select an activity from the list to view detailed statistics',
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
      ),
    );
  }

  Widget _buildChartCard(
    BuildContext context,
    String title,
    String dataType,
    List<Map<String, dynamic>> data,
  ) {
    if (data.isEmpty) {
      // Show a chart with zero value when no data
      final spots = [FlSpot(0.0, 0.0)];
      final minX = 0.0;
      final maxX = 60.0; // 1 minute default
      final minY = 0.0;
      final maxY = 10.0; // Default max

      return Container(
        height: 280,
        padding: const EdgeInsets.symmetric(vertical: 16),
        decoration: BoxDecoration(
          color: Theme.of(context).brightness == Brightness.dark
              ? const Color(0xFF2A2A2A)
              : ColorUtils.white,
          borderRadius: BorderRadius.circular(12),
          boxShadow: [
            BoxShadow(
              color: Colors.grey.withValues(alpha: 0.1),
              spreadRadius: 1,
              blurRadius: 5,
              offset: const Offset(0, 2),
            ),
          ],
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.center,
          children: [
            Text(
              title,
              style: TextStyle(
                fontSize: 16,
                fontWeight: FontWeight.bold,
                color: Theme.of(context).brightness == Brightness.dark
                    ? Colors.white
                    : Colors.black87,
              ),
            ),
            const SizedBox(height: 16),
            Expanded(
              child: LineChart(
                LineChartData(
                  gridData: FlGridData(
                    show: true,
                    drawVerticalLine: true,
                    drawHorizontalLine: true,
                    horizontalInterval: null,
                    verticalInterval: null,
                    getDrawingHorizontalLine: (value) {
                      return FlLine(
                        color: Theme.of(context).brightness == Brightness.dark
                            ? Colors.white.withValues(alpha: 0.1)
                            : Colors.grey.withValues(alpha: 0.2),
                        strokeWidth: 1,
                      );
                    },
                    getDrawingVerticalLine: (value) {
                      return FlLine(
                        color: Theme.of(context).brightness == Brightness.dark
                            ? Colors.white.withValues(alpha: 0.1)
                            : Colors.grey.withValues(alpha: 0.2),
                        strokeWidth: 1,
                      );
                    },
                  ),
                  titlesData: FlTitlesData(
                    leftTitles: AxisTitles(
                      sideTitles: SideTitles(
                        showTitles: true,
                        reservedSize: 45,
                        getTitlesWidget: (value, meta) {
                          return Padding(
                            padding: const EdgeInsets.only(right: 8),
                            child: Text(
                              value.toStringAsFixed(1),
                              style: TextStyle(
                                fontSize: 10,
                                color:
                                    Theme.of(context).brightness ==
                                        Brightness.dark
                                    ? Colors.white70
                                    : Colors.black87,
                                fontWeight: FontWeight.w500,
                              ),
                              textAlign: TextAlign.right,
                              overflow: TextOverflow.visible,
                              maxLines: 1,
                            ),
                          );
                        },
                      ),
                    ),
                    bottomTitles: AxisTitles(
                      axisNameWidget: Text(
                        'seconds',
                        style: TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.bold,
                          color: Theme.of(context).brightness == Brightness.dark
                              ? Colors.white70
                              : Colors.black87,
                        ),
                      ),
                      axisNameSize: 20,
                      sideTitles: SideTitles(
                        showTitles: true,
                        reservedSize: 60,
                        getTitlesWidget: (value, meta) {
                          final seconds = value.toInt();
                          return RotatedBox(
                            quarterTurns: 1,
                            child: Text(
                              _formatDuration(Duration(seconds: seconds)),
                              style: const TextStyle(fontSize: 10),
                            ),
                          );
                        },
                      ),
                    ),
                    rightTitles: const AxisTitles(
                      sideTitles: SideTitles(showTitles: false),
                    ),
                    topTitles: const AxisTitles(
                      sideTitles: SideTitles(showTitles: false),
                    ),
                  ),
                  backgroundColor:
                      Theme.of(context).brightness == Brightness.dark
                      ? const Color(0xFF2A2A2A)
                      : Colors.white,
                  borderData: FlBorderData(show: true),
                  minX: minX,
                  maxX: maxX,
                  minY: minY,
                  maxY: maxY,
                  lineTouchData: LineTouchData(
                    enabled: true,
                    touchTooltipData: LineTouchTooltipData(
                      getTooltipItems: (touchedSpots) {
                        return touchedSpots.map((spot) {
                          return LineTooltipItem(
                            spot.y.toStringAsFixed(1),
                            const TextStyle(
                              color: Colors.white,
                              fontWeight: FontWeight.bold,
                              fontSize: 12,
                            ),
                          );
                        }).toList();
                      },
                    ),
                  ),
                  lineBarsData: [
                    LineChartBarData(
                      spots: spots,
                      isCurved: true,
                      curveSmoothness: 0.05,
                      color: Theme.of(context).brightness == Brightness.dark
                          ? Colors.grey.shade400
                          : ColorUtils.main,
                      barWidth: 3,
                      belowBarData: BarAreaData(
                        show: true,
                        color: Theme.of(context).brightness == Brightness.dark
                            ? Colors.grey.shade400.withValues(alpha: 0.1)
                            : ColorUtils.main.withValues(alpha: 0.1),
                      ),
                      dotData: FlDotData(show: false),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      );
    }

    final spots = data.map((entry) {
      final timestamp = (entry['timestamp'] as DateTime);
      final startTime = data.isNotEmpty
          ? (data.first['timestamp'] as DateTime)
          : timestamp;
      final secondsElapsed = timestamp
          .difference(startTime)
          .inSeconds
          .toDouble();
      final value = (entry[dataType] as num?)?.toDouble() ?? 0.0;
      return FlSpot(secondsElapsed, value);
    }).toList();

    // Find min and max seconds for proper scaling
    final timestamps = data
        .map((entry) => (entry['timestamp'] as DateTime))
        .toList();
    final startTime = timestamps.isNotEmpty ? timestamps.first : DateTime.now();
    final secondsList = timestamps
        .map(
          (timestamp) => timestamp.difference(startTime).inSeconds.toDouble(),
        )
        .toList();
    final minX = secondsList.isNotEmpty
        ? secondsList.reduce((a, b) => a < b ? a : b)
        : 0.0;
    final maxX = secondsList.isNotEmpty
        ? secondsList.reduce((a, b) => a > b ? a : b)
        : 1.0;

    // Calculate Y-axis range: start at 0, end at a nice grid value
    final values = data
        .map((entry) => (entry[dataType] as num?)?.toDouble() ?? 0.0)
        .toList();
    final maxValue = values.isNotEmpty
        ? values.reduce((a, b) => a > b ? a : b)
        : 0.0;
    final minY = 0.0;
    final maxY = _calculateNiceMax(maxValue);

    return Container(
      height: 280,
      padding: const EdgeInsets.symmetric(vertical: 16),
      decoration: BoxDecoration(
        color: Theme.of(context).brightness == Brightness.dark
            ? const Color(0xFF2A2A2A)
            : ColorUtils.white,
        borderRadius: BorderRadius.circular(12),
        boxShadow: [
          BoxShadow(
            color: Colors.grey.withValues(alpha: 0.1),
            spreadRadius: 1,
            blurRadius: 5,
            offset: const Offset(0, 2),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          Text(
            title,
            style: TextStyle(
              fontSize: 16,
              fontWeight: FontWeight.bold,
              color: Theme.of(context).brightness == Brightness.dark
                  ? Colors.white
                  : Colors.black87,
            ),
          ),
          const SizedBox(height: 16),
          Expanded(
            child: LineChart(
              LineChartData(
                gridData: FlGridData(
                  show: true,
                  drawVerticalLine: true,
                  drawHorizontalLine: true,
                  horizontalInterval: null, // Use default intervals
                  verticalInterval: null, // Use default intervals
                  getDrawingHorizontalLine: (value) {
                    return FlLine(
                      color: Theme.of(context).brightness == Brightness.dark
                          ? Colors.white.withValues(alpha: 0.1)
                          : Colors.grey.withValues(alpha: 0.2),
                      strokeWidth: 1,
                    );
                  },
                  getDrawingVerticalLine: (value) {
                    return FlLine(
                      color: Theme.of(context).brightness == Brightness.dark
                          ? Colors.white.withValues(alpha: 0.1)
                          : Colors.grey.withValues(alpha: 0.2),
                      strokeWidth: 1,
                    );
                  },
                ),
                titlesData: FlTitlesData(
                  leftTitles: AxisTitles(
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 45,
                      getTitlesWidget: (value, meta) {
                        return Padding(
                          padding: const EdgeInsets.only(
                            right: 8,
                          ), // Add padding between Y-axis labels and chart
                          child: Text(
                            value.toStringAsFixed(
                              1,
                            ), // Format to 1 decimal place
                            style: TextStyle(
                              fontSize: 10,
                              color:
                                  Theme.of(context).brightness ==
                                      Brightness.dark
                                  ? Colors.white70
                                  : Colors.black87,
                              fontWeight: FontWeight.w500,
                            ),
                            textAlign: TextAlign.right,
                            overflow:
                                TextOverflow.visible, // Prevent text wrapping
                            maxLines: 1,
                          ),
                        );
                      },
                    ),
                  ),
                  bottomTitles: AxisTitles(
                    axisNameWidget: Text(
                      'time',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: Theme.of(context).brightness == Brightness.dark
                            ? Colors.white70
                            : Colors.black87,
                      ),
                    ),
                    axisNameSize: 20,
                    sideTitles: SideTitles(
                      showTitles: true,
                      reservedSize: 60,
                      getTitlesWidget: (value, meta) {
                        final seconds = value.toInt();
                        return RotatedBox(
                          quarterTurns: 1, // Rotate 90 degrees
                          child: Text(
                            _formatDuration(Duration(seconds: seconds)),
                            style: const TextStyle(fontSize: 10),
                          ),
                        );
                      },
                    ),
                  ),
                  rightTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                  topTitles: const AxisTitles(
                    sideTitles: SideTitles(showTitles: false),
                  ),
                ),
                backgroundColor: Theme.of(context).brightness == Brightness.dark
                    ? const Color(0xFF2A2A2A)
                    : Colors.white,
                borderData: FlBorderData(show: true),
                minX: minX,
                maxX: maxX,
                minY: minY,
                maxY: maxY,
                lineTouchData: LineTouchData(
                  enabled: true,
                  touchTooltipData: LineTouchTooltipData(
                    getTooltipItems: (touchedSpots) {
                      return touchedSpots.map((spot) {
                        return LineTooltipItem(
                          spot.y.toStringAsFixed(1),
                          const TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.bold,
                            fontSize: 12,
                          ),
                        );
                      }).toList();
                    },
                  ),
                ),
                lineBarsData: [
                  LineChartBarData(
                    spots: spots,
                    isCurved: true,
                    curveSmoothness: 0.01,
                    color: Theme.of(context).brightness == Brightness.dark
                        ? Colors.grey.shade400
                        : ColorUtils.main,
                    barWidth: 3,
                    belowBarData: BarAreaData(
                      show: true,
                      color: Theme.of(context).brightness == Brightness.dark
                          ? Colors.grey.shade400.withValues(alpha: 0.1)
                          : ColorUtils.main.withValues(alpha: 0.1),
                    ),
                    dotData: FlDotData(show: false),
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStatCard(
    BuildContext context,
    String title,
    String value, [
    IconData? icon,
  ]) {
    return SizedBox(
      width: 140,
      height: 140,
      child: Card(
        elevation: 4,
        child: Padding(
          padding: const EdgeInsets.all(12.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              if (icon != null) ...[
                Icon(icon, size: 28, color: ColorUtils.main),
                const SizedBox(height: 4),
              ],
              Text(
                title,
                style: TextStyle(
                  fontSize: 14,
                  fontWeight: FontWeight.bold,
                  color: Theme.of(context).brightness == Brightness.dark
                      ? Colors.white
                      : Colors.black87,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 4),
              Text(
                value,
                style: const TextStyle(
                  fontSize: 12,
                  color: Colors.grey,
                  height: 1.3,
                ),
                textAlign: TextAlign.center,
                maxLines: 4,
                overflow: TextOverflow.ellipsis,
              ),
            ],
          ),
        ),
      ),
    );
  }

  /// Calculates a nice maximum value for the Y-axis that creates clean grid lines.
  /// Returns the next higher "nice" number (like 10, 20, 50, 100, etc.) that encompasses the maxValue.
  double _calculateNiceMax(double maxValue) {
    if (maxValue <= 0) return 10.0; // Minimum chart height

    // Find the magnitude (power of 10)
    final magnitude = (log(maxValue) / ln10).floor(); // log10
    final power = magnitude.toDouble();

    // Get the first digit
    final firstDigit = (maxValue / pow(10.0, power)).floor();

    // Calculate nice maximum based on first digit
    double niceMax;
    if (firstDigit <= 1) {
      niceMax = 2.0;
    } else if (firstDigit <= 2) {
      niceMax = 5.0;
    } else if (firstDigit <= 5) {
      niceMax = 10.0;
    } else {
      niceMax = 20.0;
    }

    // Scale back to original magnitude
    return niceMax * pow(10.0, power);
  }

  /// Formats a Duration into a readable string (HH:MM:SS).
  String _formatDuration(Duration duration) {
    // If negative, use absolute duration (handles cases where end < start)
    if (duration.isNegative) {
      duration = Duration(microseconds: duration.inMicroseconds.abs());
    }

    String twoDigits(int n) => n.toString().padLeft(2, '0');
    final hours = twoDigits(duration.inHours);
    final minutes = twoDigits(duration.inMinutes.remainder(60));
    final seconds = twoDigits(duration.inSeconds.remainder(60));
    return '$hours:$minutes:$seconds';
  }
}
