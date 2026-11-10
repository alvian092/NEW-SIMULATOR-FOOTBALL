# Football Quant Lab

Android-native football probability simulator with a Monte Carlo engine, private app-local CSV storage, and a Prediction Tracker.

## Build

The GitHub Actions workflow builds a debug APK with Java 17 and Gradle 8.9. Artifact: `Football-Quant-Lab-debug-apk`.

## Current foundation

- 10,000-run Monte Carlo simulation using independent Poisson goal models
- Home win, draw, away win, and under/over 4.5 goal estimates
- CSV import through Android's document picker; imported file is copied into app-private storage and remains available between launches
- Save a prediction before kickoff and record the actual score in Prediction Tracker
- Dark dashboard with mint-green accents, responsive stacked cards, and progress-style probability presentation

## Important model note

The current engine uses expected-goals (xG) inputs and independent Poisson sampling. Imported CSV files are persistently stored but are not yet used to train or calibrate the model. Model calibration, richer tracker evaluation metrics, and device-level QA are next steps. Probabilities are estimates, never guarantees.
