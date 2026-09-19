# Changes from Olauncher

MinLauncher is an independent, unofficial fork of
[Olauncher](https://github.com/tanujnotes/Olauncher), forked at upstream commit
[`1d438f8`](https://github.com/tanujnotes/Olauncher/commit/1d438f8)
(`v6.9.1`, version code 112). It is not affiliated with, endorsed by, or supported
by the original author. Report issues for this fork to this repository, not upstream.

## Identity and platform

- Uses the name **MinLauncher** and application ID `io.github.ouj4k2q5.minlauncher`.
- Restarts release versioning at `1.0.0` instead of continuing upstream version code 112.
- Requires Android 11 (API 30) or later, allowing obsolete version-specific code paths
  and legacy device-admin screen locking to be removed.

## Product and privacy changes

- Removes promotional UI, timed review/share/wallpaper prompts, seasonal dialogs, and
  outbound promotional or support links.
- Removes the remote daily-photo wallpaper, WorkManager dependency, and `INTERNET`
  permission. The existing local solid-colour wallpaper remains.
- Removes unused dependencies and resources, and explicitly declares the coroutine and
  lifecycle dependencies used by the app.
- Removes the ability to long-press the clock, date, or screen-time text and assign
  any installed app to it. Tapping them now always opens a fixed destination: the
  installed clock app (falling back to the system alarm intent), the calendar, or
  Digital Wellbeing, matching stock Android home-screen conventions.

For current permissions and their purposes, see [Privacy and permissions](privacy.md).
