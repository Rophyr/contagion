# Contagion: a hardcore disease mod (Fabric, Minecraft 26.3)

## Quick start (no spoilers)

1. Install **Fabric Loader** for **26.3** plus **Fabric API**, and drop the Contagion jar into `mods`
   (in multiplayer: the server **and** every player).
2. Someone runs **`/virus outbreak`**: one random player is secretly infected. Nobody is told who.
   Or use `/virus infect <player>`.
3. Craft a **Research Journal** (book + paper + coal/charcoal) and open it. It has a short **lab manual**
   and keeps track of everything you learn. Recipes for the other tools show up in your recipe book.
4. Survive.

Languages: **English** and **French** (Français). Each player sees the mod in their own game language,
including the Research Journal.

That's all you need to know. Everything below spoils how the disease works.

---

<details>
<summary><b>Spoilers: how it works</b></summary>

### Timeline
- **Incubation (1 Minecraft day):** no symptoms. In the second half you're already contagious and run a slight temperature.
- **Days 0-21:** mild, then moderate, then severe symptoms. Coughing, fever, chills, fatigue, vomiting, dizziness and more. You lose max hearts as it gets worse.
- **After day 21:** terminal. One more max heart lost about every third of a day until it kills you (about 2 days).

Each world's strain has a profile (respiratory, neurological or hemorrhagic) that decides which symptoms dominate.
Neurological strains make you *hear things*. Sleeping through the night counts as time passing.

### Spreading
Air (being near someone contagious), coughs, hits, and transfusing infected blood. Villagers catch it too.
A **Face Mask** (wear it on your head) cuts airborne spread a lot.

### The cure
A serum of **3 specific ingredients**, different in every world. Each one is a reagent (24 vanilla items) **in the
right preparation**: raw, ground (Mortar and Pestle) or boiled into an extract (Beaker over heat). Some reagents are **toxic**.
- **Microscope:** analyze blood (are they infected? how badly?) and sometimes learn a fact: which category an active agent
  belongs to, how it must be prepared, which reagents are toxic or useless. A donor can only reveal one new fact per day.
  Samples keep 3 days, and a donor's veins need a few hours to recover between draws.
- **Petri Dish:** blood on a dish grows a culture in half a day (if there's enough pathogen). Cultures die after 2 days.
- **Culture tests:** a sealed serum on a grown culture (Microscope) tells you how many ingredients are exactly right,
  hints when a right reagent is in the wrong preparation, and shows whether it harms tissue. Safe, but slow.
- **Injecting:** all 3 right cures you for good. 1-2 give temporary remission (weaker each time). Toxic reagents hurt,
  and enough of them can kill.
- Blood from a cured person carries antibodies. Infected blood can be transfused into a villager to keep a test subject.

### Tools
| Item | Recipe |
|---|---|
| Syringe x4 | iron nugget (top right), glass pane (middle), iron ingot (bottom left) |
| Petri Dish x3 | dried kelp (top middle), 3 glass panes below |
| Mortar and Pestle | bowl + flint + cobblestone (shapeless) |
| Beaker | glass in a U: `G G` / `GGG` |
| Empty Vial x3 | iron nugget (top middle), glass panes in a U below it |
| Thermometer | glass pane (top right), redstone (middle), iron nugget (bottom left) |
| Microscope | glass on top, iron + copper + iron in the middle, 3 iron at the bottom |
| Face Mask | string, _, string / 3 wool |
| Research Journal | book + paper + coal or charcoal |

### Commands
- Anyone (configurable): `/virus infect <targets>`, `/virus outbreak`
- Operators (spoilers!): `/virus status`, `/virus cure`, `/virus immunize`, `/virus setday <targets> <day>`, `/virus strain`, `/virus newstrain`

### Config: `config/contagion.properties`
Incubation length, days until terminal, contagiousness, villager infection, microscope/culture/beaker times, blood draw cooldown, and who can use the commands.

</details>

## Building the jar

Needs **JDK 25**: run `./gradlew build`, then the jar is in `build/libs/`.

No Java installed? Upload this folder to a new GitHub repository. The included workflow builds it automatically.
Download the jar from the run's **Artifacts** on the repository's *Actions* tab.
