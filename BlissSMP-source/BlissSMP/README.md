# BlissSMP (Paper 1.21, Java 21)

A community recreation plugin that implements Season 3-style **Gems**, **Dragon Egg Half Cooldown**, and simple **Progs (1–4)** gating.

## Build (GitHub Actions - easiest)
1. Upload this whole folder to a new GitHub repo.
2. Ensure `.github/workflows/build.yml` exists (included here).
3. Go to **Actions → Build Plugin → Run workflow**.
4. Download the artifact → you'll get `BlissSMP.jar`.

## Build (local)
```bash
# Requires Java 21 + Maven
mvn -DskipTests package
# Output: target/BlissSMP.jar
```

## Install
- Drop `BlissSMP.jar` into your Paper 1.21 server's `plugins/` folder.
- Restart the server.
- (Optional) Install the matching resource pack separately.

## Commands
- `/bliss help`
- `/bliss gem list`
- `/bliss gem give <player> <ASTRA|FIRE|FLUX|LIFE|PUFF|SPEED|STRENGTH|WEALTH>`
- `/bliss prog <1|2|3|4>`

## Notes
- **Cooldowns** are halved if the player has a **Dragon Egg** in their inventory.
- **Progs** gate Nether/End portal travel: Prog1 = Overworld, Prog2 = Nether enabled, Prog3+ = End enabled.
- Configurable values live in `config.yml` after first run.
