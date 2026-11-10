# Football Quant Lab

Android football probability simulator with a **CSV-first, offline architecture**. No API key, provider account, cloud backend, or manual server setup is required.

## Data flow

1. Import a CSV from the Android document picker.
2. The app copies it into private app storage as `matches.csv`; the dataset remains available after closing and reopening the app.
3. Choose the competition, home team, away team, and fixture date from the imported dataset.
4. The simulator reads each team's 20 most recent matches before the fixture date and derives expected goals from the CSV xG columns.
5. Save the prediction in the local Prediction Tracker, then enter the actual score after the match.

## Required CSV columns

`date,home_team,away_team,home_goals,away_goals,home_xg,away_xg`

Optional column: `competition`.

Use dates in ISO format (`YYYY-MM-DD`). Include numeric xG values for both teams. The app must not invent missing xG. Each selected team needs 20 prior matches with usable xG data for a prediction.

## Build

GitHub Actions builds the Android debug APK with Java 17 and Gradle 8.9. Workflow artifact: `Football-Quant-Lab-debug-apk`.

## Model limitations

The current prediction engine uses independent Poisson goal sampling with 10,000 iterations. It is a probability estimate, not a guarantee. CSV import and local storage do not require an internet connection after the file has been selected.
