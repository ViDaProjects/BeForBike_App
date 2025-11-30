import 'package:flutter/material.dart';
import 'package:hooks_riverpod/hooks_riverpod.dart';
import 'package:intl/intl.dart';

/// A widget that displays a formatted date.
class Date extends HookConsumerWidget {
  /// The date to display.
  final DateTime date;

  /// Creates a [Date] widget.
  ///
  /// The [date] is the date to display.
  const Date({super.key, required this.date});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final formattedDate = DateFormat('MM/dd/yyyy').format(date);

    final formattedTime = DateFormat('HH:mm').format(date);

    return Text('On $formattedDate at $formattedTime');
  }
}
