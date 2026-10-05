# Decisions checklist

The design choices still marked **unconfirmed** in `design.md` and `ui-spec.md`, gathered
on 2026-10-05 so they can be settled in one pass. Each item says what it is, where it
lives, and offers a few options. **(recommended)** marks my suggestion.

**How to fill it in:** tick one box per item (`[x]`). Write anything else under *Notes*,
including "other: …" if none of the options fits. Leave an item unticked to keep it open.
Once it comes back, I'll write every outcome into the docs and the code, and remove the
settled items from this file.

The items are ordered by impact. Section A sets the game's pace and Section B its economy.
The later sections have smaller, mostly cosmetic choices, and several of them can be settled
with "keep what's built" in one tick.

---

## A. Game pace and the race

### A1. Game start date
Turn 1 is Winter 1955, one season per turn, as in BASPM (`design.md` §5 M5 "Game info").
Real Sputnik flew in fall 1957, so players have about 11 turns of setup before the
historical first satellite.
- [ ] Keep Winter 1955 **(recommended)**: time to hire and research before the race starts.
- [ ] Start in Winter 1957: a tighter, faster start.
- [ ] Start in 1956: in between.

*Notes:*

### A2. When the race is won (the default declared goals)
A game is won by the first agency to reach all of the declared goals
(`GameMeta.DEFAULT_DECLARED_GOALS`). Right now they're satellite in orbit, human in space and
human in orbit, which ends the race at the first crewed orbital flight, around turn 24, about
six real weeks at one turn a day. A host can change the list per game in `game-meta.json`.
- [ ] Keep the three for the first play test, and decide again before the colleagues' game
  **(recommended)**.
- [ ] Add "A day in space" (duration level 1): roughly two to three more weeks.
- [ ] Add "A day in space" and "Two crews together" (joint launch): the longest of the three.

*Notes:*

### A3. Ties in the final placement
Players who finish on the same turn share a place, and the next place skips (1, 1, 3), which
is standard competition ranking (`design.md` §2.9). The medal table only breaks a tie for
first.
- [ ] Keep standard competition ranking **(recommended)**.
- [ ] Dense ranking (1, 1, 2).

*Notes:*

---

## B. Economy

### B1. Prestige-to-funds conversion rate (debt emergency)
A player in debt can convert prestige into funds (`PrestigeConversion`). It's meant as an
emergency tool that is always worse than letting the prestige count at the next review.
Today's rate, 4 prestige per fund, was my own number and never checked against the reviews.

**What a point is worth at a review** (`BudgetSchedule.baspmClose`): each review sets the
funding rate linearly from prestige within its bracket, and that rate pays for 16 seasons.
Prestige isn't spent at a review, so the same point counts again at every later one.

| Review | Game year | Prestige range | Over 16 seasons, per point |
|---|---|---|---|
| 1 | 1959 | 250-2,500 | about 30 funds |
| 2 | 1963 | 2,000-12,500 | about 25 |
| 3 | 1967 | 5,000-35,000 | about 16 |
| 4 | 1971 | 7,500-70,000 | about 10 |
| 5-10 | 1975-1995 | up to 170,000 | about 6, falling to 3 |

Today's conversion pays 0.25 funds per point: 120 times less than the first review, and the
point is gone, lowering every later review too. Clearing a 1,000-fund debt costs 4,000
prestige, more than an agency earns in its first years (Sputnik 1 plus its goals bring about
1,400). In practice the tool can't be used.
- [ ] A flat 1 prestige = 2 funds **(recommended)**: simple, still far worse than a review
  early, and a 1,000-fund debt costs 500 prestige.
- [ ] Tied to the next review: a tenth of what the point would earn there (about 3 funds
  early, 0.3 late). Always proportionally worse, but harder to explain on screen.
- [ ] Keep 4 prestige per fund: debt is close to unrecoverable before the next review.
- [ ] Another flat rate (in the notes).

*Notes:*

### B2. Budget numbers taken from BASPM
Starting funds 25,000, prestige 250, funding 3,000 per season, BASPM's review table, salaries
of 25–60, and component maintenance all follow BASPM's values, at your instruction of
2026-10-03. `design.md` §1.2 normally asks for our own numbers, and this is flagged as an
exception.
- [ ] Keep BASPM's values, and tune from play tests **(recommended)**. Numbers aren't
  creative text or art, and they're what make the game feel right.
- [ ] Re-derive our own numbers before the repo goes public.
- [ ] Re-derive our own numbers now.

*Notes:*

### B3. After the tenth funding review
BASPM's review table has ten brackets. After the tenth review, ours repeats the last bracket.
- [ ] Keep repeating the last bracket **(recommended)**: games rarely last that long (reviews
  come every 16 seasons, so the tenth is around game year 40).
- [ ] Keep growing the brackets in the same steps.

*Notes:*

### B4. Hiring cost
Hiring is free; a new employee only costs their salary every season.
- [ ] Keep free hiring **(recommended)**: salary is already a running cost.
- [ ] Charge a one-time hiring fee (for example one season's salary).

*Notes:*

### B5. Department salary changes per season
"Manage department" raises or cuts a whole department's salaries, which raises or lowers its
morale (G8). As in BASPM, there's no limit on how often.
- [ ] Keep no limit **(recommended)**: every raise costs salary for good, which limits it.
- [ ] Once per department per season.

*Notes:*

### B6. Repeat-flight prestige
A configuration pays 100 % of its prestige on the first flight, 50 % on the second and 30 %
after that (BASPM's ratios).
- [ ] Keep 100 / 50 / 30 **(recommended)**.
- [ ] 100 / 60 / 40: repeats pay a little more.

*Notes:*

### B7. Client-side budget numbers
The Public Affairs Office computes its review forecast and conversion limits with the
standard budget table, because the server doesn't send the game's own. That's correct for
every game today, but a host who changes the table would see wrong numbers.
- [ ] Send the game's budget table with the game info, before any game uses a different
  one **(recommended)**.
- [ ] Leave it until someone needs a custom table.

*Notes:*

---

## C. Personnel

### C1. Age trade-off strength
Older candidates start with up to about 20 points more skill (centred, so the average hire is
unchanged), younger ones learn faster (45–80 learning potential for a rookie, 10–35 for a
veteran). No candidate starts above 95. Rookies catch up after about 5 years
(`design.md` §5 M5, G5).
- [ ] Keep these numbers and judge in play **(recommended)**.
- [ ] Stronger veterans (a 25-point range): the head start matters more.
- [ ] Milder (a 15-point range): age matters less.

*Notes:*

### C2. New hires' first training
A new hire starts Basic training at once, on a random skill of their group. *(Update
2026-10-05: basic training is now BASPM's mandatory, uninterruptible wait, see C4, and you chose
to keep its skill gain. Only the question below, which skill it trains, is still open.)*
- [ ] Train their weakest skill instead **(recommended)**: more sensible, and it changes no
  balance.
- [ ] Keep a random skill.
- [ ] Let the player pick the skill when hiring (needs a UI change).

*Notes:*

### C3. Morale and retirement numbers
Idle staff lose 2 morale per season, busy staff gain 3. Anyone at or below 35 resigns.
Retirement at 50 (astronauts), 60 (flight controllers) and 65 (SET). These are our own
values, re-derived from BASPM's shape.
- [ ] Keep them and judge in play **(recommended)**.
- [ ] Change something (say what in the notes).

*Notes:*

### C4. Training lengths — settled 2026-10-05
**Settled in the play-test feedback round:** BASPM's lengths, basic 4 (astronauts) / 2 (SET) /
2 (flight controllers) and advanced 3. Basic training is now mandatory after hiring and can't be
cut short or chosen in the dialog. Nothing to tick; it can go when the file comes back.

### C5. Hiring pool size
Each department sees 6 new candidates every season (BASPM shows 10).
- [ ] Keep 6 **(recommended)**: about one real choice per season, and portraits repeat less.
- [ ] 8.
- [ ] 10, as in BASPM.

*Notes:*

### C6. Lithuanian women's surnames
Every Lithuanian woman gets the married surname form (-ienė). A young unmarried woman would
really use -aitė or -ytė.
- [ ] Keep the married form for everyone **(recommended)**: simple, and correct for most
  adults of the time.
- [ ] Use the unmarried form under 30.

*Notes:*

### C7. Expert rescue details
Expert controllers and astronauts can rescue a failing step. The details:
- An Expert on another flight helps at 75 % if they're a controller (still on the ground),
  and not at all if they're an astronaut (in space).
- A rescue can't push the saving roll above 95 %.
- Every consultation counts toward an Expert's tiredness, even an unsuccessful one.
- [ ] Keep all three **(recommended)**.
- [ ] Change something (say what in the notes).

*Notes:*

### C8. CAPCOM's skill growth
The astronaut serving as CAPCOM grows the skills they used that season, as a controller does.
- [ ] Keep **(recommended)**.
- [ ] CAPCOM duty grows no skills.

*Notes:*

---

## D. Missions and rules

### D1. Opening a program charges for its components
Opening a program also pays the creation cost of every component it grants that the agency
doesn't own yet, as BASPM does. The price card shows the full sum.
- [ ] Keep **(recommended)**.
- [ ] Charge only the opening cost.

*Notes:*

### D2. Mission odds tuning
The step-resolution numbers are our own placeholders, re-derived from BASPM's shape: crew skill
counts from component reliability 65 up, crew weight 20 %, and the size of the failure bands.
The outcomes in the simulations look sensible.
- [ ] Keep them until play tests say otherwise **(recommended)**.
- [ ] Ask me to tune for harder or easier missions (say which in the notes).

*Notes:*

### D3. Random events
Each season rolls once for a bad event (5 %) and once for a good one (20 %), as in BASPM,
with our own triggering conditions where Launch Window has no BASPM equivalent.
- [ ] Keep **(recommended)**.
- [ ] Fewer bad events (for example 3 %).

*Notes:*

### D4. Auto-assign order
Auto-assign fills the crew seats before the control slots, so CAPCOM never takes the best
pilot. Ties go to roster order.
- [ ] Keep **(recommended)**.

*Notes:*

---

## E. The memorial and history

### E1. Repeat Sputnik 2 flights
Laika is remembered on the first Sputnik 2 flight. Later flights add "An unnamed dog" each.
- [ ] Keep "An unnamed dog" **(recommended)**.
- [ ] Let Sputnik 2 fly only once, as in history.
- [ ] Other wording (in the notes).

*Notes:*

### E2. Korabl-Sputnik 2's dogs
Belka and Strelka survived in real life, so nothing marks them today. If a player's flight
fails, the game could remember the dogs aboard. I'd use the names of the two dogs really lost
on the failed July 1960 attempt (Chaika and Lisichka, to be verified first), not Belka and
Strelka.
- [ ] Leave it for now **(recommended)**.
- [ ] Remember the dogs on a failed flight, with Chaika and Lisichka's names.

*Notes:*

### E3. A turn report line for a lost passenger
The turn report doesn't mention a passenger lost that season ("Laika did not come home.").
It needs a new report event.
- [ ] Add it **(recommended)**: the memorial alone is easy to miss.
- [ ] Not needed.

*Notes:*

---

## F. Screens: choices made while building them

For each screen, the full list is in that section's "Implemented" note in `ui-spec.md`. Tick
"keep" to accept the whole list, or name the exceptions.

### F1. Join Game (`ui-spec.md` §4.2)
The heading "Found your agency", the hint text ("Everyone starts from the same line…"), light
text on the blue page, "Pick an agency style." when no style is picked, and where each refusal
appears. These have been waiting for your OK since 2026-10-04.
- [ ] Keep everything as built **(recommended)**.
- [ ] Keep, with exceptions (in the notes).

*Notes:*

### F2. Turn report (`ui-spec.md` §4.4)
- An unread report opens when you enter the game; a turn resolving while you play shows a
  toast and a dot instead.
- "Read" is remembered per device, not on the server.
- One scrolling screen; running costs by kind; hires not listed.
- Morale warnings for idle people at morale 50 or below.
- [ ] Keep everything as built **(recommended)**.
- [ ] Keep, with exceptions (in the notes).

*Notes:*

### F3. Headquarters (`ui-spec.md` §4.6)
- No separate "Closed, components kept" state: a closed program shows as Available, and its
  price shows the owned parts as free.
- No "First" ribbon on goal badges.
- Program cards show the full price to open.
- [ ] Keep everything as built **(recommended)**.
- [ ] Keep, with exceptions (in the notes).

*Notes:*

### F4. Mission Control (`ui-spec.md` §4.7)
- Re-staffing needs no confirmation (it's free and reversible).
- A released console just says "Released".
- The re-staff picker lists only idle people, with no "empty this slot".
- The Expert mark is a gold star icon.
- [ ] Keep everything as built **(recommended)**.
- [ ] Keep, with exceptions (in the notes).

*Notes:*

### F5. Replay viewer (`ui-spec.md` §4.7)
It shows the chances and pass or fail, not the dice numbers, and steps every 1.5 seconds.
- [ ] Keep **(recommended)**.
- [ ] Show the dice numbers too.
- [ ] A different speed (in the notes).

*Notes:*

### F6. Astronaut Complex (`ui-spec.md` §4.9)
- Expert training is offered to flight controllers too, through the shared training dialog.
- "Near retirement" means within 5 years of the retirement age.
- [ ] Keep **(recommended)**.
- [ ] Keep, with exceptions (in the notes).

*Notes:*

### F7. Public Affairs Office (`ui-spec.md` §4.10)
- Three columns: funds and costs; prestige and the next review; effects and news.
- The next review as a scale with two marks (you, and what keeps today's funding).
- Converting prestige is a dialog with a slider.
- The news lists random events only; reviews have their own card.
- The footer shows no funds change per season yet.
- [ ] Keep everything as built **(recommended)**.
- [ ] Keep, with exceptions (in the notes).

*Notes:*

### F8. Museum (`ui-spec.md` §4.11)
Tabs in the header; the race as a table next to "This game is won by"; achievements in two
columns; one card per flown configuration; one plaque per loss, now with portraits and
animals.
- [ ] Keep **(recommended)**.
- [ ] Keep, with exceptions (in the notes).

*Notes:*

---

## G. Technical defaults

These hardly affect play.

### G1. Server data folder
The server keeps its data in a plain `data/` folder next to it, unless a path is given.
- [ ] Keep **(recommended)**.

*Notes:*

### G2. When the client's content pack differs from the game's
The client still opens the game but warns, and uses its own copy. The plan (ui-spec G1) is to
download the game's pack instead.
- [ ] Build the download before inviting other players **(recommended)**: once you change
  content between games, players with older clients would otherwise see wrong numbers.
- [ ] Later.

*Notes:*

---

## H. Ideas waiting for a yes, a later, or a no

### H1. PR duty for astronauts
An idle astronaut could do public-relations work, earning a little prestige each season based
on Leadership (`design.md` §2.2). Real astronauts did this.
- [ ] Build it soon.
- [ ] Later **(recommended)**: worth a design pass of its own.
- [ ] Drop it.

*Notes:*

### H2. A real space-race story in each turn report
One real event from the same three months of the real space race in each report's news
(`design.md` §1.1, your idea of 2026-10-04).
- [ ] Build it soon **(recommended)**: cheap, and it suits the history notes.
- [ ] Later.
- [ ] Drop it.

*Notes:*

### H3. Honour the animals that came home
The Museum's Flights tab could honour Belka and Strelka, Ham, Enos and others (`design.md`
§5 M5, your idea of 2026-10-05).
- [ ] Build it soon.
- [ ] Later **(recommended)**: after the play test, together with E2.
- [ ] Drop it.

*Notes:*

### H4. Credits in Settings
Show `assets/CREDITS.md` (fonts, Laika's photo) in the Settings dialog's About section, so the
credit is visible in the game, not only in the files.
- [ ] Build it soon **(recommended)**: small, and the right thing to do for CC BY.
- [ ] Later.

*Notes:*
